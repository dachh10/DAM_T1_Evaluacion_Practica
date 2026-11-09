package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.ui.components.AppTopBar
import com.example.dam_t1_evaluacion_practica.ui.components.BottomNavBar
import com.example.dam_t1_evaluacion_practica.ui.components.MainTab
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectionViewModel
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile

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
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val OnPrimary = Color(0xFFFFFFFF)
private val SuccessColor = Color(0xFF16A34A)
private val ErrorColor = Color(0xFFDC2626)

@Composable
fun ProfileScreen(
    viewModel: InspectionViewModel,
    onTabSelected: (MainTab) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val records = uiState.savedInspections
    val totalFindings = records.sumOf { it.findings.size }
    val averageScore = if (records.isEmpty()) 0 else records.map { it.overallScore }.average().toInt()

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            AppTopBar(
                title = "Perfil del Inspector",
                subtitle = "Credenciales y configuración",
                onNotificationsClick = {},
                onProfileClick = { onTabSelected(MainTab.PERFIL) },
                initials = InspectorProfile.initialsOf(uiState.currentUser)
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = MainTab.PERFIL,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            val userName = uiState.currentUser?.name ?: InspectorProfile.NAME
            val userCedula = uiState.currentUser?.cedula ?: InspectorProfile.CEDULA
            val userEmail = uiState.currentUser?.email ?: "inspector@seguridad.mx"
            val initials = userName.split(" ")
                .filter { it.isNotBlank() && !it.startsWith("Ing", ignoreCase = true) && !it.startsWith("Lic", ignoreCase = true) }
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifBlank { "IN" }

            // Header del inspector
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AmberSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyPrimary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = userName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(AmberContainer.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Activo en Campo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberContainer
                            )
                        }
                    }
                    Text(
                        text = "$userEmail • $userCedula",
                        fontSize = 12.sp,
                        color = OnPrimary.copy(alpha = 0.75f)
                    )
                }
            }

            // Grid de estadísticas
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatCell(
                        icon = Icons.Outlined.Badge,
                        value = records.size.toString(),
                        label = "Auditorías"
                    )
                    ProfileStatCell(
                        icon = Icons.Outlined.ReportProblem,
                        value = totalFindings.toString(),
                        label = "Hallazgos"
                    )
                    ProfileStatCell(
                        icon = Icons.Outlined.Checklist,
                        value = "$averageScore%",
                        label = "Cumplimiento"
                    )
                }
            }

            // Credenciales
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
                        text = "Credenciales",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    CredentialRow(label = "Cédula STPS", value = userCedula)
                    CredentialRow(label = "Certificación DC-5", value = InspectorProfile.DC5)
                    CredentialRow(label = "Código de Certificación", value = InspectorProfile.CERT_CODE)
                }
            }

            // Almacenamiento
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
                        text = "Almacenamiento",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    SyncRow(
                        icon = Icons.Outlined.Lock,
                        title = "Bóveda offline",
                        subtitle = "Incidencias cifradas en el dispositivo",
                        iconTint = SuccessColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cerrar sesión: cierra la app por completo (finishAffinity en la Activity)
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor,
                    contentColor = OnPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(width = 8.dp, height = 1.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileStatCell(
    icon: ImageVector,
    value: String,
    label: String
) {
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
            fontSize = 16.sp,
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
private fun CredentialRow(label: String, value: String) {
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
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

@Composable
private fun SyncRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = OnSurfaceVariant
            )
        }
    }
}