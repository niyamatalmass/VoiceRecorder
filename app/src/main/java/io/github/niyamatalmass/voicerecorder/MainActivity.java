package io.github.niyamatalmass.voicerecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_CALL_PHONE_AND_RECORD_AUDIO = 201;
    private static final int REQUEST_PERMISSION_ONLY_CALL = 202;
    // Timer to update the elapsedTime display
    private final long mFrequency = 100;    // milliseconds
    private final int TICK_WHAT = 2;


    @BindView(R.id.stopButton)
    Button stopButton;
    @BindView(R.id.stopWatchTextView)
    TextView m_elapsedTime;
    @BindView(R.id.callButton)
    Button callButton;
    @BindView(R.id.playButton)
    Button playButton;
    @BindView(R.id.uploadButton)
    Button uploadButton;
    @BindView(R.id.phoneEditTextView)
    EditText phoneNumberEditText;
    @BindView(R.id.rootElement)
    RelativeLayout rootElement;
    @BindView(R.id.copyButton)
    Button linkCopyButton;
    @BindView(R.id.audioLinkTextView)
    TextView audioShareAbleLinkTextView;

    private DbxClientV2 dbxClientV2;
    private String enteredPhoneNumber;
    private boolean isSpeakerOn;
    private boolean mBound = false;
    private Intent recordIntent;
    private RecordService m_RecordService;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            updateElapsedTime();
            sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            Log.d(Constants.TAG, "from onService Connected : mBound : " + mBound);

            m_RecordService = ((RecordService.LocalBinder) service).getService();
            showCorrectButtons();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.d(Constants.TAG, "onServiceDisconnected");
        }
    };


    private void showCorrectButtons() {
        Log.d(Constants.TAG, "showCorrectButtons");

        if (mBound) {
            if (m_RecordService.isStopwatchRunning()) {
                stopButton.setVisibility(View.VISIBLE);
                Log.d(Constants.TAG, "from showCorrectButtons method : isStopwatchRunning" + m_RecordService.isStopwatchRunning());
            } else {
                stopButton.setVisibility(View.GONE);
            }
        }
    }

    public void updateElapsedTime() {
        if (mBound)
            m_elapsedTime.setText(m_RecordService.getFormattedElapsedTime());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Constants.TAG, "onCreate from Activity");

        ButterKnife.bind(this);

        if (!mBound) {
            recordIntent = new Intent(this, RecordService.class);
            bindService(recordIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);

        // set this three view's visibility to gone
        audioShareAbleLinkTextView.setVisibility(View.GONE);
        linkCopyButton.setVisibility(View.GONE);

        Log.d(Constants.TAG, "onCreate end from activity");
    }


    @OnClick(R.id.callButton)
    public void callButtonClicked(View view) {
        Log.d(Constants.TAG, "Call Button clicked");
        enteredPhoneNumber = String.valueOf(phoneNumberEditText.getText());
        if (enteredPhoneNumber != null) {
            showRecordingDialogue();
        } else {
            Snackbar.make(rootElement, "Please enter a phone number first!", Snackbar.LENGTH_SHORT);
        }
        Log.d(Constants.TAG, "Call Button clicked end");
    }

    @OnClick(R.id.playButton)
    public void playButtonClicked(View view) {
        Log.d(Constants.TAG, "Play Button clicked");
        String audioPath = OAuthUtil.get(OAuthUtil.AUDIO_PATH_SHARED);
        Log.d(Constants.TAG, "audioPath from playButtonClicked method : " + audioPath);
        if (audioPath != null) {
            Uri intentUri = Uri.parse("file://" + audioPath);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(intentUri, "audio/3gpp");
            startActivity(intent);
        } else {
            Snackbar.make(rootElement, "You haven't record anything", Snackbar.LENGTH_SHORT).show();
        }
        Log.d(Constants.TAG, "Play Button clicked end");
    }

    @OnClick(R.id.stopButton)
    public void stopButtonClicked(View view) {
        Log.d(Constants.TAG, "Stop Button clicked");

        if (mBound) {
            m_RecordService.stopRecording();
            m_RecordService.pause();
            stopButton.setVisibility(View.GONE);
        }
        Log.d(Constants.TAG, "Stop Button clicked end");
    }

    @OnClick(R.id.uploadButton)
    public void uploadButtonClicked(View view) {
        Log.d(Constants.TAG, "Upload Button clicked");
        if (OAuthUtil.get(OAuthUtil.ACCESS_TOKEN) != null && OAuthUtil.get(OAuthUtil.AUDIO_PATH_SHARED) != null && OAuthUtil.get(OAuthUtil.FILE_NAME_SHARED) != null) {
            uploadRecordedAudio();
        } else {
            if (OAuthUtil.get(OAuthUtil.AUDIO_PATH_SHARED) == null) {
                Snackbar.make(rootElement, "Can't find the file", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(rootElement, "You are not singed in! Sign in and try again!", Snackbar.LENGTH_LONG).setAction("SIGN IN", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Auth.startOAuth2Authentication(MainActivity.this, Constants.APP_KEY);
                    }
                }).show();
            }
        }
        Log.d(Constants.TAG, "Upload Button clicked end");
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
        if (id == R.id.action_reset) {
            OAuthUtil.remove(OAuthUtil.AUDIO_PATH_SHARED);
            OAuthUtil.remove(OAuthUtil.FILE_NAME_SHARED);
            audioShareAbleLinkTextView.setVisibility(View.GONE);
            linkCopyButton.setVisibility(View.GONE);
            stopService(new Intent(this, RecordService.class));
            phoneNumberEditText.clearFocus();
            if (mBound) {
                m_RecordService.reset();
                m_elapsedTime.setText(m_RecordService.getFormattedElapsedTime());
            }
            if (stopButton.getVisibility() == View.VISIBLE) {
                stopButton.setVisibility(View.GONE);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }

        OAuthUtil.remove(OAuthUtil.AUDIO_PATH_SHARED);
        OAuthUtil.remove(OAuthUtil.FILE_NAME_SHARED);
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
                makeCallAndStartRecording();
            }
        } else if (requestCode == REQUEST_PERMISSION_ONLY_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeCall();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(Constants.TAG, "Activity onResume");

        if (Auth.getOAuth2Token() != null) {
            OAuthUtil.set(OAuthUtil.ACCESS_TOKEN, Auth.getOAuth2Token());
            Snackbar.make(rootElement, "You are signed in!", Snackbar.LENGTH_SHORT).show();
        }

        Log.d(Constants.TAG, "Activity onResume end");
    }

    private void showRecordingDialogue() {
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
                                    requestPermission(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CALL_PHONE_AND_RECORD_AUDIO, "Call and Record Audio");
                                } else {
                                    makeCallAndStartRecording();
                                }
                            } else {
                                makeCallAndStartRecording();
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
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // we just make a call
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                requestPermission(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PERMISSION_ONLY_CALL, "Call");
                            } else {
                                makeCallWithOutStartRecording();
                            }
                        } else {
                            makeCallWithOutStartRecording();
                        }
                    }
                });
        builder.create().show();
    }

    private void makeCallAndStartRecording() {
        Log.d(Constants.TAG, "makeCallAndStartRecording method start");
        // audioPath where record will be saved
        String audioPath = FileHelper.getFileLocation(enteredPhoneNumber);

        Log.d(Constants.TAG, "audioPath from makeCallAndStartRecording method : " + audioPath);

        String fileName = FileHelper.getFileName(enteredPhoneNumber);

        Log.d(Constants.TAG, "fileName from makeCallAndStartRecording method : " + fileName);

        OAuthUtil.setInstantly(OAuthUtil.AUDIO_PATH_SHARED, audioPath);
        OAuthUtil.setInstantly(OAuthUtil.FILE_NAME_SHARED, fileName);

        if (mBound) {
            Intent intent = new Intent(this, RecordService.class);
            startService(intent);

            Log.d(Constants.TAG, "makeCallAndStartRecording method mBound is true. So sending message to recordMessenger for showing pause notification");


            m_RecordService.startRecording();
            m_RecordService.reset();
            m_RecordService.start();


            linkCopyButton.setVisibility(View.GONE);
            audioShareAbleLinkTextView.setVisibility(View.GONE);

            makeCall();
        } else {
            Log.d(Constants.TAG, "mBound is false.............");
        }

        Log.d(Constants.TAG, "makeCallAndStartRecording method end");
    }

    private void makeCallWithOutStartRecording() {
        Log.d(Constants.TAG, "makeCallWithOutRecording method start");
        String audioPath = null;
        try {
            // audioPath where record will be saved
            audioPath = FileHelper.getFileLocation(enteredPhoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String fileName = FileHelper.getFileName(enteredPhoneNumber);

        OAuthUtil.setInstantly(OAuthUtil.AUDIO_PATH_SHARED, audioPath);
        OAuthUtil.setInstantly(OAuthUtil.FILE_NAME_SHARED, fileName);

        if (mBound) {
            Intent intent = new Intent(this, RecordService.class);
            intent.putExtra(Constants.EXTRA_AUDIO_PATH, audioPath);
            startService(intent);

            m_RecordService.reset();
            m_RecordService.updateNotification1();
            makeCall();

            Log.d(Constants.TAG, "makeCallWithOutRecording method end");
        }
    }

    @SuppressWarnings("MissingPermission")
    private void makeCall() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeSpeaker(true);
            }
        }, 500);

        Log.d(Constants.TAG, "Starting call from makeCall method");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + enteredPhoneNumber.trim()));
        startActivity(callIntent);
        Log.d(Constants.TAG, "Call started from makeCall method");
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
            File fileFromPath = new File(OAuthUtil.get(OAuthUtil.AUDIO_PATH_SHARED));
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(fileFromPath);
                FileMetadata fileMetadata = dbxClientV2.files().uploadBuilder(OAuthUtil.get(OAuthUtil.FILE_NAME_SHARED))
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
                SharedLinkMetadata sharedLinkMetadata = dbxClientV2.sharing().createSharedLinkWithSettings(OAuthUtil.get(OAuthUtil.FILE_NAME_SHARED), SharedLinkSettings.newBuilder().withRequestedVisibility(RequestedVisibility.PUBLIC).build());
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
