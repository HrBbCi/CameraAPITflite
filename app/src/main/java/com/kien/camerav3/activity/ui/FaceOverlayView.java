package com.kien.camerav3.activity.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.kien.camerav3.R;
import com.kien.camerav3.utils.Classifier;

import java.text.DecimalFormat;
import java.util.List;


public class FaceOverlayView extends View {

    private Paint mPaint;
    private Paint mTextPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private int previewWidth;
    private int previewHeight;
    private List<Classifier.Recognition> mFaces;
    private double fps;
    private boolean isFront = false;
    private static final float THRESHOLD = 0.8f;
    private Bitmap mBitmap;
    private double scale;
    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    private void initialize() {
        // We want a green box around the face:
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int stroke = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(stroke);
        mPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, metrics);
        mTextPaint.setTextSize(size);
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setStyle(Paint.Style.FILL);
    }

    public void setFPS(double fps) {
        this.fps = fps;
    }

    public void setFaces(List<Classifier.Recognition> faces) {
        mFaces = faces;
        invalidate();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    private double ratio(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double ratio = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        return ratio;
    }

    private void drawFaceBox(Canvas canvas, double scale) {
        TypedValue tv = new TypedValue();
        int actionBarHeight = 0;
        if (getContext().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        canvas.save();
        for (Classifier.Recognition face : mFaces) {
            RectF location = face.getLocation();
            if (location != null && face.getConfidence() >= THRESHOLD) {
                RectF rectF = new RectF();
                rectF.set(new RectF(location.left * (float) scale, location.top * (float) scale + actionBarHeight
                        , location.right * (float) scale, location.bottom * (float) scale + actionBarHeight));
//                rectF.set(new RectF(location.left * (float) scale , location.top * (float) scale
//                        , location.right * (float) scale, location.bottom * (float) scale));
//                if (isFront) {
//                    float left = rectF.left;
//                    float right = rectF.right;
//                    rectF.left = (getWidth() - right);
//                    rectF.right = (getWidth() - left);
//                }
                canvas.drawRect(rectF, mPaint);
                canvas.drawText("ID " + face.getId(), rectF.left, rectF.bottom + mTextPaint.getTextSize(), mTextPaint);
                canvas.drawText("" + face.getConfidence(), rectF.left, rectF.bottom + mTextPaint.getTextSize() * 2, mTextPaint);
            }
        }
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null && mFaces.size() > 0) {
            double scale = getScale();
            drawFaceBox(canvas, scale);
        }
        DecimalFormat df2 = new DecimalFormat(".##");
        canvas.drawText("Detected_Frame/s: " + df2.format(fps) + " @ " + previewWidth + "x" + previewHeight, mTextPaint.getTextSize(), mTextPaint.getTextSize(), mTextPaint);
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public void setFront(boolean front) {
        isFront = front;
    }
}