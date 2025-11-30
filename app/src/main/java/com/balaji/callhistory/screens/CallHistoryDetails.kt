package com.balaji.callhistory.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.utils.CallHelper
import com.balaji.callhistory.utils.ContactHelper
import com.balaji.callhistory.utils.UiHelper
import com.balaji.callhistory.utils.UiHelper.CallIcon
import com.balaji.callhistory.viewmodel.CallHistoryDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryDetailsScreen(
    phoneNumber: String,
    viewModel: CallHistoryDetailsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val callEntryList by viewModel.getCallHistoryForNumber(phoneNumber).collectAsState(initial = emptyList())
    val contactName = remember(phoneNumber) { ContactHelper.getContactName(context, phoneNumber) }
    val photoUri =
        remember(phoneNumber) { ContactHelper.getContactPhotoUri(context, phoneNumber) }
    AnalyticsManager.logAnalyticEvent(
        context = context,
        eventType = AnalyticsManager.TrackingEvent.ENTERED_CALL_HISTORY_DETAILS
    )

    CallHistoryDetailsLayout(
        phoneNumber = phoneNumber,
        contactName = contactName,
        photoUri = photoUri,
        callEntryList = callEntryList,
        onBackClick = onBackClick,
        onCallClick = { CallHelper.makeCall(context, phoneNumber) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryDetailsLayout(
    phoneNumber: String,
    contactName: String?,
    photoUri: String?,
    callEntryList: List<CallEntity>,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val callHistoryItemState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !callHistoryItemState.isScrollInProgress) {
                FloatingActionButton(onClick = onCallClick) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            UiHelper.ContactAvatar(
                contactName = contactName,
                photoUri = photoUri,
                size = 80.dp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = contactName ?: phoneNumber,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (contactName != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))

            LazyColumn(
                state = callHistoryItemState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                reverseLayout = true
            ) {
                items(callEntryList) { callHistoryItem ->
                    CallHistoryItem(callEntity = callHistoryItem)
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(callEntity: CallEntity) {
    val callTypeText = UiHelper.getCallTypeText(callEntity.callType)
    val callIcon = CallIcon(callEntity)
    val iconColor =
        if (callEntity.callType == "missed") Color.Red else MaterialTheme.colorScheme.primary
    val timeStampColor =
        if (callEntity.callType == "missed") Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
    
    val durationText = if (callEntity.duration >= 60) {
        val minutes = callEntity.duration / 60
        val seconds = callEntity.duration % 60
        "${minutes}m ${seconds}s"
    } else {
        "${callEntity.duration}s"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = callIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callTypeText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = UiHelper.formatDateTimeWithYear(callEntity.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = timeStampColor
            )
        }
        if (callEntity.duration > 0) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = timeStampColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallHistoryDetailsLayoutPreview() {
    val sampleCalls = listOf(
        CallEntity(1, "9876543210", System.currentTimeMillis(), "received", duration = 10),
    )
    CallHistoryDetailsLayout(
        phoneNumber = "9876543210",
        contactName = "John Doe",
        callEntryList = sampleCalls,
        onBackClick = {},
        onCallClick = {},
        photoUri = ""
    )
}


