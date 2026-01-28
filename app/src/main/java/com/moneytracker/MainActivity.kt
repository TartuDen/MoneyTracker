package com.moneytracker

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moneytracker.ui.MainScreen
import com.moneytracker.ui.theme.MoneyTrackerTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val auth = FirebaseAuth.getInstance()
            var isSignedIn by remember { mutableStateOf(auth.currentUser != null) }
            var signedInLabel by remember {
                mutableStateOf(auth.currentUser?.displayName ?: auth.currentUser?.email ?: "User")
            }
            var userId by remember { mutableStateOf(auth.currentUser?.uid ?: "") }
            var familyId by remember { mutableStateOf<String?>(null) }
            var familyName by remember { mutableStateOf<String?>(null) }
            var isResolvingFamily by remember { mutableStateOf(false) }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (auth.currentUser == null) {
                    val savedUser = loadPrototypeUser(context)
                    if (savedUser != null) {
                        signedInLabel = savedUser.first
                        userId = savedUser.second
                        isSignedIn = true
                    }
                }
            }

            LaunchedEffect(isSignedIn, userId) {
                if (!isSignedIn || userId.isBlank() || familyId != null) {
                    return@LaunchedEffect
                }
                val cached = loadFamilySelection(context, userId)
                if (cached != null) {
                    familyId = cached.first
                    familyName = cached.second
                    return@LaunchedEffect
                }
                isResolvingFamily = true
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val foundFamilyId = userDoc.getString("familyId")
                        if (foundFamilyId.isNullOrBlank()) {
                            isResolvingFamily = false
                            return@addOnSuccessListener
                        }
                        db.collection("families").document(foundFamilyId).get()
                            .addOnSuccessListener { famDoc ->
                                val name = famDoc.getString("name") ?: "Family"
                                saveFamilySelection(context, userId, foundFamilyId, name)
                                familyId = foundFamilyId
                                familyName = name
                                isResolvingFamily = false
                            }
                            .addOnFailureListener {
                                isResolvingFamily = false
                            }
                    }
                    .addOnFailureListener {
                        isResolvingFamily = false
                    }
            }

            MoneyTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isSignedIn) {
                        if (familyId == null) {
                            if (isResolvingFamily) {
                                LoadingScreen()
                            } else {
                                FamilyScreen(
                                    userId = userId,
                                    onFamilyReady = { id, name ->
                                        saveFamilySelection(context, userId, id, name)
                                        familyId = id
                                        familyName = name
                                    },
                                    onError = { message ->
                                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        } else {
                            MainScreen(
                                signedInLabel = signedInLabel,
                                familyName = familyName ?: "Family",
                                familyId = familyId,
                                userId = userId
                            )
                        }
                    } else {
                        LoginScreen(
                            onSignedIn = { label, id ->
                                signedInLabel = label
                                userId = id
                                isSignedIn = true
                                if (id.startsWith("email:")) {
                                    savePrototypeUser(context, id, label)
                                }
                                Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show()
                            },
                            onSignInError = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    onSignedIn: (String, String) -> Unit,
    onSignInError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var userName by remember { mutableStateOf(auth.currentUser?.displayName) }
    var emailInput by remember { mutableStateOf("test@prototype.local") }
    val context = LocalContext.current

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val idToken = account.idToken
            if (idToken == null) {
                onSignInError("Missing ID token")
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val label = auth.currentUser?.displayName
                            ?: auth.currentUser?.email
                            ?: "User"
                        userName = label
                        val id = auth.currentUser?.uid ?: "user"
                        onSignedIn(label, id)
                    } else {
                        onSignInError(authTask.exception?.message ?: "Sign-in failed")
                    }
                }
        } else {
            onSignInError(task.exception?.message ?: "Google sign-in failed")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "MoneyTracker MVP")
            Text(
                text = if (userName != null) "Signed in as $userName" else "Not signed in",
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(onClick = {
                val webClientId = context.getString(R.string.default_web_client_id)
                if (webClientId.isBlank() || webClientId == "REPLACE_WITH_WEB_CLIENT_ID") {
                    onSignInError("Missing web client id")
                    return@Button
                }
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                launcher.launch(client.signInIntent)
            }) {
                Text(text = "Continue with Google")
            }
            TextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email (prototype)") },
                modifier = Modifier.padding(top = 16.dp),
                singleLine = true
            )
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val trimmed = emailInput.trim()
                    if (trimmed.isEmpty() || !trimmed.contains("@")) {
                        onSignInError("Enter a valid email")
                        return@Button
                    }
                    Toast.makeText(context, "Signed in as $trimmed", Toast.LENGTH_SHORT).show()
                    onSignedIn(trimmed, "email:$trimmed")
                }
            ) {
                Text(text = "Continue with Email (Prototype)")
            }
        }
    }
}

@Composable
private fun FamilyScreen(
    userId: String,
    onFamilyReady: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var familyNameInput by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun generateInviteCode(): String {
        val code = Random.nextInt(0, 1_000_000)
        return code.toString().padStart(6, '0')
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Create or Join Family")
            TextField(
                value = familyNameInput,
                onValueChange = { familyNameInput = it },
                label = { Text("Family name") },
                singleLine = true,
                modifier = Modifier.padding(top = 12.dp)
            )
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val name = familyNameInput.trim()
                    if (name.isEmpty()) {
                        onError("Enter a family name")
                        return@Button
                    }
                    val familyDoc = db.collection("families").document()
                    val newFamilyId = familyDoc.id
                    val inviteCode = generateInviteCode()
                    val expiresAt = Timestamp.now().let { ts ->
                        Timestamp(ts.seconds + 1800, ts.nanoseconds)
                    }

                    val familyData = mapOf(
                        "name" to name,
                        "memberIds" to listOf(userId),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    val inviteData = mapOf(
                        "familyId" to newFamilyId,
                        "createdBy" to userId,
                        "expiresAt" to expiresAt
                    )

                    familyDoc.set(familyData)
                        .addOnSuccessListener {
                            db.collection("invites").document(inviteCode)
                                .set(inviteData)
                                .addOnSuccessListener {
                                    updateUserFamily(db, userId, newFamilyId, onError)
                                    onFamilyReady(newFamilyId, name)
                                    Toast.makeText(
                                        context,
                                        "Family created. Code: $inviteCode",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    onError(e.message ?: "Failed to create invite")
                                }
                        }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Failed to create family")
                        }
                }
            ) {
                Text(text = "Create Family")
            }

            TextField(
                value = inviteCodeInput,
                onValueChange = { inviteCodeInput = it },
                label = { Text("Invite code") },
                singleLine = true,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val code = inviteCodeInput.trim()
                    if (code.length != 6) {
                        onError("Enter 6-digit code")
                        return@Button
                    }
                    db.collection("invites").document(code).get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                onError("Invite code not found")
                                return@addOnSuccessListener
                            }
                            val foundFamilyId = snapshot.getString("familyId")
                            val expiresAt = snapshot.getTimestamp("expiresAt")
                            if (foundFamilyId.isNullOrEmpty()) {
                                onError("Invalid invite code")
                                return@addOnSuccessListener
                            }
                            if (expiresAt != null && expiresAt.seconds < Timestamp.now().seconds) {
                                onError("Invite code expired")
                                return@addOnSuccessListener
                            }
                            db.collection("families").document(foundFamilyId)
                                .update("memberIds", FieldValue.arrayUnion(userId))
                                .addOnSuccessListener {
                                    db.collection("families").document(foundFamilyId).get()
                                        .addOnSuccessListener { fam ->
                                            val name = fam.getString("name") ?: "Family"
                                            updateUserFamily(db, userId, foundFamilyId, onError)
                                            onFamilyReady(foundFamilyId, name)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    onError(e.message ?: "Failed to join family")
                                }
                        }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Failed to read invite")
                        }
                }
            ) {
                Text(text = "Join Family")
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Loading your family...")
    }
}

private const val PREFS_NAME = "moneytracker_prefs"
private const val PREFS_KEY_AUTH_PROVIDER = "authProvider"
private const val PREFS_KEY_USER_ID = "userId"
private const val PREFS_KEY_USER_LABEL = "userLabel"
private const val AUTH_PROVIDER_PROTOTYPE = "prototype_email"

private fun familyIdKey(userId: String) = "familyId:$userId"
private fun familyNameKey(userId: String) = "familyName:$userId"

private fun loadFamilySelection(context: Context, userId: String): Pair<String, String?>? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val familyId = prefs.getString(familyIdKey(userId), null) ?: return null
    val familyName = prefs.getString(familyNameKey(userId), null)
    return familyId to familyName
}

private fun saveFamilySelection(
    context: Context,
    userId: String,
    familyId: String,
    familyName: String?
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
        .putString(familyIdKey(userId), familyId)
    if (familyName.isNullOrBlank()) {
        editor.remove(familyNameKey(userId))
    } else {
        editor.putString(familyNameKey(userId), familyName)
    }
    editor.apply()
}

private fun savePrototypeUser(context: Context, userId: String, label: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(PREFS_KEY_AUTH_PROVIDER, AUTH_PROVIDER_PROTOTYPE)
        .putString(PREFS_KEY_USER_ID, userId)
        .putString(PREFS_KEY_USER_LABEL, label)
        .apply()
}

private fun loadPrototypeUser(context: Context): Pair<String, String>? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val provider = prefs.getString(PREFS_KEY_AUTH_PROVIDER, null)
    if (provider != AUTH_PROVIDER_PROTOTYPE) {
        return null
    }
    val userId = prefs.getString(PREFS_KEY_USER_ID, null) ?: return null
    val label = prefs.getString(PREFS_KEY_USER_LABEL, userId) ?: userId
    return label to userId
}

private fun updateUserFamily(
    db: FirebaseFirestore,
    userId: String,
    familyId: String,
    onError: (String) -> Unit
) {
    val data = mapOf(
        "familyId" to familyId,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    db.collection("users").document(userId)
        .set(data, SetOptions.merge())
        .addOnFailureListener { e ->
            onError(e.message ?: "Failed to save user profile")
        }
}
