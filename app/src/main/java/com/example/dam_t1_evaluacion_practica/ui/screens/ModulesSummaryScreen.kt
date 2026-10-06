package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.R
import com.example.dam_t1_evaluacion_practica.data.local.SampleData
import com.example.dam_t1_evaluacion_practica.domain.model.ModuleCompletion
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel

private val NavyPrimary        = Color(0xFF0B192C)
private val AmberSecondary     = Color(0xFFF59E0B)
private val AmberContainer     = Color(0xFFFEA619)
private val SurfaceBg          = Color(0xFFFAF8FF)
private val SurfaceLowest      = Color(0xFFFFFFFF)
private val SurfaceLow         = Color(0xFFF2F3FF)
private val SurfaceContainer   = Color(0xFFEAEDFF)
private val SurfaceHigh        = Color(0xFFE2E7FF)
private val SurfaceHighest     = Color(0xFFDAE2FD)
private val SurfaceTint        = Color(0xFF525F75)
private val OnSurface          = Color(0xFF131B2E)
private val OnSurfaceVariant   = Color(0xFF44474C)
private val OnPrimary          = Color(0xFFFFFFFF)
private val ErrorColor         = Color(0xFFBA1A1A)
private val ErrorContainer     = Color(0xFFFFDAD6)
private val OnErrorContainer   = Color(0xFF93000A)
private val SuccessColor       = Color(0xFF16A34A)
private val SuccessContainer   = Color(0xFFDCFCE7)
private val OnSuccessContainer = Color(0xFF166534)

@Composable
fun ModulesSummaryScreen(
    viewModel: InspectionViewModel,
    onBack: () -> Unit = {},
    onSelectModule: (InspectionModule) -> Unit = {},
    onFinishInspection: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Progreso real de la sesión: suma de verificaciones respondidas sobre el total
    // de los tres módulos (los módulos aún no abiertos aportan 0 respondidas).
    val totalItems =
        SampleData.eppItems.size + SampleData.machineryItems.size + SampleData.environmentItems.size
    val answeredItems = uiState.sessionModules.sumOf { it.answeredCount }
    val snapshots = uiState.sessionModules

    // "Terminar Inspección" only appears once the session has a completed snapshot for
    // every one of the three modules (EPP, Maquinaria, Entorno).
    val allModulesCompleted = listOf(
        InspectionModule.EPP.title,
        InspectionModule.MACHINERY.title,
        InspectionModule.FACILITIES.title
    ).all { title -> snapshots.any { it.moduleTitle == title && it.completed } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModulesSummaryTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Contextual de la Planta y Progreso General (real, desde el estado)
                MetaProgressHeader(
                    answeredCount = answeredItems,
                    totalCount = totalItems,
                    companyName = uiState.companyName,
                    zoneName = uiState.zoneName
                )

                // Módulo 1: EPP (NOM-017-STPS)
                ModuleEppCard(
                    snapshot = snapshots.firstOrNull { it.moduleTitle == InspectionModule.EPP.title },
                    totalItems = SampleData.eppItems.size,
                    onClick = { onSelectModule(InspectionModule.EPP) }
                )

                // Módulo 2: Maquinaria y Herramientas (NOM-004-STPS)
                ModuleMachineryCard(
                    snapshot = snapshots.firstOrNull { it.moduleTitle == InspectionModule.MACHINERY.title },
                    totalItems = SampleData.machineryItems.size,
                    onClick = { onSelectModule(InspectionModule.MACHINERY) }
                )

                // Módulo 3: Entorno de Trabajo (NOM-001/002-STPS)
                ModuleEnvironmentCard(
                    snapshot = snapshots.firstOrNull { it.moduleTitle == InspectionModule.FACILITIES.title },
                    totalItems = SampleData.environmentItems.size,
                    onClick = { onSelectModule(InspectionModule.FACILITIES) }
                )

                // Cierre de la inspección: solo visible cuando los tres módulos están completos.
                if (allModulesCompleted) {
                    Button(
                        onClick = onFinishInspection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terminar Inspección", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // Tarjeta Informativa: Bitácora Oficial STPS
                OfficialLogbookBanner()

                // Pie Normativo
                NormativeFooter()
            }
        }
    }
}

@Composable
private fun ModulesSummaryTopBar(onBack: () -> Unit) {
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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = OnSurface
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.logo1),
                        contentDescription = "Logo",
                        modifier = Modifier.height(28.dp)
                    )
                    Text(
                        text = "Módulos de Inspección",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = OnSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Inspector",
                            tint = OnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaProgressHeader(
    answeredCount: Int,
    totalCount: Int,
    companyName: String,
    zoneName: String
) {
    val fraction = if (totalCount == 0) 0f else answeredCount.toFloat() / totalCount
    val percent = (fraction * 100).toInt()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceHigh,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(AmberContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary)
                        )
                        Text(
                            text = "EN PROCESO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = companyName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Factory,
                            contentDescription = null,
                            tint = SurfaceTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = zoneName,
                            fontSize = 13.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLowest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Assignment,
                        contentDescription = null,
                        tint = SurfaceTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Barra de progreso
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceLowest.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DonutLarge,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Progreso Total",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                        }
                        Text(
                            text = "$percent%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberSecondary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$answeredCount de $totalCount verificaciones",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun moduleStatusText(snapshot: ModuleCompletion?): String = when {
    snapshot?.completed == true -> "Completado"
    snapshot != null -> "En curso"
    else -> "Pendiente"
}

@Composable
private fun moduleAnsweredCount(snapshot: ModuleCompletion?): Int = snapshot?.answeredCount ?: 0

@Composable
private fun moduleConformeCount(snapshot: ModuleCompletion?): Int =
    snapshot?.answers?.values?.count { it } ?: 0

@Composable
private fun ModuleEppCard(
    snapshot: ModuleCompletion?,
    totalItems: Int,
    onClick: () -> Unit
) {
    val answered = moduleAnsweredCount(snapshot)
    val conformes = moduleConformeCount(snapshot)
    val noConformes = answered - conformes
    val statusText = moduleStatusText(snapshot)
    val sideColor = when {
        snapshot?.completed == true -> SuccessColor
        snapshot != null -> AmberContainer
        else -> SurfaceTint
    }
    val statusChipBg = when {
        snapshot?.completed == true -> SuccessContainer
        snapshot != null -> AmberContainer
        else -> SurfaceHigh
    }
    val statusChipContent = when {
        snapshot?.completed == true -> OnSuccessContainer
        snapshot != null -> NavyPrimary
        else -> OnSurfaceVariant
    }
    val isCompleted = snapshot?.completed == true
    val buttonLabel = when {
        isCompleted -> "Completado"
        snapshot != null -> "Continuar Módulo A"
        else -> "Iniciar Módulo A"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = if (snapshot != null) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted, onClick = onClick)
    ) {
        Box {
            // Barra indicadora lateral izquierda
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(sideColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(SurfaceHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOM-017-STPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SurfaceTint
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(statusChipBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = statusChipContent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusChipContent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(statusChipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = statusChipContent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Módulo A: Equipo de Protección Personal (EPP)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TaskAlt,
                                contentDescription = null,
                                tint = SurfaceTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$answered de $totalItems verificaciones",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                        }
                    }
                    Text(
                        text = if (answered == 0) "Sin respuestas" else "$conformes Conformes • $noConformes No conformes",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(SurfaceHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (snapshot != null) "Score ${snapshot.score}%" else "Sin evaluación",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { if (!isCompleted) onClick() },
                        enabled = !isCompleted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainer,
                            contentColor = OnSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleMachineryCard(
    snapshot: ModuleCompletion?,
    totalItems: Int,
    onClick: () -> Unit
) {
    val answered = moduleAnsweredCount(snapshot)
    val conformes = moduleConformeCount(snapshot)
    val noConformes = answered - conformes
    val statusText = moduleStatusText(snapshot)
    val fraction = if (totalItems == 0) 0f else answered.toFloat() / totalItems
    val percent = (fraction * 100).toInt()
    val sideColor = when {
        snapshot?.completed == true -> SuccessColor
        snapshot != null -> AmberContainer
        else -> SurfaceTint
    }
    val statusChipBg = when {
        snapshot?.completed == true -> SuccessContainer
        snapshot != null -> AmberContainer
        else -> SurfaceHigh
    }
    val statusChipContent = when {
        snapshot?.completed == true -> OnSuccessContainer
        snapshot != null -> NavyPrimary
        else -> OnSurfaceVariant
    }
    val isCompleted = snapshot?.completed == true
    val buttonLabel = when {
        isCompleted -> "Completado"
        snapshot != null -> "Continuar Módulo B"
        else -> "Iniciar Módulo B"
    }
    val riskVisible = snapshot?.score != null && snapshot.score < 85

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = if (snapshot != null) 4.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted, onClick = onClick)
    ) {
        Box {
            // Barra indicadora lateral izquierda
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(sideColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(SurfaceHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOM-004-STPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SurfaceTint
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(statusChipBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusChipContent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusChipBg.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PrecisionManufacturing,
                            contentDescription = null,
                            tint = if (snapshot?.completed == true) SuccessColor else AmberSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Módulo B: Maquinaria y Herramientas Críticas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                // Mini barra de progreso del módulo
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$answered de $totalItems verificaciones",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = "$percent% avance",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SurfaceHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AmberContainer)
                            )
                        }
                        Text(
                            text = if (answered == 0) "Sin respuestas" else "$conformes Conformes • $noConformes No conformes",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (riskVisible) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = OnErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Riesgo Alto detectado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnErrorContainer
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = { if (!isCompleted) onClick() },
                        enabled = !isCompleted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = OnPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleEnvironmentCard(
    snapshot: ModuleCompletion?,
    totalItems: Int,
    onClick: () -> Unit
) {
    val answered = moduleAnsweredCount(snapshot)
    val statusText = moduleStatusText(snapshot)
    val statusChipBg = when {
        snapshot?.completed == true -> SuccessContainer
        snapshot != null -> AmberContainer
        else -> SurfaceHigh
    }
    val statusChipContent = when {
        snapshot?.completed == true -> OnSuccessContainer
        snapshot != null -> NavyPrimary
        else -> OnSurfaceVariant
    }
    val iconTint = when {
        snapshot?.completed == true -> SuccessColor
        snapshot != null -> AmberSecondary
        else -> SurfaceTint
    }
    val isCompleted = snapshot?.completed == true
    val buttonLabel = when {
        isCompleted -> "Completado"
        snapshot != null -> "Continuar Módulo C"
        else -> "Iniciar Módulo C"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = if (snapshot != null) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted, onClick = onClick)
    ) {
        Box {
            // Barra indicadora lateral izquierda
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(
                        when {
                            snapshot?.completed == true -> SuccessColor
                            snapshot != null -> AmberContainer
                            else -> SurfaceTint
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(SurfaceHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NOM-001/002-STPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SurfaceTint
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(statusChipBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusChipContent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(statusChipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Domain,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Módulo C: Condiciones del Entorno de Trabajo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Emergency,
                            contentDescription = null,
                            tint = SurfaceTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Vías de evacuación, orden y señalética extintores",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$answered de $totalItems verificaciones",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )

                    Button(
                        onClick = { if (!isCompleted) onClick() },
                        enabled = !isCompleted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceHigh,
                            contentColor = OnSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialLogbookBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bitácora Oficial STPS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = "Cédula de cumplimiento con valor probatorio laboral.",
                    fontSize = 12.sp,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NormativeFooter() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Gavel,
                contentDescription = null,
                tint = SurfaceTint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Normas vinculadas a este dictamen:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Text(
                    text = "NOM-017-STPS (EPP), NOM-004-STPS (Maquinaria Segura) y NOM-002-STPS (Protección contra Incendios).",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
