package com.youxiang.scrollrefreshlayout.header;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.youxiang.scrollrefreshlayout.IHeader;

/**********************************************************
 * @文件名称：
 * @文件作者：yangyouxiang
 * @创建时间：2016/10/21 11:42
 * @文件描述：
 * @修改历史：2016/10/21 创建初始版本
 **********************************************************/

public class DefaultHeader extends FrameLayout implements IHeader {
    public DefaultHeader(Context context) {
        this(context, null);
    }

    public DefaultHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View get() {
        return this;
    }
}
