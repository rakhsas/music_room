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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    isCreating: Boolean,
    onCreateEvent: (title: String, location: String, description: String?, isPublic: Boolean, startTime: String, endTime: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var eventTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    
    // Simple date and time states
    var showStartDateTimePicker by remember { mutableStateOf(false) }
    var showEndDateTimePicker by remember { mutableStateOf(false) }
    
    // Use simple state for date and time
    var startYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var startMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var startDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var startHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    var endYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var endMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var endDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var endHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 2) }
    var endMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    var hasEndTime by remember { mutableStateOf(false) }
    
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
                text = "Create New Event",
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
                    Text("Creating...")
                } else {
                    Text("Create Event")
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
        SimpleFixedDateTimePicker(
            title = "Select Start Date & Time",
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
        SimpleFixedDateTimePicker(
            title = "Select End Date & Time",
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

@Composable
private fun SimpleFixedDateTimePicker(
    title: String,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onDateTimeSelected: (Int, Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Current selection display
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "${monthNames[selectedMonth]} $selectedDay, $selectedYear at ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        modifier = Modifier.padding(12.dp),
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Year Selection
                Text("Year", color = TextPrimary, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((2025..2030).toList()) { year ->
                        FilterChip(
                            onClick = { selectedYear = year },
                            label = { Text(year.toString()) },
                            selected = selectedYear == year,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Month Selection
                Text("Month", color = TextPrimary, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(monthNames.size) { index ->
                        FilterChip(
                            onClick = { selectedMonth = index },
                            label = { Text(monthNames[index].take(3)) },
                            selected = selectedMonth == index,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Day Selection
                Text("Day", color = TextPrimary, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items((1..31).toList()) { day ->
                        FilterChip(
                            onClick = { selectedDay = day },
                            label = { Text(day.toString()) },
                            selected = selectedDay == day,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Hour Selection
                Text("Hour", color = TextPrimary, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items((0..23).toList()) { hour ->
                        FilterChip(
                            onClick = { selectedHour = hour },
                            label = { Text(String.format("%02d", hour)) },
                            selected = selectedHour == hour,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Minute Selection
                Text("Minute", color = TextPrimary, fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items((0..59 step 5).toList()) { minute ->
                        FilterChip(
                            onClick = { selectedMinute = minute },
                            label = { Text(String.format("%02d", minute)) },
                            selected = selectedMinute == minute,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateTimeSelected(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
