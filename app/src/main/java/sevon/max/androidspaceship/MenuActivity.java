package sevon.max.androidspaceship;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void startGameButtonClick(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void highScoreButtonClick(View view) {
        startActivity(new Intent(this, HighScoreActivity.class));
    }
}
