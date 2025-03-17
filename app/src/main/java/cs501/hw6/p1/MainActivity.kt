package cs501.hw6.p1

import android.Manifest.permission
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.abs



const val THRESHOLD = 0.0035

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
            }
        }
        setContent {
            MainScreen(fusedLocationClient, locationRequest, locationCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun MainScreen(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            } catch (e: SecurityException) {
                Log.e("RequestPermissions", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasLocationPermission) {
        MapScreen(fusedLocationClient)
    }
}

@Composable
fun MapScreen(fusedLocationClient: FusedLocationProviderClient) {

    var markers by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val cameraPositionState = rememberCameraPositionState()

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true
            )
        )
    }

    val properties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true
            )
        )
    }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val initialPosition = LatLng(it.latitude, it.longitude)
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(
                                initialPosition,
                                15f
                            )
                        )
                    }
                }
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                if(!alreadyMarked(latLng, markers)) {
                    markers = markers + latLng
                }else {
                    val index = findMarkerIndex(latLng, markers)
                    if(index != -1) {
                        markers = markers.toMutableList().also { it.removeAt(index) }
                    }
                }
            }
        ) {

            markers.forEach { markerLocation ->
                Marker(
                    state = MarkerState(position = markerLocation),
                    title = "Custom Marker",
                    snippet = "User added marker",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

        }
    }
}

fun alreadyMarked(latLng: LatLng, markers: List<LatLng>): Boolean {
    for(marker in markers) {
        if(abs(marker.latitude - latLng.latitude) <= THRESHOLD && abs(marker.longitude - latLng.longitude) <= THRESHOLD) {
            return true
        }
    }
    return false
}

fun findMarkerIndex(latLng: LatLng, markers: List<LatLng>): Int {
    for(marker in markers) {
        if(abs(marker.latitude - latLng.latitude) <= THRESHOLD && abs(marker.longitude - latLng.longitude) <= THRESHOLD) {
            return markers.indexOf(marker)
        }
    }
    return -1
}