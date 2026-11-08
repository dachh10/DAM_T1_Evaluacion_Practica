package com.example.dam_t1_evaluacion_practica.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Native LocationManager-based GPS capture. No play-services dependency.
 * Resolves GPS_PROVIDER first, NETWORK_PROVIDER as fallback; prefers the last known fix
 * and, when missing, requests a single one-shot fix with a ~5s timeout. Invokes the
 * callback with (null, null) when permission is missing, providers are disabled or no
 * fix is available, so the caller degrades gracefully (emulator without GPS included).
 */
object LocationManagerHelper {

    private const val SINGLE_FIX_TIMEOUT_MS = 5_000L
    private const val MIN_TIME_MS = 0L
    private const val MIN_DISTANCE_M = 0f

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun requestCurrentLocation(context: Context, callback: (lat: Double?, lng: Double?) -> Unit) {
        if (!hasPermission(context)) {
            callback(null, null)
            return
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            callback(null, null)
            return
        }

        @Suppress("DEPRECATION")
        val enabledProvider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        if (enabledProvider == null) {
            callback(null, null)
            return
        }

        @SuppressLint("MissingPermission")
        val lastKnown = runCatching { locationManager.getLastKnownLocation(enabledProvider) }.getOrNull()
        if (lastKnown != null) {
            callback(lastKnown.latitude, lastKnown.longitude)
            return
        }

        requestSingleFix(locationManager, enabledProvider, callback)
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleFix(
        locationManager: LocationManager,
        provider: String,
        callback: (lat: Double?, lng: Double?) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        var resolved = false
        var listener: LocationListener? = null

        fun onResult(location: Location?) {
            if (resolved) return
            resolved = true
            handler.removeCallbacksAndMessages(null)
            listener?.let { locationManager.removeUpdates(it) }
            callback(location?.latitude, location?.longitude)
        }

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onResult(location)

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // No-op: kept for Android 9- (API 24-29) platform compatibility.
            }

            @Suppress("DEPRECATION")
            override fun onProviderEnabled(provider: String) {
                // No-op.
            }

            @Suppress("DEPRECATION")
            override fun onProviderDisabled(provider: String) {
                // No-op.
            }
        }

        handler.postDelayed({ onResult(null) }, SINGLE_FIX_TIMEOUT_MS)

        try {
            locationManager.requestLocationUpdates(
                provider, MIN_TIME_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            onResult(null)
        }
    }
}