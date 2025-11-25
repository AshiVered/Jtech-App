package com.ashivered.aiv.jtech;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import android.app.DownloadManager;
import android.os.Environment;
import android.webkit.URLUtil;
import android.content.Context;
import org.mozilla.geckoview.WebResponse;
import android.webkit.CookieManager;

public class MainActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;
    private GeckoSession session;
    private GeckoView geckoView;
    private boolean canGoBack = false;
    private final Set<String> whitehosts = new HashSet<>();
    private final String whitelistUrl = "https://ashivered.github.io/SafeBrowserResources/jtech.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        geckoView = findViewById(R.id.geckoview);
        session = new GeckoSession();

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
                // בגרסאות החדשות המידע נמצא ב-headers
                String contentType = response.headers.get("Content-Type");
                String contentDisposition = response.headers.get("Content-Disposition");

                // קריאה לפונקציית ההורדה שיצרנו מקודם
                downloadFile(response.uri, contentDisposition, contentType);
            }
        });

        // ניווט
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @NonNull
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session,
                                                          @NonNull LoadRequest request) {
                try {
                    URI uri = new URI(request.uri);
                    String host = uri.getHost();
                    if (host != null && isHostAllowed(host)) {
                        return GeckoResult.allow();
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "This site is blocked", Toast.LENGTH_SHORT).show());
                        return GeckoResult.deny();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return GeckoResult.deny();
                }
            }

            @Override
            public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBackNow) {
                canGoBack = canGoBackNow;
            }
        });

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }

        session.open(sRuntime);
        geckoView.setSession(session);

        // טען את רשימת ההיתרים
        loadWhitelist(() -> runOnUiThread(() ->
                session.loadUri("https://forums.jtechforums.org/")
        ));
    }
    private void downloadFile(String url, String contentDisposition, String mimeType) {
        // 1. בדיקה שזו כתובת תקינה (DownloadManager לא תומך ב-blob)
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            Toast.makeText(this, "הורדת קבצים מסוג BLOB עדיין לא נתמכת", Toast.LENGTH_LONG).show();
            Log.e("GeckoDownload", "Cannot download blob/data url with DownloadManager");
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            // 2. הגדרת סוג הקובץ
            request.setMimeType(mimeType);

            // 3. הוספת עוגיות (Cookies) כדי לאפשר הורדה מאתרים מוגנים
            // הערה: ב-GeckoView זה מורכב יותר, אך לעיתים ה-CookieManager הכללי תופס חלק מהמידע.
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }

            // 4. הוספת User-Agent (קריטי!)
            // זה גורם לשרת לחשוב שהבקשה מגיעה מדפדפן אמיתי ולא מסתם בוט
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0");

            // 5. ניחוש שם הקובץ
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            request.setTitle(filename);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            // וידוא שההורדה מתבצעת גם ב-WiFi וגם בנתונים סלולריים
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Downloading: " + filename, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("GeckoDownload", "Download failed", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isHostAllowed(String host) {
        if (whitehosts.contains(host)) return true;
        for (String allowed : whitehosts) {
            if (host.endsWith("." + allowed)) return true;
        }
        return false;
    }

    private void loadWhitelist(Runnable onSuccess) {
        new Thread(() -> {
            try {
                URL url = new URL(whitelistUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                in.close();
                parseWhitelistJson(response.toString());
                new Handler(Looper.getMainLooper()).post(onSuccess);

            } catch (Exception e) {
                Log.e("Whitelist", "Error while loading list: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Error while loading list", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void parseWhitelistJson(String jsonStr) throws JSONException {
        JSONArray array = new JSONArray(jsonStr);
        whitehosts.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String host = obj.getString("host");
            whitehosts.add(host.toLowerCase());
        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type != null ? type : "*/*";
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            session.goBack();
        } else {
            super.onBackPressed(); // סוגר את האפליקציה
        }
    }
}