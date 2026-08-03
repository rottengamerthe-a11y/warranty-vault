package com.warrantyvault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrantyvault.data.WarrantyItemWithAttachments
import androidx.compose.ui.res.stringResource
import com.warrantyvault.R

@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onAdd: () -> Unit,
    onAlerts: () -> Unit,
    onReceipts: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    var exportPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) settingsViewModel.exportJson(it, exportPassword.takeIf { p -> p.isNotEmpty() })
        exportPassword = ""
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) settingsViewModel.exportCsv(it)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) settingsViewModel.restoreJson(it, restorePassword.takeIf { p -> p.isNotEmpty() })
        restorePassword = ""
    }
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    
    fun launchJsonExport() {
        exportPassword = ""
        showExportPasswordDialog = true
    }
    
    fun launchRestore() {
        restorePassword = ""
        showRestorePasswordDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(HomeGradients.screen)
    ) {
        AmbientColorFields()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 184.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader(
                    totalCount = state.items.size,
                    expiringCount = state.items.count { row ->
                        listOf(row.item.warrantyEndDate, row.item.returnDeadline).any { date ->
                            val days = daysUntil(date)
                            days != null && days in 0..30
                        }
                    },
                    menuOpen = menuOpen,
                    onMenuOpenChange = { menuOpen = it },
                    onCsvExport = { csvExport.launch("warranty-vault.csv") },
                    onJsonExport = { launchJsonExport() },
                    onRestore = { launchRestore() },
                    onSettings = onSettings
                )
            }
            item {
                SearchAndFilters(
                    state = state,
                    onQueryChange = { viewModel.query.value = it },
                    onDeadlineFilter = { viewModel.deadlineFilter.value = it },
                    onCategory = { viewModel.selectedCategory.value = it }
                )
            }
            if (state.items.isEmpty()) {
                item { EmptyState(onAdd = onAdd) }
            } else {
                items(state.items, key = { it.item.id }) { row ->
                    WarrantyBentoCard(row = row, onClick = { onOpen(row.item.id) })
                }
            }
        }

        ScrollGlassScrim(
            visible = listState.canScrollBackward,
            alignment = Alignment.TopCenter,
            top = true
        )
        ScrollGlassScrim(
            visible = listState.canScrollForward,
            alignment = Alignment.BottomCenter,
            top = false
        )
        BottomChrome(
            onHome = {
                viewModel.query.value = ""
                viewModel.selectedCategory.value = null
                viewModel.deadlineFilter.value = DeadlineFilter.All
            },
            onAlerts = onAlerts,
            onReceipts = onReceipts,
            onAdd = onAdd
        )
    }

    // Export password dialog
    if (showExportPasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showExportPasswordDialog = false
                exportPassword = ""
            },
            title = { Text("Backup encryption") },
            text = {
                Column {
                    Text("Enter a password to encrypt your backup, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        jsonExport.launch("warranty-vault.json")
                        showExportPasswordDialog = false
                    }
                ) { Text("Backup") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportPasswordDialog = false
                    exportPassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Restore password dialog
    if (showRestorePasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                restorePassword = ""
            },
            title = { Text("Restore backup") },
            text = {
                Column {
                    Text("Enter the password if the backup is encrypted, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password (if encrypted)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        restore.launch(arrayOf("application/json"))
                        showRestorePasswordDialog = false
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    restorePassword = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BoxScope.ScrollGlassScrim(
    visible: Boolean,
    alignment: Alignment,
    top: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "scrollGlassScrim"
    )
    val colors = if (top) {
        listOf(
            VaultNight.copy(alpha = 0.82f),
            VaultNightRaised.copy(alpha = 0.48f),
            Color.Transparent
        )
    } else {
        listOf(
            Color.Transparent,
            VaultNightRaised.copy(alpha = 0.52f),
            VaultNight.copy(alpha = 0.88f)
        )
    }

    Box(
        modifier = Modifier
            .align(alignment)
            .fillMaxWidth()
            .height(if (top) 112.dp else 196.dp)
            .graphicsLayer { this.alpha = alpha }
            .blur(10.dp)
            .background(Brush.verticalGradient(colors))
    )
}

@Composable
private fun DashboardHeader(
    totalCount: Int,
    expiringCount: Int,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onCsvExport: () -> Unit,
    onJsonExport: () -> Unit,
    onRestore: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = dynamicGreeting(),
                    color = VaultTextMuted,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "WarrantyVault",
                    color = VaultText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Box {
                GlassIconButton(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    onClick = { onMenuOpenChange(true) }
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                    ActionMenuItem(Icons.Default.CloudDownload, "Export CSV") {
                        onMenuOpenChange(false)
                        onCsvExport()
                    }
                    ActionMenuItem(Icons.Default.CloudDownload, "Backup JSON") {
                        onMenuOpenChange(false)
                        onJsonExport()
                    }
                    ActionMenuItem(Icons.Default.CloudUpload, "Restore JSON") {
                        onMenuOpenChange(false)
                        onRestore()
                    }
                    ActionMenuItem(Icons.Default.Settings, "Settings") {
                        onMenuOpenChange(false)
                        onSettings()
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricGlassCard(
                label = "Tracked items",
                value = totalCount.toString(),
                tint = VaultMint,
                modifier = Modifier.weight(1f)
            )
            MetricGlassCard(
                label = "Need attention",
                value = expiringCount.toString(),
                tint = VaultAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchAndFilters(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onDeadlineFilter: (DeadlineFilter) -> Unit,
    onCategory: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
            singleLine = true,
            placeholder = { Text("Search items, stores, serials") },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = VaultGlass,
                unfocusedContainerColor = VaultGlass,
                focusedIndicatorColor = VaultGlassBorder,
                unfocusedIndicatorColor = VaultGlassBorder,
                focusedTextColor = VaultText,
                unfocusedTextColor = VaultText,
                focusedPlaceholderColor = VaultTextMuted,
                unfocusedPlaceholderColor = VaultTextMuted,
                focusedLeadingIconColor = VaultMint,
                unfocusedLeadingIconColor = VaultTextMuted,
                focusedTrailingIconColor = VaultTextMuted,
                unfocusedTrailingIconColor = VaultTextMuted
            )
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DeadlineFilter.entries) { filter ->
                FilterToken(
                    label = filter.label,
                    selected = state.filter == filter,
                    onClick = { onDeadlineFilter(filter) }
                )
            }
            if (state.categories.isNotEmpty()) {
                item {
                    FilterToken(
                        label = "All categories",
                        selected = state.selectedCategory == null,
                        onClick = { onCategory(null) }
                    )
                }
                items(state.categories) { category ->
                    FilterToken(
                        label = category,
                        selected = state.selectedCategory == category,
                        onClick = { onCategory(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WarrantyBentoCard(row: WarrantyItemWithAttachments, onClick: () -> Unit) {
    val item = row.item
    val warrantyDays = daysUntil(item.warrantyEndDate)
    val returnDays = daysUntil(item.returnDeadline)
    val accent = accentFor(item.id)
    val interactionSource = remember { MutableInteractionSource() }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        brush = Brush.linearGradient(listOf(accent.copy(alpha = 0.20f), VaultGlassStrong))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = item.name,
                        color = VaultText,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(item.category, item.storeOrBrand).filter { it.isNotBlank() }.joinToString(" / "),
                        color = VaultTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AttachmentBadge(count = row.attachments.size, accent = accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DeadlineChip("Warranty", warrantyDays, item.warrantyEndDate.formatDate(), Modifier.weight(1f))
                DeadlineChip("Return", returnDays, item.returnDeadline.formatDate(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val ctaInteraction = remember { MutableInteractionSource() }
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(VaultMint.copy(alpha = 0.36f), VaultGlass))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = VaultMint
                )
            }
            Text("Your vault is ready", color = VaultText, style = MaterialTheme.typography.titleLarge)
            Text(
                "Scan the first receipt to track warranties, manuals, and return windows.",
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .height(52.dp)
                    .pressScale(ctaInteraction),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultMint, contentColor = Color(0xFF102219)),
                interactionSource = ctaInteraction
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.cd_camera))
                Spacer(Modifier.width(8.dp))
                Text("Scan receipt")
            }
        }
    }
}

@Composable
private fun BoxScope.BottomChrome(
    onHome: () -> Unit,
    onAlerts: () -> Unit,
    onReceipts: () -> Unit,
    onAdd: () -> Unit
) {
    val ctaInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .pressScale(ctaInteraction),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VaultMint, contentColor = Color(0xFF102219)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            interactionSource = ctaInteraction
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.cd_camera))
            Spacer(Modifier.width(10.dp))
            Text("Scan new receipt", style = MaterialTheme.typography.labelLarge)
        }
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(Icons.Default.GridView, "Home", selected = true, onClick = onHome)
                BottomNavItem(Icons.Default.NotificationsActive, "Alerts", selected = false, onClick = onAlerts)
                BottomNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Receipts", selected = false, onClick = onReceipts)
                BottomNavItem(Icons.Default.Add, "Add", selected = false, onClick = onAdd)
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .size(width = 58.dp, height = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) VaultMint else VaultTextMuted)
        Text(label, color = if (selected) VaultText else VaultTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MetricGlassCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier.height(96.dp), brush = Brush.linearGradient(listOf(tint.copy(alpha = 0.18f), VaultGlass))) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(value, color = VaultText, style = MaterialTheme.typography.headlineMedium)
            Text(label, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FilterToken(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(48.dp)
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) VaultMint.copy(alpha = 0.22f) else VaultGlass)
            .border(1.dp, if (selected) VaultMint.copy(alpha = 0.62f) else VaultGlassBorder, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) VaultText else VaultTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DeadlineChip(label: String, days: Long?, date: String, modifier: Modifier = Modifier) {
    val text = when {
        date.isBlank() -> "No $label"
        days == null -> date
        days < 0 -> "Expired"
        days == 0L -> "Today"
        else -> "${days}d left"
    }
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(VaultGlass)
            .border(BorderStroke(1.dp, VaultGlassBorder), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(text, color = VaultText, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun AttachmentBadge(count: Int, accent: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(count.coerceAtLeast(0).toString(), color = accent, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(VaultGlass)
            .border(1.dp, VaultGlassBorder, CircleShape)
            .pressScale()
    ) {
        Icon(icon, contentDescription = contentDescription, tint = VaultText)
    }
}

@Composable
private fun ActionMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    brush: Brush = Brush.linearGradient(listOf(VaultGlassStrong, VaultGlass)),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(18.dp, shape, ambientColor = Color(0x3312171C), spotColor = Color(0x4012171C))
            .clip(shape)
            .background(brush)
            .border(1.dp, VaultGlassBorder, shape)
    ) {
        content()
    }
}

@Composable
private fun AmbientColorFields() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VaultMint.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(120f, 120f),
                    radius = 520f
                )
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(VaultCoral.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(900f, 1350f),
                    radius = 760f
                )
            )
    )
}

@Composable
private fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun Modifier.pressScale(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private fun dynamicGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Still keeping watch"
    }
}

private fun accentFor(id: Long): Color {
    val palette = listOf(VaultMint, VaultSky, VaultCoral, VaultAmber)
    return palette[(id % palette.size).toInt().coerceAtLeast(0)]
}

private object HomeGradients {
    val screen = Brush.linearGradient(
        colors = listOf(
            VaultNight,
            Color(0xFF171B20),
            Color(0xFF121417)
        )
    )
}

private val DeadlineFilter.label: String
    get() = when (this) {
        DeadlineFilter.All -> "All"
        DeadlineFilter.ExpiringSoon -> "Next 30 days"
        DeadlineFilter.Overdue -> "Overdue"
        DeadlineFilter.RemindersOn -> "Reminders on"
    }
