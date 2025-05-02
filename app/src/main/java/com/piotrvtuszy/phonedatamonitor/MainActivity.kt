package com.piotrvtuszy.phonedatamonitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.LocationServices
import com.piotrvtuszy.phonedatamonitor.adbanner.AdBanner
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this)

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
            }
        }

        requestLocationPermission()

        setContent { DashboardScreen() }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

@Composable
fun DashboardScreen() {
    val ctx = LocalContext.current

    var battPct     by remember { mutableStateOf(0) }
    var battTemp    by remember { mutableStateOf(0f) }
    var ramInfo     by remember { mutableStateOf("") }
    var storageInfo by remember { mutableStateOf("") }
    var netType     by remember { mutableStateOf("") }
    var androidVer  by remember { mutableStateOf("") }
    var model       by remember { mutableStateOf("") }
    var cpuModel    by remember { mutableStateOf("") }
    var cores       by remember { mutableStateOf(0) }
    var isCharging  by remember { mutableStateOf(false) }
    var isBatterySaverOn by remember { mutableStateOf(false) }
    var isBluetoothOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        androidVer = "Android ${Build.VERSION.RELEASE}"

        cpuModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Build.SOC_MODEL
        else
            Build.HARDWARE

        cores = Runtime.getRuntime().availableProcessors()

        while (true) {
            val (rUsed, rTotal) = getRamInfo(ctx)
            ramInfo = "${bytesToGb(rUsed)} / ${bytesToGb(rTotal)}"

            val (sUsed, sTotal) = getStorageInfo()
            storageInfo = "${bytesToGb(sUsed)} / ${bytesToGb(sTotal)}"

            val (p, t, c) = getBatteryInfo(ctx)
            battPct  = p
            battTemp = t
            isCharging = c

            netType = getNetworkType(ctx)

            val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatterySaverOn = powerManager.isPowerSaveMode

            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            isBluetoothOn = bluetoothAdapter?.isEnabled == true

            delay(5000)
        }
    }

    val tiles = listOf(
        TileData(Icons.Filled.BatteryFull, "Battery", "$battPct %\nSaver: ${if (isBatterySaverOn) "ON" else "OFF"}\nCharging: ${if (isCharging) "Yes" else "No"}", getBatteryColor(battPct)),
        TileData(Icons.Filled.BatteryFull, "Temperature", "${battTemp} °C", getTempColor(battTemp)),
        TileData(Icons.Filled.Memory, "RAM", ramInfo),
        TileData(Icons.Filled.Storage, "Storage", storageInfo),
        TileData(Icons.Filled.NetworkWifi, "Network", "$netType\nBluetooth: ${if (isBluetoothOn) "ON" else "OFF"}"),
        TileData(Icons.Filled.Android, "Device", "$model\n$androidVer\n$cpuModel ($cores cores)"),
        TileData(Icons.Filled.Security, "Safety", "Tap to share\nlocation", Color(0xFF00E5FF))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "📊 Phone Data Monitor",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tiles) { tile ->
                    TileCard(tile, ctx)
                }
            }
        }

        AdBanner(
            context = ctx,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        )
    }
}

@Composable
fun TileCard(tile: TileData, ctx: Context) {
    Card(
        backgroundColor = Color(0xFF1B1F1B),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                if (tile.label == "Safety") {
                    shareLocationSecure(ctx)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = tile.color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = tile.label, color = Color.Gray, fontSize = 14.sp)
            Text(
                text = tile.value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class TileData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String,
    val color: Color = Color(0xFF00C853)
)

fun shareLocationSecure(context: Context) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        Toast.makeText(context, "GPS is disabled. Please enable it.", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return
    }

    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Location permission not granted.", Toast.LENGTH_SHORT).show()
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val link = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "My current location: $link")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
            } else {
                Toast.makeText(context, "Unable to get current location.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve location.", Toast.LENGTH_SHORT).show()
        }
}

private fun getRamInfo(ctx: Context): Pair<Long, Long> {
    val mi = ActivityManager.MemoryInfo()
    (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val used = mi.totalMem - mi.availMem
    return used to mi.totalMem
}

private fun getStorageInfo(): Pair<Long, Long> {
    val fs = StatFs(Environment.getDataDirectory().path)
    val used = fs.totalBytes - fs.availableBytes
    return used to fs.totalBytes
}

private fun getBatteryInfo(ctx: Context): Triple<Int, Float, Boolean> {
    val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct    = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val temp   = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.div(10f) ?: 0f
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return Triple(pct, temp, charging)
}

private fun getNetworkType(ctx: Context): String {
    val cm   = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net  = cm.activeNetwork ?: return "none"
    val caps = cm.getNetworkCapabilities(net) ?: return "none"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE/5G"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "other"
    }
}

private fun bytesToGb(bytes: Long): String =
    "%.1f GB".format(bytes / 1_073_741_824.0)

private fun getBatteryColor(pct: Int) = when {
    pct >= 50 -> Color(0xFF00C853)
    pct >= 20 -> Color.Yellow
    else -> Color.Red
}

private fun getTempColor(temp: Float) = when {
    temp < 35 -> Color(0xFF00C853)
    temp < 42 -> Color.Yellow
    else -> Color.Red
}
