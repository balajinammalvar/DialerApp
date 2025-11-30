package com.balaji.callhistory.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.balaji.callhistory.R
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.ui.components.PermissionHandler
import com.balaji.callhistory.ui.components.SearchBar
import com.balaji.callhistory.utils.CallHelper
import com.balaji.callhistory.utils.ContactHelper
import com.balaji.callhistory.utils.UiHelper
import com.balaji.callhistory.repo.AppRepositoryProvider
import com.balaji.callhistory.viewmodel.CallHistoryViewModel
import com.balaji.callhistory.viewmodel.DialerViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CallHistoryScreen(
    dialerViewModel: DialerViewModel,
    onNavigateToTheme: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CallHistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CallHistoryViewModel(
                    context = context,
                    repository = AppRepositoryProvider.repository!!
                )
            }
        }
    )
    PermissionHandler {
        val uiState by viewModel.uiState.collectAsState()
        val contactCache by viewModel.contactCache.collectAsState()
        val pagingData = viewModel.callHistoryPagingData.collectAsLazyPagingItems()
        
        CallHistoryLayout(
            pagingData = pagingData,
            uiState = uiState,
            contactCache = contactCache,
            onSearchChange = viewModel::updateSearchQuery,
            onFilterChange = viewModel::updateFilter,
            onDayChange = viewModel::updateDay,
            onUpdateContactCache = { calls ->
                viewModel.buildContactCacheForSnapshot(calls)
            },
            onCallDetailsClick = { number ->
                onNavigateToDetails(number)
            },
            onNavigateToTheme = onNavigateToTheme,
            onMakeCall = { number ->
                CallHelper.makeCall(context, number)
            }
        )
    }
}

@Composable
fun CallHistoryLayout(
    pagingData: LazyPagingItems<CallEntity>,
    uiState: com.balaji.callhistory.viewmodel.DialerUiStates,
    contactCache: Map<String, String?>,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onUpdateContactCache: (List<CallEntity>) -> Unit,
    onCallDetailsClick: (String) -> Unit = {},
    onNavigateToTheme: () -> Unit = {},
    onMakeCall: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AnalyticsManager.logAnalyticEvent(
        context = context,
        eventType = AnalyticsManager.TrackingEvent.ENTERED_CALL_HISTORY
    )

    LaunchedEffect(pagingData.itemCount) {
        val items = (0 until pagingData.itemCount).mapNotNull { pagingData.peek(it) }
        if (items.isNotEmpty()) {
            onUpdateContactCache(items)
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.settings),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.theme)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTheme()
                    }
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar (same as your original)
            SearchBar(
                searchQuery = uiState.searchQuery,
                onSearchChange = onSearchChange,
                isSearchFocused = isSearchFocused,
                onFocusChanged = { isSearchFocused = it },
                onBackClick = {
                    focusManager.clearFocus()
                    isSearchFocused = false
                },
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            Spacer(Modifier.height(8.dp))

            // Filter chips + Day selector (horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterButton(
                    text = stringResource(R.string.all),
                    selected = uiState.selectedFilter == "all",
                    onClick = { onFilterChange("all") }
                )
                FilterButton(
                    text = stringResource(R.string.missed),
                    selected = uiState.selectedFilter == "missed",
                    onClick = { onFilterChange("missed") }
                )
                FilterButton(
                    text = stringResource(R.string.received),
                    selected = uiState.selectedFilter == "received",
                    onClick = { onFilterChange("received") }
                )
                FilterButton(
                    text = stringResource(R.string.dialed),
                    selected = uiState.selectedFilter == "dialed",
                    onClick = { onFilterChange("dialed") }
                )
                DaySplitButton(
                    selectedDay = uiState.selectedDay,
                    onDaySelected = onDayChange
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(state = listState) {
                items(
                    count = pagingData.itemCount,
                    key = pagingData.itemKey { it.callHistoryId }
                ) { index ->
                    val call = pagingData[index] ?: return@items
                    
                    val showHeader = (index == 0) || 
                        (pagingData.peek(index - 1)?.formattedDate != call.formattedDate)
                    if (showHeader) {
                        Text(
                            text = call.formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    val contactName = contactCache[call.number]
                    val contactPhotoUri = ContactHelper.getContactPhotoUri(context, call.number)

                    val callIcon: ImageVector = when (call.callType.lowercase(Locale.getDefault())) {
                        "missed" -> Icons.AutoMirrored.Filled.PhoneMissed
                        "received" -> Icons.AutoMirrored.Filled.CallReceived
                        "dialed" -> Icons.AutoMirrored.Filled.CallMade
                        else -> Icons.AutoMirrored.Filled.CallReceived
                    }
                    val iconTint = if (call.callType == "missed") Color.Red else MaterialTheme.colorScheme.primary
                    val timeStampColor = if (call.callType == "missed") Color.Red else MaterialTheme.colorScheme.onSurfaceVariant

                    CallRow(
                        phoneNumber = call.number,
                        contactName = contactName,
                        contactPhotoUri = contactPhotoUri,
                        callTypeIcon = callIcon,
                        callTypeIconColor = iconTint,
                        timeStampColor = timeStampColor,
                        formattedTime = call.formattedTime,
                        onCallDetailsClick = { onCallDetailsClick(call.number) },
                        onMakeCall = { onMakeCall(call.number) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(text) },
        selected = selected
    )
}

@Composable
fun DaySplitButton(
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Box {
        FilterChip(
            onClick = { expanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selectedDay == "all") stringResource(R.string.day) else selectedDay)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            selected = selectedDay != "all"
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_days)) },
                onClick = {
                    expanded = false
                    onDaySelected("all")
                }
            )
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day) },
                    onClick = {
                        expanded = false
                        onDaySelected(day)
                    }
                )
            }
        }
    }
}

@Composable
fun CallRow(
    phoneNumber: String,
    callTypeIcon: ImageVector,
    callTypeIconColor: Color,
    timeStampColor: Color,
    formattedTime: String,
    onMakeCall: (String) -> Unit,
    onCallDetailsClick: () -> Unit,
    contactName: String?,
    contactPhotoUri: String?
) {
    Card(
        onClick = onCallDetailsClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UiHelper.ContactAvatar(
                contactName = contactName,
                photoUri = contactPhotoUri
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                UiHelper.ContactInfo(contactName, phoneNumber)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = callTypeIcon,
                        contentDescription = "callType",
                        tint = callTypeIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeStampColor
                    )
                }
            }

            IconButton(onClick = { onMakeCall(phoneNumber) }) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallRowPreview() {
    MaterialTheme {
        Column {
            CallRow(
                phoneNumber = "+1234567890",
                callTypeIcon = Icons.AutoMirrored.Filled.CallReceived,
                callTypeIconColor = MaterialTheme.colorScheme.primary,
                timeStampColor = MaterialTheme.colorScheme.onSurfaceVariant,
                formattedTime = "10:30 AM",
                onMakeCall = {},
                onCallDetailsClick = {},
                contactName = "John Doe",
                contactPhotoUri = null
            )
            CallRow(
                phoneNumber = "+9876543210",
                callTypeIcon = Icons.AutoMirrored.Filled.PhoneMissed,
                callTypeIconColor = Color.Red,
                timeStampColor = Color.Red,
                formattedTime = "09:15 AM",
                onMakeCall = {},
                onCallDetailsClick = {},
                contactName = null,
                contactPhotoUri = null
            )
            CallRow(
                phoneNumber = "+1122334455",
                callTypeIcon = Icons.AutoMirrored.Filled.CallMade,
                callTypeIconColor = MaterialTheme.colorScheme.primary,
                timeStampColor = MaterialTheme.colorScheme.onSurfaceVariant,
                formattedTime = "Yesterday",
                onMakeCall = {},
                onCallDetailsClick = {},
                contactName = "Jane Smith",
                contactPhotoUri = null
            )
        }
    }
}

