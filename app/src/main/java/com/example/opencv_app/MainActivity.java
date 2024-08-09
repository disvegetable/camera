package com.example.opencv_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.opencv.android.Utils;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //make .so
    static {
        System.loadLibrary("opencv_app");
    }

    private CameraBridgeViewBase mOpencvCamera;//UI for camera

    private final String Dirname = "OpenCV_photo";//directory name

    File mDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Dirname);

    File mFile;//the files of pictures

    Mat mPicture;//cache for picture

    int numPictures = 0;//count the number of pictures

    Bitmap mBitmap;//change mat to bitmap

    UsbManager mUsbManager;

    UsbDevice mUsbDevice;

    //thread for create files of picture
    Thread createPictureFilethread = new Thread() {
        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException i) {
                i.printStackTrace();
            }
            while (true) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException i) {
                    i.printStackTrace();
                }
                Message message = new Message();
                message.what = Constant.MESSAGE_TO_CREATE_PICTURE_FILE;
                myLooperHandle.sendMessage(message);
            }
        }
    };

    //handler for capture
    Handler MainLooperHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            if (message.what == Constant.SHOW_PICTURE_NUMBER) {
                showNumberOfPictures();
            }
        }
    };

    //handler for create files of picture
    Handler myLooperHandle = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            if (message.what == Constant.MESSAGE_TO_CREATE_PICTURE_FILE) {
                createFile();
            }
        }
    };

    //broadcast for usb
    private BroadcastReceiver USBReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

        }
    };

    //make directory for pictures
    void makeDir() {
        if (!mDir.exists()) {
            boolean flag = mDir.mkdir();
            if (!flag) {
                Toast.makeText(this, "failed to create directory", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //life cycle
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "failed to load opencv", Toast.LENGTH_SHORT).show();
        }
        setContentView(R.layout.activity_main);

        //camera view
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpencvCamera = (CameraBridgeViewBase) findViewById(R.id.View);
        mOpencvCamera.setVisibility(SurfaceView.VISIBLE);
        mOpencvCamera.setCvCameraViewListener(this);
        mOpencvCamera.setCameraIndex(0);
        //send file
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectFileUriExposure().build());

        //checkPermission
        checkPermission(this);
        //make directory for pictures
        makeDir();

        //usb service
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        IntentFilter intentFilter = new IntentFilter(Constant.ACTION_USB_PERMISSION);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(USBReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        if (!mUsbManager.getDeviceList().isEmpty()) {
            //check usb permission
            Intent intent = new Intent(this, usbPermission.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.enableView();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
        try {
            createPictureFilethread.start();
        } catch (IllegalThreadStateException i) {
            i.printStackTrace();
        }
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        if (iterator.hasNext()) {
            mUsbDevice = iterator.next();
            //show some information about device
            if (mUsbManager.hasPermission(mUsbDevice)) {
                TextView textView = (TextView) findViewById(R.id.usbDevice);
                textView.setText(Constant.USB_DEVICE + mUsbDevice.getDeviceName() +
                        "\n" + "Vendor ID: " + mUsbDevice.getVendorId() + " Product ID: " + mUsbDevice.getProductId());
                connectDevice();
            } else {
                //no permission
                TextView textView = (TextView) findViewById(R.id.usbStatus);
                textView.setText(Constant.USB_STATUS + "No Permission");
                textView = (TextView) findViewById(R.id.usbDevice);
                textView.setText(Constant.USB_DEVICE + mUsbDevice.getDeviceName() +
                        "\n" + "Vendor ID: " + mUsbDevice.getVendorId() + " Product ID: " + mUsbDevice.getProductId());
            }
        } else {
            //no device
            TextView textView = (TextView) findViewById(R.id.usbStatus);
            textView.setText(Constant.USB_STATUS + "disconnected");
            textView = (TextView) findViewById(R.id.usbDevice);
            textView.setText(Constant.USB_DEVICE + "no device");
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.enableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.disableView();
        }
        this.createPictureFilethread.interrupt();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.disableView();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(this.mOpencvCamera);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        this.mPicture = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mPicture.release();
    }

    //handle the picture
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        this.mPicture = inputFrame.rgba();
        Mat tempPicture = mPicture;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename = sdf.format(new Date());
        String path = mDir.getPath() + "/" + filename + ".png";
        mFile = new File(path);
        //change mat to bitmap
        mBitmap = Bitmap.createBitmap(mPicture.width(), mPicture.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tempPicture, mBitmap);
        numPictures = Objects.requireNonNull(this.mDir.list()).length;
        if (numPictures >= 20) {
            deleteAllPictures();
        }
        Message message = new Message();
        message.what = Constant.SHOW_PICTURE_NUMBER;
        //send message to main thread for UI
        MainLooperHandler.sendMessage(message);
        return tempPicture;
    }

    //permissions
    public void checkPermission(Activity activity) {
        //permission for write and read storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void showNumberOfPictures() {
        TextView textView = (TextView) findViewById(R.id.counter);
        textView.setText("sum of pictures:" + String.valueOf(numPictures));

    }

    public void createFile() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(mFile)) {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteAllPictures() {
        String[] tempList = mDir.list();
        for (String fineName : Objects.requireNonNull(tempList)) {
            File file = new File(mDir, fineName);
            file.delete();
        }
        numPictures = 0;
    }

    //TODO:use usb to send message
    @SuppressLint("SetTextI18n")
    public void connectDevice() {
        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection != null) {
            TextView textView = (TextView) findViewById(R.id.usbStatus);
            textView.setText(Constant.USB_STATUS + "connected");
            sendMessage(connection);
        } else {
            TextView textView = (TextView) findViewById(R.id.usbStatus);
            textView.setText(Constant.USB_STATUS + "disconnected");
        }
    }

    public void sendMessage(UsbDeviceConnection connection) {

    }

}