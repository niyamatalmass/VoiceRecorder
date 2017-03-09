package io.github.niyamatalmass.voicerecorder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DIALED_NUMBER = "EXTRA_DIALED_NUMBER";
    private Intent recordIntent;
    private String enteredPhoneNumber;
    private boolean isSpeakerOn;
    private Button callButton, playButton, uploadButton;
    private EditText phoneNumberEditText;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = (EditText) findViewById(R.id.phoneEditTextView);
        callButton = (Button) findViewById(R.id.callButton);
        playButton = (Button) findViewById(R.id.playButton);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setVisibility(View.GONE);

        playButton.setActivated(false);
        uploadButton.setActivated(false);

        if (getIntent().getAction() != null) {
            String intent = getIntent().getAction();
            if (intent.equals("STOP")) {
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }
                playButton.setActivated(true);
                uploadButton.setActivated(true);
            }
        }


        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enteredPhoneNumber = String.valueOf(phoneNumberEditText.getText());
                if (enteredPhoneNumber != null) {
                    showRecordingDialogue(enteredPhoneNumber);
                }
            }
        });

        if (playButton.isActivated()) {
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String recordedAudioUrl;
                    try {
                        if (enteredPhoneNumber != null) {
                            recordedAudioUrl = FileHelper.getFilename(enteredPhoneNumber);
                            seekBar.setVisibility(View.VISIBLE);
                            PlayRecordedAudio playRecordedAudio = new PlayRecordedAudio();
                            playRecordedAudio.setSeekBar(seekBar);
                            playRecordedAudio.execute(recordedAudioUrl);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            });
        }


    }

    private void showRecordingDialogue(final String enteredPhoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Record call")
                .setMessage("Do you want to record the call?")
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recordIntent = new Intent(MainActivity.this, RecordService.class);
                        if (enteredPhoneNumber != null) {
                            recordIntent.putExtra(EXTRA_DIALED_NUMBER, enteredPhoneNumber);
                        }
                        startService(recordIntent);
                        makeCall(enteredPhoneNumber);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        makeCall(enteredPhoneNumber);
                    }
                });
        builder.create().show();
    }

    @SuppressWarnings("MissingPermission")
    private void makeCall(String enteredPhoneNumber) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeSpeaker(true);
            }
        }, 500);


        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + enteredPhoneNumber.trim()));
        startActivity(callIntent);
    }

    private void changeSpeaker(boolean b) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(b);
        isSpeakerOn = audioManager.isSpeakerphoneOn();
    }

    public class PlayRecordedAudio extends AsyncTask<String, Integer, Void> implements SeekBar.OnSeekBarChangeListener{

        MediaPlayer mediaPlayer = new MediaPlayer();
        SeekBar seekBar;

        public void setSeekBar(SeekBar seekBar) {
            this.seekBar = seekBar;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                mediaPlayer.setDataSource(params[0]);
                mediaPlayer.prepare();
                mediaPlayer.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setMax(mediaPlayer.getDuration());
                        seekBar.setOnSeekBarChangeListener(PlayRecordedAudio.this);
                    }
                });

                while (mediaPlayer.isPlaying()) {
                    publishProgress(mediaPlayer.getCurrentPosition());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            seekBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mediaPlayer.seekTo(progress / 1000);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

}
