package com.abizo.fingersdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import com.aratek.trustfinger.sdk.DeviceOpenListener;
import com.aratek.trustfinger.sdk.TrustFinger;
import com.aratek.trustfinger.sdk.TrustFingerDevice;
import com.cw.fpgabsdk.USBFingerManager;

public class MainActivity extends AppCompatActivity {
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int permissionCode = 5;

    public static final String TAG = "TAG";

    int deviceIndex = 0;
    TrustFingerDevice mTrustFingerDevice = null;

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
    protected void onStop() {
        super.onStop();
        closeSDK();
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

    public void closeSDK() {
        mTrustFingerDevice.close();
        USBFingerManager.getInstance(this).closeUSB();
    }

    public void initSDK() {
        USBFingerManager.getInstance(this).setDelayMs(1000);
        USBFingerManager.getInstance(this).openUSB(new USBFingerManager.OnUSBFingerListener() {
            @Override
            public void onOpenUSBFingerSuccess(String s, UsbManager usbManager, UsbDevice usbDevice) {
                Log.d(TAG, "Successful");
                TrustFinger mTrustfinger = TrustFinger.getInstance(getApplicationContext());
                mTrustfinger.initialize();

                mTrustfinger.openDevice(deviceIndex, new DeviceOpenListener() {
                    @Override
                    public void openSuccess(TrustFingerDevice device) {
                        mTrustFingerDevice = device;
                        Log.d(TAG, "Successful opening");
                    }

                    @Override
                    public void openFail(String msg) {
                        Log.e(TAG, "Opening device failed. " + msg);
                    }
                });
            }

            @Override
            public void onOpenUSBFingerFailure(String s) {
                Log.e(TAG, "Error on opening USB: " + s);
            }
        });

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
