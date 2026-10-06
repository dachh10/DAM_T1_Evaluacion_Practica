package com.example.dam_t1_evaluacion_practica.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.dam_t1_evaluacion_practica.util.LocationManagerHelper

/** Captures a real GPS position; the callback receives (lat, lng) or (null, null). */
typealias LocationCapture = (callback: (lat: Double?, lng: Double?) -> Unit) -> Unit

/**
 * Compose helper that obtains the device's real GPS position with the native
 * LocationManager (no play-services dependency). Requests ACCESS_FINE_LOCATION (coarse as
 * fallback) when not granted yet, then resolves GPS_PROVIDER/NETWORK_PROVIDER via
 * [LocationManagerHelper]. Returns null coordinates when permission is denied, GPS is
 * disabled or no fix is available within ~5 seconds.
 */
@Composable
fun rememberLocationCapture(): LocationCapture {
    val context = LocalContext.current.applicationContext
    var pendingCallback by remember { mutableStateOf<((Double?, Double?) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val callback = pendingCallback
        pendingCallback = null
        if (callback == null) return@rememberLauncherForActivityResult
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            LocationManagerHelper.requestCurrentLocation(context, callback)
        } else {
            callback(null, null)
        }
    }

    return remember {
        { callback: (lat: Double?, lng: Double?) -> Unit ->
            if (LocationManagerHelper.hasPermission(context)) {
                LocationManagerHelper.requestCurrentLocation(context, callback)
            } else {
                pendingCallback = callback
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}