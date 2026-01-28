# Firestore Indexes (MVP)

Suggested composite indexes for current queries:

1) lists
   - Collection: lists
   - Fields: familyId (ASC), createdAt (DESC)

2) listItems
   - Collection: listItems
   - Fields: familyId (ASC), updatedAt (DESC)

3) expenses
   - Collection: expenses
   - Fields: familyId (ASC), date (DESC)

Notes:
- These support the equality + orderBy queries used in the app.
- If you later query listItems by listId with orderBy updatedAt, add:
  listId (ASC), updatedAt (DESC)
