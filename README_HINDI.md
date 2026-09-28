# StreamBox Free — Android App

Netflix-जैसा free streaming starter, बिना premium/paywall के.

## Features
- Login / Signup
- Local fallback accounts
- Firebase Auth-ready integration
- Firestore-ready security model
- Admin dashboard
- User list/delete
- Movie catalog add/remove/reset
- Search, categories, My List
- Full-screen video playback
- StreamBox app icon

## Firebase mode
Project में Firebase Auth और Firestore dependencies जुड़ी हैं। अगर `app/google-services.json` मौजूद है तो Firebase initialize हो सकता है। अगर config नहीं है तो app local fallback mode में चलता रहेगा।

Setup के लिए `FIREBASE_SETUP_HINDI.md` देखें।

## Security
Current local admin code `98789` सिर्फ demo fallback है। Production में Firestore role-based admin access use करें।

## Content rights
सिर्फ अपनी, licensed, या public-domain videos stream करें.
