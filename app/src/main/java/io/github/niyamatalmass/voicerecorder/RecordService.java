package io.github.niyamatalmass.voicerecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * Created by niyamat on 3/7/17.
 */

public class RecordService extends Service {
    private boolean onForeground = false;
    private RecorderHandler handler;

    @Override
    public void onCreate() {
        RecorderThread recorderThread = new RecorderThread();
        recorderThread.setName("RecordThread");
        recorderThread.start();

        while (recorderThread.mHandler == null) {

        }
        handler = recorderThread.mHandler;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();

        Message message = Message.obtain();
        message.arg1 = 2;
        handler.sendMessage(message);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Message message = Message.obtain();
        message.arg1 = 1;
        handler.sendMessage(message);
        stopForeground(true);
        onForeground = false;
    }

   /* private void startRecording() {
        Log.d(Constant.TAG, "RecordService startRecording");
        mediaRecorder = new MediaRecorder();

        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(FileHelper.getFilename(String.valueOf(4556)));

            mediaRecorder.prepare();

            Thread.sleep(2000);
            mediaRecorder.start();
            Log.d(Constant.TAG, "RecordService recordStarted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void startService() {
        if (!onForeground) {
            Log.d(Constant.TAG, "RecordService startService");
            Intent intent = new Intent(this, MainActivity.class);
            // intent.setAction(Intent.ACTION_VIEW);
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getBaseContext(), 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(
                    getBaseContext())
                    .setContentTitle(
                            this.getString(R.string.notification_title))
                    .setTicker(this.getString(R.string.notification_ticker))
                    .setContentText(this.getString(R.string.notification_text))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent).setOngoing(true)
                    .build();

            notification.flags = Notification.FLAG_NO_CLEAR;

            startForeground(1337, notification);
            onForeground = true;
        }
    }
}
