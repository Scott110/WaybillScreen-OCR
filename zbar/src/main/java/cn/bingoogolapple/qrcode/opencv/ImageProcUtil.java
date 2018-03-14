package cn.bingoogolapple.qrcode.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
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
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * author: heshantao
 * data: 2018/3/9.
 */

public class ImageProcUtil {
    private static final String TAG = "ImageProcUtil";
    //开操作（先膨胀后腐蚀）
    public static final String OPENVC_OPERATE_OPEN = "open";
    //闭操作（先腐蚀后膨胀）
    public static final String OPENVC_OPERATE_CLOSE = "close";

    private static final Scalar WHITE = new Scalar(255);
    private static final Scalar BLACK = new Scalar(0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar PURPLE = new Scalar(128, 0, 128);
    private static final Scalar DARK_RED = new Scalar(128, 0, 0);
    private static final Scalar DARK_GREEN = new Scalar(0, 128, 0);


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }


    //bitmap 灰度化
    public Bitmap procBmp2Gray(Bitmap bm) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Bitmap graybm = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);  //创建图像
        Utils.bitmapToMat(bm, rgbMat);//bitmap转RGB，常用
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Utils.matToBitmap(grayMat, graybm);
        return graybm;
    }


    //bitmap 二值化
    public Bitmap binaryzationBitmap(Bitmap bm) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Utils.bitmapToMat(bm, rgbMat);
        Imgproc.threshold(rgbMat, grayMat, 100, 255, THRESH_BINARY);
        Utils.matToBitmap(grayMat, bm);
        return bm;
    }


    /**
     * 开闭操作
     *
     * @param command
     * @param bitmap
     * @return
     */
    public Bitmap openOrClose(String command, Bitmap bitmap) {
        Boolean isOpen = OPENVC_OPERATE_OPEN.equals(command);
        Mat sSrc = new Mat();
        Mat sDst = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, sSrc);
        Mat sStrElement = Imgproc.getStructuringElement(MORPH_RECT,
                new Size(3, 3), new Point(1, 1));
        if (isOpen) {
            Imgproc.morphologyEx(sSrc, sDst, Imgproc.MORPH_OPEN, sStrElement);
        } else {
            Imgproc.morphologyEx(sSrc, sDst, Imgproc.MORPH_CLOSE, sStrElement);
        }
        org.opencv.android.Utils.matToBitmap(sDst, bitmap);
        sStrElement.release();
        sSrc.release();
        sDst.release();
        return bitmap;
    }


    public void test() {

        //Imgproc.findContours();
    }


    public void detectText(Bitmap bitmap, Context context) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Mat mGrey = new Mat(h, w, CvType.CV_8UC4);
        Mat mRgba = new Mat(h, w, CvType.CV_8UC4);
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;
        int imgsize = mGrey.height() * mGrey.width();
        Scalar zeos = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        // Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));

        Mat kernel = Imgproc.getStructuringElement(MORPH_RECT,
                new Size(3, 3), new Point(1, 1));

        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();
        Rect rectan3;
        FeatureDetector detector = FeatureDetector
                .create(FeatureDetector.MSER);
        detector.detect(mGrey, keypoint);
        listpoint = keypoint.toList();
       /* for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGrey.width())
                rectanx2 = mGrey.width() - rectanx1;
            if ((rectany1 + rectany2) > mGrey.height())
                rectany2 = mGrey.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(GREEN);
            } catch (Exception ex) {
                Log.d("mylog", "mat roi error " + ex.getMessage());
            }
        }*/

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat mIntermediateMat = new Mat();
        Bitmap bmp = null;

        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            try {
                Mat croppedPart;
                croppedPart = mIntermediateMat.submat(rectan3);
                bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedPart, bmp);
            } catch (Exception e) {
                Log.d("TAG", "cropped part data error " + e.getMessage());
            }

            saveImage(bmp, context);

        }

    }


    public Bitmap recognizeImage(Bitmap bmp, Context context) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        Mat srcMat = new Mat(h, w, CvType.CV_8UC4);
        Mat destMat = new Mat(h, w, CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, srcMat);
        // 灰度
        Imgproc.cvtColor(srcMat, destMat, COLOR_BGR2GRAY);
        //高斯模糊
        Imgproc.GaussianBlur(destMat, destMat, new Size(3, 3),0);
        // Process noisy, blur, and threshold to get black-white image
        destMat = processNoisy(destMat);
        //destMat = edgeDetect(destMat, context);

        Bitmap bitmap = Bitmap.createBitmap(destMat.width(), destMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destMat, bitmap);
        saveImage(bitmap, context);
        destMat.release();
        srcMat.release();
        return bitmap;
    }


    /**
     * Process noisy or blur image with simplest filters
     *
     * @param grayMat
     * @return
     */
    private Mat processNoisy(Mat grayMat) {
        //全局二值化
        Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        /*

        Mat aux = new Mat(grayMat.size(), CvType.CV_8UC1);
        Mat KERNEL_3X3   = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_OPEN, KERNEL_3X3);                   // Open
        Imgproc.morphologyEx(grayMat, aux, Imgproc.MORPH_CLOSE, KERNEL_3X3);                  // Close
        Core.addWeighted(grayMat, 0.5, aux, 0.5, 0, grayMat);                                     // Average
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_GRADIENT, KERNEL_3X3);               // Gradient
        Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU); // Edge map
*/

        int kernelSize = 1;
        Mat kernelMat = Imgproc.getStructuringElement(MORPH_RECT, new Size(2 * kernelSize + 1, 2 * kernelSize + 1), new Point(kernelSize, kernelSize));
        //膨胀
        Imgproc.dilate(grayMat, grayMat, kernelMat);
        //腐蚀
        Imgproc.erode(grayMat, grayMat, kernelMat);
        return grayMat;
    }


    private Mat edgeDetect(Mat mat, Context context) {
       /* List<MatOfPoint> list = new ArrayList<>();
        Imgproc.findContours(mat, list, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat resultImage = Mat.zeros(mat.size(), CvType.CV_8UC1);
        Log.d(TAG, "edgeDetect: 数量"+list.size());
        Imgproc.drawContours(resultImage, list, -1, new Scalar(0,0,255));
*/

        Mat mIntermediateMat = mat.clone();

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mat, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
       /* double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        Mat mRgba=new Mat();
        mRgba.create(mat.rows(), mat.cols(), CvType.CV_8UC3);
        //绘制检测到的轮廓
        Imgproc.drawContours(mRgba, contours, maxValIdx, new Scalar(0,255,0), 5);

*/


        Bitmap bmp = null;
        Rect rectan;
        Mat croppedPart;
        int edgeHeight = mIntermediateMat.height() / 3;
        int edgeWidth = mIntermediateMat.width() / 3;
        //int edgeWidth = 20;

        for (int ind = 0; ind < contours.size(); ind++) {
            Log.d("TAG", "多少块" + ind);
            rectan = Imgproc.boundingRect(contours.get(ind));
            Log.d("TAG", "截取的宽:" + rectan.width + "截取的高度：：" + rectan.height);
            if (rectan.height < edgeHeight || rectan.width < edgeWidth) {
                continue;
            }
            try {
                croppedPart = mIntermediateMat.submat(rectan);
                bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedPart, bmp);
            } catch (Exception e) {
                Log.d("TAG", "cropped part data error " + e.getMessage());
            }


            saveImage(bmp, context);
        }


        return null;
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

