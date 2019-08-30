package com.kien.camerav3.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;


public class ImageUtils {

    public static Bitmap scaleImage(Bitmap bitmap, int maxSize) {
        int outWidth;
        int outHeight;
        int inWidth = bitmap.getWidth();
        int inHeight = bitmap.getHeight();
        if (inWidth > inHeight) {
            outWidth = maxSize;
            outHeight = (inHeight * maxSize) / inWidth;
        } else {
            outHeight = maxSize;
            outWidth = (inWidth * maxSize) / inHeight;
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
        return resizedBitmap;
    }

    //Rotate Bitmap
    public final static Bitmap rotate(Bitmap b, float degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2,
                    (float) b.getHeight() / 2);

            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                    b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }

        }
        return b;
    }


    public static Bitmap cropBitmap(Bitmap bitmap, RectF rect) {
        int w = (int) (rect.right - rect.left);
        int h = (int) (rect.bottom - rect.top);
        Bitmap ret = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        bitmap.recycle();
        return ret;
    }


    public static Bitmap cropFace(Classifier.Recognition face, Bitmap bitmap, int rotate) {
        Bitmap bmp;
        RectF location = face.getLocation();
        Bitmap.Config config = Bitmap.Config.RGB_565;
        if (bitmap.getConfig() != null) config = bitmap.getConfig();
        bmp = bitmap.copy(config, true);

        switch (rotate) {
            case 90:
                bmp = ImageUtils.rotate(bmp, 90);
                break;
            case 180:
                bmp = ImageUtils.rotate(bmp, 180);
                break;
            case 270:
                bmp = ImageUtils.rotate(bmp, 270);
                break;
        }

        bmp = ImageUtils.cropBitmap(bmp, location);
        return bmp;
    }
}
