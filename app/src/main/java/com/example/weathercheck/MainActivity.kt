package com.example.weathercheck

import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.picasso.Picasso
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task


class MainActivity : AppCompatActivity() {

    private val LANG = Locale.getDefault().getLanguage()
    private val API = "c5090c66c639d7951f8b759a2cc3da96"

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fetchLocation()

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        val button_refresh = findViewById<Button>(R.id.btn_refresh)
        button_refresh.setOnClickListener {
            button_refresh.isEnabled = false
            fetchLocation()
            button_refresh.isEnabled = true
        }
        swipeRefreshLayout.setOnRefreshListener {
            fetchLocation()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
            return
        }
        val priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val ct = CancellationTokenSource()
        val locationTask: Task<Location> = fusedLocationProviderClient.getCurrentLocation(priority, ct.token)

        locationTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {
                    Log.d("WeatherData", "latitude: ${location.latitude}")
                    Log.d("WeatherData", "longitude: ${location.longitude}")
                    WeatherTask("${location.latitude}", "${location.longitude}").execute()
                } else {
                    Log.e("WeatherData", "Местоположение недоступно")
                }
            } else {
                Log.e("WeatherData", "Ошибка при получении местоположения: ${task.exception}")
            }
        }
    }

    inner class WeatherTask(private val latitude: String, private val longitude: String) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String? {
            val client = OkHttpClient()
            Log.d("WeatherData", "doInBackground started working")
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$API&units=metric&lang=$LANG"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            return response.body?.string()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                // Обработка данных о погоде
                val jsonObject = JSONObject(result)
                val temperature = jsonObject.getJSONObject("main").getDouble("temp")
                val feelsLike = jsonObject.getJSONObject("main").getDouble("feels_like")
                val city_name = jsonObject.getString("name")
                val wind_speed = jsonObject.getJSONObject("wind").getInt("speed")
                val humidity = jsonObject.getJSONObject("main").getInt("humidity")
                val pressure = jsonObject.getJSONObject("main").getInt("pressure")
                val weatherArray = jsonObject.getJSONArray("weather")
                val weatherObject = weatherArray.getJSONObject(0)
                val description = weatherObject.getString("description")
                //Изображение погоды
                val iconCode = weatherObject.getString("icon")
                val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                val weather_icon = findViewById<ImageView>(R.id.weather_main_widget_icon)
                // Отображение результатов
                Picasso.get().load(iconUrl).into(weather_icon)
                val temp_text = findViewById<TextView>(R.id.weather_main_widget_info_temp)
                val feelslike_text = findViewById<TextView>(R.id.weather_main_widget_info_feelslike)
                val status_text = findViewById<TextView>(R.id.weather_main_widget_info_status)
                val city_text = findViewById<TextView>(R.id.weather_main_info_city)
                val wind_text = findViewById<TextView>(R.id.weather_main_info_wind)
                val humidity_text = findViewById<TextView>(R.id.weather_main_info_humidity)
                val pressure_text = findViewById<TextView>(R.id.weather_main_info_pressure)
                temp_text.text = "$temperature °C"
                feelslike_text.text = "Ощущается: $feelsLike °C"
                status_text.text = description
                city_text.text = city_name
                wind_text.text = "Скорость ветра: $wind_speed м/c"
                humidity_text.text = "Влажность: $humidity %"
                pressure_text.text = "Давление: $pressure гПа"
                // Логирование
                Log.d("WeatherData", result)
            } else {
                Log.e("WeatherData", "Ошибка при получении данных о погоде")
            }
        }
    }
}