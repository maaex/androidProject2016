package sevon.max.androidspaceship;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

/**
 * Created by Max on 2016-01-18.
 * Class handling the spaceship.
 */
public class Spaceship extends Drawable {

    private Vector2 worldPosition;
    private Vector2 direction = new Vector2(0, -1);
    private float speed = 1;
    private Bitmap bitmap;

    public Spaceship(Bitmap bitmap, Vector2 position) {
        this.worldPosition = position;
        this.bitmap = bitmap;
    }

    public void move(Vector2 moveDirection) {
        direction = moveDirection;
        Vector2 moveDistance = Vector2.multiply(direction, speed);
        worldPosition = Vector2.add(worldPosition, moveDistance);
    }

    /**
     * Draws the spaceship to a canvas.
     * @param canvas The canvas to draw on.
     */
    @Override
    public void draw(Canvas canvas) {
        Matrix m = new Matrix();
        m.setScale(World.SCALE_FACTOR_X, World.SCALE_FACTOR_Y);
        m.postRotate(getRotation(), bitmap.getWidth() * World.SCALE_FACTOR_X / 2, bitmap.getHeight()  * World.SCALE_FACTOR_Y / 2);
        m.postTranslate(World.SCREEN_WIDTH / 2 - bitmap.getWidth() * World.SCALE_FACTOR_X / 2, World.SCREEN_HEIGHT / 2 - bitmap.getHeight() * World.SCALE_FACTOR_Y / 2);
        canvas.drawBitmap(bitmap, m, null);
    }

    public Vector2 getWorldPosition() { return worldPosition; }

    /**
     * @return Returns the rotation of the spaceship. Use with matrices.
     */
    private float getRotation() {
        // this is the spaceship's standard direction.
        Vector2 zeroRotationVector = new Vector2(0, -1);

        // calculate the angle between the zero rotation vector and the current bearing.
        float rotation = (float) Math.toDegrees(Vector2.getAngle(direction, zeroRotationVector));

        // if the bearing is towards the left, rotation should be negative (counter clockwise).
        if(direction.getX() < 0)
            rotation = -rotation;

        return rotation;
    }

    /**
     * Checks if the spaceship is currently colliding with something in the game world.
     * @param world The game world to check for collisions in.
     * @return True if a collision was found.
     */
    public boolean checkCollision(World world) {

        // Array of the spaceship's 4 collision points; (10,2) (11,2) (3,22) (18,22)
        float[] collisionPoints = { 10, 2, 11, 2, 3, 22, 18, 22};

        // Matrix to translate and rotate the points into world coordinates.
        Matrix m = new Matrix();
        m.setTranslate(-bitmap.getWidth() / 2, -bitmap.getHeight() / 2);   // the spaceship's world coordinate is in the middle of the ship.
        m.postRotate(getRotation());
        m.postTranslate(worldPosition.getX(), worldPosition.getY());

        // Apply matrix to collision points.
        m.mapPoints(collisionPoints);

        // Check for collision at all points. If one is found, return true.
        for(int i = 0; i < collisionPoints.length; i += 2) {
            if(world.checkCollision(new Vector2(collisionPoints[i], collisionPoints[i+1])))
                return true;
        }

        return false;
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }


    // The following must be overridden by Drawable, but are not used for anything.
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter colorFilter) { }
    @Override public int getOpacity() { return 0; }
}
