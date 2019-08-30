package com.kien.camerav3.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.List;

public interface Classifier {
    List<Recognition> recognizeImage(Bitmap bitmap);

    void enableStatLogging(final boolean debug);

    String getStatString();

    void close();

    void setNumThreads(int num_threads);

    void setUseNNAPI(boolean isChecked);

    public class Recognition {
        private int id;
        private String title;
        private float confidence;
        private float eyeDist;
        private PointF midEye;
        private RectF location;
        private long time;

        public Recognition(
                int id, String title, float confidence, float eyeDist, PointF midEye, RectF location, long time) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.eyeDist = eyeDist;
            this.midEye = midEye;
            this.location = location;
            time = System.currentTimeMillis();
        }

        public Recognition(
                int id, String title, float confidence, RectF location, long time) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
            time = System.currentTimeMillis();
        }

        public Recognition() {
            id = 0;
            title = "";
            confidence = 0.4f;
            eyeDist = 0.0f;
            midEye = new PointF(0.0f, 0.0f);
            location = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
            time = System.currentTimeMillis();
        }

        public void setFace(int id, String title, float confidence, float eyeDist, PointF midEye, RectF location, long time) {
            set(id, title, confidence, eyeDist, midEye, location, time);
        }

        public synchronized void set(int id, String title, float confidence, float eyeDist, PointF midEye, RectF location, long time) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.eyeDist = eyeDist;
            this.midEye.set(midEye);
            this.location = location;
            this.time = time;
        }

        public void clear() {
            set(0, "", 0.4f, 0.0f,
                    new PointF(0.0f, 0.0f), new RectF(0.0f, 0.0f, 0.0f, 0.0f)
                    , System.currentTimeMillis());
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        public PointF getMidEye() {
            return midEye;
        }

        public void setMidEye(PointF midEye) {
            this.midEye = midEye;
        }

        public float getEyeDist() {
            return eyeDist;
        }

        public void setEyeDist(float eyeDist) {
            this.eyeDist = eyeDist;
        }

        public void getMidPoint(PointF pt) {
            pt.set(midEye);
        }

        @Override
        public String toString() {
            return "[" + confidence * 100 + "%;" + location + "]";
        }
    }
}
