package com.example.dam_t1_evaluacion_practica.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_t1_evaluacion_practica.R
import com.example.dam_t1_evaluacion_practica.ui.theme.IndustrialPrimaryContainer
import com.example.dam_t1_evaluacion_practica.ui.theme.IndustrialTextPrimary
import com.example.dam_t1_evaluacion_practica.ui.theme.IndustrialTextSecondary
import com.example.dam_t1_evaluacion_practica.ui.theme.SafetyAmber
import com.example.dam_t1_evaluacion_practica.ui.theme.SafetyAmberDark
import com.example.dam_t1_evaluacion_practica.ui.theme.SurfaceBright
import com.example.dam_t1_evaluacion_practica.ui.theme.SurfaceContainer
import com.example.dam_t1_evaluacion_practica.ui.theme.SurfaceContainerHigh
import com.example.dam_t1_evaluacion_practica.ui.theme.SurfaceContainerHighest
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val statusMessages = remember {
        listOf(
            "Sincronizando catálogos de normas y firmas offline...",
            "Verificando hashes criptográficos de inspectores...",
            "Cargando matriz de riesgos para Monterrey...",
            "Sistema listo para auditoría en campo."
        )
    }

    val progressTargets = remember { listOf(0.74f, 0.88f, 0.96f, 1.0f) }
    var currentStep by remember { mutableIntStateOf(0) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTargets[currentStep],
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "SplashProgressBar"
    )

    // Pulsing transition for the offline dot and the emblem ring
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LaunchedEffect(Unit) {
        delay(1400)
        currentStep = 1
        delay(1400)
        currentStep = 2
        delay(1400)
        currentStep = 3
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBright)
    ) {
        // Subtle blueprint grid (dashed lines every 28.dp)
        val gridColor = SurfaceContainerHighest.copy(alpha = 0.4f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 28.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                    pathEffect = dash
                )
                x += spacing
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = dash
                )
                y += spacing
            }
        }

        // Ambient blurred glow orbs
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .padding(top = 20.dp)
                .blur(70.dp)
                .background(SafetyAmber.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .offset(x = (-90).dp, y = 110.dp)
                .blur(80.dp)
                .background(SurfaceContainer.copy(alpha = 0.55f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(384.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = (-30).dp)
                .blur(90.dp)
                .background(SurfaceContainerHighest.copy(alpha = 0.4f), CircleShape)
        )

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center block: pill, emblem, brand, chips (now vertically centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ready pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .shadow(3.dp, RoundedCornerShape(50.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = SafetyAmberDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOM-STPS READY",
                        color = IndustrialTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDITORÍA V3",
                        color = IndustrialTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emblem with pulsing concentric ring
                Box(
                    modifier = Modifier.size(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing outer ring
                    Box(
                        modifier = Modifier
                            .size(176.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh.copy(alpha = 0.6f), CircleShape)
                            .alpha(pulseAlpha)
                    )
                    // Base white card
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner gradient container
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(SurfaceContainer, SurfaceContainerHigh)
                                    )
                                )
                        ) {
                            // Top hazard strip
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(SafetyAmber, SafetyAmberDark)
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo1),
                                    contentDescription = "Logo SegurInspección",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                    // STPS badge overhanging the bottom-right corner
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(SafetyAmber)
                            .shadow(3.dp, RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = IndustrialPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STPS",
                            color = IndustrialPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Segur",
                        color = IndustrialPrimaryContainer,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Inspección",
                        color = SafetyAmberDark,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Seguridad en campo, decisiones en tiempo real",
                    color = IndustrialTextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // NOM chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NOM-017-STPS", "NOM-004-STPS", "NOM-002-STPS").forEach { chip ->
                        Text(
                            text = chip,
                            color = IndustrialTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Bottom block: sync, footer
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sync card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SafetyAmber,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessages[currentStep],
                                color = IndustrialTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${(progressTargets[currentStep] * 100).toInt()}%",
                            color = SafetyAmberDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gradient progress bar (amber → amber-dark)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(SurfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(SafetyAmber, SafetyAmberDark)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = SafetyAmberDark
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Base SQLite Cifrada AES-256",
                            color = IndustrialTextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Buffer: 0 pendientes",
                            color = IndustrialTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Version footer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "v2.4.0 • ENTERPRISE INDUSTRIAL EDITION",
                        color = IndustrialTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = SafetyAmberDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cumplimiento Normativo STPS • Auditoría Digital Segura",
                            color = IndustrialTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}