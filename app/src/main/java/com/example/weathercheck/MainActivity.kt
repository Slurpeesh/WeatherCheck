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
import org.w3c.dom.Text
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    var lang: String = Locale.getDefault().getLanguage()
    private val API = "c5090c66c639d7951f8b759a2cc3da96"
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var forecastWeatherList: MutableList<ForecastWeather> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val button_refresh = findViewById<Button>(R.id.btn_refresh)
        button_refresh.text = "${langDict[lang]?.get("update")}"

        fetchLocation()

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
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
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        } else {
            // Location permissions have already been granted, so perform a location query
            val priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val ct = CancellationTokenSource()
            val locationTask: Task<Location> = fusedLocationProviderClient.getCurrentLocation(priority, ct.token)

            locationTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val location = task.result
                    if (location != null) {
                        WeatherTask("${location.latitude}", "${location.longitude}").execute()
                    } else {
                        Log.e("WeatherData", "Местоположение недоступно")
                        WeatherTask("51.50853", "-0.12574").execute() // Получаем погоду Лондона
                    }
                } else {
                    Log.e("WeatherData", "Ошибка при получении местоположения: ${task.exception}")
                    WeatherTask("51.50853", "-0.12574").execute() // Получаем погоду Лондона
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            WeatherTask("51.50853", "-0.12574").execute() // Получаем погоду Лондона
        }
    }

    // Функция для преобразования метки времени Unix в строку даты
    private fun convertUnixTimestampToDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(timestamp * 1000))
    }

    data class ForecastWeather(
        val date: String,
        val time: String,
        val temperature: Double,
        val description: String,
        val wind: Int,
        val pressure: Int,
        val humidity: Int,
        val iconCode: String
    )

    inner class WeatherTask(private val latitude: String, private val longitude: String) : AsyncTask<Void, Void, Pair<String?, String?>>() {

        override fun doInBackground(vararg params: Void?): Pair<String?, String?> {
            val client = OkHttpClient()
            Log.d("WeatherData", "doInBackground started working")
            val currentWeatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$API&units=metric&lang=$lang"
            val forecastWeatherUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=$latitude&lon=$longitude&cnt=40&appid=$API&units=metric&lang=$lang"

            val currentWeatherRequest = Request.Builder()
                .url(currentWeatherUrl)
                .build()

            val forecastWeatherRequest = Request.Builder()
                .url(forecastWeatherUrl)
                .build()

            val currentWeatherResponse = client.newCall(currentWeatherRequest).execute()
            val forecastWeatherResponse = client.newCall(forecastWeatherRequest).execute()

            val currentWeatherJson = currentWeatherResponse.body?.string()
            val forecastWeatherJson = forecastWeatherResponse.body?.string()

            return Pair(currentWeatherJson, forecastWeatherJson)
        }

        override fun onPostExecute(result: Pair<String?, String?>) {
            super.onPostExecute(result)
            Log.d("WeatherData", "onPostExecute started working")
            val currentWeatherJson = result.first
            val forecastWeatherJson = result.second

            Log.d("WeatherData", "Current Weather: $currentWeatherJson")
            Log.d("WeatherData", "Forecast Weather: $forecastWeatherJson")

            if (currentWeatherJson != null && forecastWeatherJson != null) {
                // Processing current weather data
                val currentWeather = JSONObject(currentWeatherJson) // Переименована переменная

                // Получение данных о текущей погоде
                val temperature = currentWeather.getJSONObject("main").getDouble("temp")
                val feelsLike = currentWeather.getJSONObject("main").getDouble("feels_like")
                val city_name = currentWeather.getString("name")
                val wind_speed = currentWeather.getJSONObject("wind").getInt("speed")
                val humidity = currentWeather.getJSONObject("main").getInt("humidity")
                val pressure = currentWeather.getJSONObject("main").getInt("pressure")
                val currentWeatherArray = currentWeather.getJSONArray("weather")
                val currentWeatherObject = currentWeatherArray.getJSONObject(0)
                val description = currentWeatherObject.getString("description")
                // Weather icon
                val iconCode = currentWeatherObject.getString("icon")
                val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                val weather_icon = findViewById<ImageView>(R.id.weather_main_widget_icon)
                // Displaying current weather results
                Picasso.get().load(iconUrl).into(weather_icon)
                val temp_text = findViewById<TextView>(R.id.weather_main_widget_info_temp)
                val feelslike_text = findViewById<TextView>(R.id.weather_main_widget_info_feelslike)
                val status_text = findViewById<TextView>(R.id.weather_main_widget_info_status)
                val city_text = findViewById<TextView>(R.id.weather_main_info_city)
                val wind_text = findViewById<TextView>(R.id.weather_main_info_wind)
                val humidity_text = findViewById<TextView>(R.id.weather_main_info_humidity)
                val pressure_text = findViewById<TextView>(R.id.weather_main_info_pressure)
                temp_text.text = "$temperature °C"
                feelslike_text.text = "${langDict[lang]?.get("feels_like")}: $feelsLike °C"
                status_text.text = description.capitalize()
                city_text.text = city_name
                wind_text.text = "${langDict[lang]?.get("wind")}: $wind_speed ${langDict[lang]?.get("m/s")}"
                humidity_text.text = "${langDict[lang]?.get("humidity")}: $humidity %"
                pressure_text.text = "${langDict[lang]?.get("pressure")}: $pressure ${langDict[lang]?.get("hPa")}"

                // Processing forecast weather data
                val forecastWeatherObject = JSONObject(forecastWeatherJson)
                val forecastWeatherArray = forecastWeatherObject.getJSONArray("list")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                val currentDate = dateFormat.format(Calendar.getInstance().time)

                for (i in 0 until forecastWeatherArray.length()) {
                    val forecastWeatherObject = forecastWeatherArray.getJSONObject(i)
                    val dateTime = forecastWeatherObject.getString("dt_txt")
                    if (dateTime.endsWith("15:00:00")) { // Фильтр для времени 15:00
                        val date = dateFormat.parse(dateTime.substring(0, 10))
                        val temperature = forecastWeatherObject.getJSONObject("main").getDouble("temp")
                        val weatherArray = forecastWeatherObject.getJSONArray("weather")
                        val weatherObject = weatherArray.getJSONObject(0)
                        val description = weatherObject.getString("description")
                        val iconCode = weatherObject.getString("icon")
                        val windSpeed = forecastWeatherObject.getJSONObject("wind").getInt("speed")
                        val humidity = forecastWeatherObject.getJSONObject("main").getInt("humidity")
                        val pressure = forecastWeatherObject.getJSONObject("main").getInt("pressure")

                        // Проверка, чтобы исключить текущий день
                        if (date != null && dateFormat.format(date) != currentDate) {
                            val formattedDate = dateFormat.format(date)

                            // Создаем объект ForecastWeather и добавляем его в список
                            val forecastWeather = ForecastWeather(
                                formattedDate,
                                "15:00",
                                temperature,
                                description,
                                windSpeed,
                                pressure,
                                humidity,
                                iconCode
                            )
                            forecastWeatherList.add(forecastWeather)
                        }
                    }
                }

                if(forecastWeatherList.size == 5) {
                    forecastWeatherList.removeAt(4)
                }

                Log.d("WeatherData", "$forecastWeatherList")

                for (i in forecastWeatherList.indices) {
                    val forecastWeather = forecastWeatherList[i]
                    val date = forecastWeather.date
                    val temperature = forecastWeather.temperature
                    val iconCode = forecastWeather.iconCode
                    val desc = forecastWeather.description
                    val wind = forecastWeather.wind
                    val pressure = forecastWeather.pressure
                    val humidity = forecastWeather.humidity

                    // Формирование идентификаторов элементов интерфейса на основе индекса i
                    val weatherIconId = resources.getIdentifier("weather_secondary_${i+1}_widget_icon", "id", packageName)
                    val tempTextId = resources.getIdentifier("weather_secondary_${i+1}_widget_info_temp", "id", packageName)
                    val dateTextId = resources.getIdentifier("weather_secondary_${i+1}_widget_info_date", "id", packageName)
                    val descTextId = resources.getIdentifier("weather_secondary_${i+1}_text_desc", "id", packageName)
                    val infoId = resources.getIdentifier("weather_secondary_${i+1}_text_allinfo", "id", packageName)

                    val weatherIcon = findViewById<ImageView>(weatherIconId)
                    val tempText = findViewById<TextView>(tempTextId)
                    val dateText = findViewById<TextView>(dateTextId)
                    val descText = findViewById<TextView>(descTextId)
                    val infoText = findViewById<TextView>(infoId)

                    val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                    Picasso.get().load(iconUrl).into(weatherIcon)
                    tempText.text = "$temperature °C"
                    dateText.text = "$date"
                    descText.text = "$desc".capitalize()
                    infoText.text = "${langDict[lang]?.get("wind")}: $wind ${langDict[lang]?.get("m/s")};" +
                            System.lineSeparator() +
                            "${langDict[lang]?.get("humidity")}: $humidity %; " +
                            System.lineSeparator() +
                            "${langDict[lang]?.get("pressure")}: $pressure ${langDict[lang]?.get("hPa")}"
                }
                // Clear list
                forecastWeatherList.clear()

            } else {
                Log.e("WeatherData", "Error fetching weather data")
            }
        }
    }
}