package com.balaji.callhistory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import java.util.concurrent.TimeUnit
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.utils.SearchHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

data class DialerUiState(
    val currentNumber: String = "",
    val searchQuery: String = "",
    val selectedFilter: String = "all",
    val selectedDay: String = "all",
    val contactNameCache: Map<String, String?> = emptyMap()
)

class DialerViewModel(private val repo: CallHistoryRepository, private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(DialerUiState())

    companion object {
        private const val TWO_DAYS = 2L
        private const val MAX_RECENT_SUGGESTIONS = 6
        private const val MAX_SEARCH_SUGGESTIONS = 10
    }
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val callHistoryForSuggestions: Flow<List<CallEntity>> = repo.refreshTrigger.flatMapLatest {
        repo.getCallsForSuggestions()
    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    val callHistoryPaging: Flow<PagingData<CallEntity>> = _uiState
//        .map { it.selectedFilter }
//        .flatMapLatest { filter ->
//            Pager(
//                config = PagingConfig(pageSize = 50, enablePlaceholders = false, prefetchDistance = 10),
//                pagingSourceFactory = {
//                    CallHistoryPagingSource(context, filter)
//                }
//            ).flow
//        }.cachedIn(viewModelScope)

    val filteredCalls: Flow<List<CallEntity>> = combine(
        callHistoryForSuggestions,
        _uiState
    ) { calls, state ->
        var filtered = calls

        // Apply search filter
        filtered = SearchHelper.filterCalls(filtered, state.searchQuery, state.contactNameCache)

        // Apply call type filter
        filtered = SearchHelper.filterCallsByType(filtered, state.selectedFilter)

        // Apply day filter
        filtered = SearchHelper.filterCallsByDay(filtered, state.selectedDay)

        filtered
    }

    val contactSuggestions: Flow<List<CallEntity>> = combine(
        callHistoryForSuggestions,
        _uiState
    ) { calls, state ->
        if (state.currentNumber.isEmpty()) {
            val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TWO_DAYS)
            calls.filter { it.timestamp >= twoDaysAgo }
                .groupBy { it.number }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(MAX_RECENT_SUGGESTIONS)
                .mapNotNull { entry -> calls.find { it.number == entry.key } }
        } else {
            calls.filter { call ->
                call.number.contains(state.currentNumber) ||
                        call.number.replace("[^\\d]".toRegex(), "").contains(state.currentNumber)
            }.distinctBy { it.number }.take(MAX_SEARCH_SUGGESTIONS)
        }
    }



    fun appendDigit(d: String) {
        _uiState.update { it.copy(currentNumber = it.currentNumber + d) }
    }

    fun backspace() {
        _uiState.update { it.copy(currentNumber = it.currentNumber.dropLast(1)) }
    }

    fun clearNumber() {
        _uiState.update { it.copy(currentNumber = "") }
    }

    fun setNumber(number: String) {
        _uiState.update { it.copy(currentNumber = number) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun updateDay(day: String) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun updateContactNameCache(cache: Map<String, String?>) {
        _uiState.update { it.copy(contactNameCache = cache) }
    }
}
