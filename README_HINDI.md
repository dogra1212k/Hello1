# StreamBox Free — Android Demo

यह Netflix-जैसा free streaming app starter है। इसमें कोई premium/paywall नहीं है।

## इसमें क्या है
- Home feed
- Categories
- Search
- My List / Favorites
- Full-screen video playback
- Remote poster loading
- Internet streaming
- कोई subscription/premium tier नहीं

## जरूरी बात
केवल वही videos जोड़ें जिनके streaming/distribution rights आपके पास हों, या जो public-domain / properly licensed हों। Netflix या किसी दूसरे paid service की protected content को बिना अनुमति जोड़ना उचित नहीं है।

## Android Studio / AndroidIDE में खोलना
1. ZIP extract करें।
2. `StreamBoxFree` folder को Android Studio या AndroidIDE में Open Project करें।
3. Gradle sync होने दें।
4. Run/Build APK करें।

## अपनी movies कैसे जोड़ें
`app/src/main/java/com/example/streambox/MainActivity.kt` में `movies = listOf(...)` खोजें।
हर item में title, category, description, videoUrl और posterUrl बदलें।

## अगला upgrade
- Login / signup
- Admin panel
- Firebase catalog
- Continue watching
- Download for offline (only for licensed content)
- Hindi UI
- Ads-supported free model
- Chromecast / TV UI
