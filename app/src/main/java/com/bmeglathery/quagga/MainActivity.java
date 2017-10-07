package com.bmeglathery.quagga;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static android.graphics.Color.WHITE;

/**
 * Plays the game of Quagga.
 *
 * The ancient game of Quagga is played with three (six-sided) dice. Certain
 * combinations of the dice form what is called a Quagga, and if
 * you roll a Quagga you win the game and you get to wear a
 * special hat for the rest of the day.
 *
 * A Quagga arises when it is possible to concatenate two of the dice
 * to obtain a number that is a multiple of the third. For example,
 * if you roll 3, 4, and 6, that is a Quagga because 3 and 6 concatenated
 * is 36 which is a multiple of 4. One small catch: 1's do not count; if
 * you roll a 1, you lose.
 *
 * @Version 1 - Player clicks the roll button to simulate rolling the dice.
 * The three die images are updated to display the result. A toast appears
 * with a win/lose message.
 *
 * @Version 2 - Floating action button customized with icon. Game
 * statistics displayed in snackbar.
 *
 * @Version 3 - Animation to shake the dice.
 *
 * @Version 4 - Sound of dice rolling.
 *
 * @Version 5 - Options menu to randomize background color.
 *
 * @Version 6 - Shared preferences to store game statistics and
 * background color. Options menu item to reset game statistics.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int[] dieValue = new int[3];
    private ImageView[] imageView = new ImageView[3];
    private int numGames = 0;
    private int numWins = 0;
    private Animation shakeAnimation;
    private MediaPlayer diceRollAudioPlayer;
    private int bgColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = null;
                if (numGames == 0) {
                    msg = "No games played yet.";
                } else {
                    DecimalFormat f= new DecimalFormat("0.00");
                    float winPercent = (float) numWins / numGames * 100;
                    msg = "Wins: " + numWins + "/" + numGames + " = " + f.format(winPercent) + "%";
                }
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        //Open shared preferences file to retrieve number of wins,
        //number of games played, and background color.
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        numWins = sharedPref.getInt(getString(R.string.saved_num_wins), 0);
        numGames = sharedPref.getInt(getString(R.string.saved_num_games), 0);
        bgColor = sharedPref.getInt(getString(R.string.saved_bg_color), 0);

        setBackgroundColor();

        //Set icon on fab
        String iconName = "fabicon.png";
        try{
            InputStream stream = getAssets().open(iconName);
            Drawable d = Drawable.createFromStream(stream, null);
            fab.setImageDrawable(d);
        } catch(IOException e) {
            e.printStackTrace();
        }

        shakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.die_shake);
        shakeAnimation.setRepeatCount(3);

        diceRollAudioPlayer = MediaPlayer.create(MainActivity.this, R.raw.shake_and_roll);
        diceRollAudioPlayer.setLooping(false);

        imageView[0] = (ImageView) findViewById(R.id.die0);
        imageView[1] = (ImageView) findViewById(R.id.die1);
        imageView[2] = (ImageView) findViewById(R.id.die2);
    }

    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.saved_num_wins), numWins);
        editor.putInt(getString(R.string.saved_num_games), numGames);
        editor.putInt(getString(R.string.saved_bg_color), bgColor);
        editor.commit();
    }

    /**
     * Rolls the dice, updates images, and displays message.
     */
    public void roll(View view){
        numGames++;

        Random rand = new Random();
        for(int i = 0; i < 3; i++)
            dieValue[i] = 1 + rand.nextInt(6);

        String rollResult = getRollResult();

        for(int i = 0; i < 3; i++){
            String imageName = "d" + dieValue[i];
            int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());
            imageView[i].setImageResource(imageId);
            imageView[i].startAnimation(shakeAnimation);
            //Uncomment after correcting the .mp3 file
            //diceRollAudioPlayer.start();
        }

        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, rollResult, Toast.LENGTH_LONG);
        //toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * Returns a win/lose message and a list of all Quaggas for the current roll.
     */
    public String getRollResult(){
        String msg = "";

        //By rule, any roll that contains a one is a losing roll
        for(int i = 0; i < 3; i++){
            if(dieValue[i] == 1)
                return "You rolled a one! YOU DIE!";
        }

        //Check all combinations of the current roll for Quaggas
        //These are stored in a set so that duplicates are ignored
        int[][] diceIndices = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 1, 0}, {2, 0, 1}
        };

        Set<String> setOfQuaggas = new HashSet<>();
        for(int[] a : diceIndices){
            int x = dieValue[a[0]];
            int y = dieValue[a[1]];
            int z = dieValue[a[2]];
            if((10 * x + y) % z == 0){
                setOfQuaggas.add(z + " divides " + x + y + ".");
            }
        }

        if(setOfQuaggas.isEmpty()){
            return "No quaggas. YOU DIE!";
        }

        numWins++;
        msg = "That's a quagga! YOU WIN!\n";
        for(String t : setOfQuaggas){
            msg += t + "\n";
        }

        return msg.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void randomizeBackgroundColor(MenuItem item){
        bgColor = randomColor();
        setBackgroundColor();
    }

    /**
     * Sets the background color of the root element of the layout
     */
    private void setBackgroundColor(){
        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.root_layout);
        layout.setBackgroundColor(bgColor);
    }

    /**
     * Reset shared preferences with 0 wins and 0 games.
     */
    public void deleteSavedResults(MenuItem item){
        numGames = 0;
        numWins = 0;
        bgColor = Color.GREEN;
        setBackgroundColor();
    }

    /**
     * Returns a random color.
     */
    private int randomColor(){
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);

        return Color.argb(255, r, g, b);
    }
}
