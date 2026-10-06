package com.example.dam_t1_evaluacion_practica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavyPrimary = Color(0xFF0B192C)
private val SurfaceBg = Color(0xFFFAF8FF)
private val OnSurfaceVariant = Color(0xFF44474C)

enum class MainTab(val label: String) {
    INSPECCION("Inicio"),
    INSPECCIONES("Inspecciones"),
    PERFIL("Perfil")
}

/**
 * Fixed 3-tab bottom bar used by Inspecciones and Profile. The active tab shows a navy
 * pill over a translucent background. Callbacks decide the navigation per host screen.
 */
@Composable
fun BottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceBg.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .navigationBarsPadding()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                MainNavItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun MainNavItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    val icon: ImageVector = when (tab) {
        MainTab.INSPECCION -> if (selected) Icons.AutoMirrored.Filled.FactCheck else Icons.AutoMirrored.Outlined.FactCheck
        MainTab.INSPECCIONES -> if (selected) Icons.Filled.History else Icons.Outlined.History
        MainTab.PERFIL -> if (selected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(if (selected) NavyPrimary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint = if (selected) Color.White else OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = tab.label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) NavyPrimary else OnSurfaceVariant
        )
    }
}