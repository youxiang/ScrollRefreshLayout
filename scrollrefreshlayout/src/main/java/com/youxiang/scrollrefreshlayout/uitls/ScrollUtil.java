package com.youxiang.scrollrefreshlayout.uitls;

import android.webkit.WebView;

/**********************************************************
 * @文件名称：
 * @文件作者：yangyouxiang
 * @创建时间：2016/10/21 14:43
 * @文件描述：
 * @修改历史：2016/10/21 创建初始版本
 **********************************************************/

public class ScrollUtil {
    public static boolean isWebViewScrollToBottom(WebView webView) {
        float contentHeight = getWebViewContentHeight(webView);
        if (contentHeight == webView.getHeight() + webView.getScrollY()) {
            return true;
        }
        return false;
    }

    public static float getWebViewContentHeight(WebView webView) {
        return webView.getContentHeight() * webView.getScale();

    }
}
