package com.balaji.callhistory.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.balaji.callhistory.R
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.data.Contact
import com.balaji.callhistory.repo.ContactRepository
import com.balaji.callhistory.ui.components.SearchBar
import com.balaji.callhistory.utils.PermissionManager
import com.balaji.callhistory.utils.UiHelper
import com.balaji.callhistory.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

@Composable
fun ContactScreen(onContactClick: (Contact) -> Unit) {
    val context = LocalContext.current
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedCount by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasContactPermission = true
        } else {
            permissionDeniedCount++
        }
    }

    if (!hasContactPermission) {
        ContactPermission(permissionDeniedCount, context, permissionLauncher)
    } else {
        val repository = remember { ContactRepository(context) }
        val viewModel = remember { ContactViewModel(repository) }
        val uiState by viewModel.uiState.collectAsState()
        val filteredContacts by viewModel.filteredContacts.collectAsState(initial = emptyList())
        ContactLayout(
            filteredContacts = filteredContacts,
            searchQuery = uiState.searchQuery,
            onSearchChange = viewModel::updateSearchQuery,
            onContactClick = onContactClick
        )
    }
}

@Composable
private fun ContactPermission(
    permissionDeniedCount: Int,
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.contacts_permission_required))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (permissionDeniedCount >= 2) {
                PermissionManager
                    .openAppSettings(context)
            } else {
                val permission = Manifest.permission.READ_CONTACTS
                permissionLauncher.launch(permission)
            }
        }) {
            Text(
                if (permissionDeniedCount >= 2) {
                    stringResource(R.string.open_settings)
                } else {
                    stringResource(R.string.grant_permission)
                }
            )
        }
    }
}

@Composable
fun ContactLayout(
    filteredContacts: List<Contact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onContactClick: (Contact) -> Unit
) {
    val context = LocalContext.current
    AnalyticsManager.logAnalyticEvent(
        context = context,
        eventType = AnalyticsManager.TrackingEvent.ENTER_CONTACT_SEARCH
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val groupedContacts = filteredContacts.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    val sectionIndices = mutableMapOf<Char, Int>()
    var currentIndex = 0
    groupedContacts.forEach { (initial, contactList) ->
        sectionIndices[initial] = currentIndex
        currentIndex += 1 + contactList.size
    }

    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                placeholder = stringResource(R.string.search_contacts),
                isSearchFocused = isSearchFocused,
                onFocusChanged = { isSearchFocused = it },
                onBackClick = {
                    focusManager.clearFocus()
                    isSearchFocused = false
                },
                showMenuIcon = false,
                showTrailingIcon = false
            )

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                groupedContacts.forEach { (initial, contactList) ->
                    item(key = "header_$initial") {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(contactList, key = { it.contactId }) { contact ->
                        ContactRow(contact = contact, onClick = { onContactClick(contact) })
                    }
                }
            }
        }

        AlphabetScroller(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 80.dp),
            onLetterClick = { letter ->
                sectionIndices[letter]?.let { index ->
                    scope.launch {
                        listState.scrollToItem(
                            index = index,
                            scrollOffset = 0
                        )
                    }
                }
            }
        )
    }
}


@Composable
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (contact.photoUri != null) {
            AsyncImage(
                model = contact.photoUri,
                contentDescription = "Contact photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            UiHelper.ContactAvatar(contactName = contact.name)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = contact.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AlphabetScroller(modifier: Modifier = Modifier, onLetterClick: (Char) -> Unit) {
    val alphabet = ('A'..'Z').toList()
    val letterPositions = remember { mutableMapOf<Char, Float>() }
    
    Column(
        modifier = modifier
            .padding(end = 4.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        findLetterAtPosition(offset.y, letterPositions)?.let { onLetterClick(it) }
                    },
                    onDrag = { change, _ ->
                        findLetterAtPosition(change.position.y, letterPositions)?.let { onLetterClick(it) }
                    }
                )
            },
        verticalArrangement = Arrangement.Center
    ) {
        alphabet.forEach { letter ->
            Text(
                text = letter.toString(),
                modifier = Modifier
                    .clickable { onLetterClick(letter) }
                    .padding(vertical = 2.dp, horizontal = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        letterPositions[letter] = coordinates.positionInParent().y + coordinates.size.height / 2f
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun findLetterAtPosition(y: Float, positions: Map<Char, Float>): Char? {
    return positions.minByOrNull { (_, pos) -> kotlin.math.abs(pos - y) }?.key
}

@Preview(showBackground = true)
@Composable
fun ContactLayoutPreview() {
    val sampleContacts = listOf(
        Contact(1, "John Doe", "+1 234-567-8900", null),
        Contact(2, "Jane Smith", "+1 987-654-3210", null)
    )

    ContactLayout(
        filteredContacts = sampleContacts,
        searchQuery = "",
        onSearchChange = {},
        onContactClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ContactRowPreview() {
    Column {
        ContactRow(
            contact = Contact(1, "John Doe", "+1 234-567-8900", null),
            onClick = {}
        )
        ContactRow(
            contact = Contact(2, "Jane Smith", "+1 987-654-3210", null),
            onClick = {}
        )
    }
}
