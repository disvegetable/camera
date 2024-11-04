package com.example.opencv_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_app");
    }

    //permission array
    private final String[] allPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    //request code
    private final int REQUEST_CODE = 100;

    ExecutorService cameraExecutor;

    String[][] Status = new String[6][3];

    MyCube cube = new MyCube();

    List<String> Label = new ArrayList<>();

    Button scrambleButton;

    Button restoreButton;

    boolean isRestoring = false;

    long beginTime = 0;

    long endTime = 0;

    private class layerComparator implements Comparator<MatOfPoint> {
        @Override
        public int compare(MatOfPoint t1, MatOfPoint t2) {
            Moments moments_1 = Imgproc.moments(t1);
            Moments moments_2 = Imgproc.moments(t2);
            int cY_1 = (int) (moments_1.m01 / moments_1.m00);
            int cY_2 = (int) (moments_2.m01 / moments_2.m00);
            return cY_1 - cY_2;
        }
    }

    //ImageAnalyzer
    private class mAnalyzer implements ImageAnalysis.Analyzer {

        //rotate image
        public void rotate(int rotation, Mat target) {
            if (rotation == 90) {
                Core.transpose(target, target);
                Core.flip(target, target, 1);
            }
        }

        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy image) {
            int height = image.getHeight();
            Log.d("yep", String.valueOf(height));

            //change format of image from yuv to mat
            int degrees = image.getImageInfo().getRotationDegrees();
            YUVtoMat yuvtomat = new YUVtoMat(image.getImage());
            Mat src = yuvtomat.rgba();//src mat
            rotate(degrees, src);
            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);

            Mat gray = new Mat(src.width(), src.height(), CvType.CV_8UC1);

            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

            //use median blur
            Imgproc.medianBlur(gray, gray, 3);
            Imgproc.adaptiveThreshold(gray, gray, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 5);

            //find center of blocks
            Mat hierachy = new Mat();
            Imgproc.findContours(gray, contours, hierachy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            Bitmap bitmap = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
            Iterator<MatOfPoint> iterator = contours.iterator();

            //remove some points
            while (iterator.hasNext()) {
                MatOfPoint con = iterator.next();
                int cX = getX(con);
                int cY = getY(con);
                if (Imgproc.contourArea(con) < 300 || Imgproc.contourArea(con) > 5000) {
                    iterator.remove();
                } else if (cX > 500 || cY < 30 || cX < 200 || cY > 400) {
                    iterator.remove();
                }
            }

            if (contours.size() == 18) {
                //make layer
                contours.sort(new layerComparator());

                //order points
                for (int i = 0; i < 6; i++) {
                    int begin = i * 3;
                    int end = begin + 2;

                    for (int j = end; j > begin; j--) {
                        for (int k = begin; k < j; k++) {
                            if (getX(contours.get(k)) > getX(contours.get(k + 1))) {
                                MatOfPoint temp = contours.get(k);
                                contours.set(k, contours.get(k + 1));
                                contours.set(k + 1, temp);
                            }
                        }
                    }

                }

                //mark point
                int index = 0;
                for (MatOfPoint con : contours) {
                    int cX = getX(con);
                    int cY = getY(con);
                    double[] hsv = src.get(cY, cX) == null ? new double[0] : src.get(cY, cX);
                    Color_Identify colorIdentify = new Color_Identify((int) hsv[0], (int) hsv[1], (int) hsv[2]);
                    //update status
                    Status[index / 3][index % 3] = colorIdentify.getColor();
                    if (hsv.length > 0) {
                        Imgproc.circle(src, new Point(cX, cY), 7, new Scalar((int) hsv[0], (int) hsv[1], (int) hsv[2]), -1);
                    } else {
                        Imgproc.circle(src, new Point(cX, cY), 7, new Scalar(0, 0, 0), -1);
                    }
                    Imgproc.putText(src, String.valueOf(index++), new Point(cX - 20, cY - 20),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

                }
            }

            //change mat to bitmap
            Utils.matToBitmap(src, bitmap);
            //release frame and source
            yuvtomat.release();
            image.close();

            //preview on ui thread
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    //show image
                    ImageView imageView = (ImageView) findViewById(R.id.grayView);
                    int h1 = imageView.getWidth();
                    int h2 = bitmap.getWidth();
                    float scale = h1 * 1.0f / h2;
                    Matrix matrix = new Matrix();
                    matrix.setScale(scale, scale);
                    imageView.setImageBitmap(bitmap);
                    imageView.setImageMatrix(matrix);

                    //show status of cube
                    if (isRestoring && !cube.isRestored()) {
                        endTime = new Date().getTime();
                        TextView timeView = (TextView) findViewById(R.id.showTime);
                        timeView.setText(String.valueOf(toTime(endTime - beginTime)));
                        TextView statusView = (TextView) findViewById(R.id.cubeStatus);
                        statusView.setText("复原中...");
                    } else if (cube.isRestored()) {
                        TextView timeView = (TextView) findViewById(R.id.showTime);
                        timeView.setText(String.valueOf(toTime(endTime - beginTime)));
                        TextView statusView = (TextView) findViewById(R.id.cubeStatus);
                        statusView.setText("已复原!!!");
                    } else {
                        endTime = beginTime;
                        TextView timeView = (TextView) findViewById(R.id.showTime);
                        timeView.setText(String.valueOf(toTime(endTime - beginTime)));
                        TextView statusView = (TextView) findViewById(R.id.cubeStatus);
                        statusView.setText("正在打乱...");
                    }
                    //show color
                    if (contours.size() == 18) {
                        try {
                            String moving = cube.resultMoving(Status);
                            showColor();

                            if (moving.equals("unstable")) {
                                return;
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            throw new RuntimeException(e);

                        }
                    }
                }
            });
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setContentView(R.layout.main_layout);

        //keep window on
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //check permission
        checkStoragePermission(this);
        if (allCameraPermissionsGranted()) {
            startCamera();
        } else {
            requestCameraPermissions();
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        //create a thread for camera
        cameraExecutor = Executors.newSingleThreadExecutor();

        scrambleButton = (Button) findViewById(R.id.beginScramble);
        restoreButton = (Button) findViewById(R.id.beginRestore);

        scrambleButton.setEnabled(true);
        restoreButton.setEnabled(false);

        //button for scrambling
        scrambleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRestoring = false;
                scrambleButton.setEnabled(false);
                restoreButton.setEnabled(true);
            }
        });

        //button for restoring
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRestoring = true;
                beginTime = new Date().getTime();
                endTime = beginTime;
                scrambleButton.setEnabled(true);
                restoreButton.setEnabled(false);
            }
        });

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    //judge camera permissions
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

                    //get ImageAnalyzer
                    ImageAnalysis analyzer = new ImageAnalysis.Builder().build();
                    analyzer.setAnalyzer(cameraExecutor, new mAnalyzer());

                    //get back camera
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    //rebind camera
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, analyzer);
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

    //request camera permission
    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, allPermissions, REQUEST_CODE);
    }

    //request permission callback
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

    //get pixel x
    public int getX(MatOfPoint point) {
        Moments moment = Imgproc.moments(point);
        return (int) (moment.m10 / moment.m00);
    }

    //get pixel y
    public int getY(MatOfPoint point) {
        Moments moment = Imgproc.moments(point);
        return (int) (moment.m01 / moment.m00);
    }

    //show color
    void showColor() throws NoSuchFieldException, IllegalAccessException {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                String index = "row" + i + "col" + j;
                int view_id = R.id.class.getField(index).getInt(null);
                TextView temp = (TextView) findViewById(view_id);
                String color = Status[i][j];
                if (color.equals("r")) {
                    temp.setTextColor(Color.RED);
                } else if (color.equals("w")) {
                    temp.setTextColor(Color.WHITE);
                } else if (color.equals("b")) {
                    temp.setTextColor(Color.BLUE);
                } else if (color.equals("o")) {
                    temp.setTextColor(Color.parseColor("#e87000"));
                } else if (color.equals("g")) {
                    temp.setTextColor(Color.GREEN);
                } else {
                    temp.setTextColor(Color.YELLOW);
                }
            }
        }
    }

    //show time
    String toTime(long time) {
        long seconds = (long) time / 1000;
        long second = seconds % 60;
        long minutes = (long) seconds / 60;
        long minute = minutes % 60;
        long hour = (long) minutes / 60;
        return hour + ":" + minute + ":" + second;
    }
}