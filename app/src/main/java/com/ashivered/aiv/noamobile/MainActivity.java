package com.ashivered.aiv.noamobile;

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
import org.mozilla.geckoview.WebExtension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;
    private GeckoSession session;
    private final Set<String> whitehosts = new HashSet<>();
    private final String whitelistUrl = "https://ashivered.github.io/SafeBrowserResources/jtech.json"; // שים כאן את כתובת ה־JSON

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GeckoView view = findViewById(R.id.geckoview);
        session = new GeckoSession();

        session.setContentDelegate(new GeckoSession.ContentDelegate() {});



        // ✅ הגבלת דומיינים
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @NonNull
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session,
                                                          @NonNull LoadRequest request) {
                String uri = request.uri;
                try {
                    URI parsedUri = new URI(uri);
                    String host = parsedUri.getHost();
                    if (host != null && isHostAllowed(host)) {
                        return GeckoResult.allow();
                    } else {
                        runOnUiThread(() -> Toast.makeText(
                                MainActivity.this,
                                "אתר זה חסום",
                                Toast.LENGTH_SHORT
                        ).show());
                        return GeckoResult.deny();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return GeckoResult.deny();
                }
            }
        });

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }

        session.open(sRuntime);
        view.setSession(session);

        loadWhitelist(() -> runOnUiThread(() ->
                session.loadUri("https://www.jtechforums.org/")
        ));
    }

    private boolean isHostAllowed(String host) {
        if (whitehosts.contains(host)) return true;
        for (String allowed : whitehosts) {
            if (host.endsWith("." + allowed)) return true; // תמיכה ב-subdomains
        }
        return false;
    }

    private void loadWhitelist(Runnable onSuccess) {
        new Thread(() -> {
            try {
                URL url = new URL(whitelistUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                in.close();
                parseWhitelistJson(response.toString());

                new Handler(Looper.getMainLooper()).post(onSuccess);

            } catch (Exception e) {
                Log.e("Whitelist", "שגיאה בטעינת הרשימה: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "שגיאה בטעינת הרשימה",
                        Toast.LENGTH_LONG
                ).show());
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

    // ✅ טיפול במקש חזור
    // ✅ מקש חזור – פשוט חוזר אחורה בלי canGoBack()
    @Override
    public void onBackPressed() {
        if (session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
