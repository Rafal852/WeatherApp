package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.RvAdapter
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.databinding.BottomSheetLayoutBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sheetBinding: BottomSheetLayoutBinding
    private lateinit var dialog: BottomSheetDialog
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var city: String = "berlin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetBinding.root)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {

                if (query != null){
                    city = query
                }
                getCurrentWeather(city)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        fetchLocation()
        getCurrentWeather(city) 

        binding.tvForecast.setOnClickListener {
            openDialog()
        }

        binding.tvLocation.setOnClickListener {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),101
            )
            return
        }

        task.addOnSuccessListener {
            val geocoder= Geocoder(this,Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                geocoder.getFromLocation(it.latitude,it.longitude,1, object: Geocoder.GeocodeListener{
                    override fun onGeocode(addresses: MutableList<Address>) {
                        city = addresses[0].locality
                    }
                })
            }else{
                val address = geocoder.getFromLocation(it.latitude,it.longitude,1) as List<Address>

                city = address[0].locality
            }

            getCurrentWeather(city)
        }
    }

    private fun openDialog(){
        getForecast()

        sheetBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, 1, RecyclerView.HORIZONTAL, false)
        }

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun getForecast(){
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getForecast(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException){
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            } catch (e: HttpException){
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (response.isSuccessful && response.body() != null){
                withContext(Dispatchers.Main){
                    val data = response.body()!!

                    var forecastArray = arrayListOf<ForecastData>()

                    forecastArray = data.list as ArrayList<ForecastData>

                    val adapter = RvAdapter(forecastArray)
                    sheetBinding.rvForecast.adapter = adapter
                    sheetBinding.tvSheet.text = "Five days forecast in ${data.city.name}"
                }
            }
        }

    }

    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(city, "metric", applicationContext.getString(R.string.api_key))
            }catch (e: IOException){
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }catch (e: HttpException){
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (response.isSuccessful && response.body()!= null){
                withContext(Dispatchers.Main){
                    val data = response.body()!!

                    binding.tvSunrise.text= dateFormatConverter(data.sys.sunrise.toLong())
                    binding.tvSunset.text= dateFormatConverter(data.sys.sunset.toLong())

                    binding.apply {
                        tvStatus.text = data.weather[0].description
                        tvWind.text = "${data.wind.speed.toString()} KM/h"
                        tvLocation.text ="${data.name}\n${data.sys.country}"
                        tvTemp.text ="${data.main.temp.toInt()} °C"
                        tvFeelsLike.text ="Feels like: ${data.main.feels_like} °C"
                        tvMinTemp.text ="Min temp: ${data.main.temp_min} °C"
                        tvMaxTemp.text ="MAx temp: ${data.main.temp_max} °C"
                        tvMaxTemp.text ="MAx temp: ${data.main.temp_max} °C"
                        tvPressure.text =" ${data.main.pressure} hPa"
                        tvUpdateTime.text ="Last Update: ${dateFormatConverter(data.dt.toLong())}"
                    }
                }
            }

        }
    }

    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(date * 1000))
    }
}