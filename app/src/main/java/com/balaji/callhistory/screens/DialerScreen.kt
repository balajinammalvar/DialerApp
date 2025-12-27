@file:Suppress("TooManyFunctions")

package com.balaji.callhistory.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balaji.callhistory.R
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.data.CallEntity
import com.balaji.callhistory.ui.components.PermissionHandler
import com.balaji.callhistory.utils.CallHelper
import com.balaji.callhistory.utils.ContactHelper
import com.balaji.callhistory.utils.UiHelper
import com.balaji.callhistory.viewmodel.DialerViewModel

private val DIAL_PAD_LETTER_SIZE = 10.sp
private const val CALL_BUTTON_COLOR = 0xFF34A853

@Composable
fun DialerScreen(viewModel: DialerViewModel) {
    PermissionHandler {
        val ui by viewModel.uiState.collectAsState()
        val callHistory by viewModel.callHistoryForSuggestions.collectAsState(initial = emptyList())
        val suggestions by viewModel.contactSuggestions.collectAsState(initial = emptyList())
        val context = LocalContext.current

        DialerLayout(
            currentNumber = ui.currentNumber,
            suggestions = suggestions,
            onDigit = viewModel::appendDigit,
            onBackspace = viewModel::backspace,
            onClearAll = viewModel::clearNumber,
            onCall = {
                if (ui.currentNumber.isNotBlank()) {
                    CallHelper.makeCall(context, ui.currentNumber)
                    viewModel.clearNumber()
                } else {
                    // Fill with last number from call history
                    callHistory.firstOrNull()?.let { lastCall ->
                        viewModel.setNumber(lastCall.number)
                    }
                }
            }
        )
    }
}

@Composable
fun DialerLayout(
    currentNumber: String,
    suggestions: List<CallEntity>,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onCall: () -> Unit
) {
    val context = LocalContext.current
    AnalyticsManager.logAnalyticEvent(
        context = context,
        eventType = AnalyticsManager.TrackingEvent.ENTERED_DIALER_SCREEN
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        // Contact suggestions - takes remaining space
        Text(
            text = stringResource(R.string.suggested),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (suggestions.isNotEmpty()) {
                val context = LocalContext.current
                ContactSuggestions(
                    suggestions = suggestions,
                    currentNumber = currentNumber,
                    onContactSelect = { number ->
                        onClearAll()
                        number.forEach { digit -> onDigit(digit.toString()) }
                    },
                    onCallClick = { number ->
                        CallHelper.makeCall(context, number)
                    }
                )
            }
        }

        DialPad(
            currentNumber = currentNumber,
            onDigit = onDigit,
            onBackspace = onBackspace,
            onClearAll = onClearAll,
            hasNumber = currentNumber.isNotEmpty()
        )

        Spacer(Modifier.height(16.dp))

        FloatingActionButton(
            onClick = onCall,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            containerColor = Color(CALL_BUTTON_COLOR)
        ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = stringResource(R.string.call),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )


        }

        Spacer(Modifier.height(16.dp))
    }
}

fun getHighlightedNumber(number: String, currentNum: String): Pair<String, IntRange?> {
    if (currentNum.isEmpty() || currentNum.isBlank()) return Pair(number, null)
    
    val cleanNumber = number.replace("\\D".toRegex(), "")
    val startIndex = cleanNumber.indexOf(currentNum)
    val indices = if (startIndex >= 0) findFormattedIndices(number, startIndex, currentNum.length) else null
    val range = indices?.let { it.first until it.second }
    return Pair(number, range)
}

fun getHighlightedName(name: String, digits: String): IntRange? {
    if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
    val nameLower = name.lowercase().replace("[^a-z]".toRegex(), "")
    
    for (i in 0..nameLower.length - digits.length) {
        if (matchesT9AtPosition(nameLower, digits, i)) {
            return i until (i + digits.length)
        }
    }
    return null
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

private fun findFormattedIndices(number: String, startIndex: Int, length: Int): Pair<Int, Int>? {
    var formattedStartIndex = -1
    var digitCount = 0
    val endDigitIndex = startIndex + length - 1
    
    for (i in number.indices) {
        if (!number[i].isDigit()) continue
        
        if (digitCount == startIndex) formattedStartIndex = i
        if (digitCount == endDigitIndex) {
            return if (formattedStartIndex >= 0) Pair(formattedStartIndex, i + 1) else null
        }
        digitCount++
    }
    
    return null
}

@Composable
fun DialPad(
    currentNumber: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    hasNumber: Boolean
) {
    val haptic = LocalHapticFeedback.current

    val dialPadButtons = listOf(
        listOf(
            DialPadButton("1", ""),
            DialPadButton("2", "ABC"),
            DialPadButton("3", "DEF")
        ),
        listOf(
            DialPadButton("4", "GHI"),
            DialPadButton("5", "JKL"),
            DialPadButton("6", "MNO")
        ),
        listOf(
            DialPadButton("7", "PQRS"),
            DialPadButton("8", "TUV"),
            DialPadButton("9", "WXYZ")
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Phone number display and backspace row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone number display (takes most space)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentNumber,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Backspace button (aligned to right)
            if (hasNumber) {
                BackspaceButton(
                    onBackspace = onBackspace,
                    onClearAll = onClearAll,
                    haptic = haptic
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Number buttons (1-9)
        dialPadButtons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { button ->
                    DialPadKey(
                        number = button.number,
                        letters = button.letters,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDigit(button.number)
                        }
                    )
                }
            }
        }

        // Bottom row: *, 0, #, backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialPadKey(
                number = "*",
                letters = "",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDigit("*")
                }
            )

            DialPadKey(
                number = "0",
                letters = "+",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDigit("0")
                }
            )

            DialPadKey(
                number = "#",
                letters = "",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDigit("#")
                }
            )
        }

    }
}

data class DialPadButton(
    val number: String,
    val letters: String
)

@Composable
fun ContactSuggestions(
    suggestions: List<CallEntity>,
    currentNumber: String,
    onContactSelect: (String) -> Unit,
    onCallClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(suggestions, key = { it.id }) { call ->
            SuggestionCallRow(
                call = call,
                currentNumber = currentNumber,
                onRowClick = { onContactSelect(call.number) },
                onCallClick = { onCallClick(call.number) }
            )
        }
    }
}

@Composable
fun SuggestionCallRow(
    call: CallEntity,
    currentNumber: String,
    onRowClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val context = LocalContext.current
    val contactName = remember(call.number) { ContactHelper.getContactName(context, call.number) }
    val (number, highlightRange) = remember(call.number, currentNumber) { 
        getHighlightedNumber(call.number, currentNumber) 
    }

    Card(
        onClick = onRowClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UiHelper.ContactAvatar(contactName = contactName)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (contactName != null) {
                    val nameHighlight = remember(contactName, currentNumber) { 
                        getHighlightedName(contactName, currentNumber) 
                    }
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val annotatedName = remember(contactName, currentNumber, nameHighlight, primaryColor) {
                        buildAnnotatedString {
                            if (nameHighlight != null) {
                                append(contactName.take(nameHighlight.first))
                                withStyle(SpanStyle(
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append(contactName.substring(nameHighlight.first, nameHighlight.last))
                                }
                                append(contactName.substring(nameHighlight.last))
                            } else {
                                append(contactName)
                            }
                        }
                    }
                    Text(
                        text = annotatedName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = call.number,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (contactName != null) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val annotatedNumber = remember(number, currentNumber, highlightRange, primaryColor) {
                        buildAnnotatedString {
                            if (highlightRange != null) {
                                append(number.substring(0, highlightRange.first))
                                withStyle(SpanStyle(
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append(number.substring(highlightRange.first, highlightRange.last))
                                }
                                append(number.substring(highlightRange.last))
                            } else {
                                append(number)
                            }
                        }
                    }
                    Text(
                        text = annotatedNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onCallClick) {
                Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackspaceButton(
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    haptic: HapticFeedback
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBackspace()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearAll()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Backspace,
            contentDescription = stringResource(R.string.backspace),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DialPadKey(
    number: String,
    letters: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = DIAL_PAD_LETTER_SIZE
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
