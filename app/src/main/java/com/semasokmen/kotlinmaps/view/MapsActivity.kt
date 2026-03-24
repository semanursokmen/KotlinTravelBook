package com.semasokmen.kotlinmaps.view


import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.semasokmen.kotlinmaps.R
import com.semasokmen.kotlinmaps.databinding.ActivityMapsBinding
import com.semasokmen.kotlinmaps.model.Place
import com.semasokmen.kotlinmaps.roomdb.PlaceDao
import com.semasokmen.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean:Boolean ?= null
    private var selectedLatitude: Double ?= null
    private var selectedLongitude: Double ?= null
    private lateinit var db:PlaceDatabase
    private lateinit var placeDao: PlaceDao
    private val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place ?= null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("android.content.Context.MODE_PRIVATE", MODE_PRIVATE)
        trackBoolean = false

        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries()
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMapLongClickListener(this)
//gelen intenti alma
        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new") {
            //yeni bir şey ekleme
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE
            //casting
            //LOCATION_SERVICE any! döndürdüğünden as LocationManager ekleyerek onun o çeşit olduğuna eminiz ve bu şekilde kaydetmek istiyoruz.
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            //konum yöneticisi: konumla ilgili tüm işlemleri ele alır.
            //konum dinleyicisi: konumda değişiklik olduğunda haber veren arayüzdür.
            locationListener = object : LocationListener {
                override fun onLocationChanged(p0: Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                    //trackboolean true oluyor ve onlocationchanged bir daha çalışmıyor.(gezinirken tekrar tekrar konuma atmıyor.)
                    if (trackBoolean==false) {
                        val userLocation = LatLng(p0.latitude,p0.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }


                }

            }

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.root,"Permission needed",Snackbar.LENGTH_INDEFINITE).setAction("Give permission") {
                        //Request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()

                } else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                }

            } else {
                //granted, request permission

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                }

                mMap.isMyLocationEnabled = true
            }

        } else {
            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain?.let {
                val latLng = LatLng (it.latitude,it.longitude)

                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE


            }
        }

        /*
        // Add a marker in Sydney and move the
        //lat: latitude, lng: longitude)
        val eiffel = LatLng(48.853915,2.2913515)
        mMap.addMarker(MarkerOptions().position(eiffel).title("Eiffel Tower"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15f))
*/



    }

    private fun registerLauncher() {

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

            if(result) {
                if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))

                }

            } else {
                //permission denied

                Toast.makeText(this,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }

    }

    override fun onMapLongClick(p0: LatLng) {
        //p0: kullanıcının en son tıkladığı konum
        //her tıklamada önceki markerı silmesi için:
        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude
        binding.saveButton.isEnabled = true


    }

    fun save(view: View) {

        //Main Thread UI, Default -> CPU (YOĞUN İŞLEMLER İÇİN)
        //IO(input output) Thread internetten gelen istekleri hem işleme alma hem de yaomayı asenkron yapmak
        if (selectedLatitude != null && selectedLongitude != null) {
            val place = Place(binding.placeText.text.toString(),selectedLatitude!!,selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
                //handleResponse sonunda() yok çünkü referans veriyoruz sadece. Çalıştırmıyoruz.
                //işlem bitince bu fonksiyonu çalıştır diye ref veriyoruz.
            )
        }

    }
//işlem bitince main activitye geri dönmek icin:
    // gelen cevabı ele al(handleresponse)

    private fun handleResponse() {
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View) {

        placeFromMain?.let{

            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )

        }
        

    }
    
}

