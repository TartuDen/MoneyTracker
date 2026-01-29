package com.moneytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private data class AppTab(
    val key: String,
    val label: String,
    val shortLabel: String
)

private data class ListDoc(val id: String, val name: String)
private data class ListItemDoc(
    val id: String,
    val name: String,
    val listId: String,
    val status: String?,
    val assignedTo: String?,
    val updatedAt: Timestamp?,
    val createdAt: Timestamp?
)
private data class ExpenseDoc(
    val id: String,
    val amount: Double,
    val category: String?,
    val date: Timestamp?
)
private data class SuggestionDoc(
    val id: String,
    val name: String,
    val count: Long,
    val lastBoughtAt: Timestamp?
)
private data class BudgetDoc(
    val id: String,
    val category: String,
    val limit: Double,
    val period: String
)
private data class UserDoc(val id: String, val displayName: String?)
private data class ActivityEntry(val label: String, val timestamp: Timestamp)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun MainScreen(
    signedInLabel: String,
    familyName: String,
    familyId: String?,
    userId: String,
    onSignOut: () -> Unit,
    onFamilyCleared: () -> Unit
) {
    val tabs = remember {
        listOf(
            AppTab("home", "Home", "H"),
            AppTab("lists", "Lists", "L"),
            AppTab("spending", "Spending", "S"),
            AppTab("profile", "Profile", "P")
        )
    }
    var selectedTab by remember { mutableStateOf(tabs.first().key) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var listLimit by remember { mutableStateOf(50) }
    var itemLimit by remember { mutableStateOf(100) }
    var expenseLimit by remember { mutableStateOf(50) }
    var isCreateListOpen by remember { mutableStateOf(false) }
    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isProfileMenuOpen by remember { mutableStateOf(false) }
    var requestSearchFocus by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    var lists by remember { mutableStateOf<List<ListDoc>>(emptyList()) }
    var listItems by remember { mutableStateOf<List<ListItemDoc>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseDoc>>(emptyList()) }
    var members by remember { mutableStateOf<List<UserDoc>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<SuggestionDoc>>(emptyList()) }
    var budgets by remember { mutableStateOf<List<BudgetDoc>>(emptyList()) }
    var listsLoaded by remember { mutableStateOf(false) }
    var itemsLoaded by remember { mutableStateOf(false) }
    var expensesLoaded by remember { mutableStateOf(false) }
    var membersLoaded by remember { mutableStateOf(false) }
    var suggestionsLoaded by remember { mutableStateOf(false) }
    var budgetsLoaded by remember { mutableStateOf(false) }
    var useItemsOrderBy by remember { mutableStateOf(true) }
    var useExpensesOrderBy by remember { mutableStateOf(true) }
    var familyOwnerId by remember { mutableStateOf<String?>(null) }
    val currentDisplayName = members.firstOrNull { it.id == userId }?.displayName
    val displayLabel = currentDisplayName ?: signedInLabel

    LaunchedEffect(familyId) {
        useItemsOrderBy = true
        useExpensesOrderBy = true
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = tabs[pagerState.currentPage].key
    }

    DisposableEffect(
        familyId,
        listLimit,
        itemLimit,
        expenseLimit,
        useItemsOrderBy,
        useExpensesOrderBy
    ) {
        if (familyId.isNullOrBlank()) {
            lists = emptyList()
            listItems = emptyList()
            expenses = emptyList()
            members = emptyList()
            suggestions = emptyList()
            budgets = emptyList()
            listsLoaded = false
            itemsLoaded = false
            expensesLoaded = false
            membersLoaded = false
            suggestionsLoaded = false
            budgetsLoaded = false
            selectedListId = null
            familyOwnerId = null
            return@DisposableEffect onDispose {}
        }


        val listsReg = db.collection("lists")
            .whereEqualTo("familyId", familyId)
            .limit(listLimit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    listsLoaded = true
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load lists",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                listsLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                lists = docs.map { doc ->
                    ListDoc(id = doc.id, name = doc.getString("name") ?: "List")
                }
            }

        val itemsQuery = db.collection("listItems")
            .whereEqualTo("familyId", familyId)
            .let { query ->
                if (useItemsOrderBy) {
                    query.orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                } else {
                    query
                }
            }
            .limit(itemLimit.toLong())
        val itemsReg = itemsQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    itemsLoaded = true
                    if (error.message?.contains("index", ignoreCase = true) == true && useItemsOrderBy) {
                        useItemsOrderBy = false
                        Toast.makeText(
                            context,
                            "Items index missing. Falling back to unsorted items.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load items",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                itemsLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                listItems = docs.map { doc ->
                    ListItemDoc(
                        id = doc.id,
                        name = doc.getString("name") ?: "Item",
                        listId = doc.getString("listId") ?: "",
                        status = doc.getString("status"),
                        assignedTo = doc.getString("assignedTo"),
                        updatedAt = doc.getTimestamp("updatedAt"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
            }

        val expenseQuery = db.collection("expenses")
            .whereEqualTo("familyId", familyId)
            .let { query ->
                if (useExpensesOrderBy) {
                    query.orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                } else {
                    query
                }
            }
            .limit(expenseLimit.toLong())
        val expenseReg = expenseQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    expensesLoaded = true
                    if (error.message?.contains("index", ignoreCase = true) == true && useExpensesOrderBy) {
                        useExpensesOrderBy = false
                        Toast.makeText(
                            context,
                            "Expenses index missing. Falling back to unsorted expenses.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load expenses",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                expensesLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                expenses = docs.mapNotNull { doc ->
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    ExpenseDoc(
                        id = doc.id,
                        amount = amount,
                        category = doc.getString("category"),
                        date = doc.getTimestamp("date")
                    )
                }
            }

        val membersReg = db.collection("users")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    membersLoaded = true
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load members",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                membersLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                members = docs.map { doc ->
                    UserDoc(id = doc.id, displayName = doc.getString("displayName"))
                }
            }

        val suggestionsReg = db.collection("suggestions")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    suggestionsLoaded = true
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load suggestions",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                suggestionsLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                suggestions = docs.mapNotNull { doc ->
                    val name = doc.getString("itemName") ?: return@mapNotNull null
                    val count = doc.getLong("count") ?: 0L
                    SuggestionDoc(
                        id = doc.id,
                        name = name,
                        count = count,
                        lastBoughtAt = doc.getTimestamp("lastBoughtAt")
                    )
                }
                    .sortedWith(
                        compareByDescending<SuggestionDoc> { it.count }
                            .thenByDescending { it.lastBoughtAt?.seconds ?: 0L }
                    )
            }

        val budgetsReg = db.collection("budgets")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    budgetsLoaded = true
                    Toast.makeText(
                        context,
                        error.message ?: "Failed to load budgets",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                budgetsLoaded = true
                val docs = snapshot?.documents ?: emptyList()
                budgets = docs.mapNotNull { doc ->
                    val category = doc.getString("category") ?: return@mapNotNull null
                    val limit = doc.getDouble("limit") ?: return@mapNotNull null
                    val period = doc.getString("period") ?: "monthly"
                    BudgetDoc(
                        id = doc.id,
                        category = category,
                        limit = limit,
                        period = period
                    )
                }
            }

        val familyReg = db.collection("families").document(familyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                familyOwnerId = snapshot?.getString("createdBy")
            }

        onDispose {
            listsReg.remove()
            itemsReg.remove()
            expenseReg.remove()
            membersReg.remove()
            suggestionsReg.remove()
            budgetsReg.remove()
            familyReg.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ShoppE",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Family: $familyName",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        requestSearchFocus = true
                        selectedTab = "lists"
                        if (selectedListId == null && lists.isNotEmpty()) {
                            selectedListId = lists.first().id
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(tabs.indexOfFirst { it.key == "lists" })
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { isCreateListOpen = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    Box {
                        IconButton(onClick = { isProfileMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile menu"
                            )
                        }
                        DropdownMenu(
                            expanded = isProfileMenuOpen,
                            onDismissRequest = { isProfileMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                onClick = {
                                    isProfileMenuOpen = false
                                    selectedTab = "profile"
                                    scope.launch {
                                        pagerState.animateScrollToPage(tabs.indexOfFirst { it.key == "profile" })
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Family settings") },
                                onClick = {
                                    isProfileMenuOpen = false
                                    selectedTab = "profile"
                                    scope.launch {
                                        pagerState.animateScrollToPage(tabs.indexOfFirst { it.key == "profile" })
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                onClick = {
                                    isProfileMenuOpen = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab.key
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            selectedTab = tab.key
                            scope.launch {
                                pagerState.animateScrollToPage(tabs.indexOfFirst { it.key == tab.key })
                            }
                        },
                        icon = {
                            val icon = when (tab.key) {
                                "home" -> Icons.Filled.Home
                                "lists" -> Icons.Filled.ListAlt
                                "spending" -> Icons.Filled.Payments
                                else -> Icons.Filled.AccountCircle
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(text = tab.label, maxLines = 1)
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        val backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            HorizontalPager(state = pagerState) { page ->
                when (tabs[page].key) {
                    "home" -> HomeTab(
                        signedInLabel = displayLabel,
                        lists = lists,
                        listItems = listItems,
                        expenses = expenses,
                        members = members,
                        listsLoaded = listsLoaded,
                        itemsLoaded = itemsLoaded,
                        expensesLoaded = expensesLoaded,
                        membersLoaded = membersLoaded,
                        userId = userId,
                        onOpenLists = {
                            selectedTab = "lists"
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenSpending = {
                            selectedTab = "spending"
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onOpenProfile = {
                            selectedTab = "profile"
                            scope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onAddList = { isCreateListOpen = true },
                        onAddExpense = { isAddExpenseOpen = true }
                    )
                    "lists" -> ListsTab(
                        lists = lists,
                        listItems = listItems,
                        members = members,
                        suggestions = suggestions,
                        suggestionsLoaded = suggestionsLoaded,
                        listsLoaded = listsLoaded,
                        itemsLoaded = itemsLoaded,
                        hasMoreLists = lists.size >= listLimit,
                        hasMoreItems = listItems.size >= itemLimit,
                        selectedListId = selectedListId,
                        userId = userId,
                        requestSearchFocus = requestSearchFocus,
                        onSearchFocusHandled = { requestSearchFocus = false },
                        onSelectList = { selectedListId = it },
                        onBackToLists = { selectedListId = null },
                        onAddList = { isCreateListOpen = true },
                        onLoadMoreLists = { listLimit += 50 },
                        onLoadMoreItems = { itemLimit += 100 },
                        onAddItem = { listId, name ->
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                Toast.makeText(context, "Enter an item name", Toast.LENGTH_SHORT).show()
                                return@ListsTab
                            }
                            if (familyId.isNullOrBlank()) {
                                Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                                return@ListsTab
                            }
                            val data = mapOf(
                                "name" to trimmed,
                                "familyId" to familyId,
                                "listId" to listId,
                                "status" to "todo",
                                "assignedTo" to null,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                                "createdBy" to userId
                            )
                            db.collection("listItems").add(data)
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to add item",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onUpdateItem = { itemId, name ->
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                Toast.makeText(context, "Enter an item name", Toast.LENGTH_SHORT).show()
                                return@ListsTab
                            }
                            db.collection("listItems").document(itemId)
                                .update(
                                    mapOf(
                                        "name" to trimmed,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to update item",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onToggleStatus = { item, newStatus ->
                            val updates = mutableMapOf<String, Any?>(
                                "status" to newStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                            if (newStatus == "bought") {
                                updates["lastBoughtAt"] = FieldValue.serverTimestamp()
                            }
                            db.collection("listItems").document(item.id)
                                .update(updates)
                                .addOnSuccessListener {
                                    if (newStatus == "bought" && !familyId.isNullOrBlank()) {
                                        recordSuggestion(
                                            db = db,
                                            familyId = familyId,
                                            itemName = item.name,
                                            userId = userId
                                        )
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to update status",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onAssignItem = { itemId, assigneeId ->
                            db.collection("listItems").document(itemId)
                                .update(
                                    mapOf(
                                        "assignedTo" to assigneeId,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to assign item",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onDeleteList = { listId ->
                            if (familyId.isNullOrBlank()) {
                                Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                                return@ListsTab
                            }
                            db.collection("listItems")
                                .whereEqualTo("listId", listId)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.documents.forEach { doc ->
                                        doc.reference.delete()
                                    }
                                    db.collection("lists").document(listId).delete()
                                        .addOnSuccessListener {
                                            selectedListId = null
                                            Toast.makeText(context, "List deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                e.message ?: "Failed to delete list",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to delete list items",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onDeleteItem = { itemId ->
                            db.collection("listItems").document(itemId).delete()
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to delete item",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                    "spending" -> SpendingTab(
                        expenses = expenses,
                        expensesLoaded = expensesLoaded,
                        hasMoreExpenses = expenses.size >= expenseLimit,
                        budgets = budgets,
                        budgetsLoaded = budgetsLoaded,
                        onLoadMoreExpenses = { expenseLimit += 50 },
                        onSaveBudget = { category, limit, period ->
                            val trimmedCategory = category.trim().ifEmpty { "General" }
                            val parsedLimit = limit.trim().toDoubleOrNull()
                            if (parsedLimit == null || parsedLimit <= 0) {
                                Toast.makeText(context, "Enter a valid budget amount", Toast.LENGTH_SHORT).show()
                                return@SpendingTab
                            }
                            if (familyId.isNullOrBlank()) {
                                Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                                return@SpendingTab
                            }
                            val budgetId = budgetDocId(familyId, trimmedCategory, period)
                            val ref = db.collection("budgets").document(budgetId)
                            db.runTransaction { transaction ->
                                val snapshot = transaction.get(ref)
                                val data = mutableMapOf<String, Any?>(
                                    "familyId" to familyId,
                                    "category" to trimmedCategory,
                                    "limit" to parsedLimit,
                                    "period" to period,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                                if (!snapshot.exists()) {
                                    data["createdAt"] = FieldValue.serverTimestamp()
                                    data["createdBy"] = userId
                                }
                                transaction.set(ref, data, SetOptions.merge())
                            }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to save budget",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onDeleteBudget = { budget ->
                            db.collection("budgets").document(budget.id).delete()
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Failed to delete budget",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                    "profile" -> {
                        ProfileTab(
                            signedInLabel = displayLabel,
                            familyName = familyName,
                            familyId = familyId,
                            userId = userId,
                            currentDisplayName = currentDisplayName,
                            onSignOut = onSignOut,
                            members = members,
                            familyOwnerId = familyOwnerId,
                            onFamilyCleared = onFamilyCleared
                        )
                    }
                }
            }
        }
    }

    if (isCreateListOpen) {
        CreateListDialog(
            onDismiss = { isCreateListOpen = false },
            onSave = { name ->
                val trimmed = name.trim()
                if (trimmed.isEmpty()) {
                    Toast.makeText(context, "Enter a list name", Toast.LENGTH_SHORT).show()
                    return@CreateListDialog
                }
                if (familyId.isNullOrBlank()) {
                    Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                    return@CreateListDialog
                }
                val data = mapOf(
                    "name" to trimmed,
                    "familyId" to familyId,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "createdBy" to userId
                )
                db.collection("lists").add(data)
                    .addOnSuccessListener {
                        isCreateListOpen = false
                        Toast.makeText(context, "List created", Toast.LENGTH_SHORT).show()
                        selectedTab = "lists"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            e.message ?: "Failed to create list",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }

    if (isAddExpenseOpen) {
        AddExpenseDialog(
            onDismiss = { isAddExpenseOpen = false },
            onSave = { amount, category ->
                val value = amount.trim().toDoubleOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@AddExpenseDialog
                }
                if (familyId.isNullOrBlank()) {
                    Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                    return@AddExpenseDialog
                }
                val data = mapOf(
                    "familyId" to familyId,
                    "amount" to value,
                    "category" to category.trim().ifEmpty { "General" },
                    "date" to Timestamp.now(),
                    "createdBy" to userId
                )
                db.collection("expenses").add(data)
                    .addOnSuccessListener {
                        isAddExpenseOpen = false
                        Toast.makeText(context, "Expense added", Toast.LENGTH_SHORT).show()
                        selectedTab = "spending"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            e.message ?: "Failed to add expense",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }
}

@Composable
private fun HomeTab(
    signedInLabel: String,
    lists: List<ListDoc>,
    listItems: List<ListItemDoc>,
    expenses: List<ExpenseDoc>,
    members: List<UserDoc>,
    listsLoaded: Boolean,
    itemsLoaded: Boolean,
    expensesLoaded: Boolean,
    membersLoaded: Boolean,
    userId: String,
    onOpenLists: () -> Unit,
    onOpenSpending: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddList: () -> Unit,
    onAddExpense: () -> Unit
) {
    val scrollState = rememberScrollState()
    val itemsByList = remember(listItems) { listItems.groupBy { it.listId } }
    val assignedItems = remember(listItems, userId) {
        listItems.filter { it.assignedTo == userId && it.status != "bought" }
    }
    val recentActivities = remember(listItems, expenses) {
        val itemEvents = listItems.mapNotNull { item ->
            val time = item.updatedAt ?: item.createdAt ?: return@mapNotNull null
            val label = when (item.status) {
                "bought" -> "Marked ${item.name} bought"
                "in_cart" -> "Moved ${item.name} to cart"
                else -> "Updated ${item.name}"
            }
            ActivityEntry(label, time)
        }
        val expenseEvents = expenses.mapNotNull { exp ->
            val time = exp.date ?: return@mapNotNull null
            val label = "Logged $${"%.2f".format(exp.amount)}"
            ActivityEntry(label, time)
        }
        (itemEvents + expenseEvents)
            .sortedByDescending { it.timestamp.seconds }
            .take(3)
    }
    val weeklyExpenses = remember(expenses) { filterExpensesByRange(expenses, "weekly") }
    val weeklySpend = remember(weeklyExpenses) { weeklyExpenses.sumOf { it.amount } }
    val topCategories = remember(weeklyExpenses) {
        weeklyExpenses.groupBy { it.category ?: "General" }
            .toList()
            .sortedByDescending { it.second.sumOf { exp -> exp.amount } }
            .take(3)
            .map { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        HeroCard(signedInLabel = signedInLabel)
        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(title = "Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                title = "Add list",
                subtitle = "Create a new list",
                modifier = Modifier.weight(1f),
                onClick = onAddList
            )
            ActionCard(
                title = "Log spend",
                subtitle = "Add an expense",
                modifier = Modifier.weight(1f),
                onClick = onAddExpense
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Today's lists")
        when {
            !listsLoaded -> LoadingStateCard(text = "Loading lists...")
            lists.isEmpty() -> EmptyStateCard(text = "No lists yet. Create your first list.")
            else -> {
                lists.take(3).forEach { list ->
                    val items = itemsByList[list.id].orEmpty()
                    val assigned = items.count { it.assignedTo != null }
                    val subtitle = "${items.size} items - $assigned assigned"
                    HomeListRow(
                        title = list.name,
                        subtitle = subtitle,
                        status = if (items.isEmpty()) "No items" else "${items.count { it.status != "bought" }} left",
                        onClick = onOpenLists
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Assigned to me")
        when {
            !itemsLoaded -> LoadingStateCard(text = "Loading assignments...")
            assignedItems.isEmpty() -> EmptyStateCard(text = "Nothing assigned to you right now.")
            else -> {
                assignedItems.take(3).forEach { item ->
                    val listName = lists.firstOrNull { it.id == item.listId }?.name ?: "List"
                    AssignedItemRow(
                        item = item.name,
                        list = listName,
                        status = item.status ?: "Todo",
                        onClick = onOpenLists
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Recent activity")
        when {
            !itemsLoaded && !expensesLoaded -> LoadingStateCard(text = "Loading activity...")
            recentActivities.isEmpty() -> EmptyStateCard(text = "No recent updates yet.")
            else -> {
                recentActivities.forEach { activity ->
                    ActivityRow(
                        label = activity.label,
                        value = formatTimestamp(activity.timestamp),
                        onClick = onOpenSpending
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Spending snapshot")
        if (!expensesLoaded) {
            LoadingStateCard(text = "Loading spending...")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "This week", value = "$${"%.2f".format(weeklySpend)}")
                StatCard(
                    title = "Top category",
                    value = topCategories.firstOrNull() ?: "General"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (topCategories.isEmpty()) {
                EmptyStateCard(text = "No spending logged yet.")
            } else {
                CategoryRow(categories = topCategories, onClick = onOpenSpending)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Family members")
        when {
            !membersLoaded -> LoadingStateCard(text = "Loading family...")
            members.isEmpty() -> EmptyStateCard(text = "Invite family members to see them here.")
            else -> {
                members.take(3).forEach { member ->
                    FamilyMemberRow(
                        name = member.displayName ?: member.id,
                        status = "Active",
                        initials = initialsFor(member.displayName ?: member.id),
                        onClick = onOpenProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(signedInLabel: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Good evening, $signedInLabel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "You are on top of 3 active lists today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(label = "Items left", value = "8")
                    MiniStat(label = "Due today", value = "2")
                    MiniStat(label = "Spend", value = "$36")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ListsTab(
    lists: List<ListDoc>,
    listItems: List<ListItemDoc>,
    members: List<UserDoc>,
    suggestions: List<SuggestionDoc>,
    suggestionsLoaded: Boolean,
    listsLoaded: Boolean,
    itemsLoaded: Boolean,
    hasMoreLists: Boolean,
    hasMoreItems: Boolean,
    selectedListId: String?,
    userId: String,
    requestSearchFocus: Boolean,
    onSearchFocusHandled: () -> Unit,
    onSelectList: (String) -> Unit,
    onBackToLists: () -> Unit,
    onAddList: () -> Unit,
    onLoadMoreLists: () -> Unit,
    onLoadMoreItems: () -> Unit,
    onAddItem: (String, String) -> Unit,
    onUpdateItem: (String, String) -> Unit,
    onToggleStatus: (ListItemDoc, String) -> Unit,
    onAssignItem: (String, String?) -> Unit,
    onDeleteList: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var isAddItemOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ListItemDoc?>(null) }
    var assigningItem by remember { mutableStateOf<ListItemDoc?>(null) }
    var deletingItem by remember { mutableStateOf<ListItemDoc?>(null) }
    var deletingListId by remember { mutableStateOf<String?>(null) }
    var isDeleteListOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var isFilterMenuOpen by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    if (selectedListId == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SectionHeader(title = "Your lists")
            when {
                !listsLoaded -> LoadingStateCard(text = "Loading lists...")
                lists.isEmpty() -> EmptyStateCard(text = "No lists yet. Create your first list.")
                else -> {
                    lists.forEach { list ->
                        val items = listItems.filter { it.listId == list.id }
                        val assigned = items.count { it.assignedTo != null }
                        val subtitle = "${items.size} items - $assigned assigned"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                HomeListRow(
                                    title = list.name,
                                    subtitle = subtitle,
                                    status = if (items.isEmpty()) "No items" else "${items.count { it.status != "bought" }} left",
                                    onClick = { onSelectList(list.id) }
                                )
                            }
                            IconButton(onClick = { deletingListId = list.id }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete list")
                            }
                        }
                    }
                }
            }
            if (hasMoreLists) {
                OutlinedButton(
                    onClick = onLoadMoreLists,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = "Load more lists")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onAddList) {
                Text(text = "Create new list")
            }
        }
        deletingListId?.let { listId ->
            val name = lists.firstOrNull { it.id == listId }?.name ?: "this list"
            ConfirmDeleteDialog(
                title = "Delete list",
                message = "Delete $name and all its items?",
                onConfirm = {
                    onDeleteList(listId)
                    deletingListId = null
                },
                onDismiss = { deletingListId = null }
            )
        }
        return
    }

    val listName = lists.firstOrNull { it.id == selectedListId }?.name ?: "List"
    val itemsForList = listItems.filter { it.listId == selectedListId }
    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredItems = itemsForList.filter { item ->
        val matchesQuery = normalizedQuery.isBlank() ||
            item.name.lowercase().contains(normalizedQuery)
        val matchesStatus = when (statusFilter) {
            "todo" -> item.status == "todo" || item.status == null
            "in_cart" -> item.status == "in_cart"
            "bought" -> item.status == "bought"
            "assigned" -> item.assignedTo == userId
            else -> true
        }
        matchesQuery && matchesStatus
    }
    val pendingItems = filteredItems.filter { it.status != "bought" }
    val boughtItems = filteredItems.filter { it.status == "bought" }

    LaunchedEffect(requestSearchFocus, selectedListId) {
        if (requestSearchFocus && selectedListId != null) {
            searchFocusRequester.requestFocus()
            onSearchFocusHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToLists) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = listName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { isDeleteListOpen = true }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete list")
                }
                IconButton(onClick = { isAddItemOpen = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add item")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search items") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { isFilterMenuOpen = true }) {
                Text(text = "Filter: ${formatFilterLabel(statusFilter)}")
            }
            DropdownMenu(
                expanded = isFilterMenuOpen,
                onDismissRequest = { isFilterMenuOpen = false }
            ) {
                listOf("all", "todo", "in_cart", "bought", "assigned").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatFilterLabel(option)) },
                        onClick = {
                            statusFilter = option
                            isFilterMenuOpen = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Suggestions")
        when {
            !suggestionsLoaded -> LoadingStateCard(text = "Loading suggestions...")
            suggestions.isEmpty() -> EmptyStateCard(text = "No suggestions yet. Mark items bought to build them.")
            else -> {
                suggestions.take(5).forEach { suggestion ->
                    SuggestionRow(
                        name = suggestion.name,
                        count = suggestion.count,
                        lastBoughtAt = suggestion.lastBoughtAt,
                        onAdd = { onAddItem(selectedListId, suggestion.name) }
                    )
                }
            }
        }
        when {
            !itemsLoaded -> LoadingStateCard(text = "Loading items...")
            itemsForList.isEmpty() -> EmptyStateCard(text = "No items yet. Add the first one.")
            filteredItems.isEmpty() -> EmptyStateCard(text = "No items match this filter.")
            else -> {
                if (pendingItems.isNotEmpty()) {
                    SectionHeader(title = "To buy (${pendingItems.size})")
                    pendingItems.forEach { item ->
                        val assignee = members.firstOrNull { it.id == item.assignedTo }
                        ListItemRow(
                            item = item,
                            assigneeName = assignee?.displayName ?: assignee?.id,
                            onToggleStatus = {
                                val newStatus = if (item.status == "bought") "todo" else "bought"
                                onToggleStatus(item, newStatus)
                            },
                            onEdit = { editingItem = item },
                            onAssign = { assigningItem = item },
                            onDelete = { deletingItem = item }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (boughtItems.isNotEmpty()) {
                    SectionHeader(title = "Bought (${boughtItems.size})")
                    boughtItems.forEach { item ->
                        val assignee = members.firstOrNull { it.id == item.assignedTo }
                        ListItemRow(
                            item = item,
                            assigneeName = assignee?.displayName ?: assignee?.id,
                            onToggleStatus = {
                                val newStatus = if (item.status == "bought") "todo" else "bought"
                                onToggleStatus(item, newStatus)
                            },
                            onEdit = { editingItem = item },
                            onAssign = { assigningItem = item },
                            onDelete = { deletingItem = item }
                        )
                    }
                }
                if (hasMoreItems) {
                    OutlinedButton(
                        onClick = onLoadMoreItems,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(text = "Load more items")
                    }
                }
            }
        }
    }

    if (isAddItemOpen) {
        AddItemDialog(
            onDismiss = { isAddItemOpen = false },
            onSave = { name ->
                onAddItem(selectedListId, name)
                isAddItemOpen = false
            }
        )
    }

    editingItem?.let { item ->
        EditItemDialog(
            initialName = item.name,
            onDismiss = { editingItem = null },
            onSave = { name ->
                onUpdateItem(item.id, name)
                editingItem = null
            }
        )
    }

    assigningItem?.let { item ->
        AssignItemDialog(
            members = members,
            onDismiss = { assigningItem = null },
            onAssign = { assigneeId ->
                onAssignItem(item.id, assigneeId)
                assigningItem = null
            }
        )
    }

    deletingItem?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete item",
            message = "Delete ${item.name}?",
            onConfirm = {
                onDeleteItem(item.id)
                deletingItem = null
            },
            onDismiss = { deletingItem = null }
        )
    }

    if (isDeleteListOpen) {
        ConfirmDeleteDialog(
            title = "Delete list",
            message = "Delete $listName and all its items?",
            onConfirm = {
                onDeleteList(selectedListId)
                isDeleteListOpen = false
            },
            onDismiss = { isDeleteListOpen = false }
        )
    }

    deletingListId?.let { listId ->
        val name = lists.firstOrNull { it.id == listId }?.name ?: "this list"
        ConfirmDeleteDialog(
            title = "Delete list",
            message = "Delete $name and all its items?",
            onConfirm = {
                onDeleteList(listId)
                deletingListId = null
            },
            onDismiss = { deletingListId = null }
        )
    }
}

@Composable
private fun SpendingTab(
    expenses: List<ExpenseDoc>,
    expensesLoaded: Boolean,
    hasMoreExpenses: Boolean,
    budgets: List<BudgetDoc>,
    budgetsLoaded: Boolean,
    onLoadMoreExpenses: () -> Unit,
    onSaveBudget: (String, String, String) -> Unit,
    onDeleteBudget: (BudgetDoc) -> Unit
) {
    val scrollState = rememberScrollState()
    var range by remember { mutableStateOf("monthly") }
    var isRangeMenuOpen by remember { mutableStateOf(false) }
    var isAddBudgetOpen by remember { mutableStateOf(false) }

    val weeklyExpenses = remember(expenses) { filterExpensesByRange(expenses, "weekly") }
    val monthlyExpenses = remember(expenses) { filterExpensesByRange(expenses, "monthly") }
    val rangeExpenses = if (range == "weekly") weeklyExpenses else monthlyExpenses
    val totalSpend = remember(rangeExpenses) { rangeExpenses.sumOf { it.amount } }
    val categories = remember(rangeExpenses) {
        rangeExpenses.groupBy { it.category ?: "General" }
            .toList()
            .sortedByDescending { it.second.sumOf { exp -> exp.amount } }
            .map { it.first }
    }
    val recentExpenses = remember(expenses) {
        expenses.sortedByDescending { it.date?.seconds ?: 0L }.take(5)
    }
    val weeklyByCategory = remember(weeklyExpenses) { sumByCategory(weeklyExpenses) }
    val monthlyByCategory = remember(monthlyExpenses) { sumByCategory(monthlyExpenses) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        SectionHeader(title = "Spending")
        if (!expensesLoaded) {
            LoadingStateCard(text = "Loading expenses...")
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCard(
                title = if (range == "weekly") "This week" else "This month",
                value = "$${"%.2f".format(totalSpend)}",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box {
                OutlinedButton(onClick = { isRangeMenuOpen = true }) {
                    Text(text = if (range == "weekly") "Weekly" else "Monthly")
                }
                DropdownMenu(
                    expanded = isRangeMenuOpen,
                    onDismissRequest = { isRangeMenuOpen = false }
                ) {
                    listOf("weekly", "monthly").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(if (option == "weekly") "Weekly" else "Monthly") },
                            onClick = {
                                range = option
                                isRangeMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(
            title = "Top category",
            value = categories.firstOrNull() ?: "General",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Spending pulse")
        if (expenses.isEmpty()) {
            EmptyStateCard(text = "No expenses yet.")
        } else {
            ChartPlaceholder()
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(title = "Categories")
        if (categories.isEmpty()) {
            EmptyStateCard(text = "No categories yet.")
        } else {
            CategoryRow(categories = categories, onClick = {})
        }
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Budgets")
        when {
            !budgetsLoaded -> LoadingStateCard(text = "Loading budgets...")
            budgets.isEmpty() -> EmptyStateCard(text = "No budgets yet. Add one to track alerts.")
            else -> {
                budgets.forEach { budget ->
                    val spendMap = if (budget.period == "weekly") weeklyByCategory else monthlyByCategory
                    val spent = spendMap[budget.category] ?: 0.0
                    val overBy = spent - budget.limit
                    BudgetRow(
                        budget = budget,
                        spent = spent,
                        overBy = overBy,
                        onDelete = { onDeleteBudget(budget) }
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { isAddBudgetOpen = true },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(text = "Add budget")
        }
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Recent activity")
        if (recentExpenses.isEmpty()) {
            EmptyStateCard(text = "No recent expenses.")
        } else {
            recentExpenses.forEach { exp ->
                val label = exp.category ?: "General"
                val value = "$${"%.2f".format(exp.amount)}"
                ActivityRow(label = label, value = value)
            }
            if (hasMoreExpenses) {
                OutlinedButton(
                    onClick = onLoadMoreExpenses,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = "Load more expenses")
                }
            }
        }
    }

    if (isAddBudgetOpen) {
        AddBudgetDialog(
            onDismiss = { isAddBudgetOpen = false },
            onSave = { category, limit, period ->
                onSaveBudget(category, limit, period)
                isAddBudgetOpen = false
            }
        )
    }
}

@Composable
private fun ProfileTab(
    signedInLabel: String,
    familyName: String,
    familyId: String?,
    userId: String,
    currentDisplayName: String?,
    onSignOut: () -> Unit,
    members: List<UserDoc>,
    familyOwnerId: String?,
    onFamilyCleared: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val functions = remember { FirebaseFunctions.getInstance() }
    val clipboard = LocalClipboardManager.current
    var displayName by remember(currentDisplayName) {
        mutableStateOf(currentDisplayName ?: signedInLabel)
    }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var inviteExpiresAt by remember { mutableStateOf<Timestamp?>(null) }
    var isDisbandOpen by remember { mutableStateOf(false) }
    var disbandPhrase by remember { mutableStateOf("") }
    val isOwner = familyOwnerId != null && familyOwnerId == userId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(title = "Profile")
        StatCard(title = "Signed in as", value = signedInLabel, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(title = "Family", value = familyName, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Display name", style = MaterialTheme.typography.bodyMedium)
        TextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )
        OutlinedButton(
            onClick = {
                val trimmed = displayName.trim()
                if (trimmed.isEmpty()) {
                    Toast.makeText(context, "Enter a display name", Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }
                db.collection("users").document(userId)
                    .set(mapOf("displayName" to trimmed), SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Display name updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            e.message ?: "Failed to update display name",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(text = "Save")
        }
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Invite members")
        OutlinedButton(
            onClick = {
                if (familyId.isNullOrBlank()) {
                    Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }
                functions.getHttpsCallable("createInvite")
                    .call(mapOf("familyId" to familyId))
                    .addOnSuccessListener { result ->
                        val data = result.data as? Map<*, *> ?: emptyMap<Any, Any>()
                        val code = data["inviteCode"] as? String
                        val expiresMs = data["expiresAt"] as? Number
                        if (!code.isNullOrBlank()) {
                            inviteCode = code
                            inviteExpiresAt = expiresMs?.toLong()?.let { Timestamp(it / 1000, 0) }
                            Toast.makeText(context, "Invite code generated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invite created", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            e.message ?: "Failed to generate invite",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        ) {
            Text(text = "Generate invite code")
        }
        inviteCode?.let { code ->
            Spacer(modifier = Modifier.height(12.dp))
            StatCard(title = "Invite code", value = code, modifier = Modifier.fillMaxWidth())
            inviteExpiresAt?.let { expiresAt ->
                Text(
                    text = "Expires ${formatTimestamp(expiresAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = "Copy code")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Family members")
        if (members.isEmpty()) {
            Text(text = "No members found.")
        } else {
            members.forEach { member ->
                val label = member.displayName ?: member.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, fontWeight = FontWeight.SemiBold)
                        if (member.id == familyOwnerId) {
                            Text(
                                text = "Owner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isOwner && member.id != userId) {
                        OutlinedButton(
                            onClick = {
                                if (familyId.isNullOrBlank()) {
                                    Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                functions.getHttpsCallable("removeMember")
                                    .call(mapOf("familyId" to familyId, "memberId" to member.id))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            e.message ?: "Failed to remove member",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        ) {
                            Text(text = "Remove")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!isOwner) {
            OutlinedButton(
                onClick = {
                    if (familyId.isNullOrBlank()) {
                        Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    functions.getHttpsCallable("leaveFamily")
                        .call(mapOf("familyId" to familyId))
                        .addOnSuccessListener {
                            onFamilyCleared()
                            Toast.makeText(context, "You left the family", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                e.message ?: "Failed to leave family",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            ) {
                Text(text = "Leave family")
            }
        } else if (members.size == 1) {
            OutlinedButton(
                onClick = {
                    isDisbandOpen = true
                }
            ) {
                Text(text = "Disband family")
            }
        } else {
            Text(
                text = "To disband, remove all members first.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onSignOut) {
            Text(text = "Sign out")
        }
    }

    if (isDisbandOpen) {
        val confirmation = "DISBAND"
        AlertDialog(
            onDismissRequest = {
                isDisbandOpen = false
                disbandPhrase = ""
            },
            title = { Text("Disband family") },
            text = {
                Column {
                    Text(
                        text = "This permanently deletes lists, items, expenses, and invites.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type $confirmation to confirm.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = disbandPhrase,
                        onValueChange = { disbandPhrase = it },
                        label = { Text("Confirmation") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    enabled = disbandPhrase == confirmation,
                    onClick = {
                        if (familyId.isNullOrBlank()) {
                            Toast.makeText(context, "Family not ready yet", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        functions.getHttpsCallable("disbandFamily")
                            .call(mapOf("familyId" to familyId))
                            .addOnSuccessListener {
                                onFamilyCleared()
                                Toast.makeText(context, "Family disbanded", Toast.LENGTH_SHORT).show()
                                isDisbandOpen = false
                                disbandPhrase = ""
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: "Failed to disband family",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {
                    Text("Disband")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isDisbandOpen = false
                        disbandPhrase = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ListCard(
    title: String,
    subtitle: String,
    progressLabel: String,
    assignees: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(text = progressLabel)
                AvatarRow(assignees = assignees)
            }
        }
    }
}

@Composable
private fun ListItemRow(
    item: ListItemDoc,
    assigneeName: String?,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.SemiBold)
                val assigneeLabel = assigneeName ?: "Unassigned"
                Text(
                    text = assigneeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onToggleStatus) {
                    val icon = if (item.status == "bought") {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.RadioButtonUnchecked
                    }
                    Icon(imageVector = icon, contentDescription = "Toggle status")
                }
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit item")
                }
                IconButton(onClick = onAssign) {
                    Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = "Assign item")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete item")
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(label: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SuggestionRow(
    name: String,
    count: Long,
    lastBoughtAt: Timestamp?,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.SemiBold)
                val detail = buildString {
                    append("Bought ")
                    append(count)
                    append("x")
                    if (lastBoughtAt != null) {
                        append(" • last ")
                        append(formatTimestamp(lastBoughtAt))
                    }
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAdd) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add suggested item")
            }
        }
    }

}

@Composable
private fun HomeListRow(
    title: String,
    subtitle: String,
    status: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AssignedItemRow(item: String, list: String, status: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = item, fontWeight = FontWeight.SemiBold)
            Text(
                text = list,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Chip(text = status)
    }
}

@Composable
private fun FamilyMemberRow(
    name: String,
    status: String,
    initials: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "●",
            color = if (status == "Online") {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            fontSize = 18.sp
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun AvatarRow(assignees: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        assignees.take(3).forEach { initials ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartPlaceholder() {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.35f, 0.6f, 0.45f, 0.8f, 0.5f, 0.7f).forEach { height ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((height * 100).dp)
                            .background(gradient, MaterialTheme.shapes.small)
                    )
                }
            }
            Text(
                text = "Weekly trend",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun CategoryRow() {
    CategoryRow(
        categories = listOf("Groceries", "Pharmacy", "Hardware"),
        onClick = {}
    )
}

@Composable
private fun CategoryRow(categories: List<String>, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.take(3).forEachIndexed { index, label ->
            val color = when (index) {
                0 -> MaterialTheme.colorScheme.primary
                1 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            }
            CategoryPill(label = label, color = color, onClick = onClick)
        }
    }
}

@Composable
private fun BudgetRow(
    budget: BudgetDoc,
    spent: Double,
    overBy: Double,
    onDelete: () -> Unit
) {
    val overBudget = overBy > 0.0
    val statusColor = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusText = if (overBudget) {
        "Over by $${"%.2f".format(overBy)}"
    } else {
        "$${"%.2f".format(budget.limit - spent)} left"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${budget.category} (${formatFilterLabel(budget.period)})", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Spent $${"%.2f".format(spent)} / $${"%.2f".format(budget.limit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete budget")
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, color: Color, onClick: () -> Unit = {}) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)
        )
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingStateCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            OutlinedButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun filterExpensesByRange(expenses: List<ExpenseDoc>, range: String): List<ExpenseDoc> {
    val days = if (range == "weekly") 7 else 30
    val cutoffSeconds = Timestamp.now().seconds - days * 86_400L
    return expenses.filter { exp -> (exp.date?.seconds ?: 0L) >= cutoffSeconds }
}

private fun sumByCategory(expenses: List<ExpenseDoc>): Map<String, Double> {
    return expenses
        .groupBy { it.category ?: "General" }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    val date = Date(timestamp.seconds * 1000)
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(date)
}

private fun suggestionDocId(familyId: String, itemName: String): String {
    val normalized = itemName.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    val safe = if (normalized.isBlank()) "item" else normalized
    return "${familyId}_$safe"
}

private fun budgetDocId(familyId: String, category: String, period: String): String {
    val normalized = category.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    val safe = if (normalized.isBlank()) "general" else normalized
    return "${familyId}_${period}_$safe"
}


private fun formatFilterLabel(value: String): String {
    return value
        .split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
    return if (initials.isEmpty()) "-" else initials.joinToString("")
}

@Composable
private fun CreateListDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create list") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true
            )
        },
        confirmButton = {
            OutlinedButton(onClick = { onSave(name) }) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = { onSave(amount, category) }) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun recordSuggestion(
    db: FirebaseFirestore,
    familyId: String,
    itemName: String,
    userId: String
) {
    val trimmed = itemName.trim()
    if (trimmed.isEmpty()) {
        return
    }
    val suggestionId = suggestionDocId(familyId, trimmed)
    val ref = db.collection("suggestions").document(suggestionId)
    db.runTransaction { transaction ->
        val snapshot = transaction.get(ref)
        val currentCount = snapshot.getLong("count") ?: 0L
        val data = mutableMapOf<String, Any?>(
            "familyId" to familyId,
            "itemName" to trimmed,
            "count" to currentCount + 1,
            "lastBoughtAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snapshot.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
            data["createdBy"] = userId
        }
        transaction.set(ref, data, SetOptions.merge())
    }
}

@Composable
private fun AddBudgetDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var limit by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("monthly") }
    var isPeriodMenuOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add budget") },
        text = {
            Column {
                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Limit") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box {
                    OutlinedButton(onClick = { isPeriodMenuOpen = true }) {
                        Text(text = if (period == "weekly") "Weekly" else "Monthly")
                    }
                    DropdownMenu(
                        expanded = isPeriodMenuOpen,
                        onDismissRequest = { isPeriodMenuOpen = false }
                    ) {
                        listOf("weekly", "monthly").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (option == "weekly") "Weekly" else "Monthly") },
                                onClick = {
                                    period = option
                                    isPeriodMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = { onSave(category, limit, period) }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item name") },
                singleLine = true
            )
        },
        confirmButton = {
            OutlinedButton(onClick = { onSave(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditItemDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit item") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item name") },
                singleLine = true
            )
        },
        confirmButton = {
            OutlinedButton(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AssignItemDialog(
    members: List<UserDoc>,
    onDismiss: () -> Unit,
    onAssign: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to") },
        text = {
            Column {
                if (members.isEmpty()) {
                    Text("No family members found.")
                } else {
                    members.forEach { member ->
                        OutlinedButton(
                            onClick = { onAssign(member.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(member.displayName ?: member.id)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onAssign(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unassign")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
