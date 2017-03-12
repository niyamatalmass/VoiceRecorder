package io.github.niyamatalmass.voicerecorder;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DIALED_NUMBER = "EXTRA_DIALED_NUMBER";
    private DbxClientV2 dbxClientV2;
    private Intent recordIntent;
    private String enteredPhoneNumber;
    private boolean isSpeakerOn;
    private Button callButton, playButton, uploadButton;
    private EditText phoneNumberEditText;
    private String audioPath;
    private RelativeLayout rootElement;
    private Button stopButton;
    private TextView audioShareAbleLinkTextView;
    private Button linkCopyButton;
    private Chronometer chronometer;
    private String fileName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = (EditText) findViewById(R.id.phoneEditTextView);
        callButton = (Button) findViewById(R.id.callButton);
        playButton = (Button) findViewById(R.id.playButton);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        chronometer = (Chronometer) findViewById(R.id.chronometerView);
        rootElement = (RelativeLayout) findViewById(R.id.rootElement);
        audioShareAbleLinkTextView = (TextView) findViewById(R.id.audioLinkTextView);
        linkCopyButton = (Button) findViewById(R.id.copyButton);

        stopButton.setVisibility(View.GONE);
        audioShareAbleLinkTextView.setVisibility(View.GONE);
        linkCopyButton.setVisibility(View.GONE);


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

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }

                chronometer.stop();
                stopButton.setVisibility(View.GONE);
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OAuthUtil.get(OAuthUtil.ACCESS_TOKEN) != null) {
                    uploadRecordedAudio();
                } else {
                    Snackbar.make(rootElement, "You are not singed in! Please sign in!", Snackbar.LENGTH_SHORT);
                }
            }
        });


    }

    private void uploadRecordedAudio() {
        final DbxRequestConfig dbxRequestConfig = DbxRequestConfig.newBuilder("example-v2-demo")
                .withHttpRequestor(OkHttp3Requestor.INSTANCE)
                .build();

        dbxClientV2 = new DbxClientV2(dbxRequestConfig, OAuthUtil.get(OAuthUtil.ACCESS_TOKEN));

        FileUploadTask fileUploadTask = new FileUploadTask();
        fileUploadTask.execute();


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Auth.getOAuth2Token() != null) {
            OAuthUtil.set(OAuthUtil.ACCESS_TOKEN, Auth.getOAuth2Token());
        }
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
        if (id == R.id.action_sign_in) {
            Auth.startOAuth2Authentication(this, Constant.APP_KEY);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                                audioPath = FileHelper.getFileLocation(enteredPhoneNumber);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            recordIntent.putExtra(EXTRA_DIALED_NUMBER, audioPath);
                        }
                        fileName = FileHelper.getFileName(enteredPhoneNumber);
                        startService(recordIntent);

                        initializeStopwatch();

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

    private void initializeStopwatch() {
        stopButton.setVisibility(View.VISIBLE);

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
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

    private class FileUploadTask extends AsyncTask<Object, Object, String> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Uploading");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Object... params) {
            File fileFromPath = new File(audioPath);
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(fileFromPath);
                FileMetadata fileMetadata = dbxClientV2.files().uploadBuilder(fileName)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
                SharedLinkMetadata sharedLinkMetadata = dbxClientV2.sharing().createSharedLinkWithSettings(fileName, SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build());
                return sharedLinkMetadata.getUrl();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (DbxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String url) {
            super.onPostExecute(url);
            progressDialog.dismiss();
            if (url != null) {
                Snackbar.make(rootElement, "The record was uploaded to your Dropbox account", Snackbar.LENGTH_LONG).show();
                audioShareAbleLinkTextView.setVisibility(View.VISIBLE);
                linkCopyButton.setVisibility(View.VISIBLE);
                audioShareAbleLinkTextView.setText(url);
                linkCopyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("label", url));
                        Toast.makeText(MainActivity.this, "Url copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Snackbar.make(rootElement, "Upload failed", Snackbar.LENGTH_INDEFINITE);
            }
        }
    }

}
