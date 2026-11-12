JAPSF Admin Android App

This Android app opens the Federation's existing secure admin panel:
https://jathedarakaliphulasinghfederation.in/admin.html

It uses the existing website and Supabase authentication; it does not create a second database or store admin credentials in the app. The app requires an internet connection. HTTPS-only navigation is enforced for the Federation domain; other links open in the device browser.

Build APK

The repository workflow named Build JAPSF Admin Android APK runs on pushes affecting this folder and can also be started manually from GitHub Actions.

After a successful run:
1. Open the repository's Actions tab.
2. Select the latest Build JAPSF Admin Android APK run.
3. Download the JAPSF-Admin-Android-APK artifact.
4. Extract it and install app-debug.apk on the Android device. Android may ask you to allow installation from that source.

This is a debug-signed test APK. A production release APK should be signed with a private release keystore before public distribution.
