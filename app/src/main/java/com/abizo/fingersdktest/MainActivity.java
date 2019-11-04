package com.abizo.fingersdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.aratek.trustfinger.sdk.DeviceListener;
import com.aratek.trustfinger.sdk.DeviceOpenListener;
import com.aratek.trustfinger.sdk.LfdLevel;
import com.aratek.trustfinger.sdk.LfdStatus;
import com.aratek.trustfinger.sdk.TrustFinger;
import com.aratek.trustfinger.sdk.TrustFingerDevice;
import com.cw.fpgabsdk.USBFingerManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int permissionCode = 5;

    public static final String TAG = "TAG";

    private byte[] rawData;
    private byte[] rawBitmap;
    private Bitmap rawToBitmap;
    int deviceIndex = 0;
    private TrustFingerDevice mTrustFingerDevice = null;

    private boolean detected = false;

    private ImageView imageViewFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewFingerprint = findViewById(R.id.imageViewFingerprint);

        initSDK();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (arePermissonsGranted()) {

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

    public void captureFingerprint() {
        CaptureTask captureTask = new CaptureTask();
        captureTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void closeSDK() {
        mTrustFingerDevice.close();
        USBFingerManager.getInstance(this).closeUSB();
    }

    public void initSDK() {
        USBFingerManager.getInstance(this).setDelayMs(5000);
        USBFingerManager.getInstance(this).openUSB(new USBFingerManager.OnUSBFingerListener() {
            @Override
            public void onOpenUSBFingerSuccess(String s, UsbManager usbManager, UsbDevice usbDevice) {
                Log.d(TAG, "Successful");
                final TrustFinger mTrustfinger = TrustFinger.getInstance(getApplicationContext());
                mTrustfinger.initialize();

                mTrustfinger.openDevice(deviceIndex, new DeviceOpenListener() {
                    @Override
                    public void openSuccess(TrustFingerDevice device) {
                        mTrustFingerDevice = device;
                        Log.d(TAG, "Successful opening");

                        captureFingerprint();
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

    private class CaptureTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            do {
                Log.d(TAG, "Do");
                if (mTrustFingerDevice.getLfdLevel() != LfdLevel.OFF) {
                    Log.d(TAG, "LFD Level: " + mTrustFingerDevice.getLfdLevel());
                    int[] lfdStatus = new int[1];
                    rawData = mTrustFingerDevice.captureRawDataLfd(lfdStatus);
                    if (lfdStatus[0] == LfdStatus.FAKE) {
                        Log.e(TAG, "Fake finger");
                    } else if (lfdStatus[0] == LfdStatus.UNKNOWN) {
                        Log.e(TAG, "Unknown finger.");
                    }
                    detected = true;
                } else {
                    rawData = mTrustFingerDevice.captureRawData();
                    if (rawData != null) {
                        Log.d(TAG, "Image quality: " + mTrustFingerDevice.rawDataQuality(rawData));
                        rawBitmap = mTrustFingerDevice.rawToBmp(
                                rawData,
                                mTrustFingerDevice.getImageInfo().getWidth(),
                                mTrustFingerDevice.getImageInfo().getHeight(),
                                mTrustFingerDevice.getImageInfo().getResolution()
                        );

                        rawToBitmap = BitmapFactory.decodeByteArray(rawBitmap, 0, rawBitmap.length);
                        //imageViewFingerprint.setImageBitmap(rawToBitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageViewFingerprint.setImageBitmap(rawToBitmap);
                            }
                        });

                        detected = true;
                    }
                }

            } while (detected == false);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            detected = false;
        }
    }
}
