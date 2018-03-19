package cn.bingoogolapple.qrcode.zbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.ocr.TessEngine;
import cn.bingoogolapple.qrcode.opencv.ImageProcUtil;

import static android.os.Environment.MEDIA_MOUNTED;

public class ZBarView extends QRCodeView {

    static {
        System.loadLibrary("iconv");
    }

    private ImageScanner mScanner;

    public ZBarView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ZBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupScanner();
    }

    public void setupScanner() {
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for (BarcodeFormat format : BarcodeFormat.ALL_FORMATS) {
            mScanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }


    @Override
    public String processData(byte[] data, int width, int height, boolean isRetry) {
        Log.d("TAG", "processData: 调用多长 ");
        String result = null;
        Image barcode = new Image(width, height, "Y800");
        Rect rect = mScanBoxView.getScanBoxAreaRect(height, getTop(), getLeft());
        if (rect != null && !isRetry && rect.left + rect.width() <= width && rect.top + rect.height() <= height) {
            barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());
        }

        //截取图片部分
        YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            //将帧数据转为图片（new Rect()是定义一个矩形提取区域，我这里是提取了整张图片，然后旋转90度后再才裁切出需要的区域，效率会较慢，实际使用的时候，照片默认横向的,可以直接计算逆向90°时，left、top的值，然后直接提取需要区域，提出来之后再压缩、旋转 速度会快一些）
            image.compressToJpeg(new Rect(rect.left, rect.top, rect.right, rect.bottom), 100, stream);
            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            bmp = new ImageProcUtil().textDetect(bmp, getContext());
            if (bmp == null) return null;
            saveImage(bmp);
            result = TessEngine.Generate(getContext()).detectText(bmp);
            return result;
        }
        //扫描条形码
        barcode.setData(data);
        result = processData(barcode);
        return result;
    }

    private String processData(Image barcode) {
        String result = null;
        if (mScanner.scanImage(barcode) != 0) {
            SymbolSet syms = mScanner.getResults();
            for (Symbol sym : syms) {
                String symData = sym.getData();
                if (!TextUtils.isEmpty(symData)) {
                    result = symData;
                    break;
                }
            }
        }
        return result;
    }


    private void saveImage(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            Calendar now = new GregorianCalendar();
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            String fileName = simpleDate.format(now.getTime());
            File file = new File(getPhotoCacheDir(getContext()) + fileName + ".jpg");
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