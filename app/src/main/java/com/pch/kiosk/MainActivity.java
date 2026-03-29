package com.pch.kiosk;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
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
    private boolean kioskEnabled = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive mode
        goImmersive();

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Start the kiosk watchdog service
        Intent svc = new Intent(this, KioskService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }

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
                if (url.startsWith(KIOSK_URL) || url.contains("pch-tv")) {
                    return false;
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
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

        // JavaScript bridge — lets the web app launch native Fire TV apps
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public boolean launchApp(String packageName) {
                try {
                    PackageManager pm = getPackageManager();
                    Intent launch = pm.getLaunchIntentForPackage(packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launch);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @JavascriptInterface
            public boolean isInstalled(String packageName) {
                try {
                    getPackageManager().getPackageInfo(packageName, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }

            @JavascriptInterface
            public boolean isKiosk() {
                return true;
            }
        }, "PCHKiosk");

        webView.loadUrl(KIOSK_URL);
    }

    // ── Immersive sticky mode ──

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

    // ── Block remote keys ──

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP
            || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
            || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_ENTER) {
            return super.onKeyDown(keyCode, event);
        }

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            long now = System.currentTimeMillis();
            if (now - firstMenuTapTime > EXIT_TAP_WINDOW_MS) {
                menuTapCount = 0;
                firstMenuTapTime = now;
            }
            menuTapCount++;
            if (menuTapCount >= EXIT_TAP_COUNT) {
                menuTapCount = 0;
                kioskEnabled = false;
                stopService(new Intent(this, KioskService.class));
                finish();
            }
            return true;
        }

        return true; // block everything else
    }

    @Override
    public void onBackPressed() {
        // blocked
    }

    // ── Auto-relaunch: when the app loses focus, bring it right back ──

    @Override
    protected void onPause() {
        super.onPause();
        if (kioskEnabled) {
            // Relaunch after a short delay — gives Android time to finish the HOME transition
            handler.postDelayed(this::bringBack, 200);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (kioskEnabled) {
            handler.postDelayed(this::bringBack, 200);
        }
    }

    private void bringBack() {
        if (!kioskEnabled) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        goImmersive();
    }
}
