package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.LocationHelper
import com.example.ui.PrayerViewModel
import com.example.ui.ScheduleScreen
import com.example.ui.SoundSettingsScreen
import com.example.ui.theme.PengingatShalatTheme

object AppRoutes {
  const val ROUTE_SCHEDULE = "schedule"
  const val ROUTE_SOUND_SETTINGS = "sound_settings"
}

class MainActivity : ComponentActivity() {

  private var prayerViewModel: PrayerViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PengingatShalatTheme {
        val context = LocalContext.current
        val vm: PrayerViewModel = viewModel()
        prayerViewModel = vm
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val navController = rememberNavController()

        var hasNotificationPermission by remember {
          mutableStateOf(checkNotificationPermission(context))
        }

        val notificationPermissionLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
          ) { granted ->
            hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
          }

        val locationPermissionLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
          ) { permissions ->
            val granted =
              permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
              vm.refreshGpsLocation()
            }
          }

        LaunchedEffect(Unit) {
          if (LocationHelper.hasLocationPermission(context)) {
            vm.refreshGpsLocation()
          } else {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
              )
            )
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }

        NavHost(
          navController = navController,
          startDestination = AppRoutes.ROUTE_SCHEDULE,
        ) {
          composable(AppRoutes.ROUTE_SCHEDULE) {
            ScheduleScreen(
              uiState = uiState,
              hasNotificationPermission = hasNotificationPermission,
              onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              },
              onRequestExactAlarmSettings = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  val intent =
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                      data = Uri.parse("package:$packageName")
                    }
                  runCatching { startActivity(intent) }
                }
              },
              onRefreshGps = {
                if (LocationHelper.hasLocationPermission(context)) {
                  vm.refreshGpsLocation()
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_COARSE_LOCATION,
                      Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                  )
                }
              },
              onSelectCity = { city -> vm.selectCity(city) },
              onOpenSoundSettings = {
                vm.syncDraftFromSaved()
                navController.navigate(AppRoutes.ROUTE_SOUND_SETTINGS)
              },
              onToggleAlreadyPrayed = { prayerType, checked ->
                vm.toggleAlreadyPrayed(prayerType, checked)
              },
              onSelectQuickOverride = { prayerType, override ->
                vm.setQuickOverride(prayerType, override)
              },
              onSnackbarShown = { vm.clearSnackbar() },
            )
          }

          composable(AppRoutes.ROUTE_SOUND_SETTINGS) {
            SoundSettingsScreen(
              uiState = uiState,
              onNavigateBack = { navController.popBackStack() },
              onSelectMode = { category, mode -> vm.updateDraftMode(category, mode) },
              onPickAudioFile = { category, uri -> vm.onAudioFilePicked(category, uri) },
              onTestSound = { category -> vm.previewDraftSound(category) },
              onStopSound = { vm.stopPreviewSound() },
              onSaveAndApply = { vm.saveAndApplySoundSettings() },
              onSnackbarShown = { vm.clearSnackbar() },
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    prayerViewModel?.refreshExactAlarmStatus()
  }

  private fun checkNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
  }
}
