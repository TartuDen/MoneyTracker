# MVP Data Model

## User
- id (uid)
- displayName
- email
- photoUrl
- createdAt
- familyId

## Family
- id
- name
- memberIds
- createdAt
- createdBy

## Invite
- code
- familyId
- createdBy
- expiresAt
- usedBy (optional)
- usedAt (optional)

## List
- id
- familyId
- name
- createdAt
- createdBy

## ListItem
- id
- listId
- familyId
- name
- quantity (optional)
- status (todo, in_cart, bought)
- assignedTo (optional)
- price (optional)
- lastBoughtAt (optional)
- createdAt
- updatedAt

## Expense
- id
- familyId
- amount
- category
- note (optional)
- date
- createdBy

## Category
- id
- familyId
- name
- createdAt

## Suggestion (optional)
- familyId
- itemName
- count
- lastBoughtAt
