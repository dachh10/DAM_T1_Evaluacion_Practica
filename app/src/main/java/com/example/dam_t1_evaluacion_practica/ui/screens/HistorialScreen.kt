package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.ui.components.AppTopBar
import com.example.dam_t1_evaluacion_practica.ui.components.BottomNavBar
import com.example.dam_t1_evaluacion_practica.ui.components.MainTab
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyPrimary = Color(0xFF0B192C)
private val AmberSecondary = Color(0xFFF59E0B)
private val SurfaceBg = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceTint = Color(0xFF525F75)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val OnPrimary = Color(0xFFFFFFFF)
private val SuccessColor = Color(0xFF16A34A)
private val ErrorColor = Color(0xFFDC2626)

private enum class HistoryFilter { ALL, CONFORME, REINSPECCION, CRITICA }

private fun recordStatus(record: InspectionSessionRecord): Pair<Color, String> = when {
    record.criticalStop -> ErrorColor to "Crítica"
    record.reinspectionDue -> AmberSecondary to "Reinspección"
    else -> SuccessColor to "Conforme"
}

private fun closingStatusPresentation(status: IncidenceStatus): Pair<Color, String> = when (status) {
    IncidenceStatus.OPEN -> ErrorColor to "Abierto"
    IncidenceStatus.IN_MITIGATION -> AmberSecondary to "En Mitigación"
    IncidenceStatus.CLOSED -> SuccessColor to "Cerrado"
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX")).format(Date(ts))

@Composable
fun HistorialScreen(
    viewModel: InspectionViewModel,
    onSelectRecord: (InspectionSessionRecord) -> Unit = {},
    onUpdateRecordStatus: (String, IncidenceStatus) -> Unit = { _, _ -> },
    onNewInspection: () -> Unit = {},
    onTabSelected: (MainTab) -> Unit = {},
    selectedTab: MainTab = MainTab.INSPECCION,
    title: String = "Historial de Auditorías",
    subtitle: String = "Dictámenes guardados en el dispositivo"
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    val records = uiState.savedInspections
    val averageScore = if (records.isEmpty()) 0 else records.map { it.overallScore }.average().toInt()
    val reinspectionCount = records.count { it.reinspectionDue }

    val filtered = records.filter { record ->
        val matchesQuery = query.isBlank() ||
            record.folio.contains(query, ignoreCase = true) ||
            record.companyName.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            HistoryFilter.ALL -> true
            HistoryFilter.CONFORME -> !record.criticalStop && !record.reinspectionDue
            HistoryFilter.REINSPECCION -> record.reinspectionDue
            HistoryFilter.CRITICA -> record.criticalStop
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            AppTopBar(
                title = title,
                subtitle = subtitle,
                onNotificationsClick = {},
                onProfileClick = { onTabSelected(MainTab.PERFIL) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewInspection,
                containerColor = NavyPrimary,
                contentColor = OnPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Nueva Auditoría")
            }
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HistorialStatsRibbon(
                totalCount = records.size,
                averageScore = averageScore,
                reinspectionCount = reinspectionCount
            )

            // Búsqueda
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por folio o empresa...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = SurfaceTint
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = {
                            Text(
                                text = when (f) {
                                    HistoryFilter.ALL -> "Todas"
                                    HistoryFilter.CONFORME -> "Conforme"
                                    HistoryFilter.REINSPECCION -> "Reinspección"
                                    HistoryFilter.CRITICA -> "Crítica"
                                },
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyHistoryState(hasAny = records.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.folio }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onClick = { onSelectRecord(record) },
                            onUpdateStatus = { status -> onUpdateRecordStatus(record.folio, status) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistorialStatsRibbon(
    totalCount: Int,
    averageScore: Int,
    reinspectionCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCell(label = "Auditorías", value = totalCount.toString(), icon = Icons.Outlined.History)
            StatCell(label = "Cumpl. Prom.", value = "$averageScore%", icon = Icons.Outlined.Timelapse)
            StatCell(label = "Reinspecciones", value = reinspectionCount.toString(), icon = Icons.Outlined.Replay)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRecordCard(
    record: InspectionSessionRecord,
    onClick: () -> Unit,
    onUpdateStatus: (IncidenceStatus) -> Unit = {}
) {
    val (accent, statusLabel) = recordStatus(record)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(108.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.folio,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                        ClosingStatusChip(
                            record = record,
                            onUpdateStatus = onUpdateStatus
                        )
                    }
                }
                Text(
                    text = "${record.companyName} • ${record.zoneName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = SurfaceTint,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatDate(record.timestamp),
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        record.normCodes.take(2).forEach { code ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = code,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SurfaceTint
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${record.overallScore}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Ver Dictamen",
                            tint = SurfaceTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Closing-status chip of the dictamen card. Non-conforming records (critical stop or
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
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = statusLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (editable) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Cambiar estado",
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
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

@Composable
private fun EmptyHistoryState(hasAny: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = SurfaceTint,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = if (hasAny) "Sin resultados para el filtro actual" else "Sin auditorías registradas",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (hasAny) "Ajusta la búsqueda o el filtro seleccionado." else "Completa una inspección y guarda el dictamen para verlo aquí.",
            fontSize = 12.sp,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}