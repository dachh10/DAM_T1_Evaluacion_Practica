package com.example.dam_t1_evaluacion_practica.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dam_t1_evaluacion_practica.R
import com.example.dam_t1_evaluacion_practica.ui.viewmodel.InspectorProfile
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeDetailBottomSheet
import com.example.dam_t1_evaluacion_practica.ui.components.NormativeBadgesRow
import com.example.dam_t1_evaluacion_practica.domain.model.NormativesCatalog
import androidx.compose.material3.ExperimentalMaterial3Api
private val NavyPrimary        = Color(0xFF0B192C)

/**
 * Devuelve la lista de normas oficiales vinculadas según la zona seleccionada.
 */
private fun normsForZone(zone: ZoneOption): List<Normativ> = when (zone.id) {
    "maquinaria" -> listOf(
        NormativesCatalog.NOM_017_STPS_2008,
        NormativesCatalog.NOM_009_STPS_2011,
        NormativesCatalog.NOM_029_STPS_2011
    )
    "almacen" -> listOf(
        NormativesCatalog.NOM_115_STPS_2009,
        NormativesCatalog.NOM_002_STPS_2010
    )
    "taller" -> listOf(
        NormativesCatalog.NOM_004_STPS_1999,
        NormativesCatalog.NOM_005_STPS_1998
    )
    "maniobras" -> listOf(
        NormativesCatalog.NOM_009_STPS_2011,
        NormativesCatalog.NOM_002_STPS_2010
    )
    "silos" -> listOf(
        NormativesCatalog.NOM_029_STPS_2011,
        NormativesCatalog.NOM_001_STPS_2008
    )
    else -> emptyList()
}
private val AmberSecondary     = Color(0xFFF59E0B)
private val AmberContainer     = Color(0xFFFEA619)
private val OnAmberContainer   = Color(0xFF684000)
private val SurfaceBg          = Color(0xFFFAF8FF)
private val SurfaceLowest      = Color(0xFFFFFFFF)
private val SurfaceLow         = Color(0xFFF2F3FF)
private val SurfaceContainer   = Color(0xFFEAEDFF)
private val SurfaceHigh        = Color(0xFFE2E7FF)
private val OnSurface          = Color(0xFF131B2E)
private val OnSurfaceVariant   = Color(0xFF44474C)
private val OnPrimary          = Color(0xFFFFFFFF)

data class ZoneOption(
    val id: String,
    val name: String,
    val icon: ImageVector
)

enum class AuditType(val label: String, val icon: ImageVector) {
    ORDINARIA("Ordinaria", Icons.Outlined.EventRepeat),
    EXTRAORDINARIA("Extraord.", Icons.Outlined.ElectricBolt),
    POST_INCIDENTE("Post-Inc.", Icons.Outlined.ReportProblem)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInspectionScreen(
    onBack: () -> Unit = {},
    onProceedToInspection: (companyName: String, zoneName: String, auditType: AuditType) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    // Estado del permiso de ubicación
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Estado del hardware GPS (encendido/apagado)
    var isGpsEnabled by remember {
        mutableStateOf(checkIsGpsEnabled(context))
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationAccuracy by remember { mutableStateOf<Float?>(null) }
    var locationTitle by remember { mutableStateOf("Buscando ubicación...") }
    var locationSubtitle by remember { mutableStateOf("Obteniendo dirección por GPS...") }

    // Launcher para pedir encender GPS
    val gpsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGpsEnabled = true
            fetchDeviceLocation(context) { loc ->
                currentLocation = LatLng(loc.latitude, loc.longitude)
                locationAccuracy = loc.accuracy
            }
        }
    }

    // Launcher para solicitar permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            isGpsEnabled = checkIsGpsEnabled(context)
            if (!isGpsEnabled) {
                promptEnableGps(context) { intentSender ->
                    gpsResolutionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            } else {
                fetchDeviceLocation(context) { loc ->
                    currentLocation = LatLng(loc.latitude, loc.longitude)
                    locationAccuracy = loc.accuracy
                }
            }
        }
    }

    // Al entrar a la pantalla: pedir permisos y encender GPS
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            isGpsEnabled = checkIsGpsEnabled(context)
            if (!isGpsEnabled) {
                promptEnableGps(context) { intentSender ->
                    gpsResolutionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            } else {
                fetchDeviceLocation(context) { loc ->
                    currentLocation = LatLng(loc.latitude, loc.longitude)
                    locationAccuracy = loc.accuracy
                }
            }
        }
    }

    // Geocodificación inversa cuando se obtiene la ubicación real
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null) {
            val (mainTitle, subTitle) = reverseGeocode(context, loc.latitude, loc.longitude)
            locationTitle = mainTitle
            locationSubtitle = subTitle
        }
    }

    val zones = remember {
        listOf(
            ZoneOption("maquinaria", "Área de Maquinaria y Calderas", Icons.Outlined.PrecisionManufacturing),
            ZoneOption("almacen", "Almacén Central", Icons.Outlined.Warehouse),
            ZoneOption("taller", "Taller de Mantenimiento", Icons.Outlined.Build),
            ZoneOption("maniobras", "Patio de Maniobras", Icons.Outlined.LocalShipping),
            ZoneOption("silos", "Silos de Carga", Icons.Outlined.Inventory2)
        )
    }

    var selectedZone by remember { mutableStateOf(zones[0]) }
    var selectedAuditType by remember { mutableStateOf(AuditType.ORDINARIA) }
    var selectedFacility by remember { mutableStateOf("Ternium Planta Monterrey Norte") }
    var expandedFacilityDropdown by remember { mutableStateOf(false) }
    var selectedNorm by remember { mutableStateOf<Normativ?>(null) }
    // Bottom sheet para mostrar detalle de la norma seleccionada
    selectedNorm?.let { norm ->
        NormativeDetailBottomSheet(
            normativ = norm,
            onDismissRequest = { selectedNorm = null }
        )
    }
    // Duplicate block removed

    val facilities = listOf(
        "Ternium Planta Monterrey Norte",
        "Metalsa Apodaca Planta Estampado",
        "Nemak Centro Industrial García"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NewInspectionTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tarjeta 1: Google Maps y GPS en tiempo real
                GpsMapCard(
                    hasLocationPermission = hasLocationPermission,
                    isGpsEnabled = isGpsEnabled,
                    currentLocation = currentLocation,
                    locationAccuracy = locationAccuracy,
                    locationTitle = locationTitle,
                    locationSubtitle = locationSubtitle,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onRequestEnableGps = {
                        promptEnableGps(context) { intentSender ->
                            gpsResolutionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                    }
                )

                // Tarjeta 2: Configuración de Sitio e Instalación con Fecha Real
                FacilityAndZoneCard(
                    facilities = facilities,
                    selectedFacility = selectedFacility,
                    onSelectFacility = { selectedFacility = it },
                    expandedFacilityDropdown = expandedFacilityDropdown,
                    onToggleFacilityDropdown = { expandedFacilityDropdown = it },
                    zones = zones,
                    selectedZone = selectedZone,
                    onSelectZone = { selectedZone = it },
                    selectedAuditType = selectedAuditType,
                    onSelectAuditType = { selectedAuditType = it }
                )

                // Tarjeta 3: Normativas Oficiales Vinculadas
                        val allNorms = listOf(
            NormativesCatalog.NOM_017_STPS_2008,
            NormativesCatalog.NOM_009_STPS_2011,
            NormativesCatalog.NOM_029_STPS_2011,
            NormativesCatalog.NOM_115_STPS_2009,
            NormativesCatalog.NOM_002_STPS_2010,
            NormativesCatalog.NOM_004_STPS_1999,
            NormativesCatalog.NOM_005_STPS_1998,
            NormativesCatalog.NOM_001_STPS_2008
        )
        LinkedNormsCard(
            primaryNorm = allNorms.firstOrNull(),
            secondaryNorms = allNorms.drop(1),
            onNormClick = { selectedNorm = it }
        )
            }
        }

        // Botón Fijo Inferior
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = SurfaceBg.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = { onProceedToInspection(selectedFacility, selectedZone.name, selectedAuditType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberContainer,
                        contentColor = OnAmberContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comenzar Inspección en Sitio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun checkIsGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
}

private fun promptEnableGps(context: Context, onResolutionRequired: (IntentSender) -> Unit) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
    val client = LocationServices.getSettingsClient(context)
    val task = client.checkLocationSettings(builder.build())

    task.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                onResolutionRequired(exception.resolution.intentSender)
            } catch (_: IntentSender.SendIntentException) {}
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onLocationResult: (Location) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationResult(location)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) onLocationResult(lastLoc)
                    }
                }
            }
    } catch (_: Exception) {}
}

private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("es", "MX"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result = Pair("Ubicación Detectada", "Coordenadas: $lat, $lon")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val title = addr.featureName ?: addr.subLocality ?: addr.locality ?: "Zona de Trabajo"
                    val subtitle = addr.getAddressLine(0) ?: "${addr.locality ?: ""}, ${addr.adminArea ?: ""}"
                    result = Pair(title, subtitle)
                }
                result
            } else {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val title = addr.featureName ?: addr.subLocality ?: addr.locality ?: "Zona de Trabajo"
                    val subtitle = addr.getAddressLine(0) ?: "${addr.locality ?: ""}, ${addr.adminArea ?: ""}"
                    Pair(title, subtitle)
                } else {
                    Pair("Zona Detectada", "Coordenadas: $lat, $lon")
                }
            }
        } catch (_: Exception) {
            Pair("Zona Detectada en Campo", "Coordenadas GPS en tiempo real")
        }
    }
}

@Composable
private fun NewInspectionTopBar(onBack: () -> Unit) {
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
                        text = "Nueva Inspección",
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
private fun GpsMapCard(
    hasLocationPermission: Boolean,
    isGpsEnabled: Boolean,
    currentLocation: LatLng?,
    locationAccuracy: Float?,
    locationTitle: String,
    locationSubtitle: String,
    onRequestPermission: () -> Unit,
    onRequestEnableGps: () -> Unit
) {
    val defaultPos = LatLng(25.6866, -100.3161)
    val displayPos = currentLocation ?: defaultPos

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(displayPos, 16f)
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(currentLocation, 16.5f),
                durationMs = 1000
            )
        }
    }

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
            // Header del Estado GPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isActive = hasLocationPermission && isGpsEnabled
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (isActive) AmberContainer.copy(alpha = 0.2f)
                            else Color(0xFFFFDAD6)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isActive) AmberSecondary else Color(0xFFBA1A1A))
                    )
                    Text(
                        text = if (!hasLocationPermission) {
                            "Permiso de GPS requerido"
                        } else if (!isGpsEnabled) {
                            "GPS apagado en el dispositivo"
                        } else {
                            val accText = locationAccuracy?.let { " (±${it.toInt()}m)" } ?: " (±2.5m)"
                            "GPS Activo • Tiempo Real$accText"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Color(0xFF653E00) else Color(0xFF93000A)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = if (isActive) AmberSecondary else Color(0xFFBA1A1A),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Nombre y dirección leídos de la ubicación real
            Column {
                Text(
                    text = if (currentLocation != null) locationTitle else "Parque Industrial Monterrey Norte",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (currentLocation != null) locationSubtitle else "Vía FFCC Km 14, Apodaca, N.L.",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Contenedor del Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (hasLocationPermission && isGpsEnabled) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = true,
                            compassEnabled = true,
                            rotationGesturesEnabled = true,
                            scrollGesturesEnabled = true,
                            tiltGesturesEnabled = false,
                            zoomGesturesEnabled = true
                        ),
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = displayPos),
                            title = "Zona de Inspección",
                            snippet = locationTitle
                        )
                    }
                } else if (!hasLocationPermission) {
                    // Pedir permiso
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceLow)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationDisabled,
                            contentDescription = null,
                            tint = AmberSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Se requiere acceso al GPS para fijar la zona de inspección",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = OnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Permitir GPS", fontSize = 13.sp)
                        }
                    }
                } else {
                    // GPS apagado en el sistema
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceLow)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GpsOff,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "El GPS de tu dispositivo está apagado",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onRequestEnableGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberSecondary,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Encender GPS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Coordenadas en tiempo real (sin Geocerca OK)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Coordenadas Verificadas",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                        val latStr = String.format(Locale.US, "%.4f° N", displayPos.latitude)
                        val lonStr = String.format(Locale.US, "%.4f° W", Math.abs(displayPos.longitude))
                        Text(
                            text = "$latStr, $lonStr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = OnSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = AmberSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ubicación en vivo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityAndZoneCard(
    facilities: List<String>,
    selectedFacility: String,
    onSelectFacility: (String) -> Unit,
    expandedFacilityDropdown: Boolean,
    onToggleFacilityDropdown: (Boolean) -> Unit,
    zones: List<ZoneOption>,
    selectedZone: ZoneOption,
    onSelectZone: (ZoneOption) -> Unit,
    selectedAuditType: AuditType,
    onSelectAuditType: (AuditType) -> Unit
) {
    val currentDateText = remember {
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "MX"))
        val raw = formatter.format(Date())
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
    }

    val currentTimeText = remember {
        val formatter = SimpleDateFormat("HH:mm 'hrs'", Locale("es", "MX"))
        formatter.format(Date())
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Selector Empresa / Instalación
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Domain,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Empresa / Instalación",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleFacilityDropdown(true) },
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedFacility,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Outlined.UnfoldMore,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedFacilityDropdown,
                        onDismissRequest = { onToggleFacilityDropdown(false) }
                    ) {
                        facilities.forEach { facility ->
                            DropdownMenuItem(
                                text = { Text(facility, fontSize = 14.sp) },
                                onClick = {
                                    onSelectFacility(facility)
                                    onToggleFacilityDropdown(false)
                                }
                            )
                        }
                    }
                }
            }

            // 2. Área o Zona de Trabajo Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Área o Zona de Trabajo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                    }
                    Text(
                        text = "${zones.size} Detectadas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberSecondary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    zones.forEach { zone ->
                        val isSelected = zone.id == selectedZone.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectZone(zone) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NavyPrimary else SurfaceLow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = zone.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) OnPrimary else OnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = zone.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) OnPrimary else OnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = AmberSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tipo de Auditoría (Segmented Buttons)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FactCheck,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tipo de Auditoría",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuditType.entries.forEach { type ->
                        val isSelected = type == selectedAuditType
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectAuditType(type) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SurfaceLowest else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) OnSurface else OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = type.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OnSurface else OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Inspector Asignado & Fecha Actual Real
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = InspectorProfile.INITIALS,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = InspectorProfile.NAME,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = "Céd. ${InspectorProfile.CEDULA} • ${InspectorProfile.LICENSE}",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = AmberSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Fila con fecha y hora dinámicas en tiempo real
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentDateText,
                                fontSize = 12.sp,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceLowest)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AmberSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentTimeText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedNormsCard(
    primaryNorm: Normativ?,
    secondaryNorms: List<Normativ> = emptyList(),
    onNormClick: (Normativ) -> Unit = {}
) {
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
                        imageVector = Icons.Outlined.Gavel,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Normativas Oficiales Vinculadas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val total = listOfNotNull(primaryNorm) + secondaryNorms
                Text(
                    text = "${total.size} NOMs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant
                )
                }
            }

            Text(
                text = "Detectadas automáticamente según maquinaria y factores de riesgo en la zona seleccionada:",
                fontSize = 13.sp,
                color = OnSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Placeholder for dynamic norm rows; actual rows will be generated below
                primaryNorm?.let {
                    NormativeBadgesRow(
                        primaryNorm = it,
                        secondaryNorms = secondaryNorms,
                        onNormClick = onNormClick
                    )
                }
            }
        }
    }
}

@Composable
private fun NormRowItem(normCode: String, description: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AmberSecondary)
                )
                Column {
                    Text(
                        text = normCode,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
