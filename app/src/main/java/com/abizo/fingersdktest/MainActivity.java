package com.abizo.fingersdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.aratek.trustfinger.sdk.TrustFinger;

public class MainActivity extends AppCompatActivity {
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int permissionCode = 5;

    public static final String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSDK();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (arePermissonsGranted()) {
            Log.d(TAG, "Permissions have been granted.");
        } else {
            requestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case permissionCode:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions have been granted");
                }
                break;
        }
    }

    ////////////////////////////////////

    public void initSDK() {
        try {
            TrustFinger trustFinger = TrustFinger.getInstance(getApplicationContext());
            trustFinger.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Trust Finger SDK. " + e.getMessage());
        }
    }

    public void requestPermissions() {
        Log.d(TAG, "Requesting permissions...");
        ActivityCompat.requestPermissions(MainActivity.this, permissions, permissionCode);
    }

    public boolean arePermissonsGranted() {
        boolean writePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean readPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return writePermission && readPermission;
    }
}
