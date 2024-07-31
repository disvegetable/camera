package com.example.opencv_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
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
import java.util.List;
import java.util.Objects;

import com.example.opencv_app.Torch_JSON;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //make .so
    static {
        System.loadLibrary("opencv_app");
    }
    private CameraBridgeViewBase mOpencvCamera;

    private final String Dirname = "OpenCV_photo";

    File mDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Dirname);

    File mFile;

    Mat mPicture;//cache for picture

    int numPictures=0;//count the number of pictures

    UsbManager mUsbManager;

    Bitmap mBitmap;

    //thread for create files of picture
    Thread createPictureFilethread=new Thread(){
        @Override
        public void run(){
            try {
                Thread.sleep(500);
            }catch (InterruptedException i){
                i.printStackTrace();
            }
            while(true){
                try {
                    Thread.sleep(700);
                    Message message=new Message();
                    message.what=Constant.MESSAGE_TO_CREATE_PICTURE_FILE;
                    CreatePictureFileHandle.sendMessage(message);
                }catch (InterruptedException i){
                    i.printStackTrace();
                }
            }
        }
    };

    //handler for capture
    Handler PictureNumHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message message){
            super.handleMessage(message);
            if(message.what==Constant.SHOW_PICTURE_NUMBER){
                showNumberOfPictures();
            }
        }
    };

    //handler for create files of picture
    Handler CreatePictureFileHandle=new Handler(Objects.requireNonNull(Looper.myLooper())){
        @Override
        public void handleMessage(@NonNull Message message){
            super.handleMessage(message);
            if(message.what==Constant.MESSAGE_TO_CREATE_PICTURE_FILE){
                createFile();
            }
        }
    };

    //broadcastReceiver for usb
    BroadcastReceiver USBReceiver=new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Objects.equals(intent.getAction(), Constant.USB_ACTION)){
                TextView textView=(TextView) findViewById(R.id.usbDevice);
                textView.setText("USB Status: connected");
            }
            else{
                TextView textView=(TextView) findViewById(R.id.usbDevice);
                textView.setText("USB status: no device");
            }
        }
    };

    //make directory for pictures
    void makeDir() {
        if (!mDir.exists()) {
            boolean flag=mDir.mkdir();
            if(!flag){
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

        //use python
        Torch_JSON torchJson=new Torch_JSON();
        String res=torchJson.pythonToJava(this);
        AlertDialog alertDialog=new AlertDialog.Builder(this).setTitle("tips").setMessage(res).create();
        alertDialog.show();

        //use usb by broadcast
        mUsbManager=(UsbManager) getSystemService(USB_SERVICE);
        IntentFilter filter=new IntentFilter();
        filter.addAction(Constant.USB_ACTION);
        registerReceiver(USBReceiver,filter, Context.RECEIVER_NOT_EXPORTED);

   }

    @Override
    public void onStart(){
        super.onStart();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.enableView();
        }
        Intent intent=new Intent();
        intent.setAction(Constant.USB_ACTION);
        sendBroadcast(intent);
    }

    @Override
    public void onResume(){
        super.onResume();
        try{
            createPictureFilethread.start();
        }catch (IllegalThreadStateException i) {
            i.printStackTrace();
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
        unregisterReceiver(this.USBReceiver);
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(this.mOpencvCamera);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //create cache matTextView textView=(TextView)findViewById(R.id.counter);
        //                            textView.setText(R.string.Counter+numPicture
        this.mPicture=new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mPicture.release();
    }

    //handle the picture
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        this.mPicture=inputFrame.rgba();
        Mat tempPicture=mPicture;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename=sdf.format(new Date());
        String path=mDir.getPath()+"/"+filename+".png";
        mFile=new File(path);
        //change mat to bitmap
        mBitmap = Bitmap.createBitmap(mPicture.width(),mPicture.height(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tempPicture,mBitmap);
        numPictures= Objects.requireNonNull(this.mDir.list()).length;
        if(numPictures>=20){
           deleteAllPictures();
        }
        Message message=new Message();
        message.what=Constant.SHOW_PICTURE_NUMBER;
        //send message to main thread for UI
        PictureNumHandler.sendMessage(message);
        return tempPicture;
    }

    //permissions
    public void checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void showNumberOfPictures(){
        TextView textView=(TextView) findViewById(R.id.counter);
        textView.setText("sum of pictures:"+String.valueOf(numPictures));
    }

    public void createFile(){
        try(FileOutputStream fileOutputStream=new FileOutputStream(mFile)){
            mBitmap.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
            fileOutputStream.flush();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void deleteAllPictures(){
        String[] tempList= mDir.list();
        for(String fineName: Objects.requireNonNull(tempList)){
            File file=new File(mDir,fineName);
            file.delete();
        }
        numPictures=0;
    }

    //TODO:use usb to send message
    public void send(){

    }
}