package com.youxiang.scrollrefreshlayout;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import com.youxiang.scrollrefreshlayout.footer.DefaultFooter;
import com.youxiang.scrollrefreshlayout.header.DefaultHeader;
import com.youxiang.scrollrefreshlayout.uitls.ScrollUtil;

/**
 * Created by youxiang on 2016/10/20.
 */

public class ScrollRefreshLayout extends FrameLayout {

    private static final String TAG = ScrollRefreshLayout.class.getSimpleName();
    private static final int STATE_PULL_DOWN_TO_REFRESH = 0;
    private static final int STATE_PULL_UP_TO_LOAD_MORE = 1;
    protected boolean isRefreshing;
    protected boolean isLoadingMore;
    private int mState = STATE_PULL_DOWN_TO_REFRESH;
    private boolean mRefreshEnabled;
    private boolean mLoadMoreEnabled;
    private float mHeaderHeight;
    private float mFooterHeight;

    private float mMaxScrollHeight;

    private FrameLayout mHeaderLayout;
    private IHeader mHeader;
    private FrameLayout mFooterLayout;
    private IFooter mFooter;

    private float mOverScrollHeight;

    private PullListener mPullListener;

    private DecelerateInterpolator mInterpolator;

    private View mTarget;

    private int mTouchSlop;
    private float mVelocityY;
    GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isRefreshing && distanceY >= mTouchSlop) {
                tryCancelRefreshing();
            } else if (isLoadingMore && distanceY <= -mTouchSlop) {
                tryCancelLoadMore();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mVelocityY = velocityY;
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    });
    private float mMotionX;
    private float mMotionY;

    public ScrollRefreshLayout(Context context) {
        this(context, null);
    }

    public ScrollRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) return;
        checkChildLimit();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScrollRefreshLayout, defStyleAttr, 0);
        mRefreshEnabled = a.getBoolean(R.styleable.ScrollRefreshLayout_refreshEnabled, true);
        mLoadMoreEnabled = a.getBoolean(R.styleable.ScrollRefreshLayout_loadMoreEnabled, true);
        float defaultHeaderHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());
        mHeaderHeight = a.getDimension(R.styleable.ScrollRefreshLayout_headerHeight, defaultHeaderHeight);
        float defaultFooterHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());
        mFooterHeight = a.getDimension(R.styleable.ScrollRefreshLayout_footerHeight, defaultFooterHeight);
        float defaultOverScrollHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, context.getResources().getDisplayMetrics());
        mOverScrollHeight = a.getDimension(R.styleable.ScrollRefreshLayout_overScrollHeight, defaultOverScrollHeight);
        float defaultMaxScrollHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, context.getResources().getDisplayMetrics());
        mMaxScrollHeight = a.getDimension(R.styleable.ScrollRefreshLayout_maxScrollHeight, defaultMaxScrollHeight);

        a.recycle();

        mInterpolator = new DecelerateInterpolator();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setPullListener(new DefaultPullListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMotionX = ev.getX();
                mMotionY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - mMotionX;
                float dy = ev.getY() - mMotionY;
                if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > mTouchSlop) {
                    if (dy > 0 && !ScrollUtil.canChildScrollUp(mTarget) && mRefreshEnabled) {
                        mState = STATE_PULL_DOWN_TO_REFRESH;
                        return true;
                    }
                    if (dy < 0 && !ScrollUtil.canChildScrollDown(mTarget) && mLoadMoreEnabled) {
                        mState = STATE_PULL_UP_TO_LOAD_MORE;
                        return true;
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRefreshing || isLoadingMore) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - mMotionY;
                if (mState == STATE_PULL_DOWN_TO_REFRESH) {
                    dy = Math.min(Math.max(0, dy), mMaxScrollHeight);
                    if (mTarget != null) {
                        float offsetY = mInterpolator.getInterpolation(dy / mMaxScrollHeight) * dy / 2;
                        mTarget.setTranslationY(offsetY);
                        mHeaderLayout.getLayoutParams().height = (int) offsetY;
                        mHeaderLayout.requestLayout();
                        if (mPullListener != null) {
                            float fraction = offsetY / mHeaderHeight;
                            if (fraction < 1f) {
                                mPullListener.onPullingDownLightly(ScrollRefreshLayout.this, fraction);
                            } else {
                                mPullListener.onPullingDownDeeply(ScrollRefreshLayout.this, fraction);
                            }
                        }
                    }
                } else if (mState == STATE_PULL_UP_TO_LOAD_MORE) {
                    dy = Math.min(Math.max(0, Math.abs(dy)), mMaxScrollHeight);
                    if (mTarget != null) {
                        float offsetY = -mInterpolator.getInterpolation(dy / mMaxScrollHeight) * dy / 2;
                        mTarget.setTranslationY(offsetY);
                        mFooterLayout.getLayoutParams().height = (int) -offsetY;
                        mFooterLayout.requestLayout();

                        if (mPullListener != null) {
                            float fraction = offsetY / mFooterHeight;
                            if (fraction > -1f) {
                                mPullListener.onPullingUpLightly(ScrollRefreshLayout.this, fraction);
                            } else {
                                mPullListener.onPullingUpDeeply(ScrollRefreshLayout.this, fraction);
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mTarget != null) {
                    if (mState == STATE_PULL_DOWN_TO_REFRESH) {
                        if (mTarget.getTranslationY() >= mHeaderHeight) {
                            animChildView(mHeaderHeight);
                            isRefreshing = true;
                            if (mPullListener != null) {
                                mPullListener.onRefresh(ScrollRefreshLayout.this);
                            }
                        } else {
                            animChildView(0f);
                        }
                    } else if (mState == STATE_PULL_UP_TO_LOAD_MORE) {
                        if (Math.abs(mTarget.getTranslationY()) >= mFooterHeight) {
                            animChildView(-mFooterHeight);
                            isLoadingMore = true;
                            if (mPullListener != null) {
                                mPullListener.onLoadMore(ScrollRefreshLayout.this);
                            }
                        } else {
                            animChildView(0f);
                        }
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animChildView(float translationY) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(mTarget, "translationY", mTarget.getTranslationY(), translationY);
        anim.setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float height = Math.abs((float) animation.getAnimatedValue());
                if (mState == STATE_PULL_DOWN_TO_REFRESH) {
                    mHeaderLayout.getLayoutParams().height = (int) height;
                    mHeaderLayout.requestLayout();

                    if (mPullListener != null) {
                        mPullListener.onPullingDownReleasing(ScrollRefreshLayout.this, height / mHeaderHeight);
                    }
                } else if (mState == STATE_PULL_UP_TO_LOAD_MORE) {
                    mFooterLayout.getLayoutParams().height = (int) height;
                    mFooterLayout.requestLayout();

                    if (mPullListener != null) {
                        mPullListener.onPullingUpReleasing(ScrollRefreshLayout.this, height / mFooterHeight);
                    }
                }
            }
        });
        anim.start();
    }

    private void setPullListener(PullListener pullListener) {
        mPullListener = pullListener;
    }

    private void checkChildLimit() {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollRefreshLayout can host only one direct child");
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTarget = getChildAt(0);
        if (mTarget == null) {
            return;
        }
        initHeader();
        initFooter();
        resolveAppBarLayoutStateChange();
        resolveScrollEvent();
    }

    private void resolveScrollEvent() {
        mTarget.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
        if (mTarget instanceof RecyclerView) {
            ((RecyclerView) mTarget).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (!isRefreshing && !isLoadingMore && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mVelocityY >= 5000 && ScrollUtil.isRecyclerViewToTop((RecyclerView) mTarget)) {
                            animOverScrollTop();
                        } else if (mVelocityY <= -5000 && ScrollUtil.isRecyclerViewToBottom((RecyclerView) mTarget)) {
                            animOverScrollBottom();
                        }
                    }
                    super.onScrollStateChanged(recyclerView, newState);
                }
            });

        } else if (mTarget instanceof AbsListView) {
            ((AbsListView) mTarget).setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (!isRefreshing && !isLoadingMore) {
                        if (firstVisibleItem == 0 && mVelocityY >= 5000 && ScrollUtil.isAbsListViewToTop((AbsListView) mTarget)) {
                            animOverScrollTop();
                        } else if (((AbsListView) mTarget).getLastVisiblePosition() == totalItemCount - 1 && mVelocityY <= -5000 && ScrollUtil.isAbsListViewToBottom((AbsListView) mTarget)) {
                            animOverScrollBottom();
                        }
                    }
                }
            });

        } else {
            mTarget.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    int scrollY = mTarget.getScrollY();
                    if (!isRefreshing && !isLoadingMore) {
                        if (scrollY <= 0 && mVelocityY >= 5000) {
                            animOverScrollTop();
                        } else if (mVelocityY <= -5000) {
                            if (mTarget instanceof ViewGroup) {
                                if (mTarget instanceof WebView) {
                                    if (scrollY + mTarget.getHeight() >= ScrollUtil.getWebViewContentHeight((WebView) mTarget)) {
                                        animOverScrollBottom();
                                    }
                                } else {
                                    View child = ((ViewGroup) mTarget).getChildAt(0);
                                    if (child != null) {
                                        Log.e(TAG, "onScrollChanged " + scrollY + ", " + child.getHeight());
                                        if (child != null && scrollY + mTarget.getHeight() >= child.getHeight()) {
                                            animOverScrollBottom();
                                        }
                                    }
                                }
                            } else if (mTarget.getScrollY() >= mTarget.getHeight()) {
                                animOverScrollBottom();
                            }
                        }
                    }
                }
            });
        }
    }

    private void animOverScrollTop() {
        mVelocityY = 0;
        ObjectAnimator anim = ObjectAnimator.ofFloat(mTarget, "translationY", 0, mOverScrollHeight, 0).setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void animOverScrollBottom() {
        mVelocityY = 0;
        ObjectAnimator anim = ObjectAnimator.ofFloat(mTarget, "translationY", 0, -mOverScrollHeight, 0).setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void tryCancelLoadMore() {
        // // TODO: 2016/10/21
        finishLoadMore();
    }

    private void finishLoadMore() {
        isLoadingMore = false;
        if (mPullListener != null) {
            mPullListener.onFinishLoadMore();
        }
        if (mTarget != null) {
            animChildView(0f);
        }
    }

    private void tryCancelRefreshing() {
        // // TODO: 2016/10/21
        finishRefresh();
    }

    private void finishRefresh() {
        isRefreshing = false;
        if (mPullListener != null) {
            mPullListener.onFinishRefresh();
        }
        if (mTarget != null) {
            animChildView(0f);
        }
    }

    private void resolveAppBarLayoutStateChange() {
        ViewParent p = getParent();
        while (p != null && !(p instanceof CoordinatorLayout)) {
            p = p.getParent();
        }
        if (p != null && p instanceof CoordinatorLayout) {
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) p;
            final int childCount = coordinatorLayout.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                View child = coordinatorLayout.getChildAt(i);
                if (child instanceof AppBarLayout) {
                    ((AppBarLayout) child).addOnOffsetChangedListener(new AppBarLayoutStateChangeListener() {
                        @Override
                        public void onStateChanged(AppBarLayout appBarLayout, int appbarLayoutState) {
                            mAppBarState = appbarLayoutState;
                        }
                    });
                    break;
                }
            }
        }
    }

    private void initHeader() {
        if (mHeaderLayout == null) {
            mHeaderLayout = new FrameLayout(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mHeaderHeight);
            layoutParams.gravity = Gravity.TOP;
            mHeaderLayout.setLayoutParams(layoutParams);
            addView(mHeaderLayout, 0);// Add header at index 0
        }
        if (mHeader == null) {
            setHeader(new DefaultHeader(getContext()));
        }
    }

    private void setHeader(final IHeader header) {
        if (header != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mHeaderLayout.removeAllViewsInLayout();
                    mHeaderLayout.addView(header.get());
                }
            });
        }
    }

    private void initFooter() {
        if (mFooterLayout == null) {
            mFooterLayout = new FrameLayout(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mFooterHeight);
            layoutParams.gravity = Gravity.BOTTOM;
            mFooterLayout.setLayoutParams(layoutParams);
            addView(mFooterLayout, 0);// Add footer at index 0
        }
        if (mFooter == null) {
            setFooter(new DefaultFooter(getContext()));
        }
    }

    public void setFooter(final IFooter footer) {
        if (footer != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mFooterLayout.removeAllViewsInLayout();
                    mFooterLayout.addView(footer.get());
                }
            });
            mFooter = footer;
        }
    }

    public interface PullListener {

        void onPullingDownLightly(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onPullingDownDeeply(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onPullingUpLightly(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onPullingUpDeeply(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onPullingDownReleasing(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onPullingUpReleasing(ScrollRefreshLayout scrollRefreshLayout, float fraction);

        void onRefresh(ScrollRefreshLayout scrollRefreshLayout);

        void onLoadMore(ScrollRefreshLayout scrollRefreshLayout);

        void onFinishRefresh();

        void onFinishLoadMore();

    }

    private class DefaultPullListener implements PullListener {

        @Override
        public void onPullingDownLightly(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onPullingDownDeeply(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onPullingUpLightly(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onPullingUpDeeply(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onPullingDownReleasing(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onPullingUpReleasing(ScrollRefreshLayout scrollRefreshLayout, float fraction) {

        }

        @Override
        public void onRefresh(ScrollRefreshLayout scrollRefreshLayout) {
            finishRefresh();
        }

        @Override
        public void onLoadMore(ScrollRefreshLayout scrollRefreshLayout) {
            finishLoadMore();
        }

        @Override
        public void onFinishRefresh() {

        }

        @Override
        public void onFinishLoadMore() {

        }
    }
}
