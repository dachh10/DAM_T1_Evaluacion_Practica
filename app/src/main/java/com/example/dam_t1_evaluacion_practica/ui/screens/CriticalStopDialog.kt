package com.example.dam_t1_evaluacion_practica.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import com.example.dam_t1_evaluacion_practica.ui.components.rememberLocationCapture
import com.example.dam_t1_evaluacion_practica.util.GalleryUtils
import java.util.Locale

private val ErrorColor = Color(0xFFDC2626)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val ErrorDark = Color(0xFFB71C1C)
private val WarningContainer = Color(0xFFFFECB3)
private val OnWarningContainer = Color(0xFFE65100)
private val SuccessContainer = Color(0xFFDCFCE7)
private val OnSuccessContainer = Color(0xFF166534)
private val NavyPrimary = Color(0xFF0B192C)
private val OnPrimary = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val SurfaceHigh = Color(0xFFE2E7FF)

/**
 * Critical Stop alert shown when a finished module scores below 85, or LIVE as soon as
 * a HIGH item is marked non-compliant without evidence yet. Lists the
 * module's non-conformant findings, lets the inspector take a photo of each finding
 * without evidence (saved to the gallery, same naming as the item flow) and capture the
 * device's real GPS position at that moment. The footer reports the location status and
 * the main button closes the dialog and continues the normal flow.
 */
@Composable
fun CriticalStopDialog(
    score: Int,
    findings: List<SessionFinding>,
    onAttachEvidence: (itemId: String, photoUri: String?, lat: Double?, lng: Double?, notes: String) -> Unit,
    onContinue: () -> Unit,
    liveStop: Boolean = false,
    onAcknowledge: () -> Unit = {}
) {
    val context = LocalContext.current
    val captureLocation = rememberLocationCapture()
    var capturedUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var photoTargetItemId by remember { mutableStateOf<String?>(null) }
    var lastLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val notesByItem = remember { mutableStateMapOf<String, String>() }
    var acknowledged by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val itemId = photoTargetItemId
        photoTargetItemId = null
        if (bitmap != null && itemId != null) {
            val uri = GalleryUtils.saveBitmapToGallery(context, bitmap, "incidencia_${itemId}")
            val uriString = uri?.toString()
            captureLocation { lat, lng ->
                if (lat != null && lng != null) {
                    lastLocation = lat to lng
                }
                if (uriString != null) {
                    capturedUris = capturedUris + (itemId to uriString)
                }
                val currentNote = (notesByItem[itemId] ?: findings.firstOrNull { it.itemId == itemId }?.notes).orEmpty()
                onAttachEvidence(itemId, uriString, lat, lng, currentNote)
            }
        }
    }

    // No evidence can be attached when the findings list is empty (e.g. a score-based
    // finish stop with only N/A answers, which never require evidence): the dialog must
    // never trap the inspector, so continuation is always allowed in that case.
    val evidenceComplete = findings.isEmpty() || findings.all { finding ->
        val needsPhoto = finding.requiresPhotoEvidence
        val needsNote = finding.criticalityLevel == CriticalityLevel.HIGH
        val hasPhoto = !needsPhoto || capturedUris[finding.itemId] != null || !finding.photoUri.isNullOrBlank()
        val hasNote = !needsNote || notesByItem[finding.itemId]?.isNotBlank() == true || !finding.notes.isNullOrBlank()
        hasPhoto && hasNote
    }
    // Detener labores is an explicit act: the inspector must confirm it, not just close.
    val canSave = evidenceComplete && acknowledged

    val locationStatus = lastLocation?.let { (lat, lng) ->
        "Ubicación real: ${String.format(Locale.US, "%.5f", lat)}, ${String.format(Locale.US, "%.5f", lng)}"
    } ?: "GPS no disponible — se usará ubicación aproximada"

    AlertDialog(
        // The alert cannot be dismissed by tapping outside or pressing back: the stop
        // only ends with the explicit confirmation below.
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = {
                    onAcknowledge()
                    findings.forEach { finding ->
                        onAttachEvidence(
                            finding.itemId,
                            capturedUris[finding.itemId] ?: finding.photoUri,
                            lastLocation?.first ?: finding.latitude,
                            lastLocation?.second ?: finding.longitude,
                            (notesByItem[finding.itemId] ?: finding.notes).orEmpty()
                        )
                    }
                    onContinue()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    contentColor = OnPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (findings.isEmpty()) "Confirmar y Continuar" else "Guardar Evidencia y Continuar",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorContainer, shape = RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (liveStop) {
                        "PARO CRÍTICO — Hallazgo de riesgo alto detectado"
                    } else {
                        "PARO CRÍTICO — No se alcanzó el 85% de cumplimiento"
                    },
                    color = ErrorDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                if (liveStop) {
                    Text(
                        text = "Este hallazgo exige evidencia antes de continuar: fotografía y observación de lo detectado.",
                        color = OnErrorContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Cumplimiento actual del módulo: $score%",
                        color = OnErrorContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Cumplimiento del módulo: $score% — se requiere reinspección",
                        color = OnErrorContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Registra la evidencia fotográfica y la ubicación real de cada hallazgo no conforme:",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant
                )
                if (findings.isEmpty()) {
                    Text(
                        text = "No hay hallazgos no conformes para documentar.",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(findings, key = { it.itemId }) { finding ->
                            // Seed the per-item observation state from the finding's note
                            // (skipping blank/NA notes) so an existing observation is editable.
                            finding.notes
                                ?.takeIf { it.isNotBlank() && !it.startsWith("No aplica", ignoreCase = true) }
                                ?.let { existing ->
                                    if (finding.itemId !in notesByItem) {
                                        notesByItem[finding.itemId] = existing
                                    }
                                }
                            FindingEvidenceRow(
                                finding = finding,
                                capturedUri = capturedUris[finding.itemId],
                                noteText = notesByItem[finding.itemId] ?: finding.notes.orEmpty(),
                                onNoteChange = { newText ->
                                    notesByItem[finding.itemId] = newText
                                    onAttachEvidence(
                                        finding.itemId,
                                        capturedUris[finding.itemId] ?: finding.photoUri,
                                        lastLocation?.first ?: finding.latitude,
                                        lastLocation?.second ?: finding.longitude,
                                        newText
                                    )
                                },
                                onTakePhoto = {
                                    photoTargetItemId = finding.itemId
                                    cameraLauncher.launch(null)
                                }
                            )
                        }
                    }
                }

                // Footer: estado de la ubicación capturada
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
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = if (lastLocation != null) OnSuccessContainer else OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = locationStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (lastLocation != null) OnSuccessContainer else OnSurfaceVariant
                    )
                }

                // Explicit work-stoppage confirmation: required to leave the dialog.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it }
                    )
                    Text(
                        text = "Confirmo la detención de las labores en la zona",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FindingEvidenceRow(
    finding: SessionFinding,
    capturedUri: String?,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onTakePhoto: () -> Unit
) {
    val context = LocalContext.current
    val displayUri = capturedUri ?: finding.photoUri
    val thumbnail = remember(displayUri) {
        displayUri?.let { GalleryUtils.loadBitmapFromUri(context, Uri.parse(it)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF4F4))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FindingCriticalityBadge(level = finding.criticalityLevel)
            Text(
                text = finding.normCode,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
            )
        }
        Text(
            text = finding.itemDescription,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface
        )
        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteChange,
            label = { Text("Observación / Hallazgo") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(10.dp)
        )

        if (thumbnail != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Evidencia fotográfica",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                Text(
                    text = "Evidencia guardada",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSuccessContainer
                )
            }
        } else {
            Button(
                onClick = onTakePhoto,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tomar Foto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FindingCriticalityBadge(level: CriticalityLevel) {
    val (bg, fg, label) = when (level) {
        CriticalityLevel.HIGH -> Triple(ErrorContainer, OnErrorContainer, "RIESGO ALTO")
        CriticalityLevel.MEDIUM -> Triple(WarningContainer, OnWarningContainer, "RIESGO MEDIO")
        CriticalityLevel.LOW -> Triple(SuccessContainer, OnSuccessContainer, "RIESGO BAJO")
    }
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}