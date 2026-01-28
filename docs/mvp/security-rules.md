# Firestore Security Rules (MVP)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function userDoc() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid));
    }

    function userFamilyId() {
      return userDoc().data.familyId;
    }

    function isFamilyMember(familyId) {
      return isSignedIn() && userFamilyId() == familyId;
    }

    match /users/{uid} {
      allow read, write: if isSignedIn() && request.auth.uid == uid;
    }

    match /families/{familyId} {
      allow read, write: if isFamilyMember(familyId);
    }

    match /lists/{listId} {
      allow read, write: if isFamilyMember(resource.data.familyId)
                          || (request.resource.data.familyId != null
                              && isFamilyMember(request.resource.data.familyId));
    }

    match /listItems/{itemId} {
      allow read, write: if isFamilyMember(resource.data.familyId)
                          || (request.resource.data.familyId != null
                              && isFamilyMember(request.resource.data.familyId));
    }

    match /expenses/{expenseId} {
      allow read, write: if isFamilyMember(resource.data.familyId)
                          || (request.resource.data.familyId != null
                              && isFamilyMember(request.resource.data.familyId));
    }

    match /categories/{categoryId} {
      allow read, write: if isFamilyMember(resource.data.familyId)
                          || (request.resource.data.familyId != null
                              && isFamilyMember(request.resource.data.familyId));
    }

    match /suggestions/{suggestionId} {
      allow read, write: if isFamilyMember(resource.data.familyId)
                          || (request.resource.data.familyId != null
                              && isFamilyMember(request.resource.data.familyId));
    }

    match /invites/{code} {
      allow read: if true;
      allow create: if isSignedIn()
        && isFamilyMember(request.resource.data.familyId)
        && request.resource.data.expiresAt > request.time;
      allow update: if isSignedIn()
        && isFamilyMember(resource.data.familyId);
      allow delete: if false;
    }
  }
}
```
