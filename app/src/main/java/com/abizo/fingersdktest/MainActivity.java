package com.abizo.fingersdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.aratek.trustfinger.sdk.DeviceOpenListener;
import com.aratek.trustfinger.sdk.FingerPosition;
import com.aratek.trustfinger.sdk.LfdLevel;
import com.aratek.trustfinger.sdk.LfdStatus;
import com.aratek.trustfinger.sdk.SecurityLevel;
import com.aratek.trustfinger.sdk.TrustFinger;
import com.aratek.trustfinger.sdk.TrustFingerDevice;
import com.aratek.trustfinger.sdk.TrustFingerException;
import com.aratek.trustfinger.sdk.VerifyResult;
import com.cw.fpgabsdk.USBFingerManager;

public class MainActivity extends AppCompatActivity {
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int permissionCode = 5;

    public static final String TAG = "TAG";

    private byte[] rawData;
    private byte[] rawBitmap;
    private byte[] featureData;

    private byte[] rawData1;
    private byte[] rawBitmap1;
    private byte[] rawData2;
    private byte[] rawBitmap2;
    private byte[] rawData3;
    private byte[] rawBitmap3;
    private Bitmap rawToBitmap1;
    private Bitmap rawToBitmap2;
    private Bitmap rawToBitmap3;

    private byte[] featureData1;
    private byte[] featureData2;
    private byte[] featureData3;


    int deviceIndex = 0;
    private TrustFingerDevice mTrustFingerDevice = null;

    private boolean detected = false;
    private int enrollCount = 0;
    private boolean isScanning = false;
    private boolean isPressing = false;
    private boolean isDone = false;

    private ImageView imageViewFingerprint;
    private ImageView imageViewFingerprint2;
    private ImageView imageViewFingerprint3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewFingerprint = findViewById(R.id.imageViewFingerprint);
        imageViewFingerprint2 = findViewById(R.id.imageViewFingerprint2);
        imageViewFingerprint3 = findViewById(R.id.imageViewFingerprint3);

        initSDK();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!arePermissonsGranted()) {
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

    //////////////////////////////////////////////////////////////////

    public void captureFingerprint() {
        Log.d("Avery", "Async");
        CaptureTask captureTask = new CaptureTask();
        captureTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

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
            isScanning = true;
            Log.d("Avery", "Pre execute");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("Avery", "Doing in background");
            do {
                Log.d(TAG, "Do");
                if (mTrustFingerDevice.getLfdLevel() != LfdLevel.OFF) {
                    Log.d(TAG, "Not off");
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
                    Log.d(TAG, "Off");
                    rawData = mTrustFingerDevice.captureRawData(); //Getting the raw fingerprint data (as byte array)
                    if (rawData == null) {
                        //released
                        Log.d(TAG, "Finger is released.");
                        if (rawData1 != null && rawData2 != null && rawData3 != null) {
                            enrollCount = 3;
                            if (ifFingersMatch(featureData1, featureData2, featureData3)) {
                                Log.d(TAG, "Fingerprints match!");
                            } else {
                                Log.e(TAG, "Fingerprints do not match!");
                            }
                            break;
                        } else {
                            if (rawData1 != null && rawData2 != null) {
                                enrollCount = 2;
                            } else if (rawData1 != null) {
                                enrollCount = 1;
                            } else {
                                enrollCount = 0;
                            }
                        }

                        Log.d(TAG, "Count: " + enrollCount);
                    } else {
                        //pressed
                        Log.d(TAG, "Finger is pressed");

                        try {
                            featureData = mTrustFingerDevice.extractFeature(FingerPosition.RightIndexFinger);
                            if (featureData != null) {
                                Log.d(TAG, "Feature data is not null");
                            } else {
                                Log.e(TAG, "Feature data is null");
                            }

                        } catch (TrustFingerException e) {
                            Log.e(TAG, "Trust Finger Exception: " + e.getType());
                            Log.e(TAG, "Error: " + e.getMessage());
                        }

                        rawBitmap = mTrustFingerDevice.rawToBmp(rawData,
                                mTrustFingerDevice.getImageInfo().getWidth(),
                                mTrustFingerDevice.getImageInfo().getHeight(),
                                mTrustFingerDevice.getImageInfo().getResolution() //Converting byte array into bitmap byte array, later to be converted into a bitmap
                        );
                        rawToBitmap1 = BitmapFactory.decodeByteArray(rawBitmap, 0, rawBitmap.length); //Converting into bitmap
                        //Loading the bitmap into the imageview
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Switch Count: " + enrollCount);
                                switch (enrollCount) {
                                    case 0:
                                        imageViewFingerprint.setImageBitmap(rawToBitmap1);
                                        rawData1 = rawData;
                                        featureData1 = featureData;
                                        break;
                                    case 1:
                                        imageViewFingerprint3.setImageBitmap(rawToBitmap1);
                                        rawData2 = rawData;
                                        featureData2 = featureData;
                                        break;
                                    case 2:
                                        imageViewFingerprint2.setImageBitmap(rawToBitmap1);
                                        rawData3 = rawData;
                                        featureData3 = featureData;
                                        break;
                                    default:
                                        Log.e(TAG, "Invalid enroll count: " + enrollCount);
                                        break;
                                }
                            }
                        });


                    }
                }

            } while (!detected);

            isDone = true;

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            detected = false;
            isScanning = false;
            rawData = null;
            rawBitmap = null;
            //new CaptureTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }

        public boolean ifFingersMatch(byte[] f1, byte[] f2, byte[] f3) {
            try {
                VerifyResult verifyResult1 = mTrustFingerDevice.verify(SecurityLevel.Level1, f1, f2);
                VerifyResult verifyResult2 = mTrustFingerDevice.verify(SecurityLevel.Level1, f1, f3);
                VerifyResult verifyResult3 = mTrustFingerDevice.verify(SecurityLevel.Level1, f2, f3);

                return verifyResult1.isMatched && verifyResult2.isMatched && verifyResult3.isMatched;
            } catch (Exception e) {
                Log.e(TAG, "Error on verify: " + e.getMessage());
                return false;
            }


        }
    }

    /*
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

                        rawToBitmap1 = BitmapFactory.decodeByteArray(rawBitmap, 0, rawBitmap.length);
                        //imageViewFingerprint.setImageBitmap(rawToBitmap1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                switch (enrollCount) {
                                    case 0:
                                        imageViewFingerprint.setImageBitmap(rawToBitmap1);
                                        detected = false;
                                        enrollCount++;
                                        break;
                                    case 1:
                                        imageViewFingerprint2.setImageBitmap(rawToBitmap1);
                                        detected = false;
                                        enrollCount++;
                                        break;
                                    case 2:
                                        imageViewFingerprint3.setImageBitmap(rawToBitmap1);
                                        detected = true;
                                        enrollCount++;
                                        break;
                                    default:
                                        Log.e(TAG, "Invalid enroll count.");

                                }
                            }
                        });
                        //break;
                    }
                }

            } while (!detected);

            isDone = true;

            return null;
     */
}
