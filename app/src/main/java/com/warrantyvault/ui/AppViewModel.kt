package com.warrantyvault.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantyvault.data.AttachmentType
import com.warrantyvault.data.WarrantyItemEntity
import com.warrantyvault.data.WarrantyItemWithAttachments
import com.warrantyvault.data.WarrantyRepository
import com.warrantyvault.export.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: WarrantyRepository
) : ViewModel() {
    private val _scanDraft = MutableStateFlow<ItemDraft?>(null)
    val scanDraft = _scanDraft.asStateFlow()
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    /** Exposes raw item + category data for library/alerts/settings screens */
    val homeState = combine(
        repository.observeItems(),
        repository.observeCategories()
    ) { items, categories ->
        HomeUiState(items = items, categories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun observeItem(id: Long) = repository.observeItem(id)

    fun saveItem(draft: ItemDraft, onSaved: (Long) -> Unit) {
        if (!draft.isValid) return
        viewModelScope.launch {
            runCatching { repository.saveItem(draft.toEntity()) }
                .onSuccess {
                    _messages.emit("Item saved")
                    onSaved(it)
                }
                .onFailure { _messages.emit("Could not save item: ${it.userMessage()}") }
        }
    }

    fun stageScanDraft(draft: ItemDraft) {
        _scanDraft.value = draft
    }

    fun consumeScanDraft() {
        _scanDraft.value = null
    }

    fun deleteItem(item: WarrantyItemEntity, afterDelete: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deleteItem(item) }
                .onSuccess {
                    _messages.emit("Item deleted")
                    afterDelete()
                }
                .onFailure { _messages.emit("Could not delete item: ${it.userMessage()}") }
        }
    }

    fun addAttachment(itemId: Long, uri: Uri, type: AttachmentType) {
        viewModelScope.launch {
            runCatching { repository.addAttachment(itemId, uri, type) }
                .onSuccess { _messages.emit("Attachment added") }
                .onFailure { _messages.emit("Could not add attachment: ${it.userMessage()}") }
        }
    }

    fun deleteAttachment(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteAttachment(id) }
                .onSuccess { _messages.emit("Attachment deleted") }
                .onFailure { _messages.emit("Could not delete attachment: ${it.userMessage()}") }
        }
    }
}

private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "please try again"
