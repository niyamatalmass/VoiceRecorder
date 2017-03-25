package io.github.niyamatalmass.voicerecorder;


/**
 * Created by niyamat on 3/7/17.
 */

public class Constants {
    public static final String TAG = "Voice Recorder N: ";

    public static final String FILE_DIRECTORY = "recordedCalls";
    public static final String LISTEN_ENABLED = "ListenEnabled";
    public static final String FILE_NAME_PATTERN = "^d[\\d]{14}p[_\\d]*\\.3gp$";


    public static final int START_RECORDING = 0;
    public static final int STOP_RECORDING = 1;
    public static final int IS_RECORDING = 2;

    public static final String APP_KEY = "egd5xa5gv5rnnco";
    public static final String APP_SECRET = "4rxu9pb8lb1n4kv";

    public static final int RECORDING = 3;
    public static final int NOT_RECORDING = 4;
    public static final int SHOW_DO_RECORD_NOTIFICATION = 5;
    public static final String NOTIFICATION_ACTION_BROADCAST_RECEIVER = "notification_action_broadcast_receiver";
    public static final String EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH";
}
