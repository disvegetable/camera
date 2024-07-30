package com.example.opencv_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import org.pytorch.Tensor;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
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

    Mat mPicture;//cache for picture

    int numPictures=0;//count the number of pictures

    UsbHelper mUsbHelper=new UsbHelper();

    UsbManager mUsbManager;

    PendingIntent mPendingIntent;

    //handler for capture
    Handler mHandler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message message){
            super.handleMessage(message);
            if(message.what==1){
                showNumberOfPictures();
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
        mPendingIntent=PendingIntent.getBroadcast(this,0,new Intent(Constant.USB_ACTION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter=new IntentFilter(Constant.USB_ACTION);
        registerReceiver(mUsbHelper.usbReceiver,filter, Context.RECEIVER_NOT_EXPORTED);
   }

    @Override
    public void onStart(){
        super.onStart();
        if (this.mOpencvCamera != null) {
            this.mOpencvCamera.enableView();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        TextView textView;
        String temp;
        if(mUsbHelper.connected){
            textView = (TextView) findViewById(R.id.usbDevice);
            temp = "USB Status: connected";
        }
        else{
            textView = (TextView) findViewById(R.id.usbDevice);
            temp = "USB Status: no device";
        }
        textView.setText(temp);
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
        unregisterReceiver(mUsbHelper.usbReceiver);
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
    }

    //handle the picture
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        this.mPicture=inputFrame.rgba();
        //Toast.makeText(this, "rgba", Toast.LENGTH_SHORT).show();
        Mat target=new Mat(mPicture.width(),mPicture.height(),CvType.CV_8UC4);
        Imgproc.cvtColor(mPicture,target,Imgproc.COLOR_RGB2BGR);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename=sdf.format(new Date());
        String path=mDir.getPath()+"/"+filename+".png";
        Imgcodecs.imwrite(path,target);
        numPictures= Objects.requireNonNull(this.mDir.list()).length;
        if(numPictures>=20){
            for(String fineName: Objects.requireNonNull(mDir.list())){
                File file=new File(mDir,fineName);
                file.delete();
            }
            numPictures=0;
        }
        Message message=new Message();
        message.what=1;
        //send message to main thread for UI
        mHandler.sendMessage(message);
        return this.mPicture;
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
}