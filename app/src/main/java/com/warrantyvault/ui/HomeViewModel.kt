package com.warrantyvault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantyvault.data.WarrantyRepository
import com.warrantyvault.ui.daysUntil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    warrantyRepository: WarrantyRepository
) : ViewModel() {
    val query = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val deadlineFilter = MutableStateFlow(DeadlineFilter.All)

    val homeState = combine(
        warrantyRepository.observeItems(),
        warrantyRepository.observeCategories(),
        query,
        selectedCategory,
        deadlineFilter
    ) { items, categories, query, category, filter ->
        HomeUiState(
            items = items.filter { row ->
                val item = row.item
                val matchesQuery = query.isBlank() ||
                        item.name.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        item.storeOrBrand.contains(query, ignoreCase = true) ||
                        item.serialNumber.contains(query, ignoreCase = true)
                val matchesCategory = category == null || item.category == category
                val matchesFilter = when (filter) {
                    DeadlineFilter.All -> true
                    DeadlineFilter.RemindersOn -> item.reminderDaysBefore > 0
                    DeadlineFilter.Overdue -> listOf(item.warrantyEndDate, item.returnDeadline).any {
                        val days = daysUntil(it)
                        days != null && days < 0
                    }
                    DeadlineFilter.ExpiringSoon -> listOf(item.warrantyEndDate, item.returnDeadline).any {
                        val days = daysUntil(it)
                        days != null && days in 0..30
                    }
                }
                matchesQuery && matchesCategory && matchesFilter
            },
            categories = categories,
            query = query,
            selectedCategory = category,
            filter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

