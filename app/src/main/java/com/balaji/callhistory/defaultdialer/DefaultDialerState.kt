package com.balaji.callhistory.defaultdialer

/**
 * Represents the current state of the default dialer setup step.
 */
sealed class DefaultDialerState {

    /** App is already the default dialer — gate opens */
    data object IsDefault : DefaultDialerState()

    /**
     * Standard Android / Samsung / OnePlus / Oppo / Motorola / Nokia.
     * The system role/dialog API works reliably — show a button to launch it.
     */
    data object CanRequestViaDialog : DefaultDialerState()

    /**
     * OEM that blocks or ignores the system API (Xiaomi/MIUI, Huawei/EMUI, Vivo/FuntouchOS).
     * Must guide the user to navigate Settings manually.
     *
     * @param oemName  Human-readable OEM brand shown in the UI (e.g. "Xiaomi / MIUI")
     * @param steps    Step-by-step navigation path for this OEM
     */
    data class RequiresManualSetting(
        val oemName: String,
        val steps: String
    ) : DefaultDialerState()
}

