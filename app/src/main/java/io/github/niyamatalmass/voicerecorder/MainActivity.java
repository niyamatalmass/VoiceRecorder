package io.github.niyamatalmass.voicerecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
    private static final int REQUEST_PERMISSION_CALL_PHONE_AND_RECORD_AUDIO = 201;
    private static final int REQUEST_PERMISSION_ONLY_CALL = 202;
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
    private String mEnteredPhoneNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //define all the view
        phoneNumberEditText = (EditText) findViewById(R.id.phoneEditTextView);
        callButton = (Button) findViewById(R.id.callButton);
        playButton = (Button) findViewById(R.id.playButton);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        chronometer = (Chronometer) findViewById(R.id.chronometerView);
        rootElement = (RelativeLayout) findViewById(R.id.rootElement);
        audioShareAbleLinkTextView = (TextView) findViewById(R.id.audioLinkTextView);
        linkCopyButton = (Button) findViewById(R.id.copyButton);

        // set this three view's visibility to gone
        stopButton.setVisibility(View.GONE);
        audioShareAbleLinkTextView.setVisibility(View.GONE);
        linkCopyButton.setVisibility(View.GONE);


        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enteredPhoneNumber = String.valueOf(phoneNumberEditText.getText());
                if (enteredPhoneNumber != null) {
                    showRecordingDialogue(enteredPhoneNumber);
                } else {
                    Snackbar.make(rootElement, "Please enter a phone number first!", Snackbar.LENGTH_SHORT);
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
                } else if (audioPath == null) {
                    Snackbar.make(rootElement, "Can't find the file", Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(rootElement, "You are not singed in! Sign in and try again!", Snackbar.LENGTH_LONG).setAction("SIGN IN", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Auth.startOAuth2Authentication(MainActivity.this, Constant.APP_KEY);
                        }
                    }).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CALL_PHONE_AND_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                makeCallAndStartRecording(mEnteredPhoneNumber);
            }
        } else if (requestCode == REQUEST_PERMISSION_ONLY_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeCall(mEnteredPhoneNumber);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Auth.getOAuth2Token() != null) {
            OAuthUtil.set(OAuthUtil.ACCESS_TOKEN, Auth.getOAuth2Token());
            Snackbar.make(rootElement, "You are signed in!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showRecordingDialogue(final String enteredPhoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Record call")
                .setMessage("Do you want to record the call?")
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (isExternalStorageAvailable()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                                        && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    mEnteredPhoneNumber = enteredPhoneNumber;
                                    requestPermission(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CALL_PHONE_AND_RECORD_AUDIO, "Call and Record Audio");
                                } else {
                                    makeCallAndStartRecording(enteredPhoneNumber);
                                }
                            } else {
                                makeCallAndStartRecording(enteredPhoneNumber);
                            }
                        } else {
                            Snackbar.make(rootElement, "Sorry! External storage not available", Snackbar.LENGTH_SHORT).show();
                        }


                    }

                    private boolean isExternalStorageAvailable() {
                        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // we just make a call
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                mEnteredPhoneNumber = enteredPhoneNumber;
                                requestPermission(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PERMISSION_ONLY_CALL, "Call");
                            } else {
                                makeCall(enteredPhoneNumber);
                            }
                        } else {
                            makeCall(enteredPhoneNumber);
                        }
                    }
                });
        builder.create().show();
    }

    private void makeCallAndStartRecording(String enteredPhoneNumber) {
        recordIntent = new Intent(MainActivity.this, RecordService.class);
        try {
            // audioPath where record will be saved
            audioPath = FileHelper.getFileLocation(enteredPhoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
        recordIntent.putExtra(EXTRA_DIALED_NUMBER, audioPath);
        fileName = FileHelper.getFileName(enteredPhoneNumber);
        startService(recordIntent);

        // initializing the stopwatch to count the time
        initializeStopwatch();

        // finally make call to the number
        makeCall(enteredPhoneNumber);
    }

    @SuppressLint("NewApi")
    public void requestPermission(final String[] permissionsName, final int requestCode, String displayPermissionName) {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(displayPermissionName + " Permission")
                    .setMessage("Hi there! We can't " + displayPermissionName + " anyone without " + displayPermissionName + " permission, could you please accept the " + displayPermissionName + " permission")
                    .setPositiveButton("Yep", new DialogInterface.OnClickListener() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(permissionsName, requestCode);
                        }
                    }).setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Snackbar.make(rootElement, ":(", Snackbar.LENGTH_SHORT).show();
                }
            }).show();
        } else {
            requestPermissions(permissionsName, requestCode);
        }

    }

    private void initializeStopwatch() {
        // stop button' visibility set to GONE in onCreate but we set this VISIBLE in here. so that user can see the stop button
        stopButton.setVisibility(View.VISIBLE);

        // we use chronometer for making this stopwatch
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
