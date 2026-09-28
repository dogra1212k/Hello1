# StreamBox Free — Android App

Netflix-जैसा free streaming starter, बिना premium/paywall के.

## अभी मौजूद features
- Login / Signup
- Local user accounts
- Session handling
- Admin-only dashboard
- Admin code: 98789 (demo only)
- Admin में registered users की list
- Admin से user delete
- Admin से movie add/remove
- Admin से demo catalog reset
- Home screen admin-managed catalog पढ़ती है
- नया StreamBox app icon
- Search, categories, My List
- Full-screen video playback

## Storage
अभी users और catalog इस Android device के SharedPreferences में store होते हैं। इसलिए यह single-device demo/admin system है।

## Production security
Production release के लिए Firebase Authentication / secure backend और server-side admin roles जोड़ना जरूरी है। Hard-coded admin code production में सुरक्षित नहीं है।

## Firebase next step
Firebase Console में Android package `com.example.streambox` register करें और `google-services.json` दें। उसके बाद Auth और Firestore को cloud-backed बनाया जा सकता है।

## Content rights
सिर्फ अपनी, licensed, या public-domain videos stream करें.
