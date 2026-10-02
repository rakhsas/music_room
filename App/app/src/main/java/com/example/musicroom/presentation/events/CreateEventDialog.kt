package com.example.musicroom.presentation.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.musicroom.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

/** Parses a "yyyy-MM-dd'T'HH:mm:ss..." ISO string into [year, month(0-based), day, hour, minute], or null if unparseable. */
private fun parseIsoDateTimeParts(isoString: String?): IntArray? {
    if (isoString.isNullOrBlank()) return null
    return try {
        val cal = Calendar.getInstance()
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss")) {
            val parsed = SimpleDateFormat(pattern, Locale.getDefault()).parse(isoString.take(25)) ?: continue
            cal.time = parsed
            return intArrayOf(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
            )
        }
        null
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    isCreating: Boolean,
    onCreateEvent: (title: String, location: String, description: String?, isPublic: Boolean, startTime: String, endTime: String?) -> Unit,
    onDismiss: () -> Unit,
    dialogTitle: String = "Create New Event",
    confirmButtonText: String = "Create Event",
    confirmButtonLoadingText: String = "Creating...",
    initialTitle: String = "",
    initialLocation: String = "",
    initialDescription: String = "",
    initialIsPublic: Boolean = true,
    initialStartTime: String? = null,
    initialEndTime: String? = null
) {
    var eventTitle by remember { mutableStateOf(initialTitle) }
    var location by remember { mutableStateOf(initialLocation) }
    var description by remember { mutableStateOf(initialDescription) }
    var isPublic by remember { mutableStateOf(initialIsPublic) }

    val startParts = remember(initialStartTime) { parseIsoDateTimeParts(initialStartTime) }
    val endParts = remember(initialEndTime) { parseIsoDateTimeParts(initialEndTime) }

    // Simple date and time states
    var showStartDateTimePicker by remember { mutableStateOf(false) }
    var showEndDateTimePicker by remember { mutableStateOf(false) }

    // Use simple state for date and time
    var startYear by remember { mutableIntStateOf(startParts?.get(0) ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var startMonth by remember { mutableIntStateOf(startParts?.get(1) ?: Calendar.getInstance().get(Calendar.MONTH)) }
    var startDay by remember { mutableIntStateOf(startParts?.get(2) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var startHour by remember { mutableIntStateOf(startParts?.get(3) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(startParts?.get(4) ?: Calendar.getInstance().get(Calendar.MINUTE)) }

    var endYear by remember { mutableIntStateOf(endParts?.get(0) ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var endMonth by remember { mutableIntStateOf(endParts?.get(1) ?: Calendar.getInstance().get(Calendar.MONTH)) }
    var endDay by remember { mutableIntStateOf(endParts?.get(2) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var endHour by remember { mutableIntStateOf(endParts?.get(3) ?: (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 2)) }
    var endMinute by remember { mutableIntStateOf(endParts?.get(4) ?: Calendar.getInstance().get(Calendar.MINUTE)) }

    var hasEndTime by remember { mutableStateOf(endParts != null) }
    
    // Format display
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    
    val startDateTimeDisplay = "${monthNames[startMonth]} $startDay, $startYear at ${String.format("%02d:%02d", startHour, startMinute)}"
    val endDateTimeDisplay = "${monthNames[endMonth]} $endDay, $endYear at ${String.format("%02d:%02d", endHour, endMinute)}"
    
    // Validation
    val titleError = when {
        eventTitle.isBlank() -> "Title is required"
        eventTitle.length < 3 -> "Title must be at least 3 characters"
        eventTitle.length > 100 -> "Title must be less than 100 characters"
        else -> null
    }
    
    val locationError = when {
        location.isBlank() -> "Location is required"
        else -> null
    }
    
    // Location dropdown
    var showLocationDropdown by remember { mutableStateOf(false) }
    val predefinedLocations = listOf(
        "E1", "E2", "P1", "P2", "C3", "C4", 
        "Agora", "E3", "C3-Room", "C3-Relax", 
        "C4-rooms", "Elevator-room"
    )

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Text(
                text = dialogTitle,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Event title
                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    label = { Text("Event Title *") },
                    enabled = !isCreating,
                    isError = titleError != null,
                    supportingText = if (titleError != null) {
                        { Text(titleError, color = DarkError) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )
                
                // Location dropdown
                ExposedDropdownMenuBox(
                    expanded = showLocationDropdown,
                    onExpandedChange = { showLocationDropdown = !showLocationDropdown }
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Location *") },
                        enabled = !isCreating,
                        placeholder = { Text("Select location") },
                        isError = locationError != null,
                        supportingText = if (locationError != null) {
                            { Text(locationError, color = DarkError) }
                        } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLocationDropdown) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = PrimaryPurple
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            focusedLabelColor = PrimaryPurple,
                            cursorColor = PrimaryPurple
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showLocationDropdown,
                        onDismissRequest = { showLocationDropdown = false }
                    ) {
                        predefinedLocations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    location = loc
                                    showLocationDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // Start Date and Time
                Text(
                    text = "Event Start Time *",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Custom clickable field for start date/time
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCreating) { 
                            showStartDateTimePicker = true 
                        }
                        .border(
                            width = 1.dp,
                            color = PrimaryPurple,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Start DateTime",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Date & Time",
                                color = PrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = startDateTimeDisplay,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Voting concept info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = "Voting Info",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Track voting will be displayed between event start and end time",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // End Time Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set End Time (optional)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = hasEndTime,
                        onCheckedChange = { hasEndTime = it },
                        enabled = !isCreating,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryPurple,
                            checkedTrackColor = PrimaryPurple.copy(alpha = 0.5f)
                        )
                    )
                }
                
                if (hasEndTime) {
                    // Custom clickable field for end date/time
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCreating) { 
                                showEndDateTimePicker = true 
                            }
                            .border(
                                width = 1.dp,
                                color = PrimaryPurple,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = "End DateTime",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End Date & Time",
                                    color = PrimaryPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = endDateTimeDisplay,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    enabled = !isCreating,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )
                
                // Public/Private toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isPublic) "Public Event" else "Private Event",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isPublic) "Everyone can see and join this event" else "Only invited users can join",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        enabled = !isCreating,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryPurple,
                            checkedTrackColor = PrimaryPurple.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleError == null && locationError == null) {
                        // Format start time for backend
                        val startTimeFormatted = String.format(
                            "%04d-%02d-%02d %02d:%02d",
                            startYear, startMonth + 1, startDay, startHour, startMinute
                        )
                        
                        // Format end time if enabled
                        val endTimeFormatted = if (hasEndTime) {
                            String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                endYear, endMonth + 1, endDay, endHour, endMinute
                            )
                        } else null
                        
                        onCreateEvent(
                            eventTitle.trim(),
                            location,
                            description.takeIf { it.isNotBlank() }?.trim(),
                            isPublic,
                            startTimeFormatted,
                            endTimeFormatted
                        )
                    }
                },
                enabled = !isCreating && titleError == null && locationError == null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(confirmButtonLoadingText)
                } else {
                    Text(confirmButtonText)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )

    // Start Date Time Picker
    if (showStartDateTimePicker) {
        DateTimePickerFlow(
            initialYear = startYear,
            initialMonth = startMonth,
            initialDay = startDay,
            initialHour = startHour,
            initialMinute = startMinute,
            onDateTimeSelected = { year, month, day, hour, minute ->
                startYear = year
                startMonth = month
                startDay = day
                startHour = hour
                startMinute = minute
                showStartDateTimePicker = false
            },
            onDismiss = { showStartDateTimePicker = false }
        )
    }

    // End Date Time Picker
    if (showEndDateTimePicker) {
        DateTimePickerFlow(
            initialYear = endYear,
            initialMonth = endMonth,
            initialDay = endDay,
            initialHour = endHour,
            initialMinute = endMinute,
            onDateTimeSelected = { year, month, day, hour, minute ->
                endYear = year
                endMonth = month
                endDay = day
                endHour = hour
                endMinute = minute
                showEndDateTimePicker = false
            },
            onDismiss = { showEndDateTimePicker = false }
        )
    }
}

/** Native Material3 date picker, then time picker, chained into one (year, month, day, hour, minute) result. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerFlow(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onDateTimeSelected: (Int, Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickingTime by remember { mutableStateOf(false) }

    // DatePicker's selectedDateMillis is midnight UTC for the chosen date, so read/write it in UTC
    // to avoid the local timezone shifting the day by +/-1.
    val utc = remember { TimeZone.getTimeZone("UTC") }
    val initialMillis = remember {
        Calendar.getInstance(utc).apply {
            clear()
            set(initialYear, initialMonth, initialDay)
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    if (!pickingTime) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickingTime = true },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis ?: initialMillis
                    val cal = Calendar.getInstance(utc).apply { timeInMillis = millis }
                    onDateTimeSelected(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                        timePickerState.hour,
                        timePickerState.minute
                    )
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
