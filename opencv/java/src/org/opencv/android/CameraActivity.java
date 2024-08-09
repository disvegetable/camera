package org.opencv.android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.CAMERA;

public class CameraActivity extends Activity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return new ArrayList<CameraBridgeViewBase>();
    }

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(CAMERA);
            havePermission = false;
        }
        if (!permissions.isEmpty()) {
            havePermission = false;
        }
        if (havePermission) {
            onCameraPermissionGranted();
        } else {
            requestPermissions(permissions.toArray(new String[permissions.size()]), CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
