package io.github.niyamatalmass.voicerecorder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DIALED_NUMBER = "EXTRA_DIALED_NUMBER";
    private Intent recordIntent;
    private String enteredPhoneNumber;
    private boolean isSpeakerOn;
    private Button callButton, playButton, uploadButton;
    private EditText phoneNumberEditText;
    private SeekBar seekBar;
    private String audioPath;
    private boolean recorded = false;
    private RelativeLayout rootElement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = (EditText) findViewById(R.id.phoneEditTextView);
        callButton = (Button) findViewById(R.id.callButton);
        playButton = (Button) findViewById(R.id.playButton);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        rootElement = (RelativeLayout) findViewById(R.id.rootElement);

        /*if (getIntent().getAction() != null) {
            String intent = getIntent().getAction();
            if (intent.equals("STOP")) {
                recorded = true;
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }
            }
        }*/


        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enteredPhoneNumber = String.valueOf(phoneNumberEditText.getText());
                if (enteredPhoneNumber != null) {
                    showRecordingDialogue(enteredPhoneNumber);
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (audioPath != null) {
                    Uri intentUri = Uri.parse("file://" + audioPath);

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(intentUri, "audio/3gpp");
                    startActivity(intent);
                } else {
                    Snackbar.make(rootElement, "You haven't record anything", Snackbar.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getAction() != null) {
            String intent = getIntent().getAction();
            if (intent.equals("STOP")) {
                recorded = true;
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }
            }
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
                            try {
                                audioPath = FileHelper.getFilename(enteredPhoneNumber);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            recordIntent.putExtra(EXTRA_DIALED_NUMBER, audioPath);
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
        // TODO: 3/9/17 uncomment this
        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeSpeaker(true);
            }
        }, 500);*/


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

}
