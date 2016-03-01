package sevon.max.androidspaceship;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class HighScoreActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_score);

        ScoreList highScores = ScoreList.load(this);

        // Create string of high scores for display.
        int highScoreNumber = 1;
        String highScoreString = "";
        for(int score : highScores) {
            highScoreString += highScoreNumber + ". " + score + "\n";
            highScoreNumber++;
        }

        ((TextView) findViewById(R.id.highScoreText)).setText(highScoreString);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
