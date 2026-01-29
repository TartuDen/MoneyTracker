# MoneyTracker (Prototype)

Android-only prototype for a shared family shopping + spending app.

## Main concept
- Shared, real-time family shopping lists (multiple lists like groceries/pharmacy/hardware).
- Item assignment and status updates so everyone sees progress instantly.
- Simple spending tracking with categories and basic analytics.
- Smart suggestions based on frequently bought items.
- Invite-only family groups (one-time codes) and offline-first usage.

## Current project status
- Android app scaffold with Jetpack Compose.
- Firebase Auth + Firestore dependencies added.
- Firebase Cloud Functions (Node) for create/join/invite/disband.
- Login screen with Google sign-in (ready once web client ID is set).
- Create/Join family flow wired to Firestore with invite codes.
- Basic home screen after family creation/join.
- Documentation for MVP scope, data model, and security rules.
- Budgets, analytics ranges, and suggestions wired to Firestore.
- Offline persistence enabled for Firestore.

## Next planned work
- Persist user + family selection across launches.
- Shared lists screen with real-time updates.
- Items CRUD (add/edit/mark bought/assign).
- Spending log + categories + simple analytics.
- Apply Firestore security rules + indexes in Firebase.

## Quick start (Android Studio)
1) Open this folder in Android Studio.
2) Let it sync Gradle.
3) Add Firebase config:
   - Create a Firebase project
   - Add an Android app with package name `com.moneytracker`
   - Download `google-services.json` and place it in `app/`
4) Run the app on an emulator or device.

Notes:
- If Gradle sync fails due to plugin versions, update them in `settings.gradle.kts`.
- This repo does not include `google-services.json`.

## Docs
See `docs/mvp/` for the product and data model.
