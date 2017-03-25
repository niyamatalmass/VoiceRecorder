package io.github.niyamatalmass.voicerecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

/**
 * Created by niyamat on 3/11/17.
 */

public class OAuthUtil {
    public static final String AUDIO_PATH_SHARED = "AUDIO_PATH_SHARED";
    public static final String FILE_NAME_SHARED = "FILE_NAME_SHARED";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String ACCOUNT_ID = "account_id";

    private static SharedPreferences sOAuthCredentials;

    public static void initSharedPref(Context context) {
        sOAuthCredentials = context.getSharedPreferences("oauth", Context.MODE_PRIVATE);
    }

    private static SharedPreferences getOAuthCredentials() {
        return sOAuthCredentials;
    }

    private static SharedPreferences.Editor editSharedPrefs() {
        return getOAuthCredentials().edit();
    }

    @Nullable
    public static String get(String key) {
        return sOAuthCredentials.getString(key, null);
    }

    public static Long getLong(String key) {
        return sOAuthCredentials.getLong(key, -1);
    }

    public static void set(String key, String value) {
        editSharedPrefs().putString(key, value).commit();
    }

    public static void set(String key, Long value) {
        editSharedPrefs().putLong(key, value);
    }

    public static void remove(String key) {
        editSharedPrefs().remove(key);
    }

    public static void setInstantly(String key, String value) {
        editSharedPrefs().putString(key, value).apply();
    }

    /*public static boolean isAuthorized() {
        return get(ACCESS_TOKEN) != null &&
                getLong(EXPIRES_IN) < System.currentTimeMillis();
    }*/

}
