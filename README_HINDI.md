# StreamBox 1.7.0

- Home पर पूरी playable films चलती हैं। चार Blender short films शामिल हैं; ये latest Hindi feature movies नहीं हैं।
- YouTube, trailer और browser वाली playback screens हटा दी गई हैं।
- Hindi Movies और Latest Hindi में posters और movie details मिलते हैं। Full-video source जुड़ने पर **Watch**, वरना **Unavailable** दिखता है।
- Music और Videos search अपने saved catalog में काम करती है।
- Player में pause/seek, speed, supported quality selection, retry और resume उपलब्ध हैं।
- Card को long press करके details/credits, download और favorite चुनें। Download direct video files के लिए है।

## Full movie कैसे जोड़ें

**Admin → Catalog → Add full video** खोलें। Title, category और अपनी या authorized full video का direct HTTPS `.mp4`, `.m4v`, `.webm`, `.mkv`, `.m3u8` या `.mpd` link दें। Web-page या YouTube link काम नहीं करेगा। Hindi movie card से जोड़ने के लिए उसका TMDB ID और सही movie/series type चुनें।

Latest Hindi movies की full files अभी इस repo में उपलब्ध नहीं हैं। TMDB key से movie की जानकारी आती है, पूरी movie नहीं। Shared catalog के लिए Firebase configuration चाहिए; local mode में changes उसी phone में रहते हैं।

## APK

GitHub → Actions → सफल Android Build → `streambox-debug-apk` download करें। ZIP के अंदर APK है। Tests, lint और media source check के results `streambox-check-reports` में हैं। Phone पर playback की अलग जाँच जरूरी है।
