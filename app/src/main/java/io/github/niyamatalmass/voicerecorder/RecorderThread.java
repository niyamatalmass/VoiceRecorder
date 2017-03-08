package io.github.niyamatalmass.voicerecorder;

import android.os.Looper;

/**
 * Created by niyamat on 3/8/17.
 */

public class RecorderThread extends Thread {
    public RecorderHandler mHandler;

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new RecorderHandler();
        Looper.loop();
    }
}
