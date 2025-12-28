package com.balaji.callhistory.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.utils.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class DialerUiState(
    val currentNumber: String = "",
    val searchQuery: String = "",
    val selectedFilter: String = "all",
    val selectedDay: String = "all",
    val contactNameCache: Map<String, String?> = emptyMap()
)

class DialerViewModel(private val context: Context, private val repo: CallHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DialerUiState())
    private val _allContacts = MutableStateFlow<List<ContactHelper.Contact>>(emptyList())

    companion object {
        private const val TWO_DAYS = 2L
        private const val MAX_RECENT_SUGGESTIONS = 6
        private const val MAX_SEARCH_SUGGESTIONS = 10
    }
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _allContacts.value = withContext(Dispatchers.IO) {
                ContactHelper.getAllContacts(context)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val callHistoryForSuggestions: Flow<List<CallEntity>> = repo.refreshTrigger.flatMapLatest {
        repo.getCallsForSuggestions()
    }

    val contactSuggestions: Flow<List<CallEntity>> = combine(
        callHistoryForSuggestions,
        _allContacts,
        _uiState.debounce(50)
    ) { calls, contacts, state ->
        if (state.currentNumber.isEmpty()) {
            val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TWO_DAYS)
            calls.filter { it.timestamp >= twoDaysAgo }
                .groupBy { it.number }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(MAX_RECENT_SUGGESTIONS)
                .mapNotNull { entry -> calls.find { it.number == entry.key } }
        } else {
            val callMatches = calls.filter { call ->
                call.number.contains(state.currentNumber) ||
                        call.number.replace("\\D".toRegex(), "").contains(state.currentNumber)
            }.distinctBy { it.number }

            val contactMatches = contacts.filter { contact ->
                val phoneMatch = contact.phoneNumber.replace("\\D".toRegex(), "").contains(state.currentNumber)
                val nameMatch = matchesT9(contact.name, state.currentNumber)
                phoneMatch || nameMatch
            }.map { contact ->
                CallEntity(
                    id = contact.phoneNumber.hashCode().toLong(),
                    number = contact.phoneNumber,
                    timestamp = 0L,
                    callType = "contact",
                    duration = 0L
                )
            }.distinctBy { it.number }

            (callMatches + contactMatches).distinctBy { it.number }.take(MAX_SEARCH_SUGGESTIONS)
        }
    }.flowOn(Dispatchers.Default)



    fun appendDigit(d: String) {
        _uiState.update { it.copy(
            currentNumber = it.currentNumber + d
        ) }
    }

    fun backspace() {
        _uiState.update { it.copy(
            currentNumber = it.currentNumber.dropLast(1)
        ) }
    }

    fun clearNumber() {
        _uiState.update { it.copy(currentNumber = "") }
    }

    private fun matchesT9(name: String, digits: String): Boolean {
        if (digits.isEmpty() || digits.all { !it.isDigit() }) return false
        val nameLower = name.lowercase().replace("[^a-z]".toRegex(), "")
        if (nameLower.length < digits.length) return false
        
        for (i in 0..nameLower.length - digits.length) {
            if (matchesT9AtPosition(nameLower, digits, i)) return true
        }
        return false
    }
    
    private fun matchesT9AtPosition(name: String, digits: String, startPos: Int): Boolean {
        for (i in digits.indices) {
            val digit = digits[i]
            val char = name.getOrNull(startPos + i) ?: return false
            if (!charMatchesDigit(char, digit)) return false
        }
        return true
    }
    
    private fun charMatchesDigit(char: Char, digit: Char): Boolean = when(digit) {
        '2' -> char in "abc"
        '3' -> char in "def"
        '4' -> char in "ghi"
        '5' -> char in "jkl"
        '6' -> char in "mno"
        '7' -> char in "pqrs"
        '8' -> char in "tuv"
        '9' -> char in "wxyz"
        else -> false
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
