# To-Do List (MVP Gap Check)

## Already implemented
- Google sign-in (works when web client ID is set)
- Create/join family with invite code
- Family selection cached locally
- Lists screen + items CRUD, assign, status, delete
- Spending log basic CRUD
- Firestore realtime listeners
- Profile display name edit + sign-out
- Disband family requires typed confirmation
- Basic UI for Home/Lists/Spending/Profile
- Suggestions (model, write path, list detail UI)
- Budget limits + alerts (weekly/monthly)
- Analytics ranges (weekly/monthly totals + category breakdown)
- Offline persistence enabled
- Firestore rules + indexes files added
- Cloud Functions enforce family create/join/invite/remove/disband
- Firestore rules hardened against familyId escalation
- User profile fields ensured on sign-in
- Prototype email login removed

## Still needed to match the MVP plan

### Shared lists "real-time" polish
- Sorting by updatedAt across lists, proper paging

## Suggested order
1. Shared lists real-time polish (sorting + paging)
