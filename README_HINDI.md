# StreamBox Free — Android App

Netflix-जैसा free streaming starter, बिना premium/paywall के.

## App features
- Login / Signup
- Firebase Authentication cloud mode
- Local fallback login mode
- Firestore role-based admin
- Admin dashboard
- Cloud/local movie catalog
- Search, categories, My List
- Full-screen video playback
- Offline cached catalog fallback

## GitHub build
Repo में GitHub Actions Android build workflow मौजूद है।

हर `main` push पर debug APK build check चलेगा।

APK GitHub Actions के artifact:
`streambox-debug-apk`
में मिलेगा।

Firebase cloud APK build के लिए GitHub Secret:
`GOOGLE_SERVICES_JSON_B64`
set करें।

पूरे steps के लिए:
`GITHUB_BUILD_HINDI.md`

## Firebase setup
Firebase configuration steps:
`FIREBASE_SETUP_HINDI.md`

## Security
`google-services.json` repo में commit नहीं होती। Production में Firestore admin role use करें।

## Content rights
सिर्फ अपनी, licensed, या public-domain videos stream करें.
