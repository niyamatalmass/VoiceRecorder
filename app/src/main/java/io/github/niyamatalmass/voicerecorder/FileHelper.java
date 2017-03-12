package io.github.niyamatalmass.voicerecorder;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.util.Date;

/**
 * Created by niyamat on 3/7/17.
 */

public class FileHelper {
    /**
     * returns absolute file directory
     *
     * @return
     * @throws Exception
     */
    public static String getFileLocation(String phoneNumber) throws Exception {
        String filepath = null;
        File file = null;
        if (phoneNumber == null)
            throw new Exception("Phone number can't be empty");
        try {
            filepath = getFilePath();

            file = new File(filepath, Constant.FILE_DIRECTORY);

            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.e(Constant.TAG, "Exception " + phoneNumber);
            e.printStackTrace();
        }

        return (file.getAbsolutePath() + getFileName(phoneNumber));
    }

    public static String getFileName(String phoneNumber) {
        String date = (String) DateFormat.format("yyyyMMddkkmmss", new Date());
        phoneNumber = phoneNumber.replaceAll("[\\*\\+-]", "");
        if (phoneNumber.length() > 10) {
            phoneNumber.substring(phoneNumber.length() - 10,
                    phoneNumber.length());
        }

        return ("/d" + date + "p" + phoneNumber + ".3gp");
    }

    public static String getFilePath() {
        // TODO: Change to user selected directory
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
