package com.youxiang.scrollrefreshlayout;

import android.animation.ObjectAnimator;
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
    private static final int MSG_START_SCROLL = 0;
    private static final String TAG = ScrollRefreshLayout.class.getSimpleName();
    protected boolean isRefreshing;
    protected boolean isLoadingMore;
    private boolean mRefreshEnabled;
    private boolean mLoadMoreEnabled;
    private float mHeaderHeight;
    private float mFooterHeight;
    private FrameLayout mHeaderLayout;
    private IHeader mHeader;
    private FrameLayout mFooterLayout;
    private IFooter mFooter;
    private float mMaxScrollHeaderLength;
    private float mMaxScrollFooterLength;
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
                tryCancelLoadingMore();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mVelocityY = velocityY;
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    });
    private float mOverScrollHeight;
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
        a.recycle();

        mMaxScrollHeaderLength = 4 * mHeaderHeight;
        mMaxScrollFooterLength = 4 * mFooterHeight;
        mInterpolator = new DecelerateInterpolator();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setPullListener(new DefaultPullListener());
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

    private void tryCancelLoadingMore() {
        // // TODO: 2016/10/21
    }

    private void tryCancelRefreshing() {
        // // TODO: 2016/10/21  
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

    }

    private class DefaultPullListener implements PullListener {

    }
}
