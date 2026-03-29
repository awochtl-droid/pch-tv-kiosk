package com.pch.kiosk;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    // ── CHANGE THIS to your Vercel URL ──
    private static final String KIOSK_URL = "https://pch-tv.vercel.app";

    // Staff exit combo: press MENU 5 times quickly to exit kiosk
    private static final int EXIT_TAP_COUNT = 5;
    private static final long EXIT_TAP_WINDOW_MS = 3000;

    private WebView webView;
    private int menuTapCount = 0;
    private long firstMenuTapTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive mode
        goImmersive();

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create WebView
        webView = new WebView(this);
        setContentView(webView);

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " PCHKiosk/1.0");

        // Stay inside the WebView (no external browser)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Block navigation to anything outside our domain
                if (url.startsWith(KIOSK_URL) || url.contains("pch-tv")) {
                    return false; // allow
                }
                return true; // block
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Show a simple offline message, retry after 10s
                view.loadData(
                    "<html><body style='background:#120D0A;color:#F5EDE4;font-family:sans-serif;" +
                    "display:flex;align-items:center;justify-content:center;height:100vh;margin:0'>" +
                    "<div style='text-align:center'>" +
                    "<h1 style='font-size:48px;font-style:italic'>Pink Champagne</h1>" +
                    "<p style='font-size:18px;color:#999'>Connecting to hotel services…</p>" +
                    "</div></body>" +
                    "<script>setTimeout(function(){location.href='" + KIOSK_URL + "'},10000)</script></html>",
                    "text/html", "UTF-8"
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // Load the app
        webView.loadUrl(KIOSK_URL);
    }

    // ── Immersive sticky mode — hides nav/status bars ──

    private void goImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) goImmersive();
    }

    // ── Block all remote keys except the staff exit combo ──

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Allow D-pad and Enter for navigating the web app
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP
            || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
            || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_ENTER) {
            return super.onKeyDown(keyCode, event);
        }

        // MENU key combo: press 5 times within 3 seconds to exit kiosk
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            long now = System.currentTimeMillis();
            if (now - firstMenuTapTime > EXIT_TAP_WINDOW_MS) {
                menuTapCount = 0;
                firstMenuTapTime = now;
            }
            menuTapCount++;
            if (menuTapCount >= EXIT_TAP_COUNT) {
                menuTapCount = 0;
                finish(); // exit kiosk
            }
            return true;
        }

        // Block HOME, BACK, and everything else
        if (keyCode == KeyEvent.KEYCODE_HOME
            || keyCode == KeyEvent.KEYCODE_BACK
            || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true; // consume — do nothing
        }

        return true; // block all other keys
    }

    @Override
    public void onBackPressed() {
        // Block back button entirely
    }

    // ── Lifecycle — re-lock on resume ──

    @Override
    protected void onResume() {
        super.onResume();
        goImmersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // If app somehow loses focus, bring it back
        // (Fire TV doesn't have a great API for this, but the HOME intent-filter helps)
    }
}
