package com.youxiang.scrollrefreshlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by youxiang on 2016/10/20.
 */

public class ScrollRefreshLayout extends FrameLayout {
    public ScrollRefreshLayout(Context context) {
        this(context, null);
    }

    public ScrollRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollRefreshLayout);
        a.recycle();
    }
}
