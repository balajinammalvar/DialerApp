package com.balaji.callhistory

import android.app.Application
import com.balaji.callhistory.repo.AppRepositoryProvider
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.utils.DarkModeState
import com.balaji.callhistory.utils.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallHistoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepositoryProvider.repository = CallHistoryRepository(this)
        // Load theme early in application startup
        CoroutineScope(Dispatchers.IO).launch {
            val themeMode = ThemePreferences.getThemeMode(this@CallHistoryApplication).first()
            
            val darkModeState = DarkModeState.getInstance()
            darkModeState.state.value = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> false // Default to light for system until MainActivity resolves it
            }
        }
    }
}
