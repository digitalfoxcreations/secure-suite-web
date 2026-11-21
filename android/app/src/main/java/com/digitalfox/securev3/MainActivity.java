package com.digitalfox.securev3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.widget.*;
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

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(16,42,67));
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        web = new WebView(this);
        web.setBackgroundColor(android.graphics.Color.WHITE);
        web.setSaveEnabled(true);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // Retains the Supabase login session on this device.
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
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectBlobDownloadSupport();
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
        web.loadUrl(HOME);
    }

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
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        if (web != null) { web.removeJavascriptInterface("SecureV3Native"); web.destroy(); }
        super.onDestroy();
    }
}
