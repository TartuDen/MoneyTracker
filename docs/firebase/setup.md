# Firebase Setup

1) Create a Firebase project
2) Add Android app with package name: `com.moneytracker`
3) Download `google-services.json`
4) Place it at `app/google-services.json`
5) In Firebase Console, enable:
   - Authentication > Google
   - Firestore Database

Notes:
- Security rules are in `firestore.rules`
- Suggested indexes are in `firestore.indexes.json`
- Cloud Functions live in `functions/` (deploy them before using create/join/invite/disband flows)
- Consider enabling App Check for Firestore + Functions
