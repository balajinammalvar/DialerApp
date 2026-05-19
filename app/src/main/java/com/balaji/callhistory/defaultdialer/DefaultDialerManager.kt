package com.balaji.callhistory.defaultdialer

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import com.balaji.callhistory.R

/**
 * Centralises all default-dialer logic and OEM-specific workarounds.
 *
 * OEM behaviour matrix:
 * ┌─────────────────────────────┬────────────────────────┬────────────────┐
 * │ OEM / ROM                   │ API works?             │ Strategy       │
 * ├─────────────────────────────┼────────────────────────┼────────────────┤
 * │ Stock Android / Pixel       │ ✅ Yes                 │ Dialog         │
 * │ Motorola / Nokia            │ ✅ Yes                 │ Dialog         │
 * │ Samsung One UI              │ ✅ Yes                 │ Dialog         │
 * │ OnePlus / ColorOS / Oppo    │ ✅ Yes                 │ Dialog         │
 * │ Xiaomi / MIUI / HyperOS     │ ⚠️ Partial/reverts    │ Manual guide   │
 * │ Huawei / EMUI (non-GMS)     │ ❌ Silent fail        │ Manual guide   │
 * │ Vivo / FuntouchOS           │ ⚠️ May revert         │ Manual guide   │
 * └─────────────────────────────┴────────────────────────┴────────────────┘
 */
object DefaultDialerManager {

    private const val TAG = "DefaultDialerManager"

    // ---------------------------------------------------------------------------
    // OEM detection
    // ---------------------------------------------------------------------------

    /** Manufacturer keywords that require manual settings navigation */
    private val MANUAL_REQUIRED_KEYWORDS = setOf(
        "xiaomi", "redmi", "poco",   // Xiaomi family (MIUI / HyperOS)
        "huawei", "honor",            // Huawei family (EMUI / Magic UI)
        "vivo"                        // Vivo (FuntouchOS)
    )

    private data class OemInfo(val nameResId: Int, val stepsResId: Int)

    /** OEM keyword → string resource IDs for display name and manual steps */
    private val OEM_INFO_MAP: Map<String, OemInfo> = mapOf(
        "xiaomi" to OemInfo(R.string.oem_name_xiaomi, R.string.oem_steps_xiaomi_redmi_poco),
        "redmi"  to OemInfo(R.string.oem_name_redmi,  R.string.oem_steps_xiaomi_redmi_poco),
        "poco"   to OemInfo(R.string.oem_name_poco,   R.string.oem_steps_xiaomi_redmi_poco),
        "huawei" to OemInfo(R.string.oem_name_huawei, R.string.oem_steps_huawei_honor),
        "honor"  to OemInfo(R.string.oem_name_honor,  R.string.oem_steps_huawei_honor),
        "vivo"   to OemInfo(R.string.oem_name_vivo,   R.string.oem_steps_vivo)
    )

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Returns the current [DefaultDialerState] for this device.
     * Call this on every ON_RESUME so UI reflects the latest state.
     */
    fun getState(context: Context): DefaultDialerState {
        if (isDefaultDialer(context)) return DefaultDialerState.IsDefault

        val manufacturer = Build.MANUFACTURER.lowercase()
        val matchedKey = MANUAL_REQUIRED_KEYWORDS.firstOrNull { manufacturer.contains(it) }

        return if (matchedKey != null) {
            val info = OEM_INFO_MAP[matchedKey]
            val oemName = if (info != null) context.getString(info.nameResId)
                          else context.getString(R.string.oem_name_generic, Build.MANUFACTURER)
            val steps = if (info != null) context.getString(info.stepsResId)
                        else context.getString(R.string.oem_steps_generic)
            DefaultDialerState.RequiresManualSetting(oemName, steps)
        } else {
            DefaultDialerState.CanRequestViaDialog
        }
    }

    /**
     * Returns true if this app is currently the default dialer.
     * Safe — never throws; returns false on any unexpected error.
     */
    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            } else {
                val telecomManager =
                    context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                telecomManager.defaultDialerPackage == context.packageName
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "isDefaultDialer SecurityException", e)
            false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "isDefaultDialer IllegalStateException", e)
            false
        }
    }

    /**
     * Builds the system intent to request the default dialer role.
     * Only meaningful when [getState] returns [DefaultDialerState.CanRequestViaDialog].
     * Returns null if intent creation fails (use [openDefaultAppsSettings] as fallback).
     */
    fun createRequestDefaultDialerIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(
                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                        context.packageName
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "createRequestDefaultDialerIntent SecurityException", e)
            null
        } catch (e: IllegalStateException) {
            Log.w(TAG, "createRequestDefaultDialerIntent IllegalStateException", e)
            null
        }
    }

    /**
     * Opens the system Default Apps settings page.
     * Primary action for [DefaultDialerState.RequiresManualSetting].
     * Falls back to app-specific settings if the screen is unavailable.
     */
    fun openDefaultAppsSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (e: android.content.ActivityNotFoundException) {
                Log.w(TAG, "openDefaultAppsSettings ActivityNotFoundException for ${intent.action}", e)
            }
        }
    }
}

