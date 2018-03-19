package cn.bingoogolapple.qrcode.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static android.os.Environment.MEDIA_MOUNTED;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

/**
 * author: heshantao
 * data: 2018/3/9.
 */

public class ImageProcUtil {
    private static final String TAG = "ImageProcUtil";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }


    public Bitmap textDetect(Bitmap bitmap, Context context) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Mat mRgba = new Mat(h, w, CvType.CV_8UC3);
        Mat mGray = new Mat(h, w, CvType.CV_8UC1);
        Mat mByte = new Mat();
        Utils.bitmapToMat(bitmap, mByte);
        // 灰度
        Imgproc.cvtColor(mByte, mGray, COLOR_BGR2GRAY);
        //高斯模糊
        Imgproc.GaussianBlur(mGray, mGray, new Size(3, 3), 0);

        Scalar CONTOUR_COLOR = new Scalar(255);
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint = new ArrayList<KeyPoint>();
        KeyPoint kpoint = new KeyPoint();
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;

        Scalar zeos = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();


        Rect rectan3 = null;
        Rect maxRect = new Rect();

        int imgsize = mRgba.height() * mRgba.width();

        FastFeatureDetector detector = FastFeatureDetector.create();
        detector.detect(mGray, keypoint);
        listpoint = keypoint.toList();

        Log.d(TAG, "textDetect: 扫描点" + listpoint.size());

        for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGray.width())
                rectanx2 = mGray.width() - rectanx1;
            if ((rectany1 + rectany2) > mGray.height())
                rectany2 = mGray.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            Mat roi = new Mat(mask, rectant);
            roi.setTo(CONTOUR_COLOR);

        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        Log.d(TAG, "textDetect: 轮廓块" + contour2.size());
        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100
                    || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);

            } else {
                if (rectan3.area() > maxRect.area()) {
                    maxRect = rectan3;
                }


            /*Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(),
                    BLUE);
*/
            }
        }


        if (maxRect.area() == 0) {
            return null;
        }

        Mat resultMat = mByte.submat(maxRect);

        // 灰度
        Imgproc.cvtColor(resultMat, resultMat, COLOR_BGR2GRAY);
        //二值化
        Imgproc.threshold(resultMat, resultMat, 0, 255, Imgproc.THRESH_BINARY | THRESH_OTSU); // Edge map

        int resultKernelSize = 0;
        Mat resultKernelMat = Imgproc.getStructuringElement(MORPH_RECT, new Size(2 * resultKernelSize + 1, 2 * resultKernelSize + 1));
        //膨胀
        Imgproc.dilate(resultMat, resultMat, resultKernelMat);
        //腐蚀
        Imgproc.erode(resultMat, resultMat, resultKernelMat);

        Bitmap bmp = Bitmap.createBitmap(resultMat.width(), resultMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, bmp);
        return bmp;

    }




    private void saveImage(Bitmap bitmap, Context context) {
        if (bitmap == null) return;
        Log.d("TAG", "存储图片");
        try {
            Calendar now = new GregorianCalendar();
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String fileName = simpleDate.format(now.getTime());
            File file = new File(getPhotoCacheDir(context) + "/" + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    //SD卡是否存在

    public static boolean isSDCardExsit() {
        String state = Environment.getExternalStorageState();
        if (state == null) return false;
        return MEDIA_MOUNTED.equals(state);
    }


    //压缩图片存储位置
    public static File getPhotoCacheDir(Context context) {
        return getPhotoCacheDir(context, "scnner_disk_cache");
    }

    //压缩图片存储位置
    public static File getPhotoCacheDir(Context context, String cacheName) {
        String cachePath = null;
        if (isSDCardExsit()) {
            cachePath = context.getExternalFilesDir(cacheName).getPath();
        } else {
            cachePath = context.getFilesDir().getPath();
        }
        File cacheDir = new File(cachePath);
        if (cacheDir != null) {
            if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory())) {
                // File wasn't able to create a directory, or the result exists but not a directory
                return null;
            }
            return cacheDir;
        }
        return null;
    }

}

