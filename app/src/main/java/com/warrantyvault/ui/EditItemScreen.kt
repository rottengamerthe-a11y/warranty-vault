package com.warrantyvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.warrantyvault.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemId: Long,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val existing by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    val scanDraft by viewModel.scanDraft.collectAsStateWithLifecycle()
    var draft by remember(itemId, existing?.item?.updatedAt) {
        mutableStateOf(existing?.item?.toDraft() ?: ItemDraft())
    }

    LaunchedEffect(itemId, scanDraft) {
        val scanned = scanDraft
        if (itemId == 0L && scanned != null) {
            draft = scanned
            viewModel.consumeScanDraft()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == 0L) "Review & confirm" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight)))
                .padding(contentPadding)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Field(draft.name, "Item name", true) { draft = draft.copy(name = it) }
            Field(draft.category, "Category") { draft = draft.copy(category = it) }
            Field(draft.storeOrBrand, "Store or brand") { draft = draft.copy(storeOrBrand = it) }
            DateField(draft.purchaseDate, "Purchase date") { draft = draft.copy(purchaseDate = it) }
            DateField(draft.warrantyEndDate, "Warranty end date") { draft = draft.copy(warrantyEndDate = it) }
            DateField(draft.returnDeadline, "Return deadline") { draft = draft.copy(returnDeadline = it) }
            Field(draft.serialNumber, "Serial number") { draft = draft.copy(serialNumber = it) }
            TextField(
                value = draft.reminderDaysBefore,
                label = "Remind days before",
                singleLine = true,
                onValueChange = { draft = draft.copy(reminderDaysBefore = it.filter(Char::isDigit)) },
                keyboardType = KeyboardType.Number
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 3,
                colors = fieldColors()
            )
            Button(
                onClick = { viewModel.saveItem(draft, onSaved) },
                enabled = draft.isValid,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.cd_save))
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val selectedDateMillis = parseInputDate(value)

    Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
            value = selectedDateMillis.formatDate(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text("Select date") },
                trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.cd_date)) },
            singleLine = true,
            readOnly = true,
            colors = fieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { open = true }
        )
    }

    if (open) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(datePickerState.selectedDateMillis.formatInputDate())
                        open = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun Field(
    value: String,
    label: String,
    required: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (required) "$label *" else label) },
        singleLine = true,
        colors = fieldColors()
    )
}

@Composable
private fun TextField(
    value: String,
    label: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = VaultText,
    unfocusedTextColor = VaultText,
    focusedLabelColor = VaultMint,
    unfocusedLabelColor = VaultTextMuted,
    focusedBorderColor = VaultMint,
    unfocusedBorderColor = VaultGlassBorder,
    cursorColor = VaultMint
)
