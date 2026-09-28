# StreamBox Free — Android App

अब project में दो modes हैं:

## Firebase cloud mode
अगर `app/google-services.json` मौजूद है:
- Login Firebase Authentication से
- Signup Firebase Authentication से
- User profile Firestore `users` collection में
- Admin role Firestore के `role: "admin"` से
- Movie catalog Firestore `movies` collection से
- Admin cloud catalog में movie add/remove कर सकता है
- Home cloud catalog load करती है
- Offline होने पर cached local catalog fallback मिलता है

## Local fallback mode
अगर Firebase config नहीं है:
- Local Login / Signup
- Admin code `98789`
- Local user management
- Local movie catalog management

## Firebase admin
Production में admin user के Firestore document:
`users/{uid}`
पर:
`role: "admin"`
set करें।

## Important security
Firebase Authentication user को Android client से किसी दूसरे user के रूप में delete नहीं किया जा सकता। इसके लिए trusted backend / Firebase Admin SDK चाहिए। इसलिए cloud admin screen users को list करती है, लेकिन Auth account deletion client से नहीं करती।

## Content rights
सिर्फ अपनी, licensed, या public-domain videos stream करें.
