package org.ccfcw.ert.command;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ERT Command: the CCF CW ERT Volunteer Management portal as an Android app.
 *
 * Only Volunteer Management (admin.html) runs inside the app. The Volunteer Portal,
 * Team Leader's Portal and start page open in the phone's browser, as before.
 * The portal itself lives on GitHub Pages and Firebase, so website updates reach
 * this app automatically without rebuilding it.
 */
public class MainActivity extends Activity {

    static final String HOST = "gnotesamsung7-hue.github.io";
    static final String ADMIN_PATH = "/ccf-cw-ert-portal/admin.html";
    static final String START_URL = "https://" + HOST + ADMIN_PATH + "?code=ccfcw-ert-52952";
    static final int FILE_REQUEST = 7;

    static final String OFFLINE_HTML =
        "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
      + "<body style='background:#090d16;color:#e2e8f0;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center'>"
      + "<div style='padding:24px'><div style='font-size:48px'>&#x1F4F6;</div>"
      + "<h2 style='margin:12px 0 6px'>No connection</h2>"
      + "<p style='color:#94a3b8;margin:0 0 20px'>ERT Command needs Wi-Fi or mobile data to reach the ERT records.</p>"
      + "<a href='" + START_URL + "' style='background:#dc2626;color:#fff;padding:12px 22px;border-radius:12px;text-decoration:none;font-weight:bold'>Try again</a>"
      + "</div></body></html>";

    WebView web;
    ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(0xFF090D16);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        web.addJavascriptInterface(new Bridge(), "ErtAndroid");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                boolean isWeb = "https".equals(scheme) || "http".equals(scheme);
                if (isWeb && HOST.equals(u.getHost()) && ADMIN_PATH.equals(u.getPath())) {
                    return false; // Volunteer Management stays inside the app
                }
                // Volunteer Portal, Team Leader's Portal, start page, phone numbers, Viber, other sites
                openExternal(u);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadDataWithBaseURL(null, OFFLINE_HTML, "text/html", "utf-8", null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Can't open the file picker", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(START_URL);
        }
    }

    void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** Lets the portal save backups and CSV exports into the phone's Downloads folder. */
    class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String text, String mime) {
            String message;
            try {
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(bytes);
                    }
                    message = "Saved to Downloads: " + name;
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File f = new File(dir, name);
                    try (FileOutputStream os = new FileOutputStream(f)) {
                        os.write(bytes);
                    }
                    message = "Saved: " + f.getAbsolutePath();
                }
            } catch (Exception e) {
                message = "Could not save the file: " + e.getMessage();
            }
            final String m = message;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
        }
    }
}
