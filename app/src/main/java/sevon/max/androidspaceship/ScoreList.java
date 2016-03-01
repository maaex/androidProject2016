package sevon.max.androidspaceship;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Max on 2016-01-08.
 * Class representing a high score list. Contains functionality for adding new scores to the list and saving/loading it
 * from disk.
 */
public class ScoreList extends ArrayList<Integer> implements Serializable {

    private static final String FILENAME = "highscores.dmp";

    /**
     * Adds a score to the list. The list will always hold the 10 top scores only, so adding a score below these will
     * result in the added score being removed again immediately.
     * @param playerScore The score to add.
     * @return Always returns true.
     */
    @Override
    public boolean add(Integer playerScore) {
        // add
        super.add(playerScore);

        // sort
        Collections.sort(this, Collections.reverseOrder());

        // make sure playerScore list never holds more than 10 scores.
        while(this.size() > 10) {
            remove(10);
        }

        return true;
    }

    /**
     * Loads a ScoreList from disk.
     * @return The loaded ScoreList. If no such file existed an empty ScoreList object is returned.
     */
    public static ScoreList load(Context context) {
        try {
            ScoreList scoreList;
            ObjectInputStream in = new ObjectInputStream(context.openFileInput(FILENAME));
            scoreList = (ScoreList) in.readObject();
            return scoreList;
        } catch (Exception e) {
            System.out.println("Could not load high scores. Creating new ones");
            e.printStackTrace();
            return new ScoreList();
        }
    }

    /**
     * Saves the high score list to disk.
     * @throws IOException
     */
    public void save(Context context) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(FILENAME, Context.MODE_PRIVATE));
        out.writeObject(this);
        out.close();
    }
}