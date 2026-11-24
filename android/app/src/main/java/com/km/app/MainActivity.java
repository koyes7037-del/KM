package com.km.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private FrameLayout root;
    private WebView mainView;
    private final ArrayList<WebView> popupViews = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new FrameLayout(this);
        setContentView(root);

        mainView = new WebView(this);
        root.addView(mainView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = mainView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        mainView.setWebViewClient(new WebViewClient());
        mainView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView view, boolean isDialog,
                    boolean isUserGesture, Message resultMsg) {
                WebView popup = new WebView(MainActivity.this);
                WebSettings popupSettings = popup.getSettings();
                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);
                popupSettings.setSupportMultipleWindows(false);
                popup.addJavascriptInterface(new InvoicePrintBridge(popup), "KMAndroid");
                popup.setWebViewClient(new WebViewClient());
                popup.setWebChromeClient(new WebChromeClient() {
                    @Override public void onCloseWindow(WebView window) {
                        closePopup(window);
                    }
                });
                root.addView(popup, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                popupViews.add(popup);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });
        mainView.loadUrl("file:///android_asset/km-pwa.html");
    }

    private final class InvoicePrintBridge {
        private final WebView target;
        InvoicePrintBridge(WebView target) { this.target = target; }

        @JavascriptInterface public void printInvoice() {
            runOnUiThread(() -> {
                PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (manager != null) {
                    manager.print("KM Invoice", target.createPrintDocumentAdapter("KM Invoice"),
                        new PrintAttributes.Builder().build());
                }
            });
        }
    }

    private void closePopup(WebView popup) {
        root.removeView(popup);
        popupViews.remove(popup);
        popup.destroy();
    }

    @Override public void onBackPressed() {
        if (!popupViews.isEmpty()) {
            closePopup(popupViews.get(popupViews.size() - 1));
            return;
        }
        if (mainView != null && mainView.canGoBack()) {
            mainView.goBack();
            return;
        }
        super.onBackPressed();
    }
}
