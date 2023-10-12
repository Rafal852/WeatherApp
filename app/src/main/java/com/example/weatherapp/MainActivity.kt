package com.example.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCurrentWeather()
    }

    private fun getCurrentWeather() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather("new york", "metric", "bd199e6fd842a33e4ae3f454783f53e8")
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

                    binding.tvSunrise.text= SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(data.sys.sunrise * 1000)
                    binding.tvSunset.text= SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(data.sys.sunset * 1000)

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
                        tvUpdateTime.text ="Last Update: ${SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(data.dt * 1000)} hPa"
                    }
                }
            }

        }
    }
}