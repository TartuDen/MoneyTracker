# Firestore Queries (MVP)

## Lists home
- lists where familyId == currentUser.familyId order by createdAt desc limit 50

## List detail
- listItems where familyId == currentUser.familyId order by updatedAt desc limit 100 (filter listId in app for now)

## Suggestions
- suggestions where familyId == currentUser.familyId limit 50 (sorted by count client-side)

## Expenses (monthly)
- expenses where familyId == currentUser.familyId order by date desc limit 50 (filter by date range in app)

## Categories
- categories where familyId == currentUser.familyId
