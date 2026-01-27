# Firestore Queries (MVP)

## Lists home
- lists where familyId == currentUser.familyId

## List detail
- items where listId == selectedListId order by updatedAt desc

## Suggestions
- suggestions where familyId == currentUser.familyId order by count desc limit 10

## Expenses (monthly)
- expenses where familyId == currentUser.familyId and date between start/end

## Categories
- categories where familyId == currentUser.familyId
