package sevon.max.androidspaceship;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by Max on 2016-01-21.
 */
public class SoundManager implements GameEventListener {

    private MediaPlayer mediaPlayer;

    public SoundManager(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.nbs_dark_explosion);
    }

    @Override
    public void notify(World.Event event) {
        switch(event) {
            case GAME_STARTED:
                // start music here.
                break;
            case SPACESHIP_CRASH:
                // stop music here.
                // play crash sound.
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
                break;
        }
    }
}
