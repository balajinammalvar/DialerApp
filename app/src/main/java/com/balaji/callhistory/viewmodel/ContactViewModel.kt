package com.balaji.callhistory.viewmodel

import androidx.lifecycle.ViewModel
import com.balaji.callhistory.data.Contact
import com.balaji.callhistory.repo.ContactRepository
import com.balaji.callhistory.utils.SearchHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class ContactUiState(
    val searchQuery: String = ""
)

class ContactViewModel(private val repo: ContactRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    val contacts: Flow<List<Contact>> = repo.getAllContacts()
    
    val filteredContacts: Flow<List<Contact>> = combine(
        contacts,
        _uiState
    ) { contactList, state ->
        SearchHelper.filterContacts(contactList, state.searchQuery)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
