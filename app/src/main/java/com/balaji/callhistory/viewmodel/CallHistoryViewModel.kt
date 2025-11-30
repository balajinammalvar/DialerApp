package com.balaji.callhistory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.utils.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DialerUiStates(
    val searchQuery: String = "",
    val selectedFilter: String = "all",
    val selectedDay: String = "all"
)

@OptIn(ExperimentalCoroutinesApi::class)
class CallHistoryViewModel(
    private val context: Context,
    private val repository: CallHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DialerUiStates())
    val uiState: StateFlow<DialerUiStates> = _uiState.asStateFlow()
    
    private val _contactCache = MutableStateFlow<Map<String, String?>>(emptyMap())
    val contactCache: StateFlow<Map<String, String?>> = _contactCache.asStateFlow()
    
    val callHistoryPagingData: Flow<PagingData<CallEntity>> = combine(
        _uiState,
        repository.refreshTrigger
    ) { state, _ -> state }
        .flatMapLatest { state ->
            repository.getCallHistoryPager(state.selectedFilter)
                .map { pagingData ->
                    pagingData.filter { call -> matchesUiFilters(call) }
                }
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun updateDay(day: String) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun buildContactCacheForSnapshot(snapshotItems: List<CallEntity>) {
        viewModelScope.launch {
            val cache = mutableMapOf<String, String?>()
            withContext(Dispatchers.IO) {
                snapshotItems.forEach { call ->
                    if (!cache.containsKey(call.number)) {
                        cache[call.number] = ContactHelper.getContactName(context, call.number)
                    }
                }
            }
            _contactCache.update { it + cache }
        }
    }

    private fun matchesUiFilters(call: CallEntity): Boolean {
        val state = _uiState.value
        
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim()
            val name = _contactCache.value[call.number]
            if (!(call.number.contains(q, ignoreCase = true) ||
                        name?.contains(q, ignoreCase = true) == true)
            ) return false
        }
        
        if (state.selectedDay != "all" && call.dayName != state.selectedDay) return false
        
        return true
    }
}
