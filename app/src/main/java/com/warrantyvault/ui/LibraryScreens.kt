package com.warrantyvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrantyvault.data.AttachmentType
import com.warrantyvault.data.WarrantyItemWithAttachments
import androidx.compose.ui.res.stringResource
import com.warrantyvault.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val alerts = state.items
        .filter { row ->
            listOf(row.item.warrantyEndDate, row.item.returnDeadline).any { date ->
                val days = daysUntil(date)
                days != null && days <= 30
            }
        }
        .sortedBy { row ->
            listOfNotNull(row.item.returnDeadline, row.item.warrantyEndDate).minOrNull() ?: Long.MAX_VALUE
        }

    VaultListScaffold(
        contentPadding = contentPadding,
        title = "Alerts",
        subtitle = "Deadlines that need attention",
        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = VaultAmber) },
        onBack = onBack
    ) {
        if (alerts.isEmpty()) {
            item {
                EmptyLibraryState(
                    title = "No active alerts",
                    body = "Upcoming warranty and return deadlines will appear here."
                )
            }
        } else {
            items(alerts, key = { it.item.id }) { row ->
                AlertCard(row = row, onClick = { onOpen(row.item.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val rows = state.items
        .filter { it.attachments.isNotEmpty() }
        .sortedByDescending { row -> row.attachments.maxOfOrNull { it.createdAt } ?: row.item.updatedAt }

    VaultListScaffold(
        contentPadding = contentPadding,
        title = "Receipts",
        subtitle = "Saved receipts, manuals, and photos",
        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = VaultMint) },
        onBack = onBack
    ) {
        if (rows.isEmpty()) {
            item {
                EmptyLibraryState(
                    title = "No receipts yet",
                    body = "Scan a receipt or attach a file from an item page to build this library.",
                    action = {
                        Button(onClick = onAdd) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.cd_camera))
                            Text("Scan receipt")
                        }
                    }
                )
            }
        } else {
            items(rows, key = { it.item.id }) { row ->
                ReceiptCard(row = row, onClick = { onOpen(row.item.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultListScaffold(
    contentPadding: PaddingValues,
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(padding)
                .background(
                    Brush.linearGradient(
                        listOf(VaultNight, VaultNightRaised, VaultNight)
                    )
                ),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    icon()
                    Column {
                        Text(title, color = VaultText, style = MaterialTheme.typography.headlineMedium)
                        Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            content()
        }
    }
}

@Composable
private fun AlertCard(row: WarrantyItemWithAttachments, onClick: () -> Unit) {
    val warrantyDays = daysUntil(row.item.warrantyEndDate)
    val returnDays = daysUntil(row.item.returnDeadline)
    LibraryCard(onClick = onClick) {
        Text(row.item.name, color = VaultText, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOf(row.item.storeOrBrand, row.item.category).filter { it.isNotBlank() }.joinToString(" / "),
            color = VaultTextMuted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeadlinePill("Warranty", warrantyDays, row.item.warrantyEndDate.formatDate())
            DeadlinePill("Return", returnDays, row.item.returnDeadline.formatDate())
        }
    }
}

@Composable
private fun ReceiptCard(row: WarrantyItemWithAttachments, onClick: () -> Unit) {
    LibraryCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.AttachFile, contentDescription = null, tint = VaultMint)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.item.name, color = VaultText, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${row.attachments.size} saved file${if (row.attachments.size == 1) "" else "s"}", color = VaultTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.attachments.groupBy { it.type }.forEach { (type, attachments) ->
                        AssistChip(onClick = {}, label = { Text("${type.label}: ${attachments.size}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DeadlinePill(label: String, days: Long?, date: String) {
    val value = when {
        date.isBlank() -> "Not set"
        days == null -> date
        days < 0 -> "Expired"
        days == 0L -> "Today"
        else -> "${days}d"
    }
    AssistChip(
        onClick = {},
        label = {
            Text("$label: $value", fontWeight = if (days != null && days <= 7) FontWeight.SemiBold else FontWeight.Medium)
        }
    )
}

@Composable
private fun EmptyLibraryState(
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = VaultText, style = MaterialTheme.typography.titleLarge)
            Text(body, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

private val AttachmentType.label: String
    get() = when (this) {
        AttachmentType.Receipt -> "Receipt"
        AttachmentType.Manual -> "Manual"
        AttachmentType.Photo -> "Photo"
        AttachmentType.File -> "File"
    }
