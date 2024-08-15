package com.example.opencv_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;

public class usbPermissionActivity extends Activity {

    UsbDevice mUsbDevice;

    UsbManager mUsbManager;

    //broadcast for usb
    private BroadcastReceiver USBReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constant.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    mUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        finish();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constant.ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        if (iterator.hasNext()) {
            mUsbDevice = iterator.next();
            if (mUsbManager.hasPermission(mUsbDevice)) {
                finish();
            }
            mUsbManager.requestPermission(mUsbDevice, permissionIntent);
            while (!mUsbManager.hasPermission(mUsbDevice)) {
                continue;
            }
            finish();
        }
    }
}
