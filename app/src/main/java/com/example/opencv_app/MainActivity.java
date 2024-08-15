package com.example.opencv_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Recording;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    //permission array
    private final String[] allPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    //request code
    private final int REQUEST_CODE = 100;

    ImageCapture imageCapture = null;

    Recording recording = null;

    ExecutorService cameraExecutor;

    UsbManager mUsbManager;

    UsbDevice mUsbDevice;

    UsbDeviceConnection mConnection;

    UsbInterface mInterface;

    UsbEndpoint EndpointIn;//endpoint for input

    UsbEndpoint EndpointOut;//endpoint for output

    int sentTimes = 0;//show times of sending message

    int receiveTimes = 0;//show times of receiving message

    boolean exit = false;//flag to stop the thread for sending message when detaching usb

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
                sendHandler.sendMessage(message);
            }
        }
    };

    //send handler
    Handler sendHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            if (message.what == Constant.SEND_MESSAGE) {
                sendByte();
                receiveByte();
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
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (!mUsbManager.getDeviceList().isEmpty()) {
                    HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                    Iterator<UsbDevice> iterator = deviceList.values().iterator();
                    if (iterator.hasNext()) {
                        mUsbDevice = iterator.next();
                        //check usb permission
                        if (!mUsbManager.hasPermission(mUsbDevice)) {
                            Intent secondActivity = new Intent(getApplicationContext(), usbPermissionActivity.class);
                            startActivity(secondActivity);
                        }
                        exit = false;
                    }
                }
            }
            //device detached
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "detached", Toast.LENGTH_SHORT).show();
                //release source
                mConnection.releaseInterface(mInterface);
                mConnection.close();
                //stop the thread
                exit = true;
                //no device
                TextView textView = (TextView) findViewById(R.id.usbStatus);
                textView.setText(Constant.USB_STATUS + "disconnected");
                textView = (TextView) findViewById(R.id.usbDevice);
                textView.setText(Constant.USB_DEVICE + "no device");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setContentView(R.layout.main_layout);

        //check permission
        checkStoragePermission(this);
        if (allCameraPermissionsGranted()) {
            startCamera();
        } else {
            requestCameraPermissions();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
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
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private boolean allCameraPermissionsGranted() {
        boolean flag = true;
        for (String permission : allPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    //start the camera and preview,bind the lifecycle of camera to activity lifecycle
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        PreviewView cameraPreview = (PreviewView) findViewById(R.id.viewFinder);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    //get surface provider
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                    //get back camera
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    //rebind camera
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //permissions
    public void checkStoragePermission(Activity activity) {
        //permission for write and read storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        }
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, allPermissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (allCameraPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "permissions no granted by user", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "no permissions", Toast.LENGTH_SHORT).show();
        }
    }


    //connect to device
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

    @SuppressLint("SetTextI18n")
    public void receiveByte() {
        byte[] mes = new byte[20];
        int result = mConnection.bulkTransfer(EndpointIn, mes, mes.length, 200);
        TextView textView = (TextView) findViewById(R.id.usbReceive);
        receiveTimes++;
        if (result < 0) {
            textView.setText("Receive Status: failed" + receiveTimes);
        } else {
            textView.setText("Receive Status: successful" + Arrays.toString(mes) + receiveTimes);
        }
    }
}