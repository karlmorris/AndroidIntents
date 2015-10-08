package edu.temple.androidintents;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CameraRequest extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Start camera activity,
        startActivityForResult(new Intent(this, CameraActivity.class), 1234);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        /*
         * save captured image in specified location
         * return RESULT_OK along with thumbnail
         * OR
         * Return RESULT_CANCELED if user cancels picture taking activity
         */
        setResult(resultCode, data); // assuming 'data' contains thumbnail
        finish();
    }
}
