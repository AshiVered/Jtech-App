package com.ashivered.aiv.noamobile;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends AppCompatActivity {
    private static GeckoRuntime sRuntime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GeckoView view = findViewById(R.id.geckoview);
        GeckoSession session = new GeckoSession();
        // Workaround for Bug 1758212
        session.setContentDelegate(new GeckoSession.ContentDelegate() {});

        if (sRuntime == null) {
            // GeckoRuntime can only be initialized once per process
            sRuntime = GeckoRuntime.create(this);
        }

        session.open(sRuntime);
        view.setSession(session);
        session.loadUri("https://noam.org.il"); // Or any other URL...
        

    }
}