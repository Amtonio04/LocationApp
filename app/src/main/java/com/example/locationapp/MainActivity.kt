package com.example.locationapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.locationapp.ui.theme.LocationAppTheme
import com.google.android.gms.location.*

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Registrar el lanzador de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar el callback para las actualizaciones de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Obtener la última ubicación del resultado
                val location = locationResult.lastLocation
                if (location != null) {
                    // Actualizar la UI con la nueva ubicación
                    getLocation = {
                        latitude = location.latitude.toString()
                        longitude = location.longitude.toString()
                        // Detener las actualizaciones una vez que obtengamos una ubicación válida
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }
                    getLocation()
                } else {
                    getLocation = {
                        errorMessage = "No se pudo obtener la ubicación"
                    }
                    getLocation()
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    getLocation = {
                        errorMessage = "Ubicación no disponible"
                    }
                    getLocation()
                }
            }
        }

        setContent {
            LocationAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun LocationScreen() {
        // Estados para almacenar la latitud, longitud y mensaje de error
        var latitude by remember { mutableStateOf("N/A") }
        var longitude by remember { mutableStateOf("N/A") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Asignar los estados a variables globales para que puedan ser actualizados desde el callback
        LaunchedEffect(Unit) {
            this@MainActivity.latitude = latitude
            this@MainActivity.longitude = longitude
            this@MainActivity.errorMessage = errorMessage
        }

        // Actualizar los estados cuando cambien las variables globales
        LaunchedEffect(this@MainActivity.latitude, this@MainActivity.longitude, this@MainActivity.errorMessage) {
            latitude = this@MainActivity.latitude
            longitude = this@MainActivity.longitude
            errorMessage = this@MainActivity.errorMessage
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Location App",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Latitude: $latitude",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Longitude: $longitude",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    // Reiniciar el mensaje de error
                    errorMessage = null

                    // Verificar permisos y obtener la ubicación
                    when {
                        // Caso 1: Permiso ya otorgado
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            getLocation()
                        }
                        // Caso 2: Solicitar permiso
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text(
                    text = "Get Location",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // Mostrar mensaje de error si existe
            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Función para obtener la ubicación
        LaunchedEffect(Unit) {
            getLocation = {
                // Verificar si los servicios de ubicación están habilitados
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!isGpsEnabled && !isNetworkEnabled) {
                    // Mostrar mensaje de error y opción para habilitar los servicios de ubicación
                    errorMessage = "Los servicios de ubicación están desactivados"
                    Toast.makeText(
                        this@MainActivity,
                        "Por favor, habilita los servicios de ubicación",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    // Configurar la solicitud de ubicación
                    val locationRequest = LocationRequest.create().apply {
                        interval = 10000 // Intervalo de actualización (10 segundos)
                        fastestInterval = 5000 // Intervalo más rápido (5 segundos)
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Alta precisión
                    }

                    // Solicitar actualizaciones de ubicación
                    try {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    } catch (e: SecurityException) {
                        errorMessage = "Permiso de ubicación denegado"
                    }
                }
            }
        }
    }

    // Variables para almacenar los estados y poder actualizarlos desde el callback
    private var latitude: String by mutableStateOf("N/A")
    private var longitude: String by mutableStateOf("N/A")
    private var errorMessage: String? by mutableStateOf(null)

    // Variable para almacenar la función getLocation y poder llamarla desde el botón
    private var getLocation: () -> Unit = {}
}