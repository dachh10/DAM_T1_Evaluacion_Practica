package com.example.dam_t1_evaluacion_practica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.dam_t1_evaluacion_practica.data.local.SampleData
import com.example.dam_t1_evaluacion_practica.domain.model.AuthSession
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.User
import com.example.dam_t1_evaluacion_practica.domain.repository.AuthRepository
import com.example.dam_t1_evaluacion_practica.ui.components.MainTab
import com.example.dam_t1_evaluacion_practica.ui.screens.ChecklistModuleScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.DashboardScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.HistorialScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.InspectionDetailScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.InspectionModule
import com.example.dam_t1_evaluacion_practica.ui.screens.LoginScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.ModulesSummaryScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.NewInspectionScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.ProfileScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.ResultsScreen
import com.example.dam_t1_evaluacion_practica.ui.screens.SplashScreen
import com.example.dam_t1_evaluacion_practica.ui.theme.DAM_T1_Evaluacion_PracticaTheme
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModelFactory
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object NewInspection : Screen()
    data object ModulesSummary : Screen()
    data class Checklist(val module: InspectionModule) : Screen()
    data object Results : Screen()
    data object Inspecciones : Screen()
    data class InspectionDetail(val record: InspectionSessionRecord) : Screen()
    data object Profile : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SecurityInspectionsApp

        setContent {
            DAM_T1_Evaluacion_PracticaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(app)
                }
            }
        }
    }
}

/**
 * Shows the splash art on the very first frame while the composition root is built on a
 * background dispatcher, then hands over to the app. The container is never built on the
 * main thread, so the first frame is never delayed by Room, Firebase or WorkManager init.
 */
@Composable
private fun AppRoot(app: SecurityInspectionsApp) {
    var container by remember { mutableStateOf<AppContainer?>(null) }
    var initialSession by remember { mutableStateOf<AuthSession?>(null) }
    var sessionBootstrapped by remember { mutableStateOf(false) }
    var minimumSplashElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container = withContext(Dispatchers.IO) { app.container }
    }
    LaunchedEffect(Unit) {
        delay(MINIMUM_SPLASH_MS)
        minimumSplashElapsed = true
    }
    // Resolve the restored session while the branded splash is still visible, so the app
    // lands directly on the right screen: the dashboard when the account is already
    // signed in, the login form only when there really is no session. Without this the
    // app composed Login first and jumped to the dashboard a frame later.
    LaunchedEffect(container) {
        val ready = container ?: return@LaunchedEffect
        initialSession = withTimeoutOrNull(SESSION_BOOTSTRAP_TIMEOUT_MS) {
            ready.auth.session.first()
        }
        sessionBootstrapped = true
    }

    val ready = container
    val bootstrapped = ready != null && minimumSplashElapsed && sessionBootstrapped
    Crossfade(
        targetState = bootstrapped,
        animationSpec = tween(durationMillis = 320),
        label = "AppBootstrap"
    ) { complete ->
        if (complete && ready != null) {
            InspectionApp(container = ready, initialSession = initialSession)
        } else {
            SplashScreen()
        }
    }
}

/** Minimum time the branded splash stays on screen so it does not flash on fast devices. */
private const val MINIMUM_SPLASH_MS = 900L

/**
 * Upper bound for the restored-session check. Firebase restores from its local cache and
 * emits immediately, so this only guards against a stalled provider trapping the splash.
 */
private const val SESSION_BOOTSTRAP_TIMEOUT_MS = 2_000L

@Composable
private fun InspectionApp(container: AppContainer, initialSession: AuthSession?) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel = remember(container) {
        val factory = InspectionViewModelFactory(container.inspection)
        ViewModelProvider(activity, factory)[InspectionViewModel::class.java]
    }
    val authRepository = container.auth
    InspectionApp(
        viewModel = viewModel,
        authRepository = authRepository,
        initialSession = initialSession
    )
}

@Composable
fun InspectionApp(
    viewModel: InspectionViewModel,
    authRepository: AuthRepository,
    initialSession: AuthSession? = null
) {
    // Start where the resolved session actually belongs, so a signed-in inspector never
    // sees the login screen flash on the way to the dashboard.
    var currentScreen by remember {
        mutableStateOf<Screen>(if (initialSession != null) Screen.Dashboard else Screen.Login)
    }
    var session by remember { mutableStateOf(initialSession) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authRepository) {
        authRepository.session.collect { active ->
            session = active
        }
    }

    // Auth-aware navigation (design section 9): a restored session reopens the
    // dashboard, and losing the session returns to the login screen.
    LaunchedEffect(session) {
        val active = session
        if (active == null) {
            // The provider session is gone: the local identity must go with it.
            if (viewModel.uiState.value.currentUser != null) viewModel.logout()
            if (currentScreen != Screen.Login) currentScreen = Screen.Login
            return@LaunchedEffect
        }
        // The profile comes from the local cache; an empty cache still opens the app.
        val profile = authRepository.cachedProfile(active.uid)
            ?: User(email = active.email, name = "", cedula = "", uid = active.uid)
        viewModel.restoreSession(profile)
        // A restored session goes straight to the dashboard.
        if (currentScreen == Screen.Login) {
            currentScreen = Screen.Dashboard
        }
    }

    when (val screen = currentScreen) {
        is Screen.Login -> {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
        is Screen.Dashboard -> {
            DashboardScreen(
                viewModel = viewModel,
                onStartInspection = { module ->
                    viewModel.startNewSession()
                    viewModel.initChecklist(itemsFor(module))
                    currentScreen = Screen.Checklist(module)
                },
                onNavigateToNewInspection = {
                    currentScreen = Screen.NewInspection
                },
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.INSPECCION -> Unit
                        MainTab.INSPECCIONES -> currentScreen = Screen.Inspecciones
                        MainTab.PERFIL -> currentScreen = Screen.Profile
                    }
                },
                onOpenHistorial = {
                    currentScreen = Screen.Inspecciones
                }
            )
        }
        is Screen.NewInspection -> {
            val currentUser = viewModel.uiState.value.currentUser
            NewInspectionScreen(
                onBack = {
                    currentScreen = Screen.Dashboard
                },
                onProceedToInspection = { companyName, zoneName, auditType ->
                    viewModel.setSessionContext(companyName, zoneName, auditType.label)
                    viewModel.startNewSession()
                    currentScreen = Screen.ModulesSummary
                },
                inspectorName = InspectorProfile.nameOf(currentUser),
                inspectorCedula = InspectorProfile.cedulaOf(currentUser),
                inspectorInitials = InspectorProfile.initialsOf(currentUser)
            )
        }
        is Screen.ModulesSummary -> {
            ModulesSummaryScreen(
                viewModel = viewModel,
                onBack = {
                    // A reinspection has no form to return to: backing out cancels it.
                    currentScreen = if (viewModel.uiState.value.parentInspectionId != null) {
                        Screen.Dashboard
                    } else {
                        Screen.NewInspection
                    }
                },
                onSelectModule = { module ->
                    viewModel.openModule(module.title, itemsFor(module))
                    currentScreen = Screen.Checklist(module)
                },
                onFinishInspection = {
                    currentScreen = Screen.Results
                }
            )
        }
        is Screen.Checklist -> {
            ChecklistModuleScreen(
                module = screen.module,
                viewModel = viewModel,
                onBack = {
                    viewModel.snapshotActiveModule(
                        moduleTitle = screen.module.title,
                        items = viewModel.uiState.value.currentChecklist,
                        completed = false
                    )
                    currentScreen = Screen.ModulesSummary
                },
                onFinishModule = {
                    // finishModule() is invoked by the screen itself: it must evaluate the
                    // module score and, when below 85, keep the user on this screen to
                    // capture photo + GPS evidence before returning to the module list.
                    // Results is only reachable via "Terminar Inspección" on
                    // ModulesSummary once ALL three modules are completed.
                    currentScreen = Screen.ModulesSummary
                }
            )
        }
        is Screen.Results -> {
            ResultsScreen(
                viewModel = viewModel,
                onSaveDictamen = {
                    val saved = viewModel.saveDictamen()
                    // Resolve the freshest snapshot from the VM so the detail screen
                    // renders the record exactly as persisted in the history list.
                    currentScreen = Screen.InspectionDetail(
                        viewModel.uiState.value.savedInspections.find { it.folio == saved.folio } ?: saved
                    )
                },
                onViewHistory = {
                    currentScreen = Screen.Inspecciones
                },
                onBack = {
                    currentScreen = Screen.ModulesSummary
                }
            )
        }
        is Screen.Inspecciones -> {
            HistorialScreen(
                viewModel = viewModel,
                onSelectRecord = { record ->
                    // Resolve the freshest snapshot at navigation time so the detail
                    // always shows the latest status after a transition on the list.
                    currentScreen = Screen.InspectionDetail(
                        viewModel.uiState.value.savedInspections.find { it.folio == record.folio } ?: record
                    )
                },
                onNewInspection = { currentScreen = Screen.NewInspection },
                selectedTab = MainTab.INSPECCIONES,
                title = "Inspecciones",
                subtitle = "Auditorías guardadas en el dispositivo",
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.INSPECCION -> currentScreen = Screen.Dashboard
                        MainTab.INSPECCIONES -> Unit
                        MainTab.PERFIL -> currentScreen = Screen.Profile
                    }
                }
            )
        }
        is Screen.InspectionDetail -> {
            // Collect the state while this branch is active: after a status transition
            // on the record itself, the chip must update live without re-navigating.
            val recordState by viewModel.uiState.collectAsState()
            val freshRecord = recordState.savedInspections.find { it.folio == screen.record.folio } ?: screen.record
            InspectionDetailScreen(
                record = freshRecord,
                onBack = {
                    currentScreen = Screen.Inspecciones
                },
                onReinspect = {
                    // Re-evaluates the same company/zone in a fresh, linked session.
                    viewModel.startReinspection(freshRecord)
                    currentScreen = Screen.ModulesSummary
                },
                viewModel = viewModel
            )
        }
        is Screen.Profile -> {
            ProfileScreen(
                viewModel = viewModel,
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.INSPECCION -> currentScreen = Screen.Dashboard
                        MainTab.INSPECCIONES -> currentScreen = Screen.Inspecciones
                        MainTab.PERFIL -> Unit
                    }
                },
                onLogout = {
                    viewModel.logout {
                        currentScreen = Screen.Login
                    }
                    scope.launch { authRepository.signOut() }
                }
            )
        }
    }
}

private fun itemsFor(module: InspectionModule): List<ChecklistItem> = when (module) {
    InspectionModule.EPP -> SampleData.eppItems
    InspectionModule.MACHINERY -> SampleData.machineryItems
    InspectionModule.FACILITIES -> SampleData.environmentItems
}

