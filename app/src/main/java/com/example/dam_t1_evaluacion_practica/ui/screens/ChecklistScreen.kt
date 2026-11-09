package com.example.dam_t1_evaluacion_practica.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeBadgesRow
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeDetailBottomSheet
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.util.GalleryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    title: String,
    viewModel: InspectionViewModel,
    onFinishInspection: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCriticalDialog by remember { mutableStateOf(false) }
    var selectedNormativ by remember { mutableStateOf<Normativ?>(null) }

    // Dialog trigger while the critical stop is still unacknowledged.
    if (uiState.answersLocked && !showCriticalDialog) {
        showCriticalDialog = true
    }

    if (showCriticalDialog) {
        CriticalStopDialog(
            score = uiState.overallComplianceScore,
            findings = uiState.sessionFindings.filter { !it.isNA },
            onAttachEvidence = { itemId, photoUri, lat, lng, notes ->
                viewModel.attachFindingEvidence(itemId, photoUri, lat, lng, notes)
            },
            liveStop = uiState.criticalEvidencePending,
            onAcknowledge = { viewModel.acknowledgeCriticalStop() },
            onContinue = { showCriticalDialog = false }
        )
    }

    selectedNormativ?.let { norm ->
        NormativeDetailBottomSheet(
            normativ = norm,
            onDismissRequest = { selectedNormativ = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (uiState.currentChecklist.isNotEmpty()) uiState.currentChecklist.first().normativ.code else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Score & Alert Banner
            InspectionScoreCard(
                score = uiState.overallComplianceScore,
                criticalStop = uiState.criticalStopTriggered,
                reinspectionDue = uiState.reinspectionDue
            )

            // Items list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(uiState.currentChecklist, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        currentAnswer = uiState.answers[item.id],
                        onNormClick = { selectedNormativ = it },
                        onAnswerChanged = { compliant, photoUri, notes ->
                            viewModel.submitAnswer(
                                item = item,
                                isCompliant = compliant,
                                photoUri = photoUri,
                                notes = notes
                            )
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Bottom action buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Regresar")
                    }

                    Button(
                        onClick = onFinishInspection,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.criticalStopTriggered) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (uiState.criticalStopTriggered) "Ver Incidencias" else "Finalizar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionScoreCard(score: Int, criticalStop: Boolean, reinspectionDue: Boolean) {
    val statusColor = when {
        criticalStop -> Color(0xFFD32F2F)
        reinspectionDue -> Color(0xFFFFA000)
        else -> Color(0xFF2E7D32)
    }

    val statusText = when {
        criticalStop -> "ESTADO: PARO CRÍTICO OBLIGATORIO"
        reinspectionDue -> "ESTADO: ALERTA DE PONDERACIÓN"
        else -> "ESTADO: CUMPLIMIENTO REGULAR"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "$score%",
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Color.LightGray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    currentAnswer: Boolean?,
    onNormClick: (Normativ) -> Unit = {},
    onAnswerChanged: (isCompliant: Boolean, photoUri: String?, notes: String?) -> Unit
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedUriString by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val uri = GalleryUtils.saveBitmapToGallery(context, bitmap, "incidencia_${item.id}")
            savedUriString = uri?.toString()
            onAnswerChanged(false, savedUriString, notes)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CriticalityBadge(level = item.criticalityLevel)

                if (item.requiresPhotoEvidence) {
                    Text(
                        text = "Requiere Evidencia",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            NormativeBadgesRow(
                primaryNorm = item.normativ,
                secondaryNorms = item.secondaryNorms,
                onNormClick = onNormClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Compliance Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button CUMPLE
                Button(
                    onClick = { onAnswerChanged(true, null, null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentAnswer == true) Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.4f),
                        contentColor = if (currentAnswer == true) Color.White else Color.Black
                    )
                ) {
                    Text("CUMPLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Button NO CUMPLE
                Button(
                    onClick = { onAnswerChanged(false, savedUriString, notes) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentAnswer == false) Color(0xFFD32F2F) else Color.LightGray.copy(alpha = 0.4f),
                        contentColor = if (currentAnswer == false) Color.White else Color.Black
                    )
                ) {
                    Text("NO CUMPLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Expanded Incident details on non-compliance
            AnimatedVisibility(visible = currentAnswer == false) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            onAnswerChanged(false, savedUriString, notes)
                        },
                        label = { Text("Observaciones / Hallazgo") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text(if (capturedBitmap != null) "Cambiar Foto" else "Tomar Foto")
                        }

                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Evidencia Fotográfica",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Guardada en Galería",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CriticalityBadge(level: CriticalityLevel) {
    val (bgColor, textColor, label) = when (level) {
        CriticalityLevel.HIGH -> Triple(Color(0xFFFFCDD2), Color(0xFFB71C1C), "CRITICIDAD ALTA (5 pts)")
        CriticalityLevel.MEDIUM -> Triple(Color(0xFFFFECB3), Color(0xFFE65100), "CRITICIDAD MEDIA (3 pts)")
        CriticalityLevel.LOW -> Triple(Color(0xFFC8E6C9), Color(0xFF1B5E20), "CRITICIDAD BAJA (1 pto)")
    }

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
