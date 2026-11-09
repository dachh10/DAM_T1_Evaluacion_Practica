package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.ModuleCompletion
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.domain.model.NormativesCatalog
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeChip
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeDetailBottomSheet
import com.example.dam_t1_evaluacion_practica.ui.components.EvidenceImage
import com.example.dam_t1_evaluacion_practica.util.GalleryUtils
import com.example.dam_t1_evaluacion_practica.util.InspectionPdfGenerator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private fun openPdf(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { context.startActivity(intent) } catch (_: Exception) {}
}

private val NavyPrimary = Color(0xFF0B192C)
private val AmberSecondary = Color(0xFFF59E0B)
private val AmberContainer = Color(0xFFFEA619)
private val SurfaceBg = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHighest = Color(0xFFDAE2FD)
private val SurfaceTint = Color(0xFF525F75)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val OnPrimary = Color(0xFFFFFFFF)
private val SuccessColor = Color(0xFF16A34A)
private val ErrorColor = Color(0xFFDC2626)

private fun detailStatus(record: InspectionSessionRecord): Pair<Color, String> = when {
    record.criticalStop -> ErrorColor to "PARO CRÍTICO"
    record.reinspectionDue -> AmberSecondary to "REINSPECCIÓN REQUERIDA"
    else -> SuccessColor to "CONFORME"
}

private fun closingStatusPresentation(status: IncidenceStatus): Pair<Color, String> = when (status) {
    IncidenceStatus.OPEN -> ErrorColor to "Abierto"
    IncidenceStatus.IN_MITIGATION -> AmberSecondary to "En Mitigación"
    IncidenceStatus.CLOSED -> SuccessColor to "Cerrado"
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX")).format(Date(ts))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailScreen(
    record: InspectionSessionRecord,
    onBack: () -> Unit = {},
    onUpdateStatus: (IncidenceStatus) -> Unit = {},
    onReinspect: () -> Unit = {},
    viewModel: InspectionViewModel? = null
) {
    val context = LocalContext.current
    val (accent, statusLabel) = detailStatus(record)
    // A reinspection points back to the critical dictamen it re-evaluates.
    val parentFolio = record.parentInspectionId?.let { parentId ->
        viewModel?.uiState?.value?.savedInspections
            ?.firstOrNull { it.inspectionId == parentId }
            ?.folio
    }
    val initialModuleKey = record.modules.firstOrNull()?.moduleTitle
    val expandedModules = remember(record.folio) {
        mutableStateMapOf<String, Boolean>().apply {
            if (initialModuleKey != null) put(initialModuleKey, true)
        }
    }
    var inspectingNormativ by remember { mutableStateOf<Normativ?>(null) }

    inspectingNormativ?.let { norm ->
        NormativeDetailBottomSheet(
            normativ = norm,
            onDismissRequest = { inspectingNormativ = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(onBack = onBack, folio = record.folio)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Identidad del dictamen
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceLowest,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = record.folio,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                            DetailInfoRow(label = "Empresa", value = record.companyName)
                            DetailInfoRow(label = "Área / Zona", value = record.zoneName)
                            DetailInfoRow(label = "Tipo de auditoría", value = record.auditType)
                            DetailInfoRow(label = "Fecha", value = formatDate(record.timestamp))
                            if (record.parentInspectionId != null) {
                                DetailInfoRow(
                                    label = "Reinspección de",
                                    value = parentFolio ?: "Dictamen anterior"
                                )
                            }
                            record.scheduledReinspectionAt?.let { scheduledAt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timelapse,
                                        contentDescription = null,
                                        tint = AmberSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Reinspección programada: ${formatDate(scheduledAt)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberSecondary
                                    )
                                }
                            } ?: run {
                                if (record.reinspectionDue) {
                                    // Old records saved before the scheduled date existed.
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timelapse,
                                            contentDescription = null,
                                            tint = AmberSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Reinspección requerida en 24 horas",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberSecondary
                                        )
                                    }
                                }
                            }
                            DetailInfoRow(
                                label = "Auditor",
                                value = "${record.inspectorName} • ${record.inspectorCedula}"
                            )
                            record.latitude?.let { lat ->
                                record.longitude?.let { lng ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = AmberSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale("es", "MX"), "%.4f° N, %.4f° W", lat, lng),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Gauge global
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
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { record.overallScore / 100f },
                                    modifier = Modifier.size(120.dp),
                                    color = accent,
                                    trackColor = SurfaceHighest,
                                    strokeWidth = 12.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${record.overallScore}%",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accent
                                    )
                                    Text(
                                        text = "Global",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                letterSpacing = 0.4.sp
                            )
                            ClosingStatusChip(
                                record = record,
                                onUpdateStatus = onUpdateStatus
                            )
                            SyncStatusChip(
                                syncState = record.syncState,
                                onRetry = { viewModel?.retrySync(record.inspectionId) }
                            )
                            // Severity drives the action, not the closing status: even a
                            // dictamen already in mitigation or closed can be re-evaluated.
                            val canReinspect = record.criticalStop || record.reinspectionDue
                            if (canReinspect) {
                                Button(
                                    onClick = onReinspect,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NavyPrimary,
                                        contentColor = OnPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reinspeccionar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Módulos (accordion)
                if (record.modules.isNotEmpty()) {
                    item {
                        Text(
                            text = "Módulos (${record.modules.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(record.modules, key = { it.moduleTitle }) { module ->
                        ModuleAccordionItem(
                            module = module,
                            expanded = expandedModules[module.moduleTitle] == true,
                            onToggle = {
                                expandedModules[module.moduleTitle] = expandedModules[module.moduleTitle] != true
                            },
                            onNormClick = { inspectingNormativ = it }
                        )
                    }
                }

                // Hallazgos
                if (record.findings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Hallazgos (${record.findings.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(record.findings, key = { it.itemId }) { finding ->
                        DetailFindingCard(
                            finding = finding,
                            onNormClick = { inspectingNormativ = it }
                        )
                    }
                }

                // Normas aplicadas
                if (record.normCodes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Normas Aplicadas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            record.normCodes.forEach { code ->
                                val norm = NormativesCatalog.ALL_NORMATIVES.firstOrNull { it.code == code }
                                    ?: Normativ(code = code)
                                NormativeChip(
                                    normativ = norm,
                                    isPrimary = true,
                                    onClick = { inspectingNormativ = norm }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // Acciones placeholder
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceBg.copy(alpha = 0.97f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    var generatingPdf by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            if (!generatingPdf) {
                                generatingPdf = true
                                scope.launch(Dispatchers.IO) {
                                    val uri = InspectionPdfGenerator.generate(context, record)
                                    withContext(Dispatchers.Main) {
                                        generatingPdf = false
                                        uri?.let { openPdf(context, it) }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = OnPrimary
                        ),
                        enabled = !generatingPdf
                    ) {
                        if (generatingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando PDF...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Descargar PDF del Dictamen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, folio: String) {
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
                        text = "Dictamen de Auditoría",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = folio,
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
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/**
 * Closing-status chip of the dictamen. Non-conforming records (critical stop or
 * reinspection due) advance one strict forward step (OPEN -> IN_MITIGATION -> CLOSED):
 * the chip opens a dropdown with the single allowed next status. Records with no next
 * status (CLOSED) or conforming records render a fixed chip with no dropdown.
 */
@Composable
private fun ClosingStatusChip(
    record: InspectionSessionRecord,
    onUpdateStatus: (IncidenceStatus) -> Unit
) {
    val (statusColor, statusLabel) = closingStatusPresentation(record.status)
    val nextStatus = IncidenceStatus.entries.firstOrNull { it.ordinal == record.status.ordinal + 1 }
    val editable = (record.criticalStop || record.reinspectionDue) && nextStatus != null
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .then(if (editable) Modifier.clickable { menuExpanded = true } else Modifier)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = statusLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (editable) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Cambiar estado",
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (editable) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                nextStatus?.let { status ->
                    val (itemColor, itemLabel) = closingStatusPresentation(status)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = itemColor
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (status != record.status) onUpdateStatus(status)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Sync status chip for the inspection detail. Shows the current cloud sync state and
 * offers a retry action when the sync has failed.
 */
@Composable
private fun SyncStatusChip(
    syncState: SyncState,
    onRetry: () -> Unit
) {
    val (color, label, showRetry) = when (syncState) {
        SyncState.LOCAL_ONLY -> Triple(AmberSecondary, "Pendiente", false)
        SyncState.PENDING -> Triple(AmberSecondary, "Sincronizando…", false)
        SyncState.SYNCED -> Triple(SuccessColor, "Sincronizado", false)
        SyncState.FAILED -> Triple(ErrorColor, "Error · Reintentar", true)
    }

    Surface(
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (showRetry) {
                androidx.compose.material3.IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = "Reintentar sincronización",
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleAccordionItem(
    module: ModuleCompletion,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNormClick: (Normativ) -> Unit = {}
) {
    val accent = when {
        module.criticalStop -> ErrorColor
        module.reinspectionDue -> AmberSecondary
        else -> SuccessColor
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
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
                        text = "${module.answeredCount}/${module.totalCount} verificados • ${module.normCodes.joinToString(" • ")}",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${module.score}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Expandir",
                    tint = SurfaceTint,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    module.items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ItemAnswerIcon(item = item, module = module)
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = OnSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            NormativeChip(
                                normativ = item.normativ,
                                isPrimary = false,
                                onClick = { onNormClick(item.normativ) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemAnswerIcon(item: ChecklistItem, module: ModuleCompletion) {
    val isNA = module.naAnswers.contains(item.id)
    val answer = module.answers[item.id]
    val (icon, tint) = when {
        isNA -> Icons.Outlined.Timelapse to AmberSecondary
        answer == true -> Icons.Filled.CheckCircle to SuccessColor
        answer == false -> Icons.Outlined.Cancel to ErrorColor
        else -> Icons.Outlined.Timelapse to SurfaceTint
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun DetailFindingCard(
    finding: SessionFinding,
    onNormClick: (Normativ) -> Unit = {}
) {
    val norm = remember(finding.normCode) {
        NormativesCatalog.ALL_NORMATIVES.firstOrNull { it.code == finding.normCode }
            ?: Normativ(code = finding.normCode)
    }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
            Text(
                text = finding.itemDescription,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NormativeChip(
                    normativ = norm,
                    isPrimary = true,
                    onClick = { onNormClick(norm) }
                )
                Text(
                    text = formatDate(finding.timestamp),
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