package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmTimeCalculator
import com.example.alarm.AlarmToneManager
import com.example.alarm.AlarmToneOption
import com.example.data.AlarmEntity
import com.example.data.AlarmRepository
import com.example.data.AlarmSettings
import com.example.data.RepeatDays
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmListUiState(
    val isLoading: Boolean = true,
    val alarms: List<AlarmEntity> = emptyList(),
    val settings: AlarmSettings = AlarmSettings(),
    val nextAlarm: AlarmEntity? = null,
    val nextTriggerMillis: Long? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val canScheduleExactAlarms: Boolean = true
)

class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    private val exactPermissionFlow = MutableStateFlow(scheduler.canScheduleExactAlarms())

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<AlarmListUiState> = combine(
        repository.alarmsFlow,
        repository.settingsFlow,
        tickerFlow,
        exactPermissionFlow
    ) { alarms, settings, nowMillis, canExact ->
        val nextPair = AlarmTimeCalculator.findNextAlarm(alarms, nowMillis)
        AlarmListUiState(
            isLoading = false,
            alarms = alarms,
            settings = settings,
            nextAlarm = nextPair?.first,
            nextTriggerMillis = nextPair?.second,
            nowMillis = nowMillis,
            canScheduleExactAlarms = canExact
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AlarmListUiState(
            canScheduleExactAlarms = scheduler.canScheduleExactAlarms()
        )
    )

    init {
        viewModelScope.launch {
            repository.rescheduleAllEnabledAlarms()
        }
    }

    fun quickAddAlarm(
        hour: Int,
        minute: Int,
        label: String,
        repeatDaysMask: Int
    ) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            repository.createAlarm(
                hour = hour,
                minute = minute,
                label = label,
                repeatDaysMask = repeatDaysMask,
                isEnabled = true,
                gradualWake = settings.defaultGradualWake,
                snoozeMinutes = settings.defaultSnoozeMinutes
            )
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, enabled)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun restoreAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
        }
    }

    fun refreshPermissions() {
        exactPermissionFlow.value = scheduler.canScheduleExactAlarms()
    }
}

data class AlarmEditorUiState(
    val alarmId: Long = 0L,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDaysMask: Int = 0,
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "Default system alarm",
    val vibrate: Boolean = true,
    val gradualWake: Boolean = true,
    val snoozeMinutes: Int = 10,
    val is24Hour: Boolean = false,
    val availableTones: List<AlarmToneOption> = listOf(
        AlarmToneOption("Default system alarm", "")
    ),
    val previewingToneUri: String? = null,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false
)

class AlarmEditorViewModel(
    private val repository: AlarmRepository,
    private val toneManager: AlarmToneManager,
    private val alarmId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditorUiState(alarmId = alarmId))
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tones = toneManager.loadAlarmTones()
            val settings = repository.settingsFlow.stateIn(viewModelScope).value
            val existing = if (alarmId > 0L) repository.getAlarmById(alarmId) else null
            _uiState.update { state ->
                if (existing != null) {
                    state.copy(
                        alarmId = existing.id,
                        hour = existing.hour,
                        minute = existing.minute,
                        label = existing.label,
                        isEnabled = existing.isEnabled,
                        repeatDaysMask = existing.repeatDaysMask,
                        ringtoneUri = existing.ringtoneUri,
                        ringtoneTitle = existing.ringtoneTitle,
                        vibrate = existing.vibrate,
                        gradualWake = existing.gradualWake,
                        snoozeMinutes = existing.snoozeMinutes,
                        is24Hour = settings.is24HourFormat,
                        availableTones = tones,
                        isLoaded = true
                    )
                } else {
                    state.copy(
                        hour = 7,
                        minute = 0,
                        snoozeMinutes = settings.defaultSnoozeMinutes,
                        gradualWake = settings.defaultGradualWake,
                        is24Hour = settings.is24HourFormat,
                        availableTones = tones,
                        isLoaded = true
                    )
                }
            }
        }
    }

    fun setTime(hour24: Int, minute: Int) {
        _uiState.update {
            it.copy(
                hour = hour24.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59)
            )
        }
    }

    fun setHour(hour24: Int) {
        _uiState.update { it.copy(hour = hour24.coerceIn(0, 23)) }
    }

    fun setMinute(minute: Int) {
        _uiState.update { it.copy(minute = minute.coerceIn(0, 59)) }
    }

    fun setAmPm(isPm: Boolean) {
        _uiState.update { state ->
            val h12 = state.hour % 12
            val newHour = if (isPm) h12 + 12 else h12
            state.copy(hour = newHour)
        }
    }

    fun setLabel(label: String) {
        _uiState.update { it.copy(label = label) }
    }

    fun toggleRepeatDay(dayBit: Int) {
        _uiState.update {
            it.copy(repeatDaysMask = RepeatDays.toggleDay(it.repeatDaysMask, dayBit))
        }
    }

    fun setRepeatMask(mask: Int) {
        _uiState.update { it.copy(repeatDaysMask = mask) }
    }

    fun selectTone(option: AlarmToneOption) {
        _uiState.update {
            it.copy(ringtoneUri = option.uriString, ringtoneTitle = option.title)
        }
    }

    fun toggleTonePreview(uriString: String) {
        val currentPreview = _uiState.value.previewingToneUri
        if (currentPreview == uriString) {
            toneManager.stopPreview()
            _uiState.update { it.copy(previewingToneUri = null) }
        } else {
            toneManager.playPreview(uriString)
            _uiState.update { it.copy(previewingToneUri = uriString) }
        }
    }

    fun stopTonePreview() {
        toneManager.stopPreview()
        _uiState.update { it.copy(previewingToneUri = null) }
    }

    fun setVibrate(enabled: Boolean) {
        _uiState.update { it.copy(vibrate = enabled) }
    }

    fun setGradualWake(enabled: Boolean) {
        _uiState.update { it.copy(gradualWake = enabled) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        _uiState.update { it.copy(snoozeMinutes = minutes) }
    }

    fun saveAlarm(onSaved: (AlarmEntity) -> Unit) {
        val s = _uiState.value
        if (s.isSaving) return
        stopTonePreview()
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val entity = AlarmEntity(
                id = s.alarmId,
                hour = s.hour,
                minute = s.minute,
                label = s.label.trim(),
                isEnabled = true,
                repeatDaysMask = s.repeatDaysMask,
                ringtoneUri = s.ringtoneUri,
                ringtoneTitle = s.ringtoneTitle,
                vibrate = s.vibrate,
                gradualWake = s.gradualWake,
                snoozeMinutes = s.snoozeMinutes
            )
            val saved = repository.saveAlarm(entity)
            onSaved(saved)
        }
    }

    fun deleteAlarm(onDeleted: () -> Unit) {
        val id = _uiState.value.alarmId
        if (id <= 0L) {
            onDeleted()
            return
        }
        stopTonePreview()
        viewModelScope.launch {
            repository.getAlarmById(id)?.let { repository.deleteAlarm(it) }
            onDeleted()
        }
    }

    override fun onCleared() {
        toneManager.stopPreview()
        super.onCleared()
    }
}

class SettingsViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    private val exactPermissionFlow = MutableStateFlow(scheduler.canScheduleExactAlarms())

    val settingsFlow: StateFlow<AlarmSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AlarmSettings()
    )

    val canScheduleExactAlarms: StateFlow<Boolean> = exactPermissionFlow.asStateFlow()

    fun setUse24Hour(enabled: Boolean) {
        viewModelScope.launch { repository.updateUse24Hour(enabled) }
    }

    fun setDefaultSnooze(minutes: Int) {
        viewModelScope.launch { repository.updateDefaultSnooze(minutes) }
    }

    fun setSilenceAfter(minutes: Int) {
        viewModelScope.launch { repository.updateSilenceAfter(minutes) }
    }

    fun setDefaultGradualWake(enabled: Boolean) {
        viewModelScope.launch { repository.updateDefaultGradualWake(enabled) }
    }

    fun refreshPermissions() {
        exactPermissionFlow.value = scheduler.canScheduleExactAlarms()
    }
}

class AlarmViewModelFactory(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val toneManager: AlarmToneManager,
    private val alarmId: Long = 0L
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AlarmListViewModel::class.java) ->
                AlarmListViewModel(repository, scheduler) as T
            modelClass.isAssignableFrom(AlarmEditorViewModel::class.java) ->
                AlarmEditorViewModel(repository, toneManager, alarmId) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, scheduler) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
