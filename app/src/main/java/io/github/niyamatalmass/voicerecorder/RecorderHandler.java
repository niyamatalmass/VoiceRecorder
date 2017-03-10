package io.github.niyamatalmass.voicerecorder;


import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by niyamat on 3/8/17.
 */

public class RecorderHandler extends Handler {
    private MediaRecorder mediaRecorder;


    @Override
    public void handleMessage(Message msg) {
        if (msg.arg1 == RecordService.MESSEGE_START_RECORDING) {
            startRecording(msg.obj.toString());
        } else if (msg.arg1 == RecordService.MESSEGE_STOP_RECORDING) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
    }

    private void startRecording(String audioPath) {
        Log.d(Constant.TAG, "RecordService startRecording");
        mediaRecorder = new MediaRecorder();

        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioPath);

            mediaRecorder.prepare();

            Thread.sleep(2000);
            mediaRecorder.start();
            Log.d(Constant.TAG, "RecordService recordStarted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
