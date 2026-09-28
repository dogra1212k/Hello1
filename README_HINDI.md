# StreamBox Free — Android App

Netflix-जैसा free streaming starter, बिना premium/paywall के.

## अभी मौजूद features
- Login
- Signup
- Local user accounts
- Session handling
- Admin-only dashboard
- Admin code: 98789 (demo only)
- नया StreamBox app icon
- Home, categories, search
- My List / Favorites
- Full-screen video playback
- Demo streaming catalog

## Security note
Current login/signup local SharedPreferences demo है. Production release के लिए Firebase Authentication और server-side admin roles जोड़ना जरूरी है. Hard-coded admin code production में सुरक्षित नहीं है.

## Firebase जोड़ने के लिए
Firebase Console में Android app package `com.example.streambox` बनाएं और `google-services.json` डाउनलोड करें. उसे `app/google-services.json` में रखने के बाद Firebase Auth/Firestore integration किया जा सकता है.

## Content rights
सिर्फ अपनी, licensed, या public-domain videos stream करें.
