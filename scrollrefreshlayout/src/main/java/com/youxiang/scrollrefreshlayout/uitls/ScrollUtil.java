package com.youxiang.scrollrefreshlayout.uitls;

import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.webkit.WebView;
import android.widget.AbsListView;

import java.lang.reflect.Field;

/**********************************************************
 * @文件名称：
 * @文件作者：yangyouxiang
 * @创建时间：2016/10/21 14:43
 * @文件描述：
 * @修改历史：2016/10/21 创建初始版本
 **********************************************************/

public class ScrollUtil {
    public static boolean isWebViewToBottom(WebView webView) {
        return webView != null && getWebViewContentHeight(webView) == webView.getHeight() + webView.getScrollY();
    }

    public static float getWebViewContentHeight(WebView webView) {
        return webView.getContentHeight() * webView.getScale();

    }

    public static boolean isRecyclerViewToTop(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null || manager.getItemCount() == 0) return true;
            if (manager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;

                int firstChildTop = 0;
                if (recyclerView.getChildCount() > 0) {
                    View firstVisibleChild = recyclerView.getChildAt(0);

                    if (firstVisibleChild != null && firstVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                        // item高度比recyclerView的高度还大
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            return !(ViewCompat.canScrollVertically(recyclerView, -1) || recyclerView.getScrollY() > 0);
                        } else {
                            return !ViewCompat.canScrollVertically(recyclerView, -1);
                        }
                    }

                    View firstChild = recyclerView.getChildAt(0);
                    RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) firstChild.getLayoutParams();
                    firstChildTop = firstChild.getTop() - lp.topMargin - recyclerView.getPaddingTop() - getRecyclerViewItemTopInset(lp);
                }

                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() < 1 && firstChildTop == 0) {
                    return true;
                }
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) manager;
                int[] out = staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(null);
                if (out[0] < 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过反射获取RecyclerView的item的topInset
     *
     * @param lp
     * @return
     */
    private static int getRecyclerViewItemTopInset(RecyclerView.LayoutParams lp) {
        try {
            Field f = RecyclerView.LayoutParams.class.getDeclaredField("mDecorInsets");
            f.setAccessible(true);
            Rect decorInsets = (Rect) f.get(lp);
            return decorInsets.top;
        } catch (Exception e) {
        }
        return 0;

    }

    public static boolean isRecyclerViewToBottom(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null || manager.getItemCount() == 0) return false;
            if (manager instanceof LinearLayoutManager) {
                View lastVisibleChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                if (lastVisibleChild != null && lastVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        return !(ViewCompat.canScrollVertically(recyclerView, 1) || recyclerView.getScaleY() < 0);
                    } else {
                        return !ViewCompat.canScrollVertically(recyclerView, 1);
                    }
                }
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == linearLayoutManager.getItemCount() - 1) {
                    return true;
                }
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;
                int[] out = layoutManager.findLastCompletelyVisibleItemPositions(null);
                int lastPosition = layoutManager.getItemCount() - 1;
                for (int position : out) {
                    if (position == lastPosition) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public static boolean isAbsListViewToTop(AbsListView absListView) {
        if (absListView != null) {
            if (absListView.getFirstVisiblePosition() != 0) return false;
            int firstChildTop = 0;
            if (absListView.getChildCount() > 0) {
                firstChildTop = absListView.getChildAt(0).getTop() - absListView.getPaddingTop();
            }
            return firstChildTop == 0;
        }
        return false;
    }

    public static boolean isAbsListViewToBottom(AbsListView absListView) {
        if (absListView != null && absListView.getAdapter() != null) {
            if (absListView.getLastVisiblePosition() != absListView.getAdapter().getCount() - 1)
                return false;
            if (absListView.getChildCount() > 0) {
                View lastChild = absListView.getChildAt(absListView.getChildCount() - 1);
                return lastChild.getBottom() == absListView.getMeasuredHeight();
            }
        }
        return false;
    }
}
