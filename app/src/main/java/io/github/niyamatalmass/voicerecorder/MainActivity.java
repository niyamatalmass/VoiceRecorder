package io.github.niyamatalmass.voicerecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Intent recordIntent;
    private Button recordButton, stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton = (Button) findViewById(R.id.recordButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordIntent = new Intent(MainActivity.this, RecordService.class);
                startService(recordIntent);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordIntent == null) {
                    stopService(getIntent());
                } else {
                    stopService(recordIntent);
                }
            }
        });


    }
}
