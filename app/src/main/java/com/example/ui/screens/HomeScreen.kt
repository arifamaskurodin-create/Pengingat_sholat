package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.alarm.AlarmTimeCalculator
import com.example.data.AlarmEntity
import com.example.data.RepeatDays
import com.example.ui.components.TabularTimeWheelPicker
import com.example.ui.theme.IceBlueFixedDim
import com.example.ui.theme.NavyAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer
import com.example.ui.theme.OnNavyContainer
import com.example.ui.theme.tabularNums
import com.example.ui.viewmodel.AlarmListUiState
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: AlarmListUiState,
    onAddAlarm: () -> Unit,
    onQuickAddAlarm: (hour: Int, minute: Int, label: String, repeatDaysMask: Int) -> Unit,
    onEditAlarm: (Long) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onRestoreAlarm: (AlarmEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onPreviewRinging: (Long) -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pickerHour by rememberSaveable { mutableIntStateOf(4) }
    var pickerMinute by rememberSaveable { mutableIntStateOf(35) }
    var pickerLabel by rememberSaveable { mutableStateOf("Subuh") }
    var pickerRepeatDaily by rememberSaveable { mutableStateOf(true) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                containerColor = NavyPrimaryContainer,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                shape = CircleShape,
                modifier = Modifier.testTag("add_alarm_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.cd_add_alarm)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideLayout = maxWidth >= 680.dp

            if (isWideLayout) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 104.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .align(Alignment.TopCenter)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        HomeHeader(
                            nowMillis = uiState.nowMillis,
                            is24Hour = uiState.settings.is24HourFormat,
                            onOpenSettings = onOpenSettings
                        )
                    }

                    if (!hasNotificationPermission || !uiState.canScheduleExactAlarms) {
                        item(span = { GridItemSpan(2) }) {
                            PermissionCallout(
                                needsNotifications = !hasNotificationPermission,
                                needsExactAlarm = !uiState.canScheduleExactAlarms,
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                },
                                onOpenExactAlarmSettings = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        runCatching { context.startActivity(intent) }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        RoyalNavyTimePickerHeroCard(
                            hour24 = pickerHour,
                            minute = pickerMinute,
                            label = pickerLabel,
                            repeatDaily = pickerRepeatDaily,
                            is24Hour = uiState.settings.is24HourFormat,
                            nextAlarm = uiState.nextAlarm,
                            nextTriggerMillis = uiState.nextTriggerMillis,
                            nowMillis = uiState.nowMillis,
                            onHourChange = { pickerHour = it },
                            onMinuteChange = { pickerMinute = it },
                            onAmPmChange = { isPm ->
                                val base = pickerHour % 12
                                pickerHour = if (isPm) base + 12 else base
                            },
                            onPresetSelected = { h, m, title ->
                                pickerHour = h
                                pickerMinute = m
                                pickerLabel = title
                            },
                            onToggleRepeatDaily = { pickerRepeatDaily = !pickerRepeatDaily },
                            onScheduleAlarm = {
                                val mask = if (pickerRepeatDaily) RepeatDays.EVERY_DAY else 0
                                onQuickAddAlarm(pickerHour, pickerMinute, pickerLabel, mask)
                                val (t, p) = AlarmTimeCalculator.formatTimeParts(
                                    pickerHour,
                                    pickerMinute,
                                    uiState.settings.is24HourFormat
                                )
                                val formatted = if (p != null) "$t $p" else t
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Scheduled $pickerLabel at $formatted",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onPreviewRinging = onPreviewRinging
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AlarmsSectionHeader(
                                totalCount = uiState.alarms.size,
                                enabledCount = uiState.alarms.count { it.isEnabled }
                            )
                            if (uiState.alarms.isEmpty() && !uiState.isLoading) {
                                EmptyAlarmsCard(onAddAlarm = onAddAlarm)
                            }
                        }
                    }

                    items(uiState.alarms, key = { it.id }) { alarm ->
                        AlarmListItemCard(
                            alarm = alarm,
                            nowMillis = uiState.nowMillis,
                            is24Hour = uiState.settings.is24HourFormat,
                            onClick = { onEditAlarm(alarm.id) },
                            onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                            onDelete = {
                                onDeleteAlarm(alarm)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Alarm deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        onRestoreAlarm(alarm)
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .align(Alignment.TopCenter)
                ) {
                    item {
                        HomeHeader(
                            nowMillis = uiState.nowMillis,
                            is24Hour = uiState.settings.is24HourFormat,
                            onOpenSettings = onOpenSettings
                        )
                    }

                    if (!hasNotificationPermission || !uiState.canScheduleExactAlarms) {
                        item {
                            PermissionCallout(
                                needsNotifications = !hasNotificationPermission,
                                needsExactAlarm = !uiState.canScheduleExactAlarms,
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                },
                                onOpenExactAlarmSettings = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        runCatching { context.startActivity(intent) }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        RoyalNavyTimePickerHeroCard(
                            hour24 = pickerHour,
                            minute = pickerMinute,
                            label = pickerLabel,
                            repeatDaily = pickerRepeatDaily,
                            is24Hour = uiState.settings.is24HourFormat,
                            nextAlarm = uiState.nextAlarm,
                            nextTriggerMillis = uiState.nextTriggerMillis,
                            nowMillis = uiState.nowMillis,
                            onHourChange = { pickerHour = it },
                            onMinuteChange = { pickerMinute = it },
                            onAmPmChange = { isPm ->
                                val base = pickerHour % 12
                                pickerHour = if (isPm) base + 12 else base
                            },
                            onPresetSelected = { h, m, title ->
                                pickerHour = h
                                pickerMinute = m
                                pickerLabel = title
                            },
                            onToggleRepeatDaily = { pickerRepeatDaily = !pickerRepeatDaily },
                            onScheduleAlarm = {
                                val mask = if (pickerRepeatDaily) RepeatDays.EVERY_DAY else 0
                                onQuickAddAlarm(pickerHour, pickerMinute, pickerLabel, mask)
                                val (t, p) = AlarmTimeCalculator.formatTimeParts(
                                    pickerHour,
                                    pickerMinute,
                                    uiState.settings.is24HourFormat
                                )
                                val formatted = if (p != null) "$t $p" else t
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Scheduled $pickerLabel at $formatted",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onPreviewRinging = onPreviewRinging
                        )
                    }

                    item {
                        AlarmsSectionHeader(
                            totalCount = uiState.alarms.size,
                            enabledCount = uiState.alarms.count { it.isEnabled }
                        )
                    }

                    if (uiState.alarms.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyAlarmsCard(onAddAlarm = onAddAlarm)
                        }
                    } else {
                        items(uiState.alarms, key = { it.id }) { alarm ->
                            AlarmListItemCard(
                                alarm = alarm,
                                nowMillis = uiState.nowMillis,
                                is24Hour = uiState.settings.is24HourFormat,
                                onClick = { onEditAlarm(alarm.id) },
                                onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                                onDelete = {
                                    onDeleteAlarm(alarm)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Alarm deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            onRestoreAlarm(alarm)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    nowMillis: Long,
    is24Hour: Boolean,
    onOpenSettings: () -> Unit
) {
    val cal = remember(nowMillis / 1000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val (timeText, period) = AlarmTimeCalculator.formatTimeParts(
        hour24 = cal.get(Calendar.HOUR_OF_DAY),
        minute = cal.get(Calendar.MINUTE),
        is24Hour = is24Hour
    )
    val seconds = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))
    val dateLabel = remember(cal.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.YEAR)) {
        DateFormat.format("EEEE, MMM d", cal).toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                shape = CircleShape,
                color = NavyPrimary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = NavyPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineLarge.tabularNums(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = ":$seconds",
                    style = MaterialTheme.typography.titleMedium.tabularNums(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
                if (period != null) {
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelLarge,
                        color = NavyPrimaryContainer,
                        modifier = Modifier.padding(start = 6.dp, bottom = 5.dp)
                    )
                }
            }
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("settings_button")
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = NavyPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RoyalNavyTimePickerHeroCard(
    hour24: Int,
    minute: Int,
    label: String,
    repeatDaily: Boolean,
    is24Hour: Boolean,
    nextAlarm: AlarmEntity?,
    nextTriggerMillis: Long?,
    nowMillis: Long,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (Boolean) -> Unit,
    onPresetSelected: (Int, Int, String) -> Unit,
    onToggleRepeatDaily: () -> Unit,
    onScheduleAlarm: () -> Unit,
    onPreviewRinging: (Long) -> Unit
) {
    val prayerPresets = remember {
        listOf(
            Triple(4, 35, "Subuh"),
            Triple(11, 55, "Dzuhur"),
            Triple(15, 15, "Ashar"),
            Triple(17, 55, "Maghrib"),
            Triple(19, 5, "Isya")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(NavyPrimary, NavyPrimaryContainer, NavyAccent)
                )
            )
            .testTag("royal_navy_time_picker_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.90f, size.height * 0.14f)
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.minDimension * 0.48f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.minDimension * 0.78f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Next Alarm Royal Navy Status Strip
            if (nextAlarm != null && nextTriggerMillis != null) {
                val (nextTimeStr, nextPeriod) = AlarmTimeCalculator.formatTimeParts(
                    nextAlarm.hour,
                    nextAlarm.minute,
                    is24Hour
                )
                val nextDisplay = if (nextPeriod != null) "$nextTimeStr $nextPeriod" else nextTimeStr
                val countdown = AlarmTimeCalculator.formatCountdown(nextTriggerMillis, nowMillis)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = IceBlueFixedDim,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${nextAlarm.label.ifBlank { "Next" }} • $nextDisplay",
                                style = MaterialTheme.typography.labelSmall.tabularNums().copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Rings $countdown",
                            style = MaterialTheme.typography.labelMedium.tabularNums(),
                            color = IceBlueFixedDim
                        )
                        Surface(
                            onClick = { onPreviewRinging(nextAlarm.id) },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Preview alarm ringing",
                                tint = IceBlueFixedDim,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AlarmOff,
                                contentDescription = null,
                                tint = IceBlueFixedDim,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_upcoming_alarms),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Prayer Quick-Select Chips inside Royal Navy Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                prayerPresets.forEach { (pHour, pMinute, pName) ->
                    val selected = label == pName && hour24 == pHour && minute == pMinute
                    Surface(
                        onClick = { onPresetSelected(pHour, pMinute, pName) },
                        shape = CircleShape,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("prayer_chip_$pName")
                    ) {
                        Text(
                            text = pName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) NavyPrimary else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Integrated Royal Navy Tabular Time Wheel Picker
            TabularTimeWheelPicker(
                hour24 = hour24,
                minute = minute,
                is24Hour = is24Hour,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange,
                onAmPmChange = onAmPmChange,
                onRoyalNavy = true,
                itemHeight = 48.dp
            )

            // Bottom Action Row: Repeat Toggle + Schedule Alarm CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onToggleRepeatDaily,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = IceBlueFixedDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (repeatDaily) "Every day" else "Once",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onScheduleAlarm,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NavyPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_schedule_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AlarmAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule $label",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCallout(
    needsNotifications: Boolean,
    needsExactAlarm: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = NavyPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (needsNotifications) {
                        stringResource(R.string.permission_notifications_title)
                    } else {
                        stringResource(R.string.permission_exact_alarm_title)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (needsNotifications) {
                        stringResource(R.string.permission_notifications_desc)
                    } else {
                        stringResource(R.string.permission_exact_alarm_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = if (needsNotifications) onRequestNotifications else onOpenExactAlarmSettings,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimaryContainer,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(
                        if (needsNotifications) R.string.allow else R.string.open_settings
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AlarmsSectionHeader(
    totalCount: Int,
    enabledCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.alarms_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (totalCount == 0) "0 scheduled" else "$enabledCount of $totalCount active",
            style = MaterialTheme.typography.labelMedium.tabularNums(),
            color = NavyPrimaryContainer
        )
    }
}

@Composable
private fun AlarmListItemCard(
    alarm: AlarmEntity,
    nowMillis: Long,
    is24Hour: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val (timeText, period) = AlarmTimeCalculator.formatTimeParts(alarm.hour, alarm.minute, is24Hour)
    val isSnoozed = alarm.isEnabled && alarm.snoozeUntilMillis != null && alarm.snoozeUntilMillis > nowMillis
    val contentAlpha = if (alarm.isEnabled) 1f else 0.56f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (alarm.isEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).alpha(contentAlpha)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.displayMedium.tabularNums(),
                            color = if (alarm.isEnabled) NavyPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (period != null) {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.labelLarge,
                                color = NavyPrimaryContainer,
                                modifier = Modifier.padding(start = 6.dp, bottom = 5.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = alarm.label.ifBlank { AlarmTimeCalculator.formatRepeatSummary(alarm.repeatDaysMask) },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSnoozed) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Snoozed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NavyPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NavyPrimaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("alarm_switch_${alarm.id}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    if (alarm.repeatDaysMask == 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "One-time",
                                style = MaterialTheme.typography.labelSmall,
                                color = NavyPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        RepeatDays.orderedBits.forEach { (bit, letter) ->
                            val selected = RepeatDays.isDaySelected(alarm.repeatDaysMask, bit)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            selected && alarm.isEnabled -> NavyPrimaryContainer
                                            selected -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = when {
                                        selected && alarm.isEnabled -> Color.White
                                        selected -> NavyPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedVisibility(visible = alarm.vibrate) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = "Vibration enabled",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).alpha(contentAlpha)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp).alpha(contentAlpha)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_alarm_icon_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_alarm),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmsCard(onAddAlarm: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = NavyPrimaryContainer
                )
            }
            Text(
                text = "Your schedule is clear",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Use the Royal Navy time picker above or tap Custom to add a reminder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddAlarm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimaryContainer,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.new_alarm))
            }
        }
    }
}
