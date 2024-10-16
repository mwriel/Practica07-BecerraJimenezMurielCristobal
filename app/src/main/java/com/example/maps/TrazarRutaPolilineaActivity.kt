package com.example.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.maps.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import okhttp3.ResponseBody
import okhttp3.Response

class TrazarRutaPolilineaActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private lateinit var mMap: GoogleMap
    private var minimumDistance = 30
    private val PERMISSION_LOCATION = 999
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var marker: Marker? = null

    private var selectedLocation: LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 1000
            smallestDisplacement = minimumDistance.toFloat()
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.e("APP 06", "${locationResult.lastLocation?.latitude}, ${locationResult.lastLocation?.longitude}")
            }
        }

        findViewById<View>(R.id.fab_open_google_maps).setOnClickListener { openGoogleMaps() }
        findViewById<View>(R.id.fab_open_route_google_maps).setOnClickListener { openRouteInGoogleMaps() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_LOCATION) {
            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION, ignoreCase = true) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Disable default UI controls
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_LOCATION)
        }

        val tlaquepaqueCentro = LatLng(20.64047, -103.31154)
        mMap.addMarker(MarkerOptions().position(tlaquepaqueCentro).title("Tlaquepaque Centro").snippet("Fin de la ruta"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tlaquepaqueCentro, 15f))
        mMap.setOnMapClickListener(this)
    }

    fun map(view: View) {
        when (view.id) {
            R.id.activity_maps_map -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.activity_maps_terrain -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.activity_maps_hybrid -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.activity_maps_polylines -> showPolylines()
        }
    }

    private fun startLocationUpdates() {
        try {
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showPolylines() {
        if (::mMap.isInitialized) {
            // Limpiar polilíneas y marcadores previos
            mMap.clear()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)

                    // Mover la cámara a la ubicación actual
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f))

                    // Configurar opciones de la polilínea
                    val polylineOptions = PolylineOptions()
                        .geodesic(true)
                        .color(Color.RED)
                        .width(5f)
                        .add(currentLocation) // Añadir la ubicación actual como punto inicial

                    if (selectedLocation != null) {
                        // Añadir la ubicación seleccionada como punto final
                        polylineOptions.add(selectedLocation)

                        // Añadir marcador en la ubicación seleccionada
                        mMap.addMarker(MarkerOptions().position(selectedLocation!!).title("Ubicación seleccionada"))
                    }

                    // Dibujar la polilínea en el mapa
                    mMap.addPolyline(polylineOptions)

                    // (Opcional) Puedes mover la cámara para mostrar ambos puntos
                    if (selectedLocation != null) {
                        val boundsBuilder = LatLngBounds.Builder()
                        boundsBuilder.include(currentLocation)
                        boundsBuilder.include(selectedLocation!!)

                        val bounds = boundsBuilder.build()
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    }
                }

            }
        }
    }


    private fun getMarkersFromJson(): List<LatLng> {
        return listOf(
            LatLng(20.73882, -103.40063),
            LatLng(20.69676, -103.37541),
            LatLng(20.67806, -103.34673),
            LatLng(20.64047, -103.31154)
        )
    }


    override fun onMapClick(latLng: LatLng) {
        marker?.remove()
        marker = mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        // Guarda las coordenadas seleccionadas
        selectedLocation = latLng

        //showPolylines()

        // Aquí puedes imprimir o utilizar las coordenadas
        Log.d("MAP_CLICK", "Coordenadas seleccionadas: ${latLng.latitude}, ${latLng.longitude}")
    }

    private fun openGoogleMaps() {
        val gmmIntentUri = Uri.parse("geo:0,0?q=Guadalajara")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    private fun openRouteInGoogleMaps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = "${location.latitude},${location.longitude}"
                val destination = "20.64047,-103.31154" // Tlaquepaque Centro
                val gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$currentLocation&destination=$destination")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }
    }


    //---------------------------





    fun getPolylineFromDirections(context: Context, origin: LatLng, destination: LatLng): String? {
        // Obtener la clave de API desde strings.xml
        val apiKey = context.getString(R.string.apikey)

        // Construir la URL para la solicitud Directions API usando LatLng
        val originParam = "${origin.latitude},${origin.longitude}"
        val destinationParam = "${destination.latitude},${destination.longitude}"
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originParam&destination=$destinationParam&key=$apiKey"

        // Inicializa OkHttpClient
        val client = OkHttpClient()

        // Crea la solicitud HTTP GET
        val request = Request.Builder()
            .url(url)
            .build()

        // Ejecuta la solicitud y obtiene la respuesta
        val response: Response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            val responseBody = response.body?.string()
            if (responseBody != null) {
                // Parsear el JSON
                val jsonObject = JSONObject(responseBody)
                val routes = jsonObject.getJSONArray("routes")

                // Verifica si hay rutas disponibles
                if (routes.length() > 0) {
                    val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline")
                    overviewPolyline.getString("points") // Regresa los puntos de la polilínea
                } else {
                    null // No hay rutas disponibles
                }
            } else {
                null // No se pudo obtener el cuerpo de la respuesta
            }
        } else {
            null // Respuesta no exitosa
        }
    }



}