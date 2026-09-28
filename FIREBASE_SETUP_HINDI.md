# Firebase setup

Project अब Firebase-ready है और बिना Firebase config के local fallback mode में चलता रहेगा।

## 1. Firebase project बनाएं
Firebase Console में नया project बनाएं।

## 2. Android app register करें
Package name:
`com.example.streambox`

## 3. google-services.json
Firebase से `google-services.json` डाउनलोड करें और इसे:
`app/google-services.json`
में रखें।

यह file GitHub पर commit न करें। इसे .gitignore में रखा गया है।

## 4. Authentication
Firebase Console > Authentication > Sign-in method > Email/Password को Enable करें।

## 5. Firestore
Firestore Database create करें।

## 6. Security rules
Repo की `firestore.rules` file के rules Firebase Console में apply करें।

## 7. Admin role
किसी Firebase user को admin बनाना हो तो Firestore में:
`users/{uid}`
document पर:
`role: "admin"`
set करें।

## Current behavior
- Firebase config मौजूद: Firebase Auth उपलब्ध
- Firebase config नहीं: local login/signup चलता रहेगा
- Local Admin code 98789 अभी demo fallback के रूप में है

Production में hard-coded admin code हटाएं और सिर्फ Firestore role-based admin access रखें।
