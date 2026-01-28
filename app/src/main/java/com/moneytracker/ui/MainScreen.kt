package com.moneytracker.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class AppTab(
    val key: String,
    val label: String,
    val shortLabel: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen(signedInLabel: String, familyName: String) {
    val tabs = remember {
        listOf(
            AppTab("home", "Home", "H"),
            AppTab("lists", "Lists", "L"),
            AppTab("spending", "Spending", "S"),
            AppTab("profile", "Profile", "P")
        )
    }
    var selectedTab by remember { mutableStateOf(tabs.first().key) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MoneyTracker",
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
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab.key,
                        onClick = { selectedTab = tab.key },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (selectedTab == tab.key) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        label = {
                            Text(text = tab.label, maxLines = 1)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "home" -> HomeTab(signedInLabel = signedInLabel)
                "lists" -> ListsTab()
                "spending" -> SpendingTab()
                "profile" -> ProfileTab(signedInLabel = signedInLabel, familyName = familyName)
            }
        }
    }
}

@Composable
private fun HomeTab(signedInLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Welcome back, $signedInLabel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Here is what is happening today.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                title = "Add list",
                subtitle = "Create a new list",
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Log spend",
                subtitle = "Add an expense",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Family overview")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(title = "Open items", value = "14")
            StatCard(title = "Bought this week", value = "32")
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(
            title = "Spend this month",
            value = "$248.50",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ListsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(title = "Your lists")
        ListCard(title = "Groceries", subtitle = "8 items • 3 assigned")
        ListCard(title = "Pharmacy", subtitle = "4 items • 1 in cart")
        ListCard(title = "Hardware", subtitle = "2 items • 0 bought")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = {}) {
            Text(text = "Create new list")
        }
    }
}

@Composable
private fun SpendingTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(title = "Spending")
        StatCard(title = "This month", value = "$248.50", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(title = "Top category", value = "Groceries", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Recent activity")
        ActivityRow(label = "Groceries", value = "$62.10")
        ActivityRow(label = "Pharmacy", value = "$18.25")
        ActivityRow(label = "Hardware", value = "$34.90")
    }
}

@Composable
private fun ProfileTab(signedInLabel: String, familyName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(title = "Profile")
        StatCard(title = "Signed in as", value = signedInLabel, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(title = "Family", value = familyName, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = {}) {
            Text(text = "Sign out")
        }
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
private fun ActionCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
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
            Text(text = title, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ListCard(title: String, subtitle: String) {
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
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActivityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}
