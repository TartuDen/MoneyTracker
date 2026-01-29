const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000;

function assertSignedIn(context) {
  if (!context.auth) {
    throw new HttpsError("unauthenticated", "Sign-in required.");
  }
}

function inviteCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 8; i += 1) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return code;
}

function limitDocRef(uid, key) {
  return db.collection("users").doc(uid).collection("limits").doc(key);
}

exports.createFamily = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const name = (request.data?.name || "").trim();
  if (!name) {
    throw new HttpsError("invalid-argument", "Family name is required.");
  }

  const now = Date.now();
  const limitRef = limitDocRef(uid, "familyCreate");
  const userRef = db.collection("users").doc(uid);
  const familyRef = db.collection("families").doc();
  const inviteRef = db.collection("invites");

  const result = await db.runTransaction(async (tx) => {
    const [limitSnap, userSnap] = await Promise.all([
      tx.get(limitRef),
      tx.get(userRef),
    ]);

    const lastCreatedAt = limitSnap.exists ? limitSnap.get("lastCreatedAt") : null;
    if (lastCreatedAt && now - lastCreatedAt.toMillis() < RATE_LIMIT_WINDOW_MS) {
      throw new HttpsError("resource-exhausted", "Please wait before creating another family.");
    }

    if (userSnap.exists && userSnap.get("familyId")) {
      throw new HttpsError("failed-precondition", "Leave your current family first.");
    }

    const familyId = familyRef.id;
    const code = inviteCode();
    const expiresAt = admin.firestore.Timestamp.fromMillis(now + 30 * 60 * 1000);

    tx.set(familyRef, {
      name,
      memberIds: [uid],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      createdBy: uid,
    });
    tx.set(inviteRef.doc(code), {
      familyId,
      createdBy: uid,
      expiresAt,
      usedBy: null,
      usedAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.set(userRef, {
      familyId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(limitRef, {
      lastCreatedAt: admin.firestore.Timestamp.fromMillis(now),
    }, { merge: true });

    return { familyId, inviteCode: code, expiresAt };
  });

  return {
    familyId: result.familyId,
    inviteCode: result.inviteCode,
    expiresAt: result.expiresAt.toMillis(),
  };
});

exports.joinFamily = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const code = (request.data?.code || "").trim();
  if (code.length !== 8) {
    throw new HttpsError("invalid-argument", "Invite code must be 8 characters.");
  }

  const inviteRef = db.collection("invites").doc(code);
  const userRef = db.collection("users").doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const [inviteSnap, userSnap] = await Promise.all([
      tx.get(inviteRef),
      tx.get(userRef),
    ]);
    if (!inviteSnap.exists) {
      throw new HttpsError("not-found", "Invite code not found.");
    }
    const invite = inviteSnap.data();
    if (!invite.familyId) {
      throw new HttpsError("failed-precondition", "Invalid invite.");
    }
    if (invite.expiresAt && invite.expiresAt.toMillis() < Date.now()) {
      throw new HttpsError("failed-precondition", "Invite code expired.");
    }
    if (invite.usedBy) {
      throw new HttpsError("failed-precondition", "Invite already used.");
    }
    if (userSnap.exists && userSnap.get("familyId")) {
      throw new HttpsError("failed-precondition", "Leave your current family first.");
    }

    const familyRef = db.collection("families").doc(invite.familyId);
    const familySnap = await tx.get(familyRef);
    if (!familySnap.exists) {
      throw new HttpsError("not-found", "Family not found.");
    }
    const familyName = familySnap.get("name") || "Family";

    tx.update(inviteRef, {
      usedBy: uid,
      usedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    tx.update(familyRef, {
      memberIds: admin.firestore.FieldValue.arrayUnion(uid),
    });
    tx.set(userRef, {
      familyId: invite.familyId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    return { familyId: invite.familyId, familyName };
  });

  return result;
});

exports.createInvite = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const familyId = (request.data?.familyId || "").trim();
  if (!familyId) {
    throw new HttpsError("invalid-argument", "Family id required.");
  }

  const familyRef = db.collection("families").doc(familyId);
  const familySnap = await familyRef.get();
  if (!familySnap.exists) {
    throw new HttpsError("not-found", "Family not found.");
  }
  if (familySnap.get("createdBy") !== uid) {
    throw new HttpsError("permission-denied", "Only the owner can create invites.");
  }

  const code = inviteCode();
  const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 60 * 1000);
  await db.collection("invites").doc(code).set({
    familyId,
    createdBy: uid,
    expiresAt,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    usedBy: null,
    usedAt: null,
  });

  return { inviteCode: code, expiresAt: expiresAt.toMillis() };
});

exports.leaveFamily = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const familyId = (request.data?.familyId || "").trim();
  if (!familyId) {
    throw new HttpsError("invalid-argument", "Family id required.");
  }
  const familyRef = db.collection("families").doc(familyId);
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const familySnap = await tx.get(familyRef);
    if (!familySnap.exists) {
      throw new HttpsError("not-found", "Family not found.");
    }
    if (familySnap.get("createdBy") === uid) {
      throw new HttpsError("failed-precondition", "Owner cannot leave; disband instead.");
    }
    tx.update(familyRef, {
      memberIds: admin.firestore.FieldValue.arrayRemove(uid),
    });
    tx.set(userRef, {
      familyId: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { success: true };
});

exports.removeMember = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const familyId = (request.data?.familyId || "").trim();
  const memberId = (request.data?.memberId || "").trim();
  if (!familyId || !memberId) {
    throw new HttpsError("invalid-argument", "Family id and member id required.");
  }
  if (uid === memberId) {
    throw new HttpsError("failed-precondition", "Use leave family instead.");
  }

  const familyRef = db.collection("families").doc(familyId);
  const memberRef = db.collection("users").doc(memberId);

  await db.runTransaction(async (tx) => {
    const familySnap = await tx.get(familyRef);
    if (!familySnap.exists) {
      throw new HttpsError("not-found", "Family not found.");
    }
    if (familySnap.get("createdBy") !== uid) {
      throw new HttpsError("permission-denied", "Only the owner can remove members.");
    }
    tx.update(familyRef, {
      memberIds: admin.firestore.FieldValue.arrayRemove(memberId),
    });
    tx.set(memberRef, {
      familyId: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return { success: true };
});

exports.disbandFamily = onCall(async (request) => {
  assertSignedIn(request);
  const uid = request.auth.uid;
  const familyId = (request.data?.familyId || "").trim();
  if (!familyId) {
    throw new HttpsError("invalid-argument", "Family id required.");
  }
  const familyRef = db.collection("families").doc(familyId);
  const familySnap = await familyRef.get();
  if (!familySnap.exists) {
    throw new HttpsError("not-found", "Family not found.");
  }
  if (familySnap.get("createdBy") !== uid) {
    throw new HttpsError("permission-denied", "Only the owner can disband.");
  }

  const collections = [
    "lists",
    "listItems",
    "expenses",
    "categories",
    "suggestions",
    "budgets",
    "invites",
  ];

  for (const collection of collections) {
    const snapshot = await db.collection(collection).where("familyId", "==", familyId).get();
    if (!snapshot.empty) {
      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }
  }

  const members = await db.collection("users").where("familyId", "==", familyId).get();
  if (!members.empty) {
    const batch = db.batch();
    members.docs.forEach((doc) => {
      batch.set(doc.ref, {
        familyId: null,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    });
    await batch.commit();
  }

  await familyRef.delete();
  return { success: true };
});
