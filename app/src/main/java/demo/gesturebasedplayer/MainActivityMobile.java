package demo.gesturebasedplayer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivityMobile extends Activity implements SensorEventListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private ImageButton nextB, prevB, playB;
    private TextView durL, uniL, speedL, speedVal, totalSongDuration, currentSongDuration;

    private MediaPlayer mp;
    private Utilities utils;
    /**
     * Playlist for demo purpose - songs installed with the apk
     **/
    private int[] myPlayList = {R.raw.song1, R.raw.song2, R.raw.song3, R.raw.song4, R.raw.song5};  //This is the playlist user chooses - could be from device or web

    private double startTime = 0;
    private int playListSize = myPlayList.length;
    private static int currentIndex = 0;

    private SensorManager sensorManager;

    static final float NS2S = 1.0f / 1000000000.0f;                  //because sensor data is in nanoseconds
    long last_timestamp = 0;

    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast;


    private Handler myHandler = new Handler();
    private SeekBar seekbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_mobile);

        utils = new Utilities();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        nextB = (ImageButton) findViewById(R.id.next_btn);
        prevB = (ImageButton) findViewById(R.id.previous_btn);
        playB = (ImageButton) findViewById(R.id.play_btn);

        mp = new MediaPlayer();            //MediaPlayer.create(this, myPlaylist[currentSongIndex]);
        mp.setOnCompletionListener(this);

        totalSongDuration = (TextView) findViewById(R.id.total_duration_label);
        currentSongDuration = (TextView) findViewById(R.id.duration_label);
        durL = (TextView) findViewById(R.id.duration_label);
        speedL = (TextView) findViewById(R.id.speed_level);
        speedVal = (TextView) findViewById(R.id.speed_value);
        uniL = (TextView) findViewById(R.id.speed_unit);

        seekbar = (SeekBar) findViewById(R.id.progress_bar);
        seekbar.setClickable(true);
        seekbar.setOnSeekBarChangeListener(this);

        nextB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSong(myPlayList, ++currentIndex);
            }
        });

        playB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /** Pause if the player is already on **/
                if (mp.isPlaying()) {
                    if (mp != null) {
                        mp.pause();
                        /** Change icon to play **/
                        playB.setImageResource(R.drawable.img_play);
                    }
                } else {
                    /** Resume if player was paused **/
                    if (mp != null) {
                        mp.start();
                        /** Change icon to pause **/
                        playB.setImageResource(R.drawable.img_pause);
                    }
                }
            }
        });

        prevB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSong(myPlayList, --currentIndex);
            }
        });
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            long totalDuration = mp.getDuration();
            long currentDuration = mp.getCurrentPosition();

            totalSongDuration.setText("" + utils.milliSecondsToTimer(totalDuration));
            currentSongDuration.setText("" + utils.milliSecondsToTimer(currentDuration));

            int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
            seekbar.setProgress(progress);

            myHandler.postDelayed(this, 100);

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main_activity_mobile, menu);
        return true;
    }

    public void playSong(int[] myPlayList, int index) {

        try {
            if (index >= 0 && index <= playListSize - 1) {
                mp.reset();
                mp = MediaPlayer.create(this, myPlayList[index]);
                mp.start();
                playB.setImageResource(R.drawable.img_pause);
                seekbar.setProgress(0);
                seekbar.setMax(100);
                updateSeekBar();
            } else {
                Toast.makeText(this, "Playing list again!", Toast.LENGTH_LONG).show();
                currentIndex = 0;
                playSong(myPlayList, currentIndex);
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, 5000);   //value checked every second
        } else {
            Toast.makeText(this, "Accelerometer not available!", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));

        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta; // perform low-cut filter
        float dt = (event.timestamp - last_timestamp) * NS2S;
        float velocity = mAccel * dt;    // since data is being read every 5 seconds

        if (delta > 0.1 && delta < 5 && velocity > 0)           // temporary filter for extreme (inaccurate) values
        {
            String text3 = String.format("%.3f", velocity);
            speedVal.setText(text3);
            speedL.setText("(slow)");
            if (velocity > 1) {
                speedL.setText("(medium)");
                if (mp.isPlaying()) {
                    startTime = mp.getCurrentPosition();
                    if (startTime > 10000)           //Each song is played at leaat for 10 seconds for event demo purpose. This interval could be upto a minute in real time implementation
                    {
                        playSong(myPlayList, ++currentIndex);
                    }
                }
            }
            last_timestamp = event.timestamp;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /* Abstract method from OnCompletionListener */
    @Override
    public void onCompletion(MediaPlayer arg0) {
        if (currentIndex < (playListSize - 1)) {
            playSong(myPlayList, ++currentIndex);
        } else {
            playSong(myPlayList, 0);
            currentIndex = 0;
        }
    }

    public void updateSeekBar() {
        myHandler.postDelayed(UpdateSongTime, 100);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        /** Prevent message Handler from updating progress bar **/
        myHandler.removeCallbacks(UpdateSongTime);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        myHandler.removeCallbacks(UpdateSongTime);
        int totalDuration = mp.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
        mp.seekTo(currentPosition);
        updateSeekBar();
    }

    /* Abstract methods from Seekbar */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        /** TODO - Implement seekbar touch event **/
    }
}
