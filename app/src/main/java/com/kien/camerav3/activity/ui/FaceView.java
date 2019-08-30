package com.kien.camerav3.activity.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.kien.camerav3.utils.Classifier;

import java.util.List;


public class FaceView extends View {
    private static final float ID_TEXT_SIZE = 60.0f;
    private Bitmap mBitmap;
    private List<Classifier.Recognition> mFaces;
    private static final float THRESHOLD = 0.5f;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the bitmap background and the associated face detections.
     */
    public void setContent(Bitmap bitmap, List<Classifier.Recognition> faces) {
        mBitmap = bitmap;
        mFaces = faces;
        invalidate();
    }

    /**
     * Draws the bitmap background and the associated face landmarks.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((mBitmap != null) && (mFaces != null)) {
            double scale = drawBitmap(canvas);
            drawFaceBox(canvas, scale);
        }
    }


    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        Rect destBounds = new Rect(0, 0, (int) (imageWidth * scale), (int) (imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }

    private void drawFaceBox(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int stroke = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        paint.setStrokeWidth(stroke);

        for (int i = 0; i < mFaces.size(); ++i) {
            Classifier.Recognition face = mFaces.get(i);
            RectF location = face.getLocation();
            if (location != null && mFaces.get(i).getConfidence() >= THRESHOLD) {
                RectF rectF = new RectF();
                rectF.set(new RectF(location.left * (float) scale, location.top * (float) scale
                        , location.right * (float) scale, location.bottom * (float) scale));
                canvas.drawRect(rectF, paint);
            }
        }
    }

    /**
     * Release Memory
     */
    public void reset() {
        if (mBitmap != null)
            mBitmap.recycle();
    }
}
