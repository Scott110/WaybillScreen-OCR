package cn.bingoogolapple.qrcode.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by scott on 3/19/2018.
 */
public class TessEngine {

    static final String TAG = "DBG_" + TessEngine.class.getName();

    private Context context;

    private TessEngine(Context context) {
        this.context = context;
    }

    public static TessEngine Generate(Context context) {
        return new TessEngine(context);
    }

    public String detectText(Bitmap bitmap) {
        Log.d(TAG, "Initialization of TessBaseApi");
        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        tessBaseAPI.setDebug(true);
        boolean isInit = tessBaseAPI.init(path, "num");
        Log.d(TAG, "init success: " + isInit);
        // 白名单
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        // 黑名单
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        Log.d(TAG, "Ended initialization of TessEngine");
        tessBaseAPI.setImage(bitmap);
        String inspection = tessBaseAPI.getHOCRText(0);
        tessBaseAPI.end();
        System.gc();
        return getTelNum(inspection);
    }


    private static Pattern pattern = Pattern.compile("(1|861)(3|5|7|8)\\d{9}$*");

    public static String getTelNum(String sParam) {
        if (TextUtils.isEmpty(sParam)) {
            return "";
        }

        Matcher matcher = pattern.matcher(sParam);
        StringBuilder bf = new StringBuilder();
        while (matcher.find()) {
            bf.append(matcher.group()).append(",");
        }
        int len = bf.length();
        if (len > 0) {
            bf.deleteCharAt(len - 1);
        }
        Log.d(TAG, "识别的内容: " + bf.toString());
        return bf.toString();
    }

}
