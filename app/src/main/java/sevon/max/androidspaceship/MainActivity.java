package sevon.max.androidspaceship;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

public class MainActivity extends Activity {

    public static final int FRAMETIME = 1000 / 60;  // 60 fps.
    private World world;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorListener;
    private float accelerationX = 0;
    private float accelerationY = 0;
    private Thread gameThread;                      // Thread running the game loop.

    private final Object pauseObject = new Object();      // Used when pausing the game.
    private boolean pause = false;                  // Indicates the game is paused.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        world = (World) findViewById(R.id.world);

        // create and register the sound manager that handles the playing of all sounds.
        world.registerListener(new SoundManager(this));

        // Set up sensor listener.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer != null) {
            sensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    // This is from Simple Bouncing Ball.
                    if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        accelerationX = event.values[0];
                        accelerationY = -event.values[1];
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) { }
            };
        }

        // Start the game.
        runGame();
    }

    @Override
    public void onResume() {
        if(sensorListener != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        pause = false;
        synchronized (pauseObject) {
            pauseObject.notify();   // resume game thread.
        }
        super.onResume();
    }

    /**
     * This method creates and starts the game loop. The loop runs on a separate thread from the UI
     * thread to not prevent the UI from redrawing itself.
     */
    private void runGame() {
        gameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean quit = false;
                while(!quit) {
                    world.restart();
                    while(true) {
                        // get current time
                        long ts = System.currentTimeMillis();

                        // Update game. If update returns false it means player has died.
                        if(world.update(accelerationX, accelerationY) == false) {
                            break;
                        }

                        // Redraw
                        world.postInvalidate();

                        // Wait remaining frame time.
                        long timePassed = System.currentTimeMillis() - ts;
                        if(timePassed < FRAMETIME) {
                            try {
                                Thread.sleep(FRAMETIME - timePassed);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // Check if we should pause the thread
                        // (for example if app has been minimized).
                        while(pause) {
                            synchronized (pauseObject) {
                                try {
                                    pauseObject.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } // while
                } // while
                finish();
            }
        });
        gameThread.setDaemon(true); // gameThread stops when creator stops.
        gameThread.start();
    }

    @Override
    public void onPause() {
        if(sensorListener != null)
            sensorManager.unregisterListener(sensorListener);
        pause = true;   // pause game thread.
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameThread.interrupt();
        gameThread = null;
        super.onDestroy();
    }
}
