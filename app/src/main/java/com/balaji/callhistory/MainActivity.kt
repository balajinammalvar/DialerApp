package com.balaji.callhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.balaji.callhistory.analytics.AnalyticsManager
import com.balaji.callhistory.navigation.CallHistoryDetailsRoute
import com.balaji.callhistory.navigation.ContactDetailsRoute
import com.balaji.callhistory.navigation.ContactsRoute
import com.balaji.callhistory.navigation.DialerRoute
import com.balaji.callhistory.navigation.HistoryRoute
import com.balaji.callhistory.navigation.ThemeRoute
import com.balaji.callhistory.repo.AppRepositoryProvider
import com.balaji.callhistory.repo.CallHistoryRepository
import com.balaji.callhistory.screens.CallHistoryDetailsScreen
import com.balaji.callhistory.screens.CallHistoryScreen
import com.balaji.callhistory.screens.ContactDetailsScreen
import com.balaji.callhistory.screens.ContactScreen
import com.balaji.callhistory.screens.DialerScreen
import com.balaji.callhistory.screens.ThemeScreen
import com.balaji.callhistory.ui.theme.AppTheme
import com.balaji.callhistory.utils.DarkModeState
import com.balaji.callhistory.utils.PermissionManager
import com.balaji.callhistory.viewmodel.CallHistoryDetailsViewModel
import com.balaji.callhistory.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    companion object {
        // keep for backward compatibility (optional)
        var repository: CallHistoryRepository? = null
    }
    
    private var pendingPhoneNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var hasPermissions by mutableStateOf(false)
        var isCallHistoryLoaded by mutableStateOf(false)
        pendingPhoneNumber = intent?.data?.schemeSpecificPart

        // Check permissions early
        hasPermissions = PermissionManager.hasAllPermissions(applicationContext)

        // Keep splash screen until call history is ready or no permissions
        AnalyticsManager.logAnalyticEvent(
            this,
            AnalyticsManager.TrackingEvent.ENTERED_SPLASH_SCREEN
        )
        splashScreen.setKeepOnScreenCondition {
            hasPermissions && !isCallHistoryLoaded
        }

        // Create repo with context for call log access, expose via provider for BroadcastReceiver
        val repo = CallHistoryRepository(applicationContext)
        repository = repo
        AppRepositoryProvider.repository = repo

        // ViewModel factories
        val dialerFactory = viewModelFactory {
            initializer { DialerViewModel(applicationContext, repo) }
        }

        setContent {
            val darkModeState = DarkModeState.getInstance()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            
            LaunchedEffect(isSystemInDarkTheme) {
                darkModeState.state.value = isSystemInDarkTheme
            }
            
            val currentTheme by darkModeState.state.collectAsState()

            AppTheme(darkTheme = currentTheme) {
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !currentTheme
                }
                val nav = rememberNavController()
                val navBackStackEntry by nav.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // instantiate DialerViewModel (used across multiple screens in your app)
                val dialerViewModel = viewModel<DialerViewModel>(factory = dialerFactory)

                // Set phone number from intent if available
                LaunchedEffect(pendingPhoneNumber) {
                    pendingPhoneNumber?.let { 
                        dialerViewModel.clearNumber()
                        dialerViewModel.setNumber(it)
                    }
                }

                // Check if call history is loaded when permissions are available (original logic)
                if (hasPermissions) {
                    // This just triggers collection to ensure any initial load occurs (keeps splash until done)
                    val callHistory by dialerViewModel.callHistoryForSuggestions.collectAsState(initial = emptyList())
                    isCallHistoryLoaded = true
                } else {
                    isCallHistoryLoaded = true
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomAppBar(currentRoute, nav)
                    }
                ) { paddingValues ->
                    BackHandler(
                        enabled = currentRoute?.let { route ->
                            route.contains("HistoryRoute") ||
                                    route.contains("ContactsRoute") ||
                                    route.contains("DialerRoute")
                        } == true
                    ) {
                        finish()
                    }
                    
                    // Navigate to dialer if phone number from intent
                    LaunchedEffect(pendingPhoneNumber) {
                        if (pendingPhoneNumber != null) {
                            nav.navigate(DialerRoute) {
                                popUpTo(nav.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                    
                    NavHostBuilder(nav, paddingValues, dialerViewModel)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPhoneNumber = intent.data?.schemeSpecificPart
    }
}

@Composable
private fun BottomAppBar(currentRoute: String?, nav: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "History"
                )
            },
            label = { Text("Home") },
            selected = currentRoute?.contains("HistoryRoute") == true,
            onClick = {
                nav.navigate(HistoryRoute) {
                    popUpTo<HistoryRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Dialpad,
                    contentDescription = "Dialer"
                )
            },
            label = { Text("Dialer") },
            selected = currentRoute?.contains("DialerRoute") == true,
            onClick = {
                nav.navigate(DialerRoute) {
                    popUpTo(nav.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Contacts,
                    contentDescription = "Contacts"
                )
            },
            label = { Text("Contacts") },
            selected = currentRoute?.contains("ContactsRoute") == true,
            onClick = {
                nav.navigate(ContactsRoute) {
                    popUpTo(nav.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun NavHostBuilder(
    nav: NavHostController,
    paddingValues: PaddingValues,
    dialerViewModel: DialerViewModel,
) {
    NavHost(
        navController = nav,
        startDestination = HistoryRoute,
        modifier = Modifier.padding(paddingValues)
    ) {
        // History screen
        composable<HistoryRoute> {
            CallHistoryScreen(
                onNavigateToTheme = { nav.navigate(ThemeRoute) },
                onNavigateToDetails = { phoneNumber ->
                    nav.navigate(CallHistoryDetailsRoute(phoneNumber))
                }
            )
        }

        // Call History Details
        composable<CallHistoryDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CallHistoryDetailsRoute>()
            val context = LocalContext.current
            val detailsViewModel = viewModel<CallHistoryDetailsViewModel>(
                factory = viewModelFactory {
                    initializer { CallHistoryDetailsViewModel(context) }
                }
            )
            CallHistoryDetailsScreen(
                phoneNumber = route.phoneNumber,
                viewModel = detailsViewModel,
                onBackClick = { nav.popBackStack() }
            )
        }

        // Theme
        composable<ThemeRoute> {
            ThemeScreen(onBackClick = { nav.popBackStack() })
        }

        // Contacts list
        composable<ContactsRoute> {
            ContactScreen(onContactClick = { contact ->
                nav.navigate(ContactDetailsRoute(contact.id))
            })
        }

        // Contact details
        composable<ContactDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactDetailsRoute>()
            ContactDetailsScreen(
                contactId = route.contactId,
                onBackClick = { nav.popBackStack() }
            )
        }

        // Dialer
        composable<DialerRoute> {
            DialerScreen(viewModel = dialerViewModel)
        }
    }
}
