# PCH-TV Kiosk APK

A minimal Android WebView app that locks a Fire TV to the Pink Champagne guest TV web app.

## What it does
- Loads your Vercel URL (`pch-tv.vercel.app`) full-screen
- Blocks HOME, BACK, and app-switch buttons
- Auto-launches on boot
- Shows an offline fallback if WiFi drops, auto-retries after 10s
- **Staff exit**: press the MENU button 5 times within 3 seconds to exit

## Change the URL
Edit `KIOSK_URL` in `app/src/main/java/com/pch/kiosk/MainActivity.java`

## Build

### Option A: Android Studio (easiest)
1. Open this folder in Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)
3. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`

### Option B: Command line (requires Android SDK)
```bash
./gradlew assembleDebug
```

## Install on Fire TV

1. On the Fire TV: Settings → My Fire TV → Developer Options → enable **ADB debugging**
2. Find the Fire TV's IP: Settings → My Fire TV → About → Network
3. From your computer:
```bash
adb connect <FIRE-TV-IP>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```
4. Launch: Settings → Applications → Manage Installed Applications → Pink Champagne TV → Launch

## Set as default launcher (optional)
After installing, the next time you press HOME the Fire TV will ask which launcher to use.
Select "Pink Champagne TV" and choose "Always". Now the TV boots directly into your app.

To undo:
```bash
adb shell cmd package set-home-activity com.amazon.tv.launcher/.ui.HomeActivity_vNext
```
