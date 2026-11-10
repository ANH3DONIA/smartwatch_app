/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.focuspause.data.DataStoreFocusRepository
import com.example.focuspause.haptics.HapticFeedbackController
import com.example.focuspause.notifications.AndroidTimerAlarmScheduler
import com.example.focuspause.sync.WearSyncManager
import com.example.focuspause.ui.phone.PhoneFocusPauseApp
import com.example.focuspause.ui.wear.WearFocusPauseApp
import com.example.focuspause.viewmodel.FocusTimerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class AppScreen {
    TIMER,
    SETTINGS,
    STATS,
    REVIEWS
}

class MainActivity : ComponentActivity() {

    private val repository by lazy { DataStoreFocusRepository(applicationContext) }

    // Sincroniza con el otro dispositivo (celular <-> reloj) mientras la app está visible.
    private val syncManager by lazy {
        WearSyncManager(applicationContext, repository, lifecycleScope) { viewModel.reloadFromRepository() }
    }

    private val viewModel: FocusTimerViewModel by viewModels {
        viewModelFactory {
            initializer {
                FocusTimerViewModel(
                    hapticProvider = HapticFeedbackController(applicationContext),
                    repository = repository,
                    alarmScheduler = AndroidTimerAlarmScheduler(applicationContext),
                    onDataChanged = { syncManager.push() }
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val isWatch = packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

        // Pantalla encendida mientras el temporizador corre (si el ajuste está activo).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.isRunning && it.settings.keepScreenOn }
                    .distinctUntilChanged()
                    .collect { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
            }
        }

        setContent {
            if (isWatch) {
                WearFocusPauseApp(viewModel = viewModel)
            } else {
                PhoneFocusPauseApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncManager.start()
    }

    override fun onStop() {
        syncManager.stop()
        super.onStop()
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin acción: si se niega, solo no hay aviso */ }

    /** Android 13+ exige permiso para mostrar el aviso de fin de sesión. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
