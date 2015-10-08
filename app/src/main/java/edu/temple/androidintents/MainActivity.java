package edu.temple.androidintents;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Create an intent to launch a browser activity
        findViewById(R.id.URLButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                String urlString = ((EditText) findViewById(R.id.urlEditText)).getText().toString();
                urlIntent.setData(Uri.parse(urlString));

                startActivity(urlIntent);
            }
        });

        //  Create an intent and launch internal camera activity
        findViewById(R.id.cameraMethodOne).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoIntent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(photoIntent);
            }
        });

        //  Create an intent and launch an external camera activity
        findViewById(R.id.cameraMethodTwo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(photoIntent);
            }
        });
    }
}
