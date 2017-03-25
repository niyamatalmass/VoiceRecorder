package io.github.niyamatalmass.voicerecorder;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by niyamat on 3/7/17.
 */

public class RecordService extends Service {


    private static final String TAG = "StopwatchService";
    private static final int NOTIFICATION_ID = 1;
    // Timer to update the ongoing notification
    private final long mFrequency = 1000;    // milliseconds
    private final int TICK_WHAT = 2;
    private MediaRecorder mediaRecorder;
    private Stopwatch m_stopwatch;
    private LocalBinder m_binder = new LocalBinder();
    private NotificationManager m_notificationMgr;
    private boolean isUpdateNotification1 = false;
    private Notification m_notification;
    private Handler mHandler = new Handler() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        public void handleMessage(Message m) {
            updateNotification();
            sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
        }
    };

    public boolean isUpdateNotification1() {
        return isUpdateNotification1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "bound");

        return m_binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "created");

        m_stopwatch = new Stopwatch();
        mediaRecorder = new MediaRecorder();
        m_notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //createNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        Log.d(Constants.TAG, "onStartCommand Started from RecordService");

        Log.d(Constants.TAG, "from onStartCommand. the action is : " + intent.getAction());

        if ("RECORD".equals(intent.getAction())) {
            startRecording();
            start();
        } else if ("STOP".equals(intent.getAction())) {
            Log.d(Constants.TAG, "STOP action reached");
            pause();
            stopRecording();
        } else if ("REMOVE".equals(intent.getAction())) {
            removeNotification();
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_NOT_STICKY;
    }

    public void createNotification() {
        Log.d(TAG, "creating notification");

        int icon = R.drawable.ic_stat_fiber_manual_record;
        CharSequence tickerText = "Stopwatch";
        long when = System.currentTimeMillis();

//        m_notification = new Notification(icon, tickerText, when);
        m_notification = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .build();

        m_notification.flags |= Notification.FLAG_ONGOING_EVENT;
        m_notification.flags |= Notification.FLAG_NO_CLEAR;
    }

    public void updateNotification() {
        //Log.d(Constants.TAG, "updating notification");

        CharSequence contentTitle = "Recording";
        CharSequence contentText = getFormattedElapsedTime();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent intent = new Intent(this, RecordService.class);
        intent.setAction("STOP");
        PendingIntent pendingIntent = PendingIntent.getService(this, 301, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        // the next two lines initialize the Notification, using the configurations above
        m_notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_fiber_manual_record)
                .setTicker("The call is recording")
                .setWhen(System.currentTimeMillis())
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .addAction(R.drawable.ic_stat_fiber_manual_record, "STOP", pendingIntent)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();
        m_notification.flags = Notification.FLAG_NO_CLEAR;

        // m_notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        //Log.d(Constants.TAG, "Notification updating : " + m_notification.toString());
        m_notificationMgr.notify(NOTIFICATION_ID, m_notification);
    }

    public void updateNotification1() {
        // Log.d(TAG, "updating notification");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent intent = new Intent(this, RecordService.class);
        intent.setAction("RECORD");
        PendingIntent pendingIntent = PendingIntent.getService(this, 301, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        // the next two lines initialize the Notification, using the configurations above
        m_notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_fiber_manual_record)
                .setTicker("Do you want to record the call")
                .setContentTitle("Record the call")
                .setContentText("Click record button to record. Swipe to dismiss")
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_stat_fiber_manual_record, "RECORD", pendingIntent)
                .build();

        // m_notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        Log.d(Constants.TAG, "Notification updating 1 : " + m_notification.toString());

        m_notificationMgr.notify(NOTIFICATION_ID, m_notification);
    }

    public void removeNotification() {
        m_notificationMgr.cancel(NOTIFICATION_ID);
    }

    public void showNotification() {
        Log.d(TAG, "showing notification");

        updateNotification();
        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);
    }

    public void hideNotification() {
        Log.d(TAG, "removing notification");

        m_notificationMgr.cancel(NOTIFICATION_ID);
        mHandler.removeMessages(TICK_WHAT);
    }

    public void start() {
        Log.d(TAG, "start");
        m_stopwatch.start();

        showNotification();
    }

    public void pause() {
        Log.d(TAG, "pause");
        m_stopwatch.pause();

        hideNotification();
    }

    public void lap() {
        Log.d(TAG, "lap");
        m_stopwatch.lap();
    }

    public void reset() {
        Log.d(TAG, "reset");
        m_stopwatch.reset();
    }

    public long getElapsedTime() {
        return m_stopwatch.getElapsedTime();
    }

    public String getFormattedElapsedTime() {
        return formatElapsedTime(getElapsedTime());
    }

    public boolean isStopwatchRunning() {
        return m_stopwatch.isRunning();
    }

    /***
     * Given the time elapsed in tenths of seconds, returns the string
     * representation of that time.
     *
     * @param now, the current time in tenths of seconds
     * @return String with the current time in the format MM:SS.T or
     * 			HH:MM:SS.T, depending on elapsed time.
     */
    private String formatElapsedTime(long now) {
        long hours = 0, minutes = 0, seconds = 0, tenths = 0;
        StringBuilder sb = new StringBuilder();

        if (now < 1000) {
            tenths = now / 100;
        } else if (now < 60000) {
            seconds = now / 1000;
            now -= seconds * 1000;
            tenths = (now / 100);
        } else if (now < 3600000) {
            hours = now / 3600000;
            now -= hours * 3600000;
            minutes = now / 60000;
            now -= minutes * 60000;
            seconds = now / 1000;
            now -= seconds * 1000;
            tenths = (now / 100);
        }

        if (hours > 0) {
            sb.append(hours).append(":")
                    .append(formatDigits(minutes)).append(":")
                    .append(formatDigits(seconds)).append(".")
                    .append(tenths);
        } else {
            sb.append(formatDigits(minutes)).append(":")
                    .append(formatDigits(seconds)).append(".")
                    .append(tenths);
        }

        return sb.toString();
    }

    private String formatDigits(long num) {
        return (num < 10) ? "0" + num : new Long(num).toString();
    }

    public void startRecording() {
        Log.d(Constants.TAG, "Start recording method start from RecordService class");
        try {
            Log.d(Constants.TAG, "MediaRecorder reset from startRecording method from RecordService class");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(OAuthUtil.get(OAuthUtil.AUDIO_PATH_SHARED));

            mediaRecorder.prepare();
            Thread.sleep(2000);
            mediaRecorder.start();
            Log.d(Constants.TAG, "RecordService recordStarted");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(Constants.TAG, "Start recording method end from RecordService class");
    }

    public void stopRecording() {
        Log.d(Constants.TAG, "StopRecording method from RecordService started");
        try {
            mediaRecorder.stop();
        } catch (IllegalStateException e) {
            Log.d(Constants.TAG, "Can't record the audio");
            e.printStackTrace();
        }
        mediaRecorder.reset();
        Log.d(Constants.TAG, "MediaRecorder reset");
        pause();
        stopSelf();
        Log.d(Constants.TAG, "StopRecording method from RecordService end");
    }

    public class LocalBinder extends Binder {
        RecordService getService() {
            return RecordService.this;
        }
    }
}
