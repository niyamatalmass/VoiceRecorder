package io.github.niyamatalmass.voicerecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * Created by niyamat on 3/7/17.
 */

public class RecordService extends Service {
    public static final int MESSEGE_STOP_RECORDING = 1;
    public static final int MESSEGE_START_RECORDING = 2;
    private static final int REQUEST_CODE_NOTIFICATION_PENDING_INTENT = 99;
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
        String audioPath = null;
        startService();
        if (intent.getStringExtra(MainActivity.EXTRA_DIALED_NUMBER) != null) {
            audioPath = intent.getStringExtra(MainActivity.EXTRA_DIALED_NUMBER);
        }
        Message message = Message.obtain();
        if (null != intent.getAction() && intent.getAction().equals("STOP")) {
            message.arg1 = MESSEGE_STOP_RECORDING;
            stopForeground(true);
            onForeground = false;
        } else {
            message.arg1 = MESSEGE_START_RECORDING;
            if (audioPath != null) {
                message.obj = audioPath;
            }

        }
        handler.sendMessage(message);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Message message = Message.obtain();
        message.arg1 = MESSEGE_STOP_RECORDING;
        handler.sendMessage(message);
        stopForeground(true);
        onForeground = false;
    }

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
                    .setSmallIcon(R.drawable.ic_stat_fiber_manual_record)
                    .setContentIntent(pendingIntent).setOngoing(true)
                    .build();

            notification.flags = Notification.FLAG_NO_CLEAR;

            startForeground(1337, notification);
            onForeground = true;
        }
    }
}
