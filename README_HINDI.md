# StreamBox 1.8.0

- Home पर पूरी playable films चलती हैं। चार Blender short films शामिल हैं; ये latest Hindi feature movies नहीं हैं।
- पुराने Google sample links की जगह उपलब्ध open-film mirror इस्तेमाल होता है। पहले से saved built-in links भी update होते हैं; आपके custom videos और deleted entries सुरक्षित रहते हैं।
- **Romantic Hindi** में 170 romantic Hindi movie titles हैं। हर page पर **3 × 3 यानी 9 movies**, **Prev/Next** और title/year search मिलते हैं। यह list बिना TMDB key भी खुलती है।
- Hindi Movies और Latest Hindi में full-video source जुड़ने पर **Watch**, वरना **Watch options** दिखता है। वहाँ JustWatch/TMDB से India के streaming और rental options मिलते हैं। **Find on JustWatch** बिना key भी काम करता है और browser में matching film ढूँढता है।
- Saved full videos app के native player में चलते हैं। Broken film link पर Retry के साथ **Watch options** भी मिलता है।
- Music और Videos search अपने saved catalog में काम करती है।
- Player में pause/seek, speed, supported quality selection, retry और resume उपलब्ध हैं।
- Card को long press करके details/credits, download और favorite चुनें। Download direct video files के लिए है।

## Full movie कैसे जोड़ें

**Admin → Catalog → Add full video** खोलें। Title, category और अपनी या authorized full video का direct HTTPS `.mp4`, `.m4v`, `.webm`, `.mkv`, `.m3u8` या `.mpd` link दें। Web-page या YouTube link काम नहीं करेगा। Hindi movie card से जोड़ने के लिए उसका TMDB ID और सही movie/series type चुनें।

170 romantic entries movie खोजने के लिए हैं; उनकी full files APK में नहीं हैं। Streaming service पर subscription/rental की जरूरत हो सकती है। सही title/year और Hindi audio वहीं check करें। TMDB key से movie की जानकारी और streaming availability आती है, पूरी movie नहीं। Shared catalog के लिए Firebase configuration चाहिए; local mode में changes उसी phone में रहते हैं।

## APK

GitHub → Actions → सफल Android Build → `streambox-debug-apk` download करें। ZIP के अंदर APK है। Tests, lint और media source check के results `streambox-check-reports` में हैं। Phone पर playback की अलग जाँच जरूरी है।
