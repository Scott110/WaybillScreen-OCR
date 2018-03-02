package cn.bingoogolapple.qrcode.core;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Scott on 2018/2/26.
 */

public  abstract class IScanBoxView extends View {
    public IScanBoxView(Context context) {
        super(context);
    }

    public void initCustomAttrs(Context context, AttributeSet attrs) {

    }

    public boolean getIsBarcode(){
        return false;
    }

    public void setIsBarcode(boolean isBarcode) {

    }

    public Rect getScanBoxAreaRect(int previewHeight,int marginTop,int marginLeft) {
        return null;
    }
}
