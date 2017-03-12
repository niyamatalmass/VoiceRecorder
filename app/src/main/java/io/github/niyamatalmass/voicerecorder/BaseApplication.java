package io.github.niyamatalmass.voicerecorder;

import android.app.Application;

/**
 * Created by niyamat on 3/11/17.
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        OAuthUtil.initSharedPref(this);
    }
}
