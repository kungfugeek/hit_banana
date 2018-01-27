package com.dev.nate.firsties;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int BK_NORMAL = 0xffffbb33;
    private static final int BK_RED = 0xffcc0000;
    private static final int BK_GREEN = 0xff1fff33;

    private static final String[] WORDS = {"clip", "basket", "panama", "powder", "pear",
            "baklava", "orange", "you", "glad", "did not", "say", "terrier","country", "palm",
            "coconut", "leaves", "herb", "potassium", "tires", "balderdash", "baseball", "billiards",
            "bananarama", "alakadabra", "boat", "savannah", "bonanza", "plantain", "bandanna",
            "pendant", "boomerang", "batman", "boat", "sombrero"};

    private static final int[] FAIL_COLORS = {BK_RED, BK_NORMAL, BK_RED, BK_NORMAL};
    private static final int[] HIT_COLORS = {BK_GREEN, BK_NORMAL};

    public static final String TAG = "Banana";
    public static final String HIGH_SCORE = "HIGH SCORE!!!";
    public static final String GAME_OVER = "Game over";
    public static final String START = "Start";
    public static final String BANANA = "banana";


    private Vibrator vibrator;

    private class Changer implements Runnable {
        private long wait = 800;
        private Random rand = new Random();
        private boolean stop = false;
        private List<String> randomWords = new ArrayList<>();
        private Sender sender;

        public void stop(String finalText) {
            stop = true;
            sender.sendWord(finalText);
        }

        public void incDifficulty() {
            if (wait > 100) {
                wait -= 100;
            } else if (wait > 10) {
                wait -= 10;
            }

        }

        @Override
        public void run() {
            while (!stop) {
                sender.sendWord(randomWord());
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted!");
                    stop = true;
                }
             }
        }

        private String randomWord() {
            if (randomWords.isEmpty()) {
                while (randomWords.size() < 10) {
                    randomWords.add(WORDS[rand.nextInt(WORDS.length)]);
                }
                randomWords.add(rand.nextInt(10), BANANA);
            }
            return randomWords.remove(0);
        }

     }

    private class Resetter implements Runnable {
        private Sender sender;
        @Override
        public void run() {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Log.d(TAG, "Interrupted!");
            }
            sender.sendWord(START);
        }
    }

    private class BackgroundFlasher implements Runnable {
        private Handler handler;

        private int[] colors;

        @Override
        public void run() {
            final Bundle bundle = new Bundle();
            for(int color : colors) {
                bundle.putInt("color",color);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private class Sender {

        private void sendWord(String word) {
            Bundle bundle = new Bundle();
            bundle.putString("word", word);
            Message message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }

    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (crazyView.getText().equals(BANANA)) {
                incFails();
                safe = true;
            } else {
                safe = false;
            }
            String word  = msg.getData().getString("word");

            crazyView.setText(word);
        }
    };

    private Handler backgroundHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int color = msg.getData().getInt("color");
            background.setBackgroundColor(color);
        }
    };

    private boolean safe;
    private ExecutorService executor;
    private BananaDBHandler dbHandler;
    private Changer changer;
    private TextView crazyView;
    private TextView hitsView;
    private TextView failsView;
    private TextView scoreView;
    private TextView highScoreView;
    private View background;

    private int hitsCount;
    private int failsCount;

    private long startTime;

    private Sender sender = new Sender();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        background = findViewById(R.id.background);
        crazyView = findViewById(R.id.bananaView);
        hitsView = findViewById(R.id.hitsView);
        failsView = findViewById(R.id.failsView);
        scoreView = findViewById(R.id.scoreView);
        highScoreView = findViewById(R.id.highScoreField);
        dbHandler = new BananaDBHandler(this, null);
        long highScore = getHighScore();
        highScoreView.setText(Long.toString(highScore));
        executor = Executors.newFixedThreadPool(2);
    }

    private void startGame() {
        hitsCount = 0;
        hitsView.setText(Integer.toString(hitsCount));
        failsCount = 0;
        failsView.setText(Integer.toString(failsCount));
        scoreView.setText("0");
        changer = new Changer();
        changer.sender = sender;
        executor.submit(changer);
        safe = false;
        Log.d(TAG, "Executor submitted");

        startTime = System.currentTimeMillis();
    }

    public void clicked(View view) {
        String currentText = crazyView.getText().toString();
        Log.d(TAG, currentText);
        if (currentText.equals(START)) {
            startGame();
        } else if (currentText.equals(BANANA)) {
            incHits();
        } else if (currentText.equals(GAME_OVER) || currentText.equals(HIGH_SCORE)) {
            //nothing
         } else if (!safe){
            incFails();
        }
    }

    public void reset(View view) {
        dbHandler.updateHighScore(0L);
        highScoreView.setText("0");
    }

    private void incFails() {
        failsCount++;
        failsView.setText(Integer.toString(failsCount));
        crazyView.setText("MISSED!");

        startFlasher(FAIL_COLORS);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(new long[] {10,100}, 1);
        if (failsCount > 2) {
            endGame();
        }
    }

    private void startFlasher(int[] colors) {
        BackgroundFlasher flasher = new BackgroundFlasher();
        flasher.colors = colors;
        flasher.handler = backgroundHandler;
        executor.submit(flasher);
    }

    private void endGame() {
        long score = System.currentTimeMillis() - startTime;
        scoreView.setText(Long.toString(score));
        if (score > getHighScore()) {
            setHighScore(score);
            changer.stop(HIGH_SCORE);
        } else {
            changer.stop(GAME_OVER);
        }
        Resetter resetter = new Resetter();
        resetter.sender = sender;
        executor.submit(resetter);
    }

    private void setHighScore(long score) {
        dbHandler.updateHighScore(score);
        highScoreView.setText(Long.toString(score));
    }

    private long getHighScore() {
        return dbHandler.getHighScore();
    }

    private void incHits() {
        hitsCount++;
        hitsView.setText(Integer.toString(hitsCount));
        changer.incDifficulty();
        crazyView.setText("Great!");
        startFlasher(HIT_COLORS);
    }


    @Override
    protected void onStop() {
       Log.d(TAG, "Stopping");
       if (changer != null) changer.stop("Closing...");
        if (executor != null) executor.shutdownNow();
        dbHandler.close();
        Process.killProcess(Process.myPid());
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resuming");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Pausing");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "Restarting");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back Pressed");
        finish();
    }


}
