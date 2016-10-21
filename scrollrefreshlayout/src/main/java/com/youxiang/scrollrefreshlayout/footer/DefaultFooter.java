package com.youxiang.scrollrefreshlayout.footer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.youxiang.scrollrefreshlayout.IFooter;

/**********************************************************
 * @文件名称：
 * @文件作者：yangyouxiang
 * @创建时间：2016/10/21 11:44
 * @文件描述：
 * @修改历史：2016/10/21 创建初始版本
 **********************************************************/

public class DefaultFooter extends FrameLayout implements IFooter {
    public DefaultFooter(Context context) {
        this(context, null);
    }

    public DefaultFooter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultFooter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View get() {
        return this;
    }
}
