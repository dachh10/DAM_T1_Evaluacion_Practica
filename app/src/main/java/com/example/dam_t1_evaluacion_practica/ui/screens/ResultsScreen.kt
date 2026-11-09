package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.R
import com.example.dam_t1_evaluacion_practica.domain.model.ModuleCompletion
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.components.EvidenceImage
import com.example.dam_t1_evaluacion_practica.util.GalleryUtils
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyPrimary = Color(0xFF0B192C)
private val AmberSecondary = Color(0xFFF59E0B)
private val AmberContainer = Color(0xFFFEA619)
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
private val SuccessColor = Color(0xFF16A34A)
private val SuccessContainer = Color(0xFFDCFCE7)
private val OnSuccessContainer = Color(0xFF166534)
private val ErrorColor = Color(0xFFDC2626)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val WarningContainer = Color(0xFFFFECB3)
private val OnWarningContainer = Color(0xFFE65100)

private data class ResultStatus(val container: Color, val accent: Color, val label: String)

private fun resultStatus(score: Int, criticalStop: Boolean, reinspectionDue: Boolean): ResultStatus =
    when {
        criticalStop -> ResultStatus(ErrorContainer, ErrorColor, "PARO CRÍTICO ACTIVADO")
        reinspectionDue || score < 85 -> ResultStatus(WarningContainer, AmberSecondary, "REINSPECCIÓN REQUERIDA")
        else -> ResultStatus(SuccessContainer, SuccessColor, "CUMPLIMIENTO REGULAR")
    }

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX")).format(Date(ts))

@Composable
fun ResultsScreen(
    viewModel: InspectionViewModel,
    onSaveDictamen: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val sessionScore = viewModel.computedSessionScore()
    val status = resultStatus(sessionScore, uiState.criticalStopTriggered, uiState.reinspectionDue)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ResultsTopBar(
                onBack = onBack,
                auditType = uiState.auditType,
                zoneName = uiState.zoneName
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Banner de estado
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = status.container.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (status.accent == SuccessColor)
                                    Icons.Filled.CheckCircle else Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = status.accent,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.label,
                                    color = status.accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = "Auditoría ${uiState.auditType} en ${uiState.zoneName}",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Anillo de score global
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceLowest,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { sessionScore / 100f },
                                    modifier = Modifier.size(140.dp),
                                    color = status.accent,
                                    trackColor = SurfaceHighest,
                                    strokeWidth = 14.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$sessionScore%",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = status.accent
                                    )
                                    Text(
                                        text = "Cumplimiento",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = SurfaceTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Sesión registrada ${formatTimestamp(System.currentTimeMillis())}",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Desglose por módulo
                if (uiState.sessionModules.isNotEmpty()) {
                    item {
                        Text(
                            text = "Desglose por Módulo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(uiState.sessionModules, key = { it.moduleTitle }) { module ->
                        ModuleResultCard(module = module)
                    }
                }

                // Hallazgos
                if (uiState.sessionFindings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hallazgos de la Sesión (${uiState.sessionFindings.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(uiState.sessionFindings, key = { it.itemId }) { finding ->
                        FindingResultCard(finding = finding)
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Acciones
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
                    if (uiState.criticalEvidencePending) {
                        Text(
                            text = "Evidencia pendiente: completa la foto y la observación de los hallazgos críticos antes de guardar.",
                            color = ErrorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onSaveDictamen,
                        // The dictamen cannot be saved while any finding still owes evidence
                        // or the critical stop has not been acknowledged.
                        enabled = !uiState.criticalEvidencePending && !uiState.answersLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Dictamen", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Historial", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsTopBar(
    onBack: () -> Unit,
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
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resultados de Inspección",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$auditType • $zoneName",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleResultCard(module: ModuleCompletion) {
    val status = resultStatus(module.score, module.criticalStop, module.reinspectionDue)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.moduleTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = module.normCodes.joinToString(" • "),
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${module.score}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = status.accent
                )
            }
            LinearProgressIndicator(
                progress = { module.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = status.accent,
                trackColor = SurfaceHighest
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (module.completed) SuccessContainer else SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (module.completed) "Finalizado" else "En progreso",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (module.completed) OnSuccessContainer else SurfaceTint
                    )
                }
                Text(
                    text = "${module.answeredCount}/${module.totalCount} verificados",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FindingResultCard(finding: SessionFinding) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    CriticalityBadge(level = finding.criticalityLevel)
                    if (finding.isNA) {
                        Text(
                            text = "N/A",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberSecondary
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = finding.normCode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceTint
                    )
                }
            }
            Text(
                text = finding.itemDescription,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = SurfaceTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = formatTimestamp(finding.timestamp),
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }
            (finding.notes ?: "").takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    fontSize = 12.sp,
                    color = OnSurfaceVariant
                )
            }
            EvidenceImage(
                url = finding.photoUri,
                contentDescription = "Evidencia fotográfica",
                size = 64.dp
            )
        }
    }
}