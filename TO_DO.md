# To-Do List (MVP Gap Check)

## Already implemented
- Google sign-in (works when web client ID is set)
- Prototype email login (for quick testing)
- Create/join family with invite code
- Family selection cached locally
- Lists screen + items CRUD, assign, status, delete
- Spending log basic CRUD
- Firestore realtime listeners
- Profile display name edit + sign-out
- Basic UI for Home/Lists/Spending/Profile

## Still needed to match the MVP plan

### Shared lists "real-time" polish
- Sorting by updatedAt across lists, proper paging
- "List detail supports item status, assignment, suggestions" -> suggestions not yet implemented

### Suggestions
- Data model + write path for suggestions (count, lastBoughtAt)
- UI in list detail to add suggested items

### Budget limits + alerts
- Budget data model per family/category
- Alerts based on totals (weekly/monthly)

### Analytics
- Weekly/monthly totals + category breakdown (some UI placeholders exist, needs real ranges)

### Offline mode
- Enable Firestore persistence + test offline flows

### Security rules + indexes
- Apply rules from security-rules.md in Firebase
- Add indexes from indexes.md

### Data model completeness
- Ensure users/{uid} has displayName/email/photoUrl/createdAt/familyId
- Add createdBy consistently (some places already do)

### Replace prototype email login
- MVP says Google OAuth only -> remove email flow

## Suggested order
1. Apply Firebase rules + indexes
2. Suggestions feature (model + UI)
3. Analytics ranges + budget alerts
4. Offline persistence + tests
5. Remove prototype email login
