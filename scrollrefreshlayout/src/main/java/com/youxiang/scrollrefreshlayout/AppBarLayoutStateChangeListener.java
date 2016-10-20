package com.youxiang.scrollrefreshlayout;

import android.support.design.widget.AppBarLayout;

/**
 * Created by youxiang on 2016/10/20.
 */

public abstract class AppBarLayoutStateChangeListener implements AppBarLayout.OnOffsetChangedListener {
    public static final int STATE_COLLAPSED = 0;
    public static final int STATE_INTERMEDIATE = 1;
    public static final int STATE_EXPANDED = 2;
    public int mAppBarState = STATE_INTERMEDIATE;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (mAppBarState != STATE_EXPANDED) {
                onStateChanged(appBarLayout, STATE_EXPANDED);
            }
            mAppBarState = STATE_EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (mAppBarState != STATE_COLLAPSED) {
                onStateChanged(appBarLayout, STATE_COLLAPSED);
            }
            mAppBarState = STATE_COLLAPSED;
        } else {
            if (mAppBarState != STATE_INTERMEDIATE) {
                onStateChanged(appBarLayout, STATE_INTERMEDIATE);
            }
            mAppBarState = STATE_INTERMEDIATE;
        }
    }

    public abstract void onStateChanged(AppBarLayout appBarLayout, int appbarLayoutState);
}
