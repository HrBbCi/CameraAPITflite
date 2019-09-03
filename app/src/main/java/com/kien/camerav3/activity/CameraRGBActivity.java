package com.kien.camerav3.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kien.camerav3.R;
import com.kien.camerav3.activity.ui.FaceOverlayView;
import com.kien.camerav3.utils.CameraErrorCallback;
import com.kien.camerav3.utils.Classifier;
import com.kien.camerav3.utils.ImagePreviewAdapter;
import com.kien.camerav3.utils.ImageUtils;
import com.kien.camerav3.utils.TFLiteObjectDetectionAPIModel;
import com.kien.camerav3.utils.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraRGBActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    // Number of Cameras in device.
    private int numberOfCameras;
    public static final String TAG = CameraRGBActivity.class.getSimpleName();

    private Camera mCamera;
    private int cameraId = 0;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    private int previewWidth;
    private int previewHeight;

    // The surface view for the camera data
    private SurfaceView mView;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private static final int MAX_FACE = 10;
    private boolean isThreadWorking = false;
    private Handler handler;
    private FaceDetectThread detectThread = null;
    private int prevSettingWidth;
    private int prevSettingHeight;

    private List<Classifier.Recognition> fullResults;
    private Classifier.Recognition faces[];
    private Classifier.Recognition faces_previous[];
    private int Id = 0;
    private String BUNDLE_CAMERA_ID = "camera";

    //RecylerView face image
    private HashMap<Integer, Integer> facesCount = new HashMap<>();
    private RecyclerView recyclerView;
    private ImageButton ibCapture;
    private ImageView ivResultCap;
    private ImagePreviewAdapter imagePreviewAdapter;
    private ArrayList<Bitmap> facesBitmap;

    //Image Classifier
    private Classifier detector;
    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final float THRESHOLD = 0.8f;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detectv2.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private Camera.PictureCallback mPicture;
    private Bitmap bitmapCap;

    boolean isFront = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_rgb);

        mView = (SurfaceView) findViewById(R.id.surfaceView);
        ibCapture = findViewById(R.id.ibCapture);
        ivResultCap = findViewById(R.id.ivResultCap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Create and Start the OrientationListener:

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        ((LinearLayoutManager) mLayoutManager).setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        handler = new Handler();
        faces = new Classifier.Recognition[MAX_FACE];
        faces_previous = new Classifier.Recognition[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new Classifier.Recognition();
            faces_previous[i] = new Classifier.Recognition();
        }

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Face Detect RGB");
        initDataTensor();
        if (savedInstanceState != null)
            cameraId = savedInstanceState.getInt(BUNDLE_CAMERA_ID, 0);

        ibCapture.setOnClickListener(view -> mCamera.takePicture(null, null, getPictureCallback()));

        ivResultCap.setOnClickListener(view -> {
            final Dialog dialog = new Dialog(view.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_image);
            final ImageView ivDialog = dialog.findViewById(R.id.ivDialog);
            ivDialog.setImageBitmap(bitmapCap);
            dialog.show();
        });
    }

    private void initDataTensor() {
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (!isThreadWorking) {
            if (counter == 0)
                start = System.currentTimeMillis();

            isThreadWorking = true;
            waitForFdetThreadComplete();
            detectThread = new FaceDetectThread(handler, this);
            detectThread.setData(bytes);
            detectThread.start();
        }
    }

    private void waitForFdetThreadComplete() {
        if (detectThread == null) {
            return;
        }
        if (detectThread.isAlive()) {
            try {
                detectThread.join();
                detectThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    // fps detect face (not FPS of camera)
    long start, end;
    int counter = 0;
    double fps;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        resetData();

        //Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mFaceView.setFront(true);
            isFront = true;
        }

        try {
            mCamera.setPreviewDisplay(mView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }
        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(mErrorCallback);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.switchCam:
                if (numberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Switch Camera").setMessage("Your device have one camera").setNeutralButton("Close", null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }
                cameraId = (cameraId + 1) % numberOfCameras;
                recreate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        startPreview();
        isFront = false;
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetData();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = Util.getDisplayRotation(CameraRGBActivity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);
        mCamera.setDisplayOrientation(mDisplayOrientation);
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        Log.e(TAG, "previewWidth" + previewWidth);
        Log.e(TAG, "previewHeight" + previewHeight);
        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

        if (previewWidth / 4 > 360) {
            prevSettingWidth = 360;
            prevSettingHeight = 270;
        } else if (previewWidth / 4 > 320) {
            prevSettingWidth = 320;
            prevSettingHeight = 240;
        } else if (previewWidth / 4 > 240) {
            prevSettingWidth = 240;
            prevSettingHeight = 160;
        } else {
            prevSettingWidth = 160;
            prevSettingHeight = 120;
        }


        mFaceView.setPreviewWidth(previewWidth);
        mFaceView.setPreviewHeight(previewHeight);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void startPreview() {
        if (mCamera != null) {
            isFront = false;
            isThreadWorking = false;
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            counter = 0;
        }
    }

    /**
     * Do face detect in thread
     */
    private class FaceDetectThread extends Thread {
        private Handler handler;
        private byte[] data = null;
        private Context ctx;
        private Bitmap faceCroped;

        public FaceDetectThread(Handler handler, Context ctx) {
            this.ctx = ctx;
            this.handler = handler;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {
            float aspect = (float) previewHeight / (float) previewWidth;
            int w = prevSettingWidth;
            int h = (int) (prevSettingWidth * aspect);
            Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
            // face detection: first convert the image from NV21 to RGB_565
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
                    bitmap.getWidth(), bitmap.getHeight(), null);
            Rect rectImage = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            if (!yuv.compressToJpeg(rectImage, 100, baout)) {
                Log.e("CreateBitmap", "compressToJpeg failed");
            }

            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeStream(
                    new ByteArrayInputStream(baout.toByteArray()), null, bfo);

//            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);
            Bitmap bmp = ImageUtils.scaleImage(bitmap, TF_OD_API_INPUT_SIZE);
            float xScale = (float) previewWidth / (float) prevSettingWidth;
            float yScale = (float) previewHeight / (float) h;
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotate = mDisplayOrientation;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                if (rotate + 180 > 360) {
                    rotate = rotate - 180;
                } else {
                    rotate = rotate + 180;
                }
            }
            switch (rotate) {
                case 90:
                    bmp = ImageUtils.rotate(bmp, 90);
                    xScale = (float) previewHeight / bmp.getWidth();
                    yScale = (float) previewWidth / bmp.getHeight();
                    break;
                case 180:
                    bmp = ImageUtils.rotate(bmp, 180);
                    break;
                case 270:
                    bmp = ImageUtils.rotate(bmp, 270);
                    xScale = (float) previewHeight / (float) h;
                    yScale = (float) previewWidth / (float) prevSettingWidth;
                    break;
            }
            Log.d("IsFront", isFront+"");
//            if (isFront) {
//                bmp = ImageUtils.rotate(bmp, 270);
//            } else {
//                bmp = ImageUtils.rotate(bmp, 90);
//            }
            mFaceView.setmBitmap(bmp);
            mFaceView.setScale(Math.min(xScale, yScale));
            fullResults = detector.recognizeImage(bmp);
            Log.d("FULLL", fullResults.toString());
            for (final Classifier.Recognition faceResult : fullResults) {
                RectF location = faceResult.getLocation();
                float confidence = faceResult.getConfidence();
                if (location != null && confidence >= THRESHOLD) {
                    faceCroped = ImageUtils.cropFace(faceResult, bmp, rotate);
                    if (faceCroped != null) {
                        handler.post(() -> imagePreviewAdapter.add(faceCroped));
                    }
                }
                faceResult.setLocation(location);
            }
//            for (int i = 0; i < MAX_FACE; i++) {
//                Classifier.Recognition faceResult = fullResults.get(i);
//                if (faceResult == null) {
//                    faces[i].clear();
//                } else {
//                    float eyesDis = faceResult.getEyeDist() * xScale;
//                    RectF location = faceResult.getLocation();
//                    PointF mid = new PointF();
//                    mid.x *= xScale;
//                    mid.y *= yScale;
//                    Log.d("IID",mid.x+";"+mid.y);
////                    Rect rect = new Rect(
////                            (int) (mid.x - eyesDis * 1.20f),
////                            (int) (mid.y - eyesDis * 0.55f),
////                            (int) (mid.x + eyesDis * 1.20f),
////                            (int) (mid.y + eyesDis * 1.85f));
//
//                    float confidence = faceResult.getConfidence();
//                    int idFace = Id;
//
//                    if (location != null && confidence >= THRESHOLD ) {
//                        for (int j = 0; j < MAX_FACE; j++) {
//                            float eyesDisPre = faces_previous[j].getEyeDist();
//                            PointF midPre = new PointF();
//                            faces_previous[j].getMidPoint(mid);
//                            Log.d("ABBBBB", faces_previous[j].getMidEye()+"");
//                            RectF rectCheck = new RectF(
//                                    (midPre.x - eyesDisPre * 1.5f),
//                                    (midPre.y - eyesDisPre * 1.15f),
//                                    (midPre.x + eyesDisPre * 1.5f),
//                                    (midPre.y + eyesDisPre * 1.85f));
//                            if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - faces_previous[j].getTime()) < 1000) {
//                                idFace = faces_previous[j].getId();
//                                break;
//                            }
//                        }
//                        if (idFace == Id) Id++;
//
//                        faces[i].setFace(idFace, faceResult.getTitle(), eyesDis, confidence, mid, location, System.currentTimeMillis());
//                        faces_previous[i].set(faces[i].getId(), faces[i].getTitle(), faces[i].getEyeDist(), faces[i].getConfidence(),
//                                faces[i].getMidEye(), faces[i].getLocation(), faces[i].getTime());
//
//                        if (facesCount.get(idFace) == null) {
//                            Log.d("ABC", "ABC");
//                            facesCount.put(idFace, 0);
//                        } else {
//                            int count = facesCount.get(idFace) + 1;
//                            Log.d("ABCount", count + ";;");
//                            if (count <= 3)
//                                facesCount.put(idFace, count);
//                            // Crop Face to display in RecylerView
//                            if (count == 3) {
//                                Log.d("Count", count + "");
//                                faceCroped = ImageUtils.cropFace(faces[i], bitmap, rotate);
//                                if (faceCroped != null) {
//                                    handler.post(() -> imagePreviewAdapter.add(faceCroped));
//                                }
//                            }
//                        }
//                    }
//                }
//            }
            handler.post(() -> {
                //send face to FaceView to draw rect
                mFaceView.setFaces(fullResults);
                //calculate FPS
                end = System.currentTimeMillis();
                counter++;
                double time = (double) (end - start) / 1000;
                if (time != 0)
                    fps = counter / time;
                mFaceView.setFPS(fps);
                if (counter == (Integer.MAX_VALUE - 1000))
                    counter = 0;
                isThreadWorking = false;
            });
        }
    }

    /**
     * Release Memory
     */
    private void resetData() {
        if (imagePreviewAdapter == null) {
            facesBitmap = new ArrayList<>();
            imagePreviewAdapter = new ImagePreviewAdapter(CameraRGBActivity.this, facesBitmap, (v, position) -> {
                imagePreviewAdapter.setCheck(position);
                imagePreviewAdapter.notifyDataSetChanged();
            });
            recyclerView.setAdapter(imagePreviewAdapter);
        } else {
            imagePreviewAdapter.clearAll();
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                bitmapCap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmapCap = ImageUtils.rotate(bitmapCap, 90);
                Bitmap reBitmap = ImageUtils.scaleImage(bitmapCap, TF_OD_API_INPUT_SIZE);
                detectFace(reBitmap);
                ivResultCap.setImageBitmap(reBitmap);
                startPreview();
            }
        };
        return picture;
    }

    private void detectFace(Bitmap bitmap) {
        resetData();
        List<Classifier.Recognition> faces = detector.recognizeImage(bitmap);
        for (final Classifier.Recognition faceResult : faces) {
            final RectF location = faceResult.getLocation();
            if (location != null && faceResult.getConfidence() >= THRESHOLD) {
                Bitmap cropedFace = ImageUtils.cropFace(faceResult, bitmap, 0);
                if (cropedFace != null) {
                    imagePreviewAdapter.add(cropedFace);
                }
            }
        }
    }
}
