package com.example.ui.scene3d

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DefaultGlobalCities
import com.example.model.SavedLocation
import com.example.model.TempUnit
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class ProjectedCityPin(
    val location: SavedLocation,
    val screenX: Float,
    val screenY: Float,
    val depthZ: Float
)

/**
 * Simplified continental landmass outlines in (lat, lon) coordinates for 3D spherical projection.
 */
private val ContinentalPolygons: List<List<Pair<Double, Double>>> = listOf(
    // North America
    listOf(70.0 to -160.0, 72.0 to -130.0, 60.0 to -65.0, 45.0 to -60.0, 25.0 to -80.0, 18.0 to -95.0, 10.0 to -84.0, 22.0 to -106.0, 35.0 to -122.0, 58.0 to -138.0),
    // South America
    listOf(10.0 to -75.0, 5.0 to -52.0, -6.0 to -35.0, -23.0 to -42.0, -38.0 to -58.0, -54.0 to -68.0, -42.0 to -74.0, -18.0 to -71.0, -4.0 to -81.0),
    // Eurasia
    listOf(36.0 to -9.0, 44.0 to -8.0, 58.0 to 8.0, 70.0 to 28.0, 72.0 to 90.0, 66.0 to 170.0, 52.0 to 142.0, 35.0 to 139.0, 22.0 to 114.0, 8.0 to 104.0, 15.0 to 96.0, 22.0 to 88.0, 8.0 to 77.0, 25.0 to 67.0, 14.0 to 48.0, 32.0 to 34.0, 38.0 to 24.0),
    // Africa
    listOf(35.0 to -6.0, 37.0 to 10.0, 31.0 to 32.0, 12.0 to 51.0, -2.0 to 41.0, -26.0 to 33.0, -34.0 to 18.0, -16.0 to 12.0, 4.0 to 9.0, 5.0 to -8.0, 15.0 to -17.0),
    // Australia
    listOf(-12.0 to 131.0, -12.0 to 142.0, -26.0 to 153.0, -38.0 to 146.0, -34.0 to 135.0, -34.0 to 115.0, -22.0 to 114.0)
)

@Composable
fun Interactive3DGlobeScreen(
    savedLocations: List<SavedLocation>,
    activeLocation: SavedLocation,
    tempUnit: TempUnit,
    onSelectAndLoadCity: (SavedLocation) -> Unit,
    onSaveCityToFavorites: (SavedLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    // Combine user's saved locations with global hubs so the 3D globe has rich regional coverage
    val allGlobeCities = remember(savedLocations) {
        val map = LinkedHashMap<String, SavedLocation>()
        savedLocations.forEach { map[it.cityName.lowercase()] = it }
        DefaultGlobalCities.hubs.forEach { hub ->
            if (!map.containsKey(hub.cityName.lowercase())) {
                map[hub.cityName.lowercase()] = hub
            }
        }
        map.values.toList()
    }

    var selectedGlobeCity by remember(activeLocation) { mutableStateOf(activeLocation) }
    // Center initial rotation on the active city's longitude/latitude
    var globeYawDeg by remember { mutableFloatStateOf((-activeLocation.longitude).toFloat()) }
    var globePitchDeg by remember { mutableFloatStateOf((activeLocation.latitude * 0.45).toFloat()) }
    var globeZoom by remember { mutableFloatStateOf(1.0f) }
    var autoRotate by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "globe_orbit")
    val orbitTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 64_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_tick"
    )

    val effectiveYawDeg = if (autoRotate) (globeYawDeg + orbitTick * 0.25f) % 360f else globeYawDeg
    val visiblePins = remember { mutableStateOf<List<ProjectedCityPin>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("globe_screen_root")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D EARTH EXPLORER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "Global Atmospheric Sphere",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (autoRotate) Color(0xFF0284C7).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { autoRotate = !autoRotate }
                        .testTag("globe_autorotate_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Toggle Orbit Rotation",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (autoRotate) "Orbiting" else "Manual",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive 3D Globe Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C2444),
                            Color(0xFF050B16),
                            Color(0xFF020409)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { autoRotate = false }
                    ) { change, dragAmount ->
                        change.consume()
                        globeYawDeg = (globeYawDeg + dragAmount.x * 0.35f) % 360f
                        globePitchDeg = (globePitchDeg - dragAmount.y * 0.25f).coerceIn(-65f, 65f)
                    }
                }
                .pointerInput(allGlobeCities) {
                    detectTapGestures { tapOffset ->
                        val hit = visiblePins.value.minByOrNull { pin ->
                            hypot(pin.screenX - tapOffset.x, pin.screenY - tapOffset.y)
                        }
                        if (hit != null && hypot(hit.screenX - tapOffset.x, hit.screenY - tapOffset.y) <= 68f) {
                            autoRotate = false
                            selectedGlobeCity = hit.location
                        }
                    }
                }
                .testTag("interactive_3d_globe_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val baseRadius = minOf(size.width, size.height) * 0.38f * globeZoom

                // 1. Outer Atmospheric Limb Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.42f),
                            Color(0xFF0284C7).copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = baseRadius * 1.28f
                    ),
                    radius = baseRadius * 1.28f,
                    center = Offset(cx, cy)
                )

                // 2. 3D Ocean Sphere with Solar Day/Night Terminator Shading
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0369A1),
                            Color(0xFF0C4A6E),
                            Color(0xFF081426)
                        ),
                        center = Offset(cx - baseRadius * 0.28f, cy - baseRadius * 0.28f),
                        radius = baseRadius * 1.35f
                    ),
                    radius = baseRadius,
                    center = Offset(cx, cy)
                )

                val yawRad = effectiveYawDeg * (PI / 180.0)
                val pitchRad = globePitchDeg * (PI / 180.0)

                // 3. 3D Latitude & Longitude Graticules (Front-Hemisphere Culling)
                for (lat in -60..60 step 30) {
                    val path = Path()
                    var penDown = false
                    for (lon in -180..180 step 8) {
                        val pt = projectLatLon3D(lat.toDouble(), lon.toDouble(), yawRad, pitchRad, cx, cy, baseRadius)
                        if (pt.third > 0f) {
                            if (!penDown) {
                                path.moveTo(pt.first, pt.second)
                                penDown = true
                            } else {
                                path.lineTo(pt.first, pt.second)
                            }
                        } else {
                            penDown = false
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF7DD3FC).copy(alpha = if (lat == 0) 0.32f else 0.14f),
                        style = Stroke(width = if (lat == 0) 2.2f else 1.1f)
                    )
                }

                for (lon in -150..180 step 30) {
                    val path = Path()
                    var penDown = false
                    for (lat in -80..80 step 8) {
                        val pt = projectLatLon3D(lat.toDouble(), lon.toDouble(), yawRad, pitchRad, cx, cy, baseRadius)
                        if (pt.third > 0f) {
                            if (!penDown) {
                                path.moveTo(pt.first, pt.second)
                                penDown = true
                            } else {
                                path.lineTo(pt.first, pt.second)
                            }
                        } else {
                            penDown = false
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF7DD3FC).copy(alpha = 0.14f),
                        style = Stroke(width = 1.1f)
                    )
                }

                // 4. 3D Projected Continental Landmasses
                ContinentalPolygons.forEach { polygon ->
                    val polyPath = Path()
                    var visibleVertices = 0
                    polygon.forEach { (lat, lon) ->
                        val pt = projectLatLon3D(lat, lon, yawRad, pitchRad, cx, cy, baseRadius)
                        if (pt.third > -0.05f) {
                            if (visibleVertices == 0) {
                                polyPath.moveTo(pt.first, pt.second)
                            } else {
                                polyPath.lineTo(pt.first, pt.second)
                            }
                            visibleVertices++
                        }
                    }
                    if (visibleVertices >= 3) {
                        polyPath.close()
                        drawPath(
                            path = polyPath,
                            color = Color(0xFF10B981).copy(alpha = 0.26f)
                        )
                        drawPath(
                            path = polyPath,
                            color = Color(0xFF34D399).copy(alpha = 0.55f),
                            style = Stroke(width = 2.0f, cap = StrokeCap.Round)
                        )
                    }
                }

                // 5. Project Interactive Global City Weather Pins
                val currentPins = mutableListOf<ProjectedCityPin>()
                allGlobeCities.forEach { city ->
                    val pt = projectLatLon3D(
                        city.latitude,
                        city.longitude,
                        yawRad,
                        pitchRad,
                        cx,
                        cy,
                        baseRadius
                    )
                    if (pt.third > 0.05f) {
                        currentPins.add(ProjectedCityPin(city, pt.first, pt.second, pt.third))
                        val isSelected = city.cityName.equals(selectedGlobeCity.cityName, ignoreCase = true)
                        val pinColor = Color(
                            (city.cachedSceneType ?: WeatherSceneType.SUNNY).accentColorHex
                        )

                        if (isSelected) {
                            drawCircle(
                                color = pinColor.copy(alpha = 0.35f),
                                radius = 22f,
                                center = Offset(pt.first, pt.second)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 12f,
                                center = Offset(pt.first, pt.second),
                                style = Stroke(width = 2.5f)
                            )
                        }
                        drawCircle(
                            color = pinColor,
                            radius = if (isSelected) 7.5f else 5.5f,
                            center = Offset(pt.first, pt.second)
                        )
                    }
                }
                visiblePins.value = currentPins

                // 6. Specular 3D Atmospheric Rim Ring
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD).copy(alpha = 0.65f),
                            Color.Transparent,
                            Color(0xFF0284C7).copy(alpha = 0.35f)
                        )
                    ),
                    radius = baseRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5f)
                )
            }

            // Zoom Controls Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                IconButton(
                    onClick = { globeZoom = (globeZoom + 0.15f).coerceAtMost(1.45f) },
                    modifier = Modifier.testTag("globe_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in globe", tint = Color.White)
                }
                IconButton(
                    onClick = { globeZoom = (globeZoom - 0.15f).coerceAtLeast(0.75f) },
                    modifier = Modifier.testTag("globe_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom out globe", tint = Color.White)
                }
            }

            Text(
                text = "Drag to rotate 3D Earth · Tap any glowing city pin or chip below",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick City Selector Strip (rotates the 3D globe directly to the tapped city)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(allGlobeCities, key = { it.id }) { city ->
                val isSelected = city.cityName.equals(selectedGlobeCity.cityName, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            autoRotate = false
                            selectedGlobeCity = city
                            globeYawDeg = (-city.longitude).toFloat()
                            globePitchDeg = (city.latitude * 0.45).toFloat()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Color((city.cachedSceneType ?: WeatherSceneType.SUNNY).accentColorHex)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = city.cityName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        city.cachedTempC?.let { temp ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = WeatherFormatter.formatTemp(temp, tempUnit),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected City Inspection Card
        val isSaved = savedLocations.any { it.cityName.equals(selectedGlobeCity.cityName, ignoreCase = true) }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.11f),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedGlobeCity.cityName.uppercase()}, ${selectedGlobeCity.country.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = selectedGlobeCity.cachedTempC?.let {
                                WeatherFormatter.formatTemp(it, tempUnit)
                            } ?: "Live",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedGlobeCity.cachedCondition
                                ?: selectedGlobeCity.cachedSceneType?.title
                                ?: "Tap Open 3D Scene",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = "Coordinates: ${String.format("%.2f", selectedGlobeCity.latitude)}°, ${String.format("%.2f", selectedGlobeCity.longitude)}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSelectAndLoadCity(selectedGlobeCity) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF041322)
                        ),
                        modifier = Modifier.testTag("globe_open_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open 3D Weather")
                    }

                    if (!isSaved) {
                        OutlinedButton(
                            onClick = { onSaveCityToFavorites(selectedGlobeCity) },
                            modifier = Modifier.testTag("globe_save_city_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save City", color = Color.White)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved in Cities",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun projectLatLon3D(
    latDeg: Double,
    lonDeg: Double,
    yawRad: Double,
    pitchRad: Double,
    centerX: Float,
    centerY: Float,
    radius: Float
): Triple<Float, Float, Float> {
    val latRad = latDeg * (PI / 180.0)
    val lonRad = lonDeg * (PI / 180.0) + yawRad

    val x = cos(latRad) * sin(lonRad)
    val y = sin(latRad)
    val z = cos(latRad) * cos(lonRad)

    // Rotate around X axis by pitchRad
    val yRot = y * cos(pitchRad) - z * sin(pitchRad)
    val zRot = y * sin(pitchRad) + z * cos(pitchRad)

    val screenX = (centerX + x * radius).toFloat()
    val screenY = (centerY - yRot * radius).toFloat()
    return Triple(screenX, screenY, zRot.toFloat())
}
