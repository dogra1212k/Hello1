# GitHub Actions से APK Build

Repo में Android build workflow जोड़ दिया गया है।

## Automatic build
हर push पर GitHub Actions debug APK build करेगा।

GitHub में:
Actions > Android Build

Build सफल होने पर:
Artifacts > streambox-debug-apk

से APK मिल जाएगा।

## Firebase cloud build
अगर Firebase वाला APK चाहिए, तो `google-services.json` को public repo में commit मत करें।

फोन/PC पर file को base64 में encode करें और GitHub repo में secret बनाएं:

Settings > Secrets and variables > Actions > New repository secret

Name:
`GOOGLE_SERVICES_JSON_B64`

Value:
आपकी `google-services.json` file की base64 encoded value.

Workflow इस secret को build के समय:
`app/google-services.json`
में restore करेगा।

## Firebase के बिना
Secret न होने पर app local fallback mode में build होगा।

## Security
- `google-services.json` GitHub source में commit न करें।
- Admin code 98789 केवल local demo fallback के लिए है।
- Production Firebase mode में Firestore `role: "admin"` use करें।
