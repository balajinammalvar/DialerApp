package com.balaji.callhistory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.data.ContactDisplayInfo
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.utils.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    /**
     * Caches [ContactDisplayInfo] (name + photo URI) per phone number.
     * Built via [buildContactCacheForSnapshot] using a single ContentResolver
     * query per number instead of two separate calls.
     */
    private val _contactCache = MutableStateFlow<Map<String, ContactDisplayInfo>>(emptyMap())
    val contactCache: StateFlow<Map<String, ContactDisplayInfo>> = _contactCache.asStateFlow()

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
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(selectedDay = day) }
        }
    }

    /**
     * Fetches [ContactDisplayInfo] for each unique number in [snapshotItems]
     * using a single ContentResolver query per number (name + photo URI together)
     * and merges the results into [_contactCache].
     */
    fun buildContactCacheForSnapshot(snapshotItems: List<CallEntity>) {
        viewModelScope.launch {
            val cache = mutableMapOf<String, ContactDisplayInfo>()
            withContext(Dispatchers.IO) {
                snapshotItems.forEach { call ->
                    if (!cache.containsKey(call.number)) {
                        cache[call.number] = ContactHelper.getContactInfo(context, call.number)
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
            val name = _contactCache.value[call.number]?.name
            if (!(call.number.contains(q, ignoreCase = true) ||
                        name?.contains(q, ignoreCase = true) == true)
            ) return false
        }

        if (state.selectedDay != "all" && call.dayName != state.selectedDay) return false

        return true
    }
}
