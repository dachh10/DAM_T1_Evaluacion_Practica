package com.example.dam_t1_evaluacion_practica.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.R
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import com.example.dam_t1_evaluacion_practica.ui.components.LocationCapture
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeBadgesRow
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeDetailBottomSheet
import com.example.dam_t1_evaluacion_practica.ui.components.rememberLocationCapture
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile
import com.example.dam_t1_evaluacion_practica.util.GalleryUtils

private val NavyPrimary = Color(0xFF0B192C)
private val AmberSecondary = Color(0xFFF59E0B)
private val AmberContainer = Color(0xFFFEA619)
private val OnAmberContainer = Color(0xFF684000)
private val SurfaceBg = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHigh = Color(0xFFE2E7FF)
private val SurfaceHighest = Color(0xFFDAE2FD)
private val SurfaceTint = Color(0xFF525F75)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val OnPrimary = Color(0xFFFFFFFF)
private val ErrorColor = Color(0xFFDC2626)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val SuccessColor = Color(0xFF16A34A)
private val SuccessContainer = Color(0xFFDCFCE7)
private val OnSuccessContainer = Color(0xFF166534)

private enum class AnswerSelection { CONFORME, NO_CONFORME, NA, NONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistModuleScreen(
    module: InspectionModule,
    viewModel: InspectionViewModel,
    onBack: () -> Unit = {},
    onFinishModule: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCriticalDialog by remember { mutableStateOf(false) }
    var dialogIsLive by remember { mutableStateOf(false) }
    var selectedNormativ by remember { mutableStateOf<Normativ?>(null) }
    val captureLocation = rememberLocationCapture()


    if (showCriticalDialog) {
        val moduleItemIds = remember(uiState.currentChecklist) {
            uiState.currentChecklist.map { it.id }.toSet()
        }
        val moduleFindings = uiState.sessionFindings.filter { it.itemId in moduleItemIds && !it.isNA }
        CriticalStopDialog(
            score = uiState.overallComplianceScore,
            findings = moduleFindings,
            onAttachEvidence = { itemId, photoUri, lat, lng, notes ->
                viewModel.attachFindingEvidence(itemId, photoUri, lat, lng, notes)
            },
            liveStop = dialogIsLive,
            onContinue = {
                showCriticalDialog = false
                if (!dialogIsLive) onFinishModule()
            }
        )
    }

    selectedNormativ?.let { norm ->
        NormativeDetailBottomSheet(
            normativ = norm,
            onDismissRequest = { selectedNormativ = null }
        )
    }

    val normCode = uiState.currentChecklist.firstOrNull()?.normativ?.code ?: "Sin NOM asignada"
    val answeredCount = uiState.answers.size
    val totalCount = uiState.currentChecklist.size

    // Item ids of HIGH findings that still demand mandatory evidence (photo OR note):
    // those rows keep "Conforme"/"N/A" locked so the finding cannot be erased while
    // the evidence gate is open.
    val pendingCriticalItemIds = uiState.sessionFindings
        .filter {
            it.criticalityLevel == CriticalityLevel.HIGH &&
                !it.isNA &&
                (it.photoUri.isNullOrBlank() || it.notes.isNullOrBlank())
        }
        .map { it.itemId }
        .toSet()

    // Item ids of EVERY HIGH non-compliant finding (evidence attached or not): those rows
    // lock "Conforme"/"N/A" FOREVER so a documented critical finding can never be flipped
    // back to compliant (or N/A) after its evidence was captured.
    val lockedHighItemIds = uiState.criticalNonCompliantItemIds
    val findingsByItem = uiState.sessionFindings.associateBy { it.itemId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChecklistModuleTopBar(
                onBack = onBack,
                normCode = normCode,
                auditType = uiState.auditType,
                zoneName = uiState.zoneName
            )

            // Score card del slice 1 (state derivado del ViewModel)
            InspectionScoreCard(
                score = uiState.overallComplianceScore,
                criticalStop = uiState.criticalStopTriggered,
                reinspectionDue = uiState.reinspectionDue
            )

            ModuleContextCard(
                companyName = uiState.companyName,
                zoneName = uiState.zoneName,
                auditType = uiState.auditType,
                normCode = normCode,
                answeredCount = answeredCount,
                totalCount = totalCount
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                itemsIndexed(uiState.currentChecklist, key = { _, item -> item.id }) { index, item ->
                    ChecklistItemModuleRow(
                        index = index,
                        item = item,
                        selection = when {
                            uiState.naAnswers.contains(item.id) -> AnswerSelection.NA
                            uiState.answers[item.id] == true -> AnswerSelection.CONFORME
                            uiState.answers[item.id] == false -> AnswerSelection.NO_CONFORME
                            else -> AnswerSelection.NONE
                        },
                        captureLocation = captureLocation,
                        evidenceLocked = item.id in pendingCriticalItemIds,
                        answerLocked = item.id in lockedHighItemIds,
                        finding = findingsByItem[item.id],
                        onNormClick = { selectedNormativ = it },
                        onSelectConforme = {
                            viewModel.markNA(item.id, false)
                            viewModel.submitAnswer(item, isCompliant = true, photoUri = null, notes = null)
                        },
                        onSelectNoConf = { photoUri, notes, lat, lng ->
                            viewModel.markNA(item.id, false)
                            viewModel.submitAnswer(item, isCompliant = false, photoUri = photoUri, notes = notes, lat = lat, lng = lng)
                            // Live Critical Stop: a HIGH non-compliant finding without
                            // photo/note opens the evidence dialog immediately (same
                            // mechanism as the finish path). The dialog closes onto the
                            // module so the inspector can keep answering the checklist.
                            val afterSubmit = viewModel.uiState.value
                            if (afterSubmit.criticalStopTriggered && afterSubmit.criticalEvidencePending) {
                                dialogIsLive = true
                                showCriticalDialog = true
                            }
                        },
                        onSelectNA = {
                            viewModel.setAnswerNA(item)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Botón fijo inferior
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceBg.copy(alpha = 0.97f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MANDATORY evidence gate: with a critical finding missing photo/note,
                    // finalizing is disabled until the evidence is attached.
                    AnimatedVisibility(visible = uiState.criticalEvidencePending) {
                        Text(
                            text = "Evidencia requerida: adjunta foto y nota para los hallazgos críticos antes de finalizar.",
                            color = ErrorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Regresar", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {

                                viewModel.finishModule(module.title, uiState.currentChecklist)
                                if (viewModel.uiState.value.criticalStopTriggered) {
                                    dialogIsLive = false
                                    showCriticalDialog = true
                                } else {
                                    onFinishModule()
                                }
                            },
                            enabled = !uiState.criticalEvidencePending,
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = OnPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar Módulo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistModuleTopBar(
    onBack: () -> Unit,
    normCode: String,
    auditType: String,
    zoneName: String
) {
    Surface(
        color = SurfaceBg.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = OnSurface
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.logo1),
                        contentDescription = "Logo",
                        modifier = Modifier.height(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Checklist de Verificación",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$auditType • $normCode",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = zoneName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SurfaceTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleContextCard(
    companyName: String,
    zoneName: String,
    auditType: String,
    normCode: String,
    answeredCount: Int,
    totalCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = AmberSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$companyName • $zoneName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$auditType • $normCode",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = SurfaceTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Modo Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SurfaceTint
                    )
                }
            }

            // Progreso del módulo activo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avance del módulo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Text(
                    text = "$answeredCount de $totalCount verificaciones",
                    fontSize = 12.sp,
                    color = OnSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { if (totalCount == 0) 0f else answeredCount.toFloat() / totalCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = AmberContainer,
                trackColor = SurfaceHighest,
                strokeCap = StrokeCap.Round
            )

            // Auditor asignado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = InspectorProfile.INITIALS,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                }
                Text(
                    text = "Auditor: ${InspectorProfile.NAME}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChecklistItemModuleRow(
    index: Int,
    item: ChecklistItem,
    selection: AnswerSelection,
    captureLocation: LocationCapture,
    evidenceLocked: Boolean = false,
    answerLocked: Boolean = false,
    finding: SessionFinding? = null,
    onNormClick: (Normativ) -> Unit = {},
    onSelectConforme: () -> Unit,
    onSelectNoConf: (photoUri: String?, notes: String, lat: Double?, lng: Double?) -> Unit,
    onSelectNA: () -> Unit
) {
    val context = LocalContext.current
    val findingNotes = finding?.notes?.takeIf { !it.startsWith("No aplica") } ?: ""
    var notes by remember(item.id, finding?.notes) { mutableStateOf(findingNotes) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedUriString by remember { mutableStateOf<String?>(null) }
    var rowLat by remember { mutableStateOf<Double?>(null) }
    var rowLng by remember { mutableStateOf<Double?>(null) }

    // Fall back to the evidence attached through the Critical Stop dialog: the finding's
    // photoUri is loaded once per uri so the row reflects what the dialog saved.
    val findingBitmap = remember(finding?.photoUri) {
        finding?.photoUri?.let { GalleryUtils.loadBitmapFromUri(context, Uri.parse(it)) }
    }
    val displayedBitmap = capturedBitmap ?: findingBitmap

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val uri = GalleryUtils.saveBitmapToGallery(context, bitmap, "incidencia_${item.id}")
            savedUriString = uri?.toString()
            // Capture the device's real position at the moment the photo of the
            // non-conformance is taken, then submit the answer with those coordinates.
            captureLocation { lat, lng ->
                if (lat != null && lng != null) {
                    rowLat = lat
                    rowLng = lng
                }
                onSelectNoConf(savedUriString, notes, rowLat, rowLng)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PUNTO ${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceTint,
                        letterSpacing = 0.5.sp
                    )
                    RiskBadge(level = item.criticalityLevel)
                }
                if (item.requiresPhotoEvidence && selection != AnswerSelection.NA) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Requiere Evidencia",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ErrorColor
                        )
                    }
                } else if (item.requiresPhotoEvidence) {
                    Text(
                        text = "Evidencia no requerida (N/A)",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
            }

            Text(
                text = item.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )

            // Ficha normativa interactiva vinculada al ítem
            NormativeBadgesRow(
                primaryNorm = item.normativ,
                secondaryNorms = item.secondaryNorms,
                onNormClick = onNormClick
            )

            // Tri-selector: Conforme / No Conf. / N/A
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriSelectButton(
                    label = "Conforme",
                    selected = selection == AnswerSelection.CONFORME,
                    selectedBg = SuccessColor,
                    selectedContent = Color.White,
                    unselectedContent = OnSurface,
                    enabled = !answerLocked,
                    modifier = Modifier.weight(1f),
                    // Defensive: once a HIGH finding exists it can never be flipped back
                    // to compliant, even if the row is reused after a snapshot restore.
                    onClick = { if (!answerLocked) onSelectConforme() }
                )
                TriSelectButton(
                    label = "No Conf.",
                    selected = selection == AnswerSelection.NO_CONFORME,
                    selectedBg = ErrorColor,
                    selectedContent = Color.White,
                    unselectedContent = OnSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectNoConf(savedUriString, notes, rowLat, rowLng) }
                )
                TriSelectButton(
                    label = "N/A",
                    selected = selection == AnswerSelection.NA,
                    selectedBg = AmberSecondary,
                    selectedContent = OnAmberContainer,
                    unselectedContent = OnSurface,
                    enabled = !answerLocked,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectNA
                )
            }

            // Zona de evidencia
            AnimatedVisibility(visible = selection == AnswerSelection.NO_CONFORME) {
                Column(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            onSelectNoConf(savedUriString, notes, rowLat, rowLng)
                        },
                        label = { Text("Observaciones / Hallazgo") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = OnPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (displayedBitmap != null) "Cambiar Foto" else "Tomar Foto", fontSize = 13.sp)
                        }

                        if (displayedBitmap != null) {
                            Image(
                                bitmap = displayedBitmap!!.asImageBitmap(),
                                contentDescription = "Evidencia Fotográfica",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Guardada en Galería",
                                fontSize = 11.sp,
                                color = SuccessColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Warning de evidencia obligatoria: hallazgo de Riesgo Alto en NO_CONFORME
            // cuya evidencia (fotografía o nota) aún no está registrada.
            AnimatedVisibility(
                visible = selection == AnswerSelection.NO_CONFORME && evidenceLocked
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ErrorContainer)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = ErrorColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Evidencia obligatoria para este hallazgo (Riesgo Alto)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorColor
                    )
                }
            }

            AnimatedVisibility(visible = selection == AnswerSelection.NA) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timelapse,
                        contentDescription = null,
                        tint = SurfaceTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Registrado como N/A (no conforme). No se requiere evidencia.",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TriSelectButton(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedContent: Color,
    unselectedContent: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = when {
            selected -> selectedBg
            !enabled -> SurfaceLow.copy(alpha = 0.5f)
            else -> SurfaceLow
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    selected -> selectedContent
                    !enabled -> unselectedContent.copy(alpha = 0.5f)
                    else -> unselectedContent
                }
            )
        }
    }
}

@Composable
private fun RiskBadge(level: CriticalityLevel) {
    val (bgColor, textColor, label) = when (level) {
        CriticalityLevel.HIGH -> Triple(ErrorContainer, OnErrorContainer, "RIESGO ALTO")
        CriticalityLevel.MEDIUM -> Triple(Color(0xFFFFECB3), Color(0xFFE65100), "RIESGO MEDIO")
        CriticalityLevel.LOW -> Triple(SuccessContainer, OnSuccessContainer, "RIESGO BAJO")
    }

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}