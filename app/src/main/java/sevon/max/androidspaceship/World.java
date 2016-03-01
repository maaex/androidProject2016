package sevon.max.androidspaceship;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by Max on 2016-01-18.
 * Class handling the game world.
 * Loading in new cells of the world is done as the player moves, this is to conserve RAM while
 * also allowing the world to be infinitely large.
 */
public class World extends View {

    public static final int GAME_WIDTH = 360;
    public static final int GAME_HEIGHT = 640;
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;
    public static float SCALE_FACTOR_X;
    public static float SCALE_FACTOR_Y;

    private ArrayList<GameEventListener> eventListeners = new ArrayList<>(1);

    Bitmap spaceShipBitmap;
    private Spaceship spaceship;
    private static final Vector2 STARTING_POSITION = new Vector2(WorldCell.WIDTH / 2, WorldCell.HEIGHT / 2);
    private Paint clearPaint = new Paint();       // Paint used when clearing the screen.
    private Paint textPaint = new Paint();       // Paint used to draw text.

    private static TypedArray BACKGROUND_DRAWABLES;
    private LinkedList<WorldCell> loadedCells = new LinkedList<>();
    private int currentCellNumber;  // used to determine whether a new cell should be loaded.
    private long timeAtStart;       // used for rng seed
    private Random rng = new Random();

    private int scoreTextPositionX = 10;
    private int scoreTextPositionY = 10;
    private int speedTextPositionX = 10;
    private int speedTextPositionY = 10;

    public void registerListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    public World(Context context) {
        super(context);
        init();
    }

    public World(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Does some necessary initialization.
     */
    public void init() {
        SCREEN_WIDTH = getWidth();
        SCREEN_HEIGHT = getHeight();
        SCALE_FACTOR_X = SCREEN_WIDTH / GAME_WIDTH;
        SCALE_FACTOR_Y = SCREEN_HEIGHT / GAME_HEIGHT;

        // Load array of background images.
        BACKGROUND_DRAWABLES = getResources().obtainTypedArray(R.array.background_drawables);

        // Load spaceship bitmap.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        spaceShipBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ship, options);

        clearPaint.setColor(Color.BLACK);
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(40);
        timeAtStart = System.currentTimeMillis();
    }

    public void restart() {
        // Place spaceship at the center of the first cell.
        spaceship = new Spaceship(spaceShipBitmap, STARTING_POSITION);

        loadedCells = new LinkedList<>();
        loadedCells.addLast(createWorldCell(new Vector2(0, 0), 0));
        loadedCells.addLast(createWorldCell(new Vector2(0, -1024), 1));

        // Load first 2 cells
        loadedCells.get(0).load();
        loadedCells.get(1).load();
        currentCellNumber = 0;

    }

    /**
     * Updates the game world and moves the spaceship according to the provided values.
     * @param accelerationX The x-component of the spaceship's move direction.
     * @param accelerationY The y-component of the spaceship's move direction.
     * @return Returns true if the game should continue. False if the game should restart (this
     * happens when the spaceship has crashed).
     */
    public boolean update(float accelerationX, float accelerationY) {
        Vector2 moveDirection = Vector2.normalize(new Vector2(accelerationX, accelerationY));
        spaceship.move(moveDirection);
        spaceship.updateScore(STARTING_POSITION);

        // Check if spaceship has moved on to another cell.
        if(currentCellNumber != getCurrentCell().getCellNumber()) {
            // It has. We should unload one cell and load a new one.
            // In order to determine which we need to check if the new current cell is ahead or
            // after the previous one.
            int newCellNumber = getCurrentCell().getCellNumber();
            if(currentCellNumber < newCellNumber) {
                // We have moved one cell forward. Unload cell two steps back if there is one.
                if(newCellNumber >= 2) {
                    loadedCells.get(0).unload();
                    loadedCells.remove(0);
                }
                // Start loading new cell.
                WorldCell nextCell = createWorldCell(new Vector2(0, loadedCells.getLast().getWorldPosition().getY() - WorldCell.HEIGHT), newCellNumber + 1);
                loadedCells.addLast(nextCell);
                nextCell.loadAsync();
            } else {
                // Start loading new cell.
                WorldCell nextCell = createWorldCell(new Vector2(0, loadedCells.getFirst().getWorldPosition().getY() + WorldCell.HEIGHT), newCellNumber - 1);
                loadedCells.addFirst(nextCell);
                nextCell.loadAsync();
            }
            // Update index of current cell.
            currentCellNumber = newCellNumber;

            // If the cell is a multiple of 3 we increase the spaceships speed by a little bit.
            if(currentCellNumber % 3 == 0)
                spaceship.setSpeed(spaceship.getSpeed() + 0.5f);

            // Finally, if the cell's bitmap hasn't been loaded yet we should wait until it has.
            while(!getCurrentCell().isBitmapLoaded()) { }
        }

        // Check if spaceship has crashed.
        if(spaceship.checkCollision(this)) {
            // It has... Game over!
            spaceship.setSpeed(0);
            notifyListeners(Event.SPACESHIP_CRASH);
            // wait a bit for listeners to do their thing.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            updateHighScores();
            return false;
        }

        return true;
    }

    private void updateHighScores() {
        ScoreList highScores = ScoreList.load(getContext());
        highScores.add(spaceship.getScore());
        try {
            highScores.save(getContext());
        } catch (IOException e) {
            System.out.println("Unable to save high scores...");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Clear screen.
        canvas.drawRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, clearPaint);

        if(spaceship != null) {
            // Get camera view.
            Rect viewRect = buildViewRect();

            // Draw world as seen from camera.
            for(WorldCell c : loadedCells)
                c.draw(canvas, viewRect);

            // Draw spaceship.
            if(spaceship != null)
                spaceship.draw(canvas);

            // Draw score text.
            canvas.drawText("Score: " + spaceship.getScore(), scoreTextPositionX, scoreTextPositionY, textPaint);

            // Draw speed text.
            canvas.drawText("Speed: " + spaceship.getSpeed(), speedTextPositionX, speedTextPositionY, textPaint);
        }
    }

    /**
     * @return Returns a view rectangle, centered over the spaceship. The coordinates of this
     * rectangle represents the part of the world that should be drawn to screen.
     */
    private Rect buildViewRect() {

        int left = (int) spaceship.getWorldPosition().getX() - GAME_WIDTH / 2;
        int right = (int) spaceship.getWorldPosition().getX() + GAME_WIDTH / 2;
        int top = (int) spaceship.getWorldPosition().getY() - GAME_HEIGHT / 2;
        int bottom = (int) spaceship.getWorldPosition().getY() + GAME_HEIGHT / 2;

        return new Rect(left, top, right, bottom);
    }

    /**
     * Checks for a collision at the specified world coordinate.
     * @param position The position to check for collisions at. In world coordinates.
     * @return True if a collision was found.
     */
    public boolean checkCollision(Vector2 position) {
        // First translate to cell coordinates.
        WorldCell currentCell = getCurrentCell();
        if(currentCell != null) {
            int x = (int) (position.getX() - currentCell.getWorldPosition().getX());
            int y = (int) (position.getY() - currentCell.getWorldPosition().getY());

            // A little cheat to prevent checking outside currentCell...
            // Some collisions might be missed on cell edges but they should be caught shortly after
            // when currentCell has been updated.
            if(x >= 0 && y >= 0 && x < WorldCell.WIDTH && y < WorldCell.HEIGHT)
                return currentCell.checkCollision(x, y);
        }

        return false;
    }

    /**
     * Creates a world cell. If cell number is 0 the start cell is generated, otherwise a random
     * world cell is created.
     * @param worldPosition The world worldPosition of the cell.
     * @param cellNumber The cell number of the cell.
     * @return The created world cell. NOTE: The bitmap is not loaded in this method but must be
     * loaded afterwards.
     */
    private WorldCell createWorldCell(Vector2 worldPosition, int cellNumber) {
        rng.setSeed(timeAtStart + cellNumber);
        int color = ColorGenerator.getRandomColor(rng.nextInt());
        int bitmapId = R.drawable.start;

        if(cellNumber != 0) {
            //bitmapId = LEVEL_LAYOUT[rng.nextInt(LEVEL_LAYOUT.length - 1) + 1];    // +1 to skip the "start" cell.
            bitmapId = BACKGROUND_DRAWABLES.getResourceId(rng.nextInt(BACKGROUND_DRAWABLES.length() - 1) + 1, 0);
        }

        return new WorldCell(worldPosition, bitmapId, cellNumber, color);
    }

    /**
     * @return Returns the cell the spaceship is currently on.
     */
    private WorldCell getCurrentCell() {
        Vector2 spaceshipPosition = spaceship.getWorldPosition();
        for(WorldCell c : loadedCells) {
            if(spaceshipPosition.getY() >= c.getWorldPosition().getY() && spaceshipPosition.getY() < (c.getWorldPosition().getY() + WorldCell.HEIGHT))
                return c;
        }

        return null;
    }

    /**
     * Notifies any registered listeners that an event has occurred.
     * @param event The occurred event.
     */
    private void notifyListeners(Event event) {
        for(int i = 0; i < eventListeners.size(); i++) {
            eventListeners.get(i).notify(event);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        SCREEN_WIDTH = w;
        SCREEN_HEIGHT = h;
        SCALE_FACTOR_X = (float) SCREEN_WIDTH / GAME_WIDTH;
        SCALE_FACTOR_Y = (float) SCREEN_HEIGHT / GAME_HEIGHT;

        // Pre-calculation of text position values so we don't have to do this each draw call.
        scoreTextPositionX = (int)(SCREEN_WIDTH * 0.10);
        scoreTextPositionY = (int)(SCREEN_HEIGHT * 0.10);
        speedTextPositionX = (int)(SCREEN_WIDTH * 0.70);
        speedTextPositionY = scoreTextPositionY;
    }

    /**
     * Prevents error messages in View Designer...
     * @return Always returns true.
     */
    @Override public boolean isInEditMode() { return true; }

    /**
     * Class representing a piece of the world, loaded into memory.
     */
    private class WorldCell {

        private static final int WIDTH = 512;
        private static final int HEIGHT = 1024;

        private int cellNumber;
        private int bitmapId;
        private Bitmap bitmap;
        private Vector2 worldPosition;
        private Rect bitmapBoundingRect;
        private Paint paint = new Paint();

        /**
         * Creates a new WorldCell object at the specified position.
         * NOTE: The bitmap is NOT loaded in this constructor! The load() method must be called
         * before trying to draw the cell.
         * @param worldPosition The cell's position in world coordinates.
         * @param bitmapId The id of the bitmap belonging to this cell.
         */
        public WorldCell(Vector2 worldPosition, int bitmapId) {
            this.worldPosition = worldPosition;
            this.bitmapId = bitmapId;

            // Create a bounding rectangle of bitmap file. This value is constant so we only need
            // to load it once. It is used when drawing.
            BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
            factoryOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), bitmapId, factoryOptions);
            bitmapBoundingRect = new Rect(0, 0, factoryOptions.outWidth, factoryOptions.outHeight);
        }

        public WorldCell(Vector2 worldPosition, int bitmapId, int cellNumber, int color) {
            this.worldPosition = worldPosition;
            this.bitmapId = bitmapId;
            this.cellNumber = cellNumber;

            // Set a random color for the graphics of the cell.
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));

            // Create a bounding rectangle of bitmap file. This value is constant so we only need
            // to load it once. It is used when drawing.
            BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
            factoryOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), bitmapId, factoryOptions);
            bitmapBoundingRect = new Rect(0, 0, factoryOptions.outWidth, factoryOptions.outHeight);
        }

        /**
         * Checks if there is a collision on the specified coordinates. This is determined by
         * checking the alpha value of the pixel on the specified coordinate on the cell's bitmap,
         * if alpha > 0 there is a collision.
         * @param x The x-coordinate to check at.
         * @param y The y-coordinate to check at.
         * @return True if a collision was found.
         */
        public boolean checkCollision(int x, int y) {
            // If pixel's alpha channel value is > 0 we have a collision.
            if(bitmap != null)
                return Color.alpha(bitmap.getPixel(x, y)) > 0;

            return false;
        }

        /**
         * Draws a portion of the world cell to a canvas.
         * @param canvas The canvas to draw on.
         * @param viewRect The view rectangle, in world coordinates.
         */
        public void draw(Canvas canvas, Rect viewRect) {
            if(bitmap != null) {
                // First translate to this cell's coordinate system.
                Rect translatedViewRect = new Rect(viewRect);     // we don't want to change the actual viewRect.
                translatedViewRect.left -= worldPosition.getX();
                translatedViewRect.right -= worldPosition.getX();
                translatedViewRect.top -= worldPosition.getY();
                translatedViewRect.bottom -= worldPosition.getY();

                // Check if the bitmap should be drawn at the top or bottom of the screen.
                // The bitmap should be drawn at the top of the screen if the top of the view rect
                // is on the bitmap.
                boolean drawTop = translatedViewRect.top >= bitmapBoundingRect.top;

                // Same thing as above but with left side.
                boolean drawLeft = translatedViewRect.left >= bitmapBoundingRect.left;

                // Get intersection of viewRect and bitmapBoundingRect.
                // This is what should be drawn.
                if (translatedViewRect.intersect(bitmapBoundingRect)) {
                    int scaledHeight = (int) (translatedViewRect.height() * SCALE_FACTOR_Y);  // used below
                    int scaledWidth = (int) (translatedViewRect.width() * SCALE_FACTOR_X);  // used below

                    // Create draw rectangle (the portion of the screen to draw to).
                    Rect drawRect = new Rect(0, 0, scaledWidth, scaledHeight);

                    if(!drawTop) {
                        drawRect.top = SCREEN_HEIGHT - scaledHeight - 1;
                        drawRect.bottom = SCREEN_HEIGHT;
                    }
                    if(!drawLeft) {
                        drawRect.left = SCREEN_WIDTH - scaledWidth - 1;
                        drawRect.right = SCREEN_WIDTH;
                    }

                    // Draw
                    canvas.drawBitmap(bitmap, translatedViewRect, drawRect, paint);
                }
            }
        }

        /**
         * Loads the bitmap that was specified by the constructor.
         */
        public void load() {
            BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
            factoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;

            // Load bitmap using optimizations defined in factoryOptions.
            bitmap = BitmapFactory.decodeResource(getResources(), bitmapId, factoryOptions);
        }

        /**
         * Asynchronously loads the bitmap that was specified by the constructor.
         */
        public void loadAsync() {
            AsyncTask t = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                    load();
                    return null;
                }
            };
            t.execute();
        }

        /**
         * Unloads the bitmap.
         */
        public void unload() {
            if(bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }

        /**
         * @return Returns the position of the cell's top left corner, in world coordinates.
         */
        public Vector2 getWorldPosition() { return worldPosition; }

        /**
         * @return Returns true if the WorldCell's bitmap is currently loaded.
         */
        public boolean isBitmapLoaded() { return bitmap != null; }

        /**
         * @return Returns the cell number of this WorldCell.
         */
        public int getCellNumber() { return cellNumber; }
    }

    private static class ColorGenerator {

        private static Random rng = new Random();

        public static int getRandomColor(long seed) {
            rng.setSeed(seed);
            int red, blue, green;
            red = rng.nextInt(50) + 20;
            blue = rng.nextInt(50) + 20;
            green = rng.nextInt(50) + 20;

            return Color.argb(255, red, blue, green);
        }
    }

    /**
     * Enum used for indicating a specific event has occurred in the game.
     */
    public enum Event {
        GAME_STARTED, SPACESHIP_CRASH
    }
}
