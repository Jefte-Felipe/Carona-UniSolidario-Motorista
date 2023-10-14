package com.tcc.unisolidariamotorista.activities


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.tcc.unisolidariamotorista.R
import com.tcc.unisolidariamotorista.databinding.ActivityMapBinding
import com.tcc.unisolidariamotorista.fragments.ModalBottomSheetBooking
import com.tcc.unisolidariamotorista.fragments.ModalBottomSheetMenu
import com.tcc.unisolidariamotorista.models.Booking
import com.tcc.unisolidariamotorista.providers.AuthProvider
import com.tcc.unisolidariamotorista.providers.BookingProvider
import com.tcc.unisolidariamotorista.providers.DriverProvider
import com.tcc.unisolidariamotorista.providers.GeoProvider
import com.tcc.unisolidariamotorista.providers.NotificationProvider

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener, SensorEventListener {

    private var bearing: Float = 0.0f
    private var bookingListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()
    private val driverProvider = DriverProvider()
    private val notificationProvider = NotificationProvider()
    private val modalBooking = ModalBottomSheetBooking()
    private val modalMenu = ModalBottomSheetMenu()

    // SENSOR CAMERA
    private var angle = 0
    private val rotationMatrix = FloatArray(16)
    private var sensorManager: SensorManager? = null
    private var vectSensor: Sensor? = null
    private var declination = 0.0f
    private var isFirstTimeOnResume = false
    private var isFirstLocation = false


    val timer = object : CountDownTimer(30000, 1000) {
        override fun onTick(counter: Long) {
            Log.d("TIMER", "Counter: $counter")
        }

        override fun onFinish() {
            Log.d("TIMER", "ON FINISH")
            modalBooking.dismiss()
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
        vectSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        listenerBooking()
        createToken()


        binding.btnConnect.setOnClickListener { connectDriver() }
        binding.btnDisconnect.setOnClickListener { disconnectDriver() }
        binding.imageViewMenu.setOnClickListener { showModalMenu() }
    }

    val locationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when {
                    permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        Log.d("LOCALIZAÇÃO", "Permissão garantida")
//                    easyWayLocation?.startLocation();
                        checkIfDriverIsConnected()
                    }

                    permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        Log.d("LOCALIZAÇÃO", "Permissão concedida com limitação")
//                    easyWayLocation?.startLocation();
                        checkIfDriverIsConnected()
                    }

                    else -> {
                        Log.d("LOCALIZAÇÃO", "Permissão não concedida")
                    }
                }
            }

        }


    private fun createToken() {
        driverProvider.createToken(authProvider.getId())
    }

    private fun showModalMenu() {
        modalMenu.show(supportFragmentManager, ModalBottomSheetMenu.TAG)
    }

    private fun showModalBooking(booking: Booking) {

        val bundle = Bundle()
        bundle.putString("booking", booking.toJson())
        modalBooking.arguments = bundle
        modalBooking.isCancelable = false // NÃO É POSSÍVEL OCULTAR A FOLHA INFERIOR MODAL
        modalBooking.show(supportFragmentManager, ModalBottomSheetBooking.TAG)
        timer.start()
    }

    private fun listenerBooking() {
        bookingListener = bookingProvider.getBooking().addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.d("FIRESTORE", "ERROR: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.documents.size > 0) {
                    val booking = snapshot.documents[0].toObject(Booking::class.java)
                    if (booking?.status == "create") {
                        showModalBooking(booking!!)
                    }
                }
            }
        }
    }


    private fun checkIfDriverIsConnected() {
        geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->
            if (document.exists()) {
                if (document.contains("l")) {
                    connectDriver()
                } else {
                    showButtonConnect()
                }
            } else {
                showButtonConnect()
            }
        }
    }

    private fun saveLocation() {
        if (myLocationLatLng != null) {
            geoProvider.saveLocation(authProvider.getId(), myLocationLatLng!!)
        }
    }

    private fun disconnectDriver() {
        easyWayLocation?.endUpdates()
        if (myLocationLatLng != null) {
            geoProvider.removeLocation(authProvider.getId())
            showButtonConnect()
        }
    }

    private fun connectDriver() {
        easyWayLocation?.endUpdates()
        easyWayLocation?.startLocation()
        showButtonDisconnect()
    }

    private fun showButtonConnect() {
        binding.btnDisconnect.visibility = View.GONE // ESCONDENDO O BOTÃO DE DESCONECTAR
        binding.btnConnect.visibility = View.VISIBLE // MOSTRANDO O BOTÃO CONECTAR
    }

    private fun showButtonDisconnect() {
        binding.btnDisconnect.visibility = View.VISIBLE // MOSTRANDO O BOTÃO DE DESCONECTAR
        binding.btnConnect.visibility = View.GONE // ESCONDENDO O BOTÃO CONECTAR
    }

    private fun addMarker() {
        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.icon_car)
        val markerIcon = getMarkerFromDrawable(drawable!!)
        if (markerDriver != null) {
            markerDriver?.remove()
        }
        if (myLocationLatLng != null) {
            markerDriver = googleMap?.addMarker(
                MarkerOptions()
                    .position(myLocationLatLng!!)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(markerIcon)
            )
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

//        easyWayLocation?.startLocation();
        startSensor()
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
        googleMap?.isMyLocationEnabled = false

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )
            if (!success!!) {
                Log.d("MAPAS", "Não foi possível encontrar o estilo")
            }

        } catch (e: Resources.NotFoundException) {
            Log.d("MAPAS", "Error: ${e.toString()}")
        }

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) {
        myLocationLatLng =
            LatLng(location.latitude, location.longitude)

        val field = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )

        declination = field.declination

//        if (!isFirstLocation) {
//            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
//                CameraPosition.builder().target(myLocationLatLng!!).zoom(19f).build()
//            ))
//            isFirstLocation = true
//
//        }
//        val orientation = FloatArray(3)
//        val bearing = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
//        updateCamera(bearing)

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).bearing(bearing).tilt(50f)
                    .zoom(19f).build()
            )
        )
        addDirectionMarker(myLocationLatLng!!, angle)
        saveLocation()
    }

    override fun locationCancelled() {

    }

    private fun updateCamera(bearing: Float) {
        val oldPos = googleMap?.cameraPosition
        val pos = CameraPosition.builder(oldPos!!).bearing(bearing).tilt(50f).build()
        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
        if (myLocationLatLng != null) {
            addDirectionMarker(myLocationLatLng!!, angle)
        }
    }

    private fun addDirectionMarker(latLng: LatLng, angle: Int) {
        val circleDrawable =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
        val markerIcon = getMarkerFromDrawable(circleDrawable!!)
        if (markerDriver != null) {
            markerDriver?.remove()
        }
        markerDriver = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .anchor(0.5f, 0.5f)
                .rotation(angle.toFloat())
                .flat(true)
                .icon(markerIcon)
        )
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            120,
            120,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, 120, 120)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
        bookingListener?.remove()
        stopSensor()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            if (Math.abs(Math.toDegrees(orientation[0].toDouble()) - angle) > 0.8) {
                bearing = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
                updateCamera(bearing)
            }
            angle = Math.toDegrees(orientation[0].toDouble()).toInt()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    private fun startSensor() {
        if (sensorManager != null) {
            sensorManager?.registerListener(
                this,
                vectSensor,
                SensorManager.SENSOR_STATUS_ACCURACY_LOW
            )
        }
    }

    private fun stopSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstTimeOnResume) {
            isFirstTimeOnResume = true
        } else {
            startSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

}