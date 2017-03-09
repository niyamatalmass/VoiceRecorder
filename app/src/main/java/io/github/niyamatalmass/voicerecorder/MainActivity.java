package io.github.niyamatalmass.voicerecorder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    Intent recordIntent;
    private boolean isSpeakerOn;
    private Button callButton;
    private EditText phoneNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getAction() != null) {
            String intent = getIntent().getAction();
            if (intent.equals("STOP")) {
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }
            }
        }


        phoneNumberEditText = (EditText) findViewById(R.id.phoneEditTextView);
        callButton = (Button) findViewById(R.id.callButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPhoneNumber = String.valueOf(phoneNumberEditText.getText());
                if (enteredPhoneNumber != null) {
                    showRecordingDialogue(enteredPhoneNumber);
                }
            }
        });

    }

    private void showRecordingDialogue(final String enteredPhoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Record call")
                .setMessage("Do you want to record the call?")
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recordIntent = new Intent(MainActivity.this, RecordService.class);
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

}
