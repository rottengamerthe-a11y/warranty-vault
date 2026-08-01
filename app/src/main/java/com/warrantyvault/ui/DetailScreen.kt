package com.warrantyvault.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrantyvault.data.AttachmentEntity
import com.warrantyvault.data.AttachmentType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemId: Long,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val row by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    var pendingDelete by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<Uri?>(null) }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) viewModel.addAttachment(itemId, it, AttachmentType.Receipt)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) viewModel.addAttachment(itemId, it, AttachmentType.Manual)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraTarget
        if (success && uri != null) viewModel.addAttachment(itemId, uri, AttachmentType.Photo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(row?.item?.name ?: "Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { pendingDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            )
        }
    ) { padding ->
        val itemRow = row
        if (itemRow == null) {
            Text("Item not found", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight)))
                    .padding(contentPadding)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(itemRow.item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (itemRow.item.category.isNotBlank()) AssistChip(onClick = {}, label = { Text(itemRow.item.category) })
                            if (itemRow.item.storeOrBrand.isNotBlank()) AssistChip(onClick = {}, label = { Text(itemRow.item.storeOrBrand) })
                        }
                        InfoCard(
                            rows = listOf(
                                "Purchase date" to itemRow.item.purchaseDate.formatDate(),
                                "Warranty ends" to itemRow.item.warrantyEndDate.formatDate(),
                                "Return deadline" to itemRow.item.returnDeadline.formatDate(),
                                "Serial number" to itemRow.item.serialNumber,
                                "Reminder" to "${itemRow.item.reminderDaysBefore} days before"
                            )
                        )
                        if (itemRow.item.notes.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Notes", fontWeight = FontWeight.SemiBold)
                                    Text(itemRow.item.notes)
                                }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { receiptPicker.launch("image/*") }) {
                                Icon(Icons.Default.Image, null)
                                Text("Receipt")
                            }
                            OutlinedButton(onClick = { filePicker.launch("*/*") }) {
                                Icon(Icons.Default.Description, null)
                                Text("Manual")
                            }
                            OutlinedButton(onClick = {
                                val uri = createCameraUri(context)
                                cameraTarget = uri
                                camera.launch(uri)
                            }) {
                                Icon(Icons.Default.CameraAlt, null)
                                Text("Photo")
                            }
                        }
                    }
                }
                items(itemRow.attachments, key = { it.id }) { attachment ->
                    AttachmentRow(
                        attachment = attachment,
                        onOpen = { openAttachment(context, attachment) },
                        onDelete = { viewModel.deleteAttachment(attachment.id) }
                    )
                }
            }
        }
    }

    if (pendingDelete && row != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("Delete item?") },
            text = { Text("This removes the item and its attachment records from WarrantyVault.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(row!!.item, onBack)
                    pendingDelete = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: AttachmentEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(attachment.displayName, fontWeight = FontWeight.SemiBold)
                Text(attachment.type.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpen) { Text("Open") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete attachment") }
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun openAttachment(context: Context, attachment: AttachmentEntity) {
    val rawUri = attachment.uri.toUri()
    val shareUri = if (rawUri.scheme == "file") {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(requireNotNull(rawUri.path)))
    } else {
        rawUri
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(shareUri, attachment.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open attachment"))
}
