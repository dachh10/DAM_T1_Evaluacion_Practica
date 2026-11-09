package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.ui.components.AppTopBar
import com.example.dam_t1_evaluacion_practica.ui.components.BottomNavBar
import com.example.dam_t1_evaluacion_practica.ui.components.MainTab
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyContainer        = Color(0xFF1E3E62)
private val AmberSecondary       = Color(0xFFF59E0B)
private val AmberContainer       = Color(0xFFFEA619)
private val AmberFixed           = Color(0xFFFFDDB8)
private val OnAmberFixed         = Color(0xFF2A1700)
private val SurfaceBg            = Color(0xFFFAF8FF)
private val SurfaceLowest        = Color(0xFFFFFFFF)
private val SurfaceLow           = Color(0xFFF2F3FF)
private val SurfaceContainer     = Color(0xFFEAEDFF)
private val SurfaceHigh          = Color(0xFFE2E7FF)
private val SurfaceHighest       = Color(0xFFDAE2FD)
private val SurfaceTint          = Color(0xFF525F75)
private val OnSurface            = Color(0xFF131B2E)
private val OnSurfaceVariant     = Color(0xFF44474C)
private val ErrorColor           = Color(0xFFBA1A1A)
private val ErrorContainer       = Color(0xFFFFDAD6)
private val OnErrorContainer     = Color(0xFF93000A)
private val OnPrimary            = Color(0xFFFFFFFF)
private val OnPrimaryContainer   = Color(0xFF77849C)
private val SuccessColor         = Color(0xFF16A34A)

@Composable
fun DashboardScreen(
    viewModel: InspectionViewModel,
    onStartInspection: (InspectionModule) -> Unit = {},
    onNavigateToNewInspection: () -> Unit = { onStartInspection(InspectionModule.EPP) },
    onTabSelected: (MainTab) -> Unit = {},
    onOpenHistorial: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val records = uiState.savedInspections

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                onNotificationsClick = {},
                onProfileClick = { onTabSelected(MainTab.PERFIL) },
                initials = InspectorProfile.initialsOf(uiState.currentUser)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WelcomeCard(recordsCount = records.size, userName = InspectorProfile.nameOf(uiState.currentUser))
                StartInspectionButton(onClick = onNavigateToNewInspection)
                RealStatsCard(records = records)
                HistorialAccessCard(onClick = onOpenHistorial)
                RecentInspectionsSection(records = records, onSeeAll = onOpenHistorial)
            }
        }
        BottomNavBar(
            selectedTab = MainTab.INSPECCION,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun WelcomeCard(recordsCount: Int, userName: String) {
    val currentDate = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "MX"))
        .format(Date())
        .replaceFirstChar { it.uppercase() }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLow).padding(16.dp)) {
        Box(modifier = Modifier.size(112.dp).clip(CircleShape).background(Color(0xFFFFDDB8).copy(alpha = 0.4f)).align(Alignment.BottomEnd).offset(x = 24.dp, y = 24.dp))
        Column {
            Text(currentDate, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(2.dp))
            Text("Hola, $userName", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = OnSurface)
            Text(InspectorProfile.LICENSE, fontSize = 14.sp, color = OnSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recordsCount > 0) {
                    StatusChip(icon = Icons.Outlined.Checklist, text = "Auditorías realizadas: $recordsCount", iconTint = AmberSecondary, background = SurfaceHighest)
                } else {
                    StatusChip(icon = Icons.Outlined.Timelapse, text = "Sin auditorías aún", iconTint = AmberSecondary, background = SurfaceHighest)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(icon: ImageVector, text: String, iconTint: Color, background: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(background).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StartInspectionButton(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = NavyContainer, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AmberContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NoteAdd, contentDescription = null, tint = OnAmberFixed, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text("Iniciar nueva inspeccion", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnPrimary)
                    Text("Seleccionar nave, cuadrilla o lista de verificacion", fontSize = 14.sp, color = OnPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceTint.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RealStatsCard(records: List<InspectionSessionRecord>) {
    if (records.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLowest).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Sin dictámenes aún", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Text("Completa una inspección para ver tu resumen", fontSize = 13.sp, color = OnSurfaceVariant)
        }
        return
    }

    val averageScore = records.map { it.overallScore }.average().toInt()
    val reinspectionCount = records.count { it.reinspectionDue }
    val findingsCount = records.sumOf { it.findings.size }
    val criticalStops = records.count { it.criticalStop }
    val closedCount = records.count { it.status == IncidenceStatus.CLOSED }

    // Score-based verdict: 85+ is the domain reinspection threshold (REINSPECTION_THRESHOLD),
    // below 70 the average is poor enough to label the panel "Crítico".
    val statusLabel: String
    val statusBg: Color
    val statusFg: Color
    when {
        averageScore in 85..100 -> {
            statusLabel = "Cumplimiento óptimo"
            statusBg = SurfaceHighest
            statusFg = OnSurface
        }
        averageScore in 70..84 -> {
            statusLabel = "Reinspección requerida"
            statusBg = AmberFixed
            statusFg = OnAmberFixed
        }
        else -> {
            statusLabel = "Crítico"
            statusBg = ErrorContainer
            statusFg = OnErrorContainer
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLowest).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dictamen Promedio", fontSize = 11.sp, color = OnSurfaceVariant, letterSpacing = 0.8.sp, fontWeight = FontWeight.Medium)
                Text("$averageScore%", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = OnSurface)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(statusBg).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusFg)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    icon = Icons.Outlined.Checklist, iconTint = OnSurfaceVariant,
                    badge = "+$closedCount", badgeBg = SurfaceHighest, badgeText = OnSurface,
                    value = closedCount.toString(), label = "Inspecciones cerradas", valueColor = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Outlined.Timelapse, iconTint = AmberSecondary,
                    badge = reinspectionCount.toString(), badgeBg = AmberFixed, badgeText = OnAmberFixed,
                    value = reinspectionCount.toString(), label = "Pendientes de reinspección", valueColor = OnSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    icon = Icons.Outlined.ReportProblem, iconTint = ErrorColor,
                    badge = findingsCount.toString(), badgeBg = ErrorContainer, badgeText = OnErrorContainer,
                    value = findingsCount.toString(), label = "Hallazgos registrados", valueColor = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Outlined.Dangerous, iconTint = ErrorColor,
                    badge = if (criticalStops > 0) "Paro" else "Sin paros",
                    badgeBg = if (criticalStops > 0) ErrorColor else SurfaceHighest,
                    badgeText = if (criticalStops > 0) OnPrimary else OnSurface,
                    value = criticalStops.toString(), label = "Paros críticos",
                    valueColor = if (criticalStops > 0) ErrorColor else OnSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HistorialAccessCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = SurfaceLowest, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Historial de Auditorías", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Ver dictámenes y evidencias guardados", fontSize = 12.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MetricCard(icon: ImageVector, iconTint: Color, badge: String, badgeBg: Color, badgeText: Color, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceLow).padding(16.dp).heightIn(min = 118.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(badgeBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
            }
        }
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = valueColor)
            Text(label, fontSize = 11.sp, color = OnSurfaceVariant, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun RecentInspectionsSection(records: List<InspectionSessionRecord>, onSeeAll: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Inspecciones Recientes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnSurface)
            TextButton(onClick = onSeeAll) {
                Text("Ver todas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AmberSecondary)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(16.dp))
            }
        }
        if (records.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceLowest, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Aún no hay inspecciones guardadas",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // savedInspections appends chronologically (oldest first), so sort descending
                // before slicing to surface the 3 most recent records.
                records.sortedByDescending { it.timestamp }.take(3).forEach { record ->
                    val critical = record.criticalStop
                    val reinspection = record.reinspectionDue
                    val chipBg = when {
                        critical -> ErrorContainer
                        reinspection -> AmberFixed
                        else -> SurfaceHighest
                    }
                    val chipFg = when {
                        critical -> OnErrorContainer
                        reinspection -> OnAmberFixed
                        else -> OnSurface
                    }
                    val chipIcon = when {
                        critical -> Icons.Outlined.Dangerous
                        reinspection -> Icons.Outlined.Timelapse
                        else -> Icons.Outlined.CheckCircle
                    }
                    val chipLabel = when {
                        critical -> "Crítica"
                        reinspection -> "Reinspección"
                        else -> "Conforme"
                    }
                    val statusText = when {
                        critical -> "Paro de labores"
                        reinspection -> "Reinspección pendiente"
                        else -> "Dictaminado"
                    }
                    val statusColor = when {
                        critical -> ErrorColor
                        reinspection -> AmberSecondary
                        else -> SuccessColor
                    }
                    val timestampText = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "MX")).format(Date(record.timestamp))
                    RecentItem(
                        icon = if (critical) Icons.Outlined.Warning else Icons.Outlined.Science,
                        iconBg = if (critical) ErrorContainer else SurfaceContainer,
                        iconTint = if (critical) OnErrorContainer else OnSurface,
                        title = record.companyName,
                        subtitle = "$timestampText · ${record.zoneName}",
                        chipText = "${record.overallScore}% $chipLabel",
                        chipBg = chipBg,
                        chipTextColor = chipFg,
                        chipIcon = chipIcon,
                        chipIconTint = chipFg,
                        norm = record.auditType,
                        statusText = statusText,
                        statusColor = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentItem(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, chipText: String, chipBg: Color, chipTextColor: Color, chipIcon: ImageVector, chipIconTint: Color, norm: String, statusText: String, statusColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLowest).clickable {}.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 11.sp, color = OnSurfaceVariant)
                }
            }
            Row(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(chipBg).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(chipIcon, contentDescription = null, tint = chipIconTint, modifier = Modifier.size(14.dp))
                Text(chipText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = chipTextColor)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(norm, fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.weight(1f))
            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
    }
}