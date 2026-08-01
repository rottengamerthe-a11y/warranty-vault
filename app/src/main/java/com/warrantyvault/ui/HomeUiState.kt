package com.warrantyvault.ui

import com.warrantyvault.data.WarrantyItemWithAttachments

data class HomeUiState(
    val items: List<WarrantyItemWithAttachments> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    val selectedCategory: String? = null,
    val filter: DeadlineFilter = DeadlineFilter.All
)
