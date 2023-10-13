package com.example.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.RvAdapter
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.databinding.BottomSheetLayoutBinding
import com.example.weatherapp.utils.RetrofitInstance
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetBinding.root)
        setContentView(binding.root)
        getCurrentWeather()

        binding.tvForecast.setOnClickListener {
            openDialog()
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
                    "berlin",
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

    private fun getCurrentWeather() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather("new york", "metric", applicationContext.getString(R.string.api_key))
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
                    val iconId = data.weather[0].icon
                    val imgUrl = "https://openweathermap.org/img/w/$iconId.png"

                    Picasso.get()
                        .load(imgUrl)
                        .into(binding.imgWeather)

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