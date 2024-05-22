package ali.emir.merhaba.grmeengelliuygulama

import android.location.Location
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var harita: GoogleMap
    private lateinit var konumIstemcisi: FusedLocationProviderClient
    private lateinit var konumIsteği: LocationRequest
    private lateinit var konumCallback: LocationCallback
    private var mevcutKonumMarker: Marker? = null
    private var hedefLatLng: LatLng? = null
    private lateinit var apiKey: String
    private lateinit var metindenKonuşmaya: TextToSpeech
    private lateinit var titreşim: Vibrator
    private val navigasyonAdımları = mutableListOf<Pair<LatLng, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Titreşim servisini başlatan kod
        titreşim = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Metinden Konuşmaya ayarlama işkemi burada yapılıyor ve Türkçe dil seçenği default ayarlanıyor
        metindenKonuşmaya = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = metindenKonuşmaya.setLanguage(Locale("tr", "TR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Türkçe dili desteklenmemektedir")
                }
            } else {
                Log.e("TTS", "Metinden Konuşmaya kurulmu başarısız oldu")
            }
        }

        konumIstemcisi = LocationServices.getFusedLocationProviderClient(this)
        konumIsteği = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        // Mevcut anlık konumu alıp haritada gösterme işlmeini yapıyor
        val haritaFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        haritaFragment.getMapAsync(this)

        // Belirli bir sıklıkta konum güncellemelerini takip eden kod
        konumCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val mevcutLatLng = LatLng(location.latitude, location.longitude)
                    mevcutKonumMarker?.remove()
                    mevcutKonumMarker = harita.addMarker(MarkerOptions().position(mevcutLatLng).title("Mevcut Konumunuz"))
                    harita.animateCamera(CameraUpdateFactory.newLatLngZoom(mevcutLatLng, 15f))

                    if (hedefLatLng != null) {
                        rotaCiz(mevcutLatLng, hedefLatLng!!)
                    }

                    sesliNavigasyonIcinYakınlıkKontrolu(mevcutLatLng)
                }
            }
        }
        apiKey = getString(R.string.my_map_api_key)

        // LocationInputActivity'den girilen hedifi alıp haritada gösterme işlemi yapan kod
        val hedef = intent.getStringExtra("destination")
        if (hedef != null) {
            val geocoder = Geocoder(this, Locale.getDefault())
            val adresler = geocoder.getFromLocationName(hedef, 1)
            if (adresler != null && adresler.isNotEmpty()) {
                val adres = adresler[0]
                hedefLatLng = LatLng(adres.latitude, adres.longitude)
                Log.d("AnaActivity", "Hedef: $hedefLatLng")
            }
        }
    }

    // Harita hazır olduğunda haritayı çağıran işlem
    override fun onMapReady(googleHarita: GoogleMap) {
        harita = googleHarita

        if (hedefLatLng != null) {
            harita.addMarker(MarkerOptions().position(hedefLatLng!!).title("Hedef"))
            harita.moveCamera(CameraUpdateFactory.newLatLngZoom(hedefLatLng!!, 10f))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            konumIstemcisi.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val mevcutLatLng = LatLng(location.latitude, location.longitude)
                    harita.animateCamera(CameraUpdateFactory.newLatLngZoom(mevcutLatLng, 15f))
                }
            }
            konumIstemcisi.requestLocationUpdates(konumIsteği, konumCallback, null)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    // Başlangıç ve hedef arasında rota çiziyor ve sesli yönlendirme işlemini yapıyor
    private fun rotaCiz(baslangic: LatLng, hedef: LatLng) {
        val servis = RetrofitClient.directionsApiService
        Log.d("AnaActivity", "Rota isteniyor: $baslangic'dan $hedef'e API anahtarı ile $apiKey")
        servis.getDirections(
            origin = "${baslangic.latitude},${baslangic.longitude}",
            destination = "${hedef.latitude},${hedef.longitude}",
            apiKey = apiKey,
            language = "tr"
        ).enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    if (directionsResponse != null && directionsResponse.routes.isNotEmpty()) {
                        val adımlar = directionsResponse.routes[0].legs[0].steps
                        for (adım in adımlar) {
                            val baslangicKonum = LatLng(adım.start_location.lat, adım.start_location.lng)
                            val bitisKonum = LatLng(adım.end_location.lat, adım.end_location.lng)
                            val talimat = adım.html_instructions


                            val polylineOptions = PolylineOptions()
                            polylineOptions.add(baslangicKonum)
                            polylineOptions.add(bitisKonum)
                            harita.addPolyline(polylineOptions)


                            sesliNavigasyonEkle(baslangicKonum, talimat)
                        }
                    }
                } else {
                    Log.e("AnaActivity", "Rota API çağrısı başarısız oldu, kod: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("AnaActivity", "Rota API çağrısı başarısız oldu: ${t.message}", t)
            }
        })
    }

    // Sesli navigasyon ekleme işlem,
    private fun sesliNavigasyonEkle(konum: LatLng, talimat: String) {
        // HTML etiketlerini talimattan temizle
        val temizTalimat = talimat.replace(Regex("<[^>]*>"), "")
        navigasyonAdımları.add(Pair(konum, temizTalimat))
    }

    // Kullanıcının konumuna göre sesli navigasyon talimatını belirleme.
    private fun sesliNavigasyonIcinYakınlıkKontrolu(mevcutLatLng: LatLng) {
        for ((konum, talimat) in navigasyonAdımları) {
            val mesafe = FloatArray(1)
            Location.distanceBetween(mevcutLatLng.latitude, mevcutLatLng.longitude, konum.latitude, konum.longitude, mesafe)
            if (mesafe[0] < 10) {
                metindenKonuşmaya.speak(talimat, TextToSpeech.QUEUE_ADD, null, null)

                titreşimYap()
                navigasyonAdımları.remove(Pair(konum, talimat))
                break
            }
        }
    }

    private fun titreşimYap() {
        if (titreşim.hasVibrator()) {
            // 500 milisaniye titre
            titreşim.vibrate(500)
        }
    }

    // Gerçek zamanlı konum izleme iznini programa otamatik vermekte bu saydee programımızın işlevselliğini kattık.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                konumIstemcisi.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val mevcutLatLng = LatLng(location.latitude, location.longitude)
                        harita.animateCamera(CameraUpdateFactory.newLatLngZoom(mevcutLatLng, 15f))
                    }
                }
                konumIstemcisi.requestLocationUpdates(konumIsteği, konumCallback, null)
            }
        }
    }

    // Bellek kullanımını azaltmak için nesne kaynaklarını serbest bırak
    override fun onDestroy() {
        if (metindenKonuşmaya != null) {
            metindenKonuşmaya.stop()
            metindenKonuşmaya.shutdown()
        }
        super.onDestroy()
    }
}
