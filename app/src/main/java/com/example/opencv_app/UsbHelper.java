package com.example.opencv_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.TextView;

//TODO:
//use usb to send message
public class UsbHelper {

    public boolean connected=false;

    public BroadcastReceiver usbReceiver=new BroadcastReceiver() {
        //catch the broadcast from main activity
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(Constant.USB_ACTION.equals(action)){
                UsbDevice usbDevice=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)){
                    //connect to usb and send message
                    connect(usbDevice);
                    connected=true;
                }
            }
        }
    };


    public void connect(UsbDevice usbDevice){

    }


}
