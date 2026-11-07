package com.example.dam_t1_evaluacion_practica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.dam_t1_evaluacion_practica.data.local.SampleData
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
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

sealed class Screen {
    data object Splash : Screen()
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

    private lateinit var viewModel: InspectionViewModel
    private lateinit var repository: InspectionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SecurityInspectionsApp).container
        repository = container.inspection
        val factory = InspectionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]

        setContent {
            DAM_T1_Evaluacion_PracticaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InspectionApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun InspectionApp(viewModel: InspectionViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

    when (val screen = currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    currentScreen = Screen.Login
                }
            )
        }
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
            NewInspectionScreen(
                onBack = {
                    currentScreen = Screen.Dashboard
                },
                onProceedToInspection = { companyName, zoneName, auditType ->
                    viewModel.setSessionContext(companyName, zoneName, auditType.label)
                    viewModel.startNewSession()
                    currentScreen = Screen.ModulesSummary
                }
            )
        }
        is Screen.ModulesSummary -> {
            ModulesSummaryScreen(
                viewModel = viewModel,
                onBack = {
                    currentScreen = Screen.NewInspection
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
                onUpdateRecordStatus = viewModel::updateRecordStatus,
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
                onUpdateStatus = { status -> viewModel.updateRecordStatus(freshRecord.folio, status) }
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