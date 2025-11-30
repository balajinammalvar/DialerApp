package com.balaji.callhistory.utils

import kotlinx.coroutines.flow.MutableStateFlow

class DarkModeState {
    val state = MutableStateFlow(false)

    val isDarkMode
        get() = state.value
    
    companion object {
        @Volatile
        private var INSTANCE: DarkModeState? = null
        
        fun getInstance(): DarkModeState {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DarkModeState().also { INSTANCE = it }
            }
        }
    }
}
