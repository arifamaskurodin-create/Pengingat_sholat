package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.CalmAlarmContainer
import com.example.data.AlarmEntity
import com.example.ui.screens.AlarmEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RingingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.AlarmEditorViewModel
import com.example.ui.viewmodel.AlarmListViewModel
import com.example.ui.viewmodel.AlarmViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel

object AlarmRoutes {
    const val HOME = "home"
    const val EDITOR = "editor/{alarmId}"
    const val SETTINGS = "settings"
    const val RINGING_PREVIEW = "ringing_preview/{alarmId}"

    fun editorRoute(alarmId: Long = 0L) = "editor/$alarmId"
    fun ringingPreviewRoute(alarmId: Long = 0L) = "ringing_preview/$alarmId"
}

@Composable
fun AlarmNavHost(container: CalmAlarmContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = AlarmRoutes.HOME,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(260)
            ) + fadeIn(tween(220))
        },
        exitTransition = {
            fadeOut(tween(180))
        },
        popEnterTransition = {
            fadeIn(tween(220))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(240)
            ) + fadeOut(tween(180))
        }
    ) {
        composable(AlarmRoutes.HOME) {
            val viewModel: AlarmListViewModel = viewModel(
                factory = AlarmViewModelFactory(
                    repository = container.alarmRepository,
                    scheduler = container.alarmScheduler,
                    toneManager = container.alarmToneManager
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState,
                onAddAlarm = { navController.navigate(AlarmRoutes.editorRoute(0L)) },
                onQuickAddAlarm = viewModel::quickAddAlarm,
                onEditAlarm = { alarmId -> navController.navigate(AlarmRoutes.editorRoute(alarmId)) },
                onToggleAlarm = viewModel::toggleAlarm,
                onDeleteAlarm = viewModel::deleteAlarm,
                onRestoreAlarm = viewModel::restoreAlarm,
                onOpenSettings = { navController.navigate(AlarmRoutes.SETTINGS) },
                onPreviewRinging = { alarmId ->
                    navController.navigate(AlarmRoutes.ringingPreviewRoute(alarmId))
                },
                onRefreshPermissions = viewModel::refreshPermissions
            )
        }

        composable(
            route = AlarmRoutes.EDITOR,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            val viewModel: AlarmEditorViewModel = viewModel(
                key = "editor_$alarmId",
                factory = AlarmViewModelFactory(
                    repository = container.alarmRepository,
                    scheduler = container.alarmScheduler,
                    toneManager = container.alarmToneManager,
                    alarmId = alarmId
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AlarmEditorScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onSetTime = viewModel::setTime,
                onSetHour = viewModel::setHour,
                onSetMinute = viewModel::setMinute,
                onSetAmPm = viewModel::setAmPm,
                onLabelChange = viewModel::setLabel,
                onToggleRepeatDay = viewModel::toggleRepeatDay,
                onSetRepeatMask = viewModel::setRepeatMask,
                onSelectTone = viewModel::selectTone,
                onToggleTonePreview = viewModel::toggleTonePreview,
                onStopTonePreview = viewModel::stopTonePreview,
                onVibrateChange = viewModel::setVibrate,
                onGradualWakeChange = viewModel::setGradualWake,
                onSnoozeMinutesChange = viewModel::setSnoozeMinutes,
                onSave = {
                    viewModel.saveAlarm {
                        navController.popBackStack()
                    }
                },
                onDelete = {
                    viewModel.deleteAlarm {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(AlarmRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = AlarmViewModelFactory(
                    repository = container.alarmRepository,
                    scheduler = container.alarmScheduler,
                    toneManager = container.alarmToneManager
                )
            )
            val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
            val canExact by viewModel.canScheduleExactAlarms.collectAsStateWithLifecycle()

            SettingsScreen(
                settings = settings,
                canScheduleExactAlarms = canExact,
                onNavigateBack = { navController.popBackStack() },
                onToggle24Hour = viewModel::setUse24Hour,
                onDefaultSnoozeChange = viewModel::setDefaultSnooze,
                onSilenceAfterChange = viewModel::setSilenceAfter,
                onDefaultGradualWakeChange = viewModel::setDefaultGradualWake,
                onPreviewRingingScreen = {
                    navController.navigate(AlarmRoutes.ringingPreviewRoute(0L))
                },
                onRefreshPermissions = viewModel::refreshPermissions
            )
        }

        composable(
            route = AlarmRoutes.RINGING_PREVIEW,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            val settings by container.alarmRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.AlarmSettings()
            )
            var previewAlarm by remember {
                mutableStateOf(
                    AlarmEntity(
                        hour = 4,
                        minute = 35,
                        label = "Subuh",
                        snoozeMinutes = settings.defaultSnoozeMinutes
                    )
                )
            }

            LaunchedEffect(alarmId) {
                if (alarmId > 0L) {
                    container.alarmRepository.getAlarmById(alarmId)?.let {
                        previewAlarm = it
                    }
                }
            }

            DisposableEffect(previewAlarm.ringtoneUri) {
                container.alarmToneManager.playPreview(previewAlarm.ringtoneUri)
                onDispose {
                    container.alarmToneManager.stopPreview()
                }
            }

            RingingScreen(
                hour = previewAlarm.hour,
                minute = previewAlarm.minute,
                label = previewAlarm.label.ifBlank { "Morning Focus" },
                snoozeMinutes = previewAlarm.snoozeMinutes.takeIf { it > 0 } ?: 10,
                is24Hour = settings.is24HourFormat,
                isPreview = true,
                onSnooze = {
                    container.alarmToneManager.stopPreview()
                    val mins = previewAlarm.snoozeMinutes.takeIf { it > 0 } ?: 10
                    Toast.makeText(
                        context,
                        "Preview: Snoozed for $mins min",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onDismiss = {
                    container.alarmToneManager.stopPreview()
                    navController.popBackStack()
                }
            )
        }
    }
}
