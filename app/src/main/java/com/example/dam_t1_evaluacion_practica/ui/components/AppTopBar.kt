package com.example.dam_t1_evaluacion_practica.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile

private val NavyPrimary = Color(0xFF0B192C)
private val AmberSecondary = Color(0xFFF59E0B)
private val AmberContainer = Color(0xFFFEA619)
private val SurfaceBg = Color(0xFFFAF8FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF44474C)
private val OnPrimary = Color(0xFFFFFFFF)

/**
 * Shared top bar used by every main tab screen. Applies the status bar inset first so the
 * content never renders under the camera cutout, then shows the brand or a title/subtitle,
 * the notification bell with an amber badge dot, and the tappable profile initials circle.
 */
@Composable
fun AppTopBar(
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceBg.copy(alpha = 0.95f),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo1),
                        contentDescription = "Logo",
                        modifier = Modifier.height(32.dp)
                    )
                    if (title != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SegurInspeccion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AmberSecondary)
                                )
                                Text(
                                    text = "En linea - Sincronizado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificaciones",
                            tint = OnSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AmberContainer)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = InspectorProfile.INITIALS,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                }
            }
        }
    }
}