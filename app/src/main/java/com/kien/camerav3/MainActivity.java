package com.kien.camerav3;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kien.camerav3.activity.CameraRGBActivity;
import com.kien.camerav3.activity.PhotoDetectActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int REQUEST_CODE_CAMERA = 123;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        Button btnCameraRGB = findViewById(R.id.btnRGB);
        btnCameraRGB.setOnClickListener(v -> {
            int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(mContext, CameraRGBActivity.class);
                startActivity(intent);
            } else {
                requestCameraPermission(RC_HANDLE_CAMERA_PERM_RGB);
            }
        });

        Button btnPhoto = findViewById(R.id.btnImage);
        btnPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PhotoDetectActivity.class);
            startActivity(intent);
        });

        Button btnMyCam = findViewById(R.id.btnMyCam);
        btnMyCam.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_CAMERA
            );

        });
    }


    private void requestCameraPermission(final int RC_HANDLE_CAMERA_PERM) {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
            Intent intent = new Intent(mContext, CameraRGBActivity.class);
            startActivity(intent);
            return;
        }
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CODE_CAMERA);
            } else {
                Toast.makeText(this, "Ko dc phep", Toast.LENGTH_SHORT).show();
            }
        }
        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            Intent intent = new Intent(mContext, PhotoDetectActivity.class);
            intent.putExtra("bitmap",bitmap);
            startActivity(intent);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
