package com.balaji.callhistory.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tty
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.balaji.callhistory.R
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.data.ContactDisplayInfo
import com.balaji.callhistory.repo.AppRepositoryProvider
import com.balaji.callhistory.ui.components.PermissionHandler
import com.balaji.callhistory.ui.components.SearchBar
import com.balaji.callhistory.utils.CallHelper
import com.balaji.callhistory.utils.CallTypeMapper
import com.balaji.callhistory.utils.DarkModeState
import com.balaji.callhistory.utils.UiHelper
import com.balaji.callhistory.viewmodel.CallHistoryViewModel
import kotlinx.coroutines.launch

// Constant — never recreated on recomposition
private val DAYS_OF_WEEK = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

@Composable
fun CallHistoryScreen(
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
            onCallDetailsClick = { number -> onNavigateToDetails(number) },
            onNavigateToTheme = onNavigateToTheme,
            onMakeCall = { number -> CallHelper.makeCall(context, number) }
        )
    }
}

@Composable
fun CallHistoryLayout(
    pagingData: LazyPagingItems<CallEntity>,
    uiState: com.balaji.callhistory.viewmodel.DialerUiStates,
    contactCache: Map<String, ContactDisplayInfo>,
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

    // Analytics fired once on entry, not on every recomposition
    LaunchedEffect(Unit) {
        AnalyticsManager.logAnalyticEvent(
            context = context,
            eventType = AnalyticsManager.TrackingEvent.ENTERED_CALL_HISTORY
        )
    }

    LaunchedEffect(pagingData.itemCount) {
        val items = (0 until pagingData.itemCount).mapNotNull { pagingData.peek(it) }
        if (items.isNotEmpty()) {
            onUpdateContactCache(items)
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    val darkModeState = DarkModeState.getInstance()
    val isDarkMode by darkModeState.state.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.7f)
            ) {
                Text(
                    stringResource(R.string.settings),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            if (isDarkMode) stringResource(R.string.theme_light_mode)
                            else stringResource(R.string.theme_dark_mode)
                        )
                    },
                    selected = false,
                    onClick = { darkModeState.state.value = !isDarkMode }
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    selected = uiState.selectedFilter == CallTypeMapper.TYPE_MISSED,
                    onClick = { onFilterChange(CallTypeMapper.TYPE_MISSED) }
                )
                FilterButton(
                    text = stringResource(R.string.received),
                    selected = uiState.selectedFilter == CallTypeMapper.TYPE_RECEIVED,
                    onClick = { onFilterChange(CallTypeMapper.TYPE_RECEIVED) }
                )
                FilterButton(
                    text = stringResource(R.string.dialed),
                    selected = uiState.selectedFilter == CallTypeMapper.TYPE_DIALED,
                    onClick = { onFilterChange(CallTypeMapper.TYPE_DIALED) }
                )
                DaySplitButton(
                    selectedDay = uiState.selectedDay,
                    onDaySelected = onDayChange,
                )
            }

            Spacer(Modifier.height(12.dp))

            if (pagingData.itemCount == 0) {
                EmptyStateContent()
            } else {
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

                        // Both name and photoUri from cache — NO UI-thread ContentResolver call
                        val contactInfo = contactCache[call.number]
                        val contactName = contactInfo?.name
                        val contactPhotoUri = contactInfo?.photoUri

                        // Use CallTypeMapper — no duplicate when() block
                        val callIcon: ImageVector = CallTypeMapper.toIcon(call.callType)
                        val iconTint = CallTypeMapper.tintColor(
                            call.callType,
                            MaterialTheme.colorScheme.primary
                        )
                        val timeStampColor = CallTypeMapper.tintColor(
                            call.callType,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
}

private const val ANIMATION_DURATION = 1500

@Composable
fun EmptyStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "flip")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(ANIMATION_DURATION),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tty,
                contentDescription = null,
                modifier = Modifier.size(64.dp).graphicsLayer(rotationZ = rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_no_call_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterButton(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(onClick = onClick, label = { Text(text) }, selected = selected)
}

@Composable
fun DaySplitButton(selectedDay: String, onDaySelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    // DAYS_OF_WEEK is a file-level constant — never recreated on recomposition

    Box {
        FilterChip(
            onClick = { isExpanded = true },
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
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_days)) },
                onClick = { isExpanded = false; onDaySelected("all") }
            )
            DAYS_OF_WEEK.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day) },
                    onClick = { isExpanded = false; onDaySelected(day) }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UiHelper.ContactAvatar(contactName = contactName, photoUri = contactPhotoUri)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                UiHelper.ContactInfo(contactName, phoneNumber)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = callTypeIcon,
                        contentDescription = stringResource(R.string.cd_call_type),
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
                Icon(Icons.Default.Call, contentDescription = stringResource(R.string.cd_call))
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
                callTypeIcon = CallTypeMapper.ICON_RECEIVED,
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
                callTypeIcon = CallTypeMapper.ICON_MISSED,
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
                callTypeIcon = CallTypeMapper.ICON_DIALED,
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
