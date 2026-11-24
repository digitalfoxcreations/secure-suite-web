package com.digitalfox.securev3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.content.Intent;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.DownloadManager;
import android.os.Environment;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.OutputStream;
import java.net.URLDecoder;

public class MainActivity extends Activity {
    private static final String HOME = "https://digitalfoxcreations.github.io/secure-suite-web/";
    private static final int FILE_CHOOSER = 41;
    private WebView web;
    private ValueCallback<Uri[]> uploadCallback;
    private FrameLayout root;
    private View splashOverlay;
    private View offlineOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean pageLoaded = false;
    private long splashShownAt = 0;
    private static final long MIN_SPLASH_MS = 3000;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(16,42,67));
        getWindow().setNavigationBarColor(Color.BLACK);
        root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.setSaveEnabled(true);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(-1, -1);
        root.addView(web, webParams);
        splashOverlay = createSplash();
        root.addView(splashOverlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSaveFormData(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new DownloadBridge(), "SecureV3Native");
        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (Exception e) { uploadCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals("digitalfoxcreations.github.io") || host.endsWith(".supabase.co") || host.equals("cdn.jsdelivr.net"))) return false;
                if ("https".equals(uri.getScheme()) && host != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri)); return true;
                }
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                pageLoaded = false;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageLoaded = true;
                handler.removeCallbacks(loadTimeout);
                long remaining = Math.max(0, MIN_SPLASH_MS - (SystemClock.elapsedRealtime() - splashShownAt));
                handler.postDelayed(() -> {
                    if (pageLoaded) hideOverlay(splashOverlay);
                }, remaining);
                hideOverlay(offlineOverlay);
                injectBlobDownloadSupport();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) showOffline();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                super.onReceivedHttpError(view, request, response);
                if (request.isForMainFrame() && response.getStatusCode() >= 500) showOffline();
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler sslHandler, android.net.http.SslError error) {
                sslHandler.cancel();
                showOffline();
            }
        });

        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { Toast.makeText(this, "Unable to download file", Toast.LENGTH_LONG).show(); }
        });

        if (hasInternet()) loadHome();
        else showOffline();
    }

    private boolean hasInternet() {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) { return false; }
    }

    private void loadHome() {
        hideOverlay(offlineOverlay);
        showOverlay(splashOverlay);
        splashShownAt = SystemClock.elapsedRealtime();
        pageLoaded = false;
        handler.removeCallbacks(loadTimeout);
        handler.postDelayed(loadTimeout, 20000);
        if (web.getUrl() == null) web.loadUrl(HOME); else web.reload();
    }

    private final Runnable loadTimeout = () -> {
        if (!pageLoaded) showOffline();
    };

    private View createSplash() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(32), dp(32), dp(32), dp(32));
        panel.setBackgroundColor(Color.rgb(16,42,67));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(logo, new LinearLayout.LayoutParams(dp(132), dp(132)));
        TextView name = new TextView(this);
        name.setText("Secure V3");
        name.setTextColor(Color.WHITE);
        name.setTextSize(27);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(18);
        panel.addView(name, nameParams);
        TextView sub = new TextView(this);
        sub.setText("SECURE OPERATIONS");
        sub.setTextColor(Color.rgb(170,198,215));
        sub.setTextSize(11);
        sub.setLetterSpacing(0.16f);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        panel.addView(sub, subParams);
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        progressParams.topMargin = dp(34);
        panel.addView(progress, progressParams);
        TextView loading = new TextView(this);
        loading.setText("Loading your secure workspace…");
        loading.setTextColor(Color.rgb(205,220,230));
        loading.setTextSize(13);
        loading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(-1, -2);
        loadingParams.topMargin = dp(12);
        panel.addView(loading, loadingParams);
        return panel;
    }

    private void showOffline() {
        handler.removeCallbacks(loadTimeout);
        hideOverlay(splashOverlay);
        if (offlineOverlay == null) offlineOverlay = createOffline();
        if (offlineOverlay.getParent() == null) root.addView(offlineOverlay, new FrameLayout.LayoutParams(-1, -1));
        showOverlay(offlineOverlay);
    }

    private View createOffline() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(30), dp(32), dp(30), dp(32));
        panel.setBackgroundColor(Color.rgb(244,248,251));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(logo, new LinearLayout.LayoutParams(dp(104), dp(104)));
        TextView title = new TextView(this);
        title.setText("You're offline");
        title.setTextColor(Color.rgb(16,42,67));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(22);
        panel.addView(title, titleParams);
        TextView message = new TextView(this);
        message.setText("Secure V3 needs an internet connection to reach your workspace. Check your Wi-Fi or mobile data, then try again.");
        message.setTextColor(Color.rgb(78,96,112));
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(12);
        panel.addView(message, messageParams);
        Button retry = new Button(this);
        retry.setText("Retry connection");
        retry.setTextColor(Color.WHITE);
        retry.setAllCaps(false);
        retry.setTextSize(15);
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(Color.rgb(20,137,158));
        buttonBg.setCornerRadius(dp(12));
        retry.setBackground(buttonBg);
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(-1, dp(52));
        retryParams.topMargin = dp(26);
        panel.addView(retry, retryParams);
        retry.setOnClickListener(v -> {
            if (hasInternet()) loadHome();
            else Toast.makeText(MainActivity.this, "Still no internet connection. Please check your network.", Toast.LENGTH_LONG).show();
        });
        TextView footer = new TextView(this);
        footer.setText("Your saved login session stays on this device.");
        footer.setTextColor(Color.rgb(112,128,140));
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(18);
        panel.addView(footer, footerParams);
        return panel;
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void showOverlay(View v) { if (v != null) v.setVisibility(View.VISIBLE); }
    private void hideOverlay(View v) { if (v != null) v.setVisibility(View.GONE); }

    private void injectBlobDownloadSupport() {
        String js = "(function(){if(window.__secureV3Dl)return;window.__secureV3Dl=true;" +
          "document.addEventListener('click',function(e){var a=e.target.closest&&e.target.closest('a[download]');if(!a)return;" +
          "var u=a.href||'';if(u.indexOf('blob:')!==0)return;e.preventDefault();e.stopImmediatePropagation();" +
          "fetch(u).then(r=>r.blob()).then(b=>{var fr=new FileReader();fr.onloadend=function(){var x=String(fr.result).split(',')[1]||'';" +
          "window.SecureV3Native.saveBase64(x,b.type||'application/pdf',a.download||'SecureV3-download.pdf');};fr.readAsDataURL(b);})" +
          ".catch(()=>alert('Could not prepare the PDF download.'));},true);})();";
        web.evaluateJavascript(js, null);
    }

    private class DownloadBridge {
        @JavascriptInterface public String getAppVersion() { try { return MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0).versionName; } catch (Exception e) { return "unknown"; } }
        @JavascriptInterface public void saveBase64(String data, String mime, String name) {
            try {
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                String safeName = name == null ? "SecureV3-download.pdf" : name.replaceAll("[^A-Za-z0-9._-]", "_");
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                values.put(MediaStore.Downloads.MIME_TYPE, mime == null ? "application/octet-stream" : mime);
                if (Build.VERSION.SDK_INT >= 29) values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Storage unavailable");
                try (OutputStream out = getContentResolver().openOutputStream(uri)) { out.write(bytes); }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved to Downloads/" + safeName, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && uploadCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (offlineOverlay != null && offlineOverlay.getVisibility() == View.VISIBLE) {
            if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
        } else if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(loadTimeout);
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        if (web != null) { web.removeJavascriptInterface("SecureV3Native"); web.destroy(); }
        super.onDestroy();
    }
}
