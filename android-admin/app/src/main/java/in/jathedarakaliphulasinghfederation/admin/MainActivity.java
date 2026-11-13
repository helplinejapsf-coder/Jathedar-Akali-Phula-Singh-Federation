package in.jathedarakaliphulasinghfederation.admin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.Toast;
import android.view.ViewGroup;

public class MainActivity extends Activity {
    private static final String ADMIN_URL = "https://jathedarakaliphulasinghfederation.in/admin.html";
    private static final String SITE_HOST = "jathedarakaliphulasinghfederation.in";
    private static final int FILE_CHOOSER_REQUEST = 4107;
    private FrameLayout root;
    private SwipeRefreshLayout swipeRefresh;
    private WebView webView;
    private WebView popupWebView;
    private ValueCallback<Uri[]> uploadCallback;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        swipeRefresh = new SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_dark, android.R.color.holo_orange_dark);
        webView = new WebView(this);
        swipeRefresh.addView(webView, new SwipeRefreshLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        root.addView(swipeRefresh, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
        configureWebView(webView, false);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            // Drop stale WebView files so the app fetches the latest website
            // pages (including appointment letter and print-button fixes).
            webView.clearCache(true);
            webView.loadUrl(ADMIN_URL);
        }
    }

    private void configureWebView(WebView view, boolean isPrintWindow) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // Keep the website at its normal responsive mobile size inside the full app screen.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(100);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        view.addJavascriptInterface(new PrintBridge(view), "AndroidPrint");
        view.setBackgroundColor(android.graphics.Color.WHITE);
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("about".equalsIgnoreCase(uri.getScheme())) return false;
                if ("https".equalsIgnoreCase(uri.getScheme()) && SITE_HOST.equalsIgnoreCase(uri.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Cannot open this link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override public void onPageFinished(WebView v, String url) {
                super.onPageFinished(v, url);
                if (v == webView && swipeRefresh != null) swipeRefresh.setRefreshing(false);
                // Reinstall native print handling after every document load.
                v.evaluateJavascript(
                    "window.print=function(){if(window.AndroidPrint){window.AndroidPrint.printPage();}" +
                    "else{alert('Print service is unavailable. Please update the JAPSF Admin app.');}};",
                    null
                );
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest request,
                    android.webkit.WebResourceError error) {
                super.onReceivedError(v, request, error);
                if (v == webView && swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView source, boolean isDialog,
                    boolean isUserGesture, android.os.Message resultMsg) {
                if (popupWebView != null) {
                    root.removeView(popupWebView);
                    popupWebView.destroy();
                }
                WebView child = new WebView(MainActivity.this);
                popupWebView = child;
                configureWebView(child, true);
                child.setFocusable(true);
                child.setFocusableInTouchMode(true);
                root.addView(child, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                root.bringChildToFront(child);
                child.requestFocus();
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                return true;
            }

            @Override public void onCloseWindow(WebView window) {
                if (window == popupWebView) {
                    root.removeView(window);
                    window.destroy();
                    popupWebView = null;
                }
            }

            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST); }
                catch (ActivityNotFoundException e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker is available", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    private class PrintBridge {
        private final WebView source;
        PrintBridge(WebView source) { this.source = source; }

        @JavascriptInterface public void printPage() {
            runOnUiThread(() -> {
                try {
                    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    String pageTitle = source.getTitle();
                    String jobName = (pageTitle == null || pageTitle.trim().isEmpty())
                            ? "JAPSF Document" : pageTitle;
                    printManager.print(jobName, source.createPrintDocumentAdapter(jobName),
                            new PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open PDF/Print. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && uploadCallback != null) {
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override public void onBackPressed() {
        if (popupWebView != null) {
            root.removeView(popupWebView);
            popupWebView.destroy();
            popupWebView = null;
        } else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        if (popupWebView != null) popupWebView.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
