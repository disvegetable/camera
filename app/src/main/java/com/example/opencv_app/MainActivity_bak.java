package com.example.opencv_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
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

public class MainActivity_bak extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

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

    UsbDeviceConnection mConnection;

    UsbInterface mInterface;

    UsbEndpoint EndpointIn;//endpoint for input

    UsbEndpoint EndpointOut;//endpoint for output

    int sentTimes = 0;//show times of sending message

    boolean exit = false;//flag to stop the thread for sending message when detaching usb

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

    //Thread for usb to send message
    Thread sendThread = new Thread() {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException i) {
                i.printStackTrace();
            }
            while (!exit) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException i) {
                    i.printStackTrace();
                }
                Message message = new Message();
                message.what = Constant.SEND_MESSAGE;
                sendHandle.sendMessage(message);
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

    Handler sendHandle = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            if (message.what == Constant.SEND_MESSAGE) {
                sendByte();
            }
        }
    };

    //broadcast for usb
    private final BroadcastReceiver USBReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //device attached
//            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
//                if (!mUsbManager.getDeviceList().isEmpty()) {
//                    //check usb permission
//                    Intent secondActivity = new Intent(getApplicationContext(), usbPermissionActivity.class);
//                    startActivity(secondActivity);
//                }
//            }
            //device detached
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "detached", Toast.LENGTH_SHORT).show();
                mConnection.releaseInterface(mInterface);
                mConnection.close();
                exit = true;
            }
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
        intentFilter.addAction(Constant.ACTION_USB_SEND_MESSAGE);
        registerReceiver(USBReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        if (!mUsbManager.getDeviceList().isEmpty()) {
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            Iterator<UsbDevice> iterator = deviceList.values().iterator();
            if (iterator.hasNext()) {
                mUsbDevice = iterator.next();
                //check usb permission
                if (!mUsbManager.hasPermission(mUsbDevice)) {
                    Intent intent = new Intent(this, usbPermissionActivity.class);
                    startActivity(intent);
                }
            }
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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

    //connect to deivce
    @SuppressLint("SetTextI18n")
    public void connectDevice() {
        mConnection = mUsbManager.openDevice(mUsbDevice);
        if (mConnection != null) {
            TextView textView = (TextView) findViewById(R.id.usbStatus);
            textView.setText(Constant.USB_STATUS + "connected");
            //configure the endpoints
            mInterface = mUsbDevice.getInterface(1);
            int SumOfEndpoint = mInterface.getEndpointCount();
            //make endpoint for sending or receiving message
            for (int i = 0; i < SumOfEndpoint; i++) {
                if (mInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (mInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
                        EndpointIn = mInterface.getEndpoint(i);
                    } else {
                        EndpointOut = mInterface.getEndpoint(i);
                    }
                }
            }
            mConnection.claimInterface(mInterface, true);
            //start the thread of sending message
            sendThread.start();
        } else {
            TextView textView = (TextView) findViewById(R.id.usbStatus);
            textView.setText(Constant.USB_STATUS + "disconnected");
        }
    }

    @SuppressLint("SetTextI18n")
    public void sendByte() {
        String message = "hellosfskjsfasdfas";
        byte[] mes = message.getBytes();
        int result = mConnection.bulkTransfer(EndpointOut, mes, mes.length, 100);
        TextView textView = (TextView) findViewById(R.id.usbSend);
        sentTimes++;
        if (result < 0) {
            textView.setText("Send Status: failed" + sentTimes);
        } else {
            textView.setText("Send Status: successful" + sentTimes);
        }
    }
}