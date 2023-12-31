package com.example.weathercheck

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.picasso.Picasso
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    var lang: String = Locale.getDefault().language
    private val API = "c5090c66c639d7951f8b759a2cc3da96"
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var forecastWeatherList: MutableList<ForecastWeather> = mutableListOf()
    private var locationPermissionDenied = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val button_refresh = findViewById<Button>(R.id.btn_refresh)
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        button_refresh.text = "${langDict[lang]?.get("update")}"

        fetchLocation()

        button_refresh.setOnClickListener {
            fetchLocation()
        }
        swipeRefreshLayout.setOnRefreshListener {
            fetchLocation()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun fetchLocation() {
        // Checking internet connection
        if (!isOnline(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(langDict[lang]?.get("internet_required"))
                .setMessage(langDict[lang]?.get("internet_ask"))
                .setPositiveButton(langDict[lang]?.get("go_to_settings")) { _, _ ->
                    // Open the wifi settings
                    val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    val packageManager = packageManager
                    if (settingsIntent.resolveActivity(packageManager) != null) {
                        startActivity(settingsIntent)
                    } else {
                        Toast.makeText(this, langDict[lang]?.get("no_wifi_settings"), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(langDict[lang]?.get("cancel")) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
            val dialog = builder.create()
            dialog.show()
        } else {
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
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val ct = CancellationTokenSource()
                val locationTask: Task<Location> =
                    fusedLocationProviderClient.getCurrentLocation(priority, ct.token)

                locationTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val location = task.result
                        if (location != null) {
                            execute_WeatherTask("${location.latitude}", "${location.longitude}")
                        } else {
                            Log.e("WeatherData", "Location not available")
                            execute_WeatherTask("51.50853", "-0.12574") // Getting the weather in London
                        }
                    } else {
                        Log.e("WeatherData", "Location acquisition error: ${task.exception}")
                        execute_WeatherTask("51.50853", "-0.12574") // Getting the weather in London
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            // Permission denied
            if (!locationPermissionDenied) {
                // This is the first denial
                // Show a message to the user informing that location permission is required
                // and provide an option to navigate to app settings for enabling the permission.
                locationPermissionDenied = true
                val builder = AlertDialog.Builder(this)
                builder.setTitle(langDict[lang]?.get("permission_required"))
                    .setMessage(langDict[lang]?.get("permission_ask"))
                    .setPositiveButton(langDict[lang]?.get("go_to_settings")) { _, _ ->
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton(langDict[lang]?.get("cancel")) { dialog, _ ->
                        dialog.dismiss()
                        execute_WeatherTask("51.50853", "-0.12574")
                    }
                    .setCancelable(false)
                val dialog = builder.create()
                dialog.show()
            } else {
                // This is the second denial, user has denied the permission twice
                // Show a message informing that weather information cannot be accessed
                // because the location permission is denied.
                locationPermissionDenied = true
                val builder = AlertDialog.Builder(this)
                builder.setTitle(langDict[lang]?.get("permission_required"))
                    .setMessage(langDict[lang]?.get("permission_alert"))
                    .setPositiveButton(langDict[lang]?.get("go_to_settings")) { _, _ ->
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton(langDict[lang]?.get("cancel")) { dialog, _ ->
                        dialog.dismiss()
                        execute_WeatherTask("51.50853", "-0.12574")
                    }
                    .setCancelable(false)
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun execute_WeatherTask(lat: String, lon: String) {
        val button_refresh = findViewById<Button>(R.id.btn_refresh)
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        button_refresh.isEnabled = false
        swipeRefreshLayout.isRefreshing = true
        WeatherTask(lat, lon).execute()
    }

    @SuppressLint("StaticFieldLeak")
    inner class WeatherTask(private val latitude: String, private val longitude: String) : AsyncTask<Void, Void, Pair<String?, String?>>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Pair<String?, String?> {
            val client = OkHttpClient()
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

        @SuppressLint("SetTextI18n", "SimpleDateFormat", "DiscouragedApi")
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: Pair<String?, String?>) {
            super.onPostExecute(result)
            val button_refresh = findViewById<Button>(R.id.btn_refresh)
            val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
            val currentWeatherJson = result.first
            val forecastWeatherJson = result.second

            if (currentWeatherJson != null && forecastWeatherJson != null) {
                // Processing current weather data
                val currentWeather = JSONObject(currentWeatherJson) // Renamed variable

                // Getting data about the current weather
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
                    val forecastWeatherObject: JSONObject = forecastWeatherArray.getJSONObject(i)
                    val dateTime = forecastWeatherObject.getString("dt_txt")
                    if (dateTime.endsWith("15:00:00")) { // Filter for the time 15:00
                        val date = dateFormat.parse(dateTime.substring(0, 10))
                        val temperature = forecastWeatherObject.getJSONObject("main").getDouble("temp")
                        val weatherArray = forecastWeatherObject.getJSONArray("weather")
                        val weatherObject = weatherArray.getJSONObject(0)
                        val description = weatherObject.getString("description")
                        val iconCode = weatherObject.getString("icon")
                        val windSpeed = forecastWeatherObject.getJSONObject("wind").getInt("speed")
                        val humidity = forecastWeatherObject.getJSONObject("main").getInt("humidity")
                        val pressure = forecastWeatherObject.getJSONObject("main").getInt("pressure")

                        // Check to exclude the current day
                        if (date != null && dateFormat.format(date) != currentDate) {
                            val formattedDate = dateFormat.format(date)

                            // Create ForecastWeather object and add it to the list
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

                for (i in forecastWeatherList.indices) {
                    val forecastWeather = forecastWeatherList[i]
                    val date = forecastWeather.date
                    val temperature = forecastWeather.temperature
                    val iconCode = forecastWeather.iconCode
                    val desc = forecastWeather.description
                    val wind = forecastWeather.wind
                    val pressure = forecastWeather.pressure
                    val humidity = forecastWeather.humidity

                    // Generation of interface element identifiers based on the index i
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
                    dateText.text = date
                    descText.text = desc.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.ROOT
                        ) else it.toString()
                    }
                    infoText.text = "${langDict[lang]?.get("wind")}: $wind ${langDict[lang]?.get("m/s")};" +
                            System.lineSeparator() +
                            "${langDict[lang]?.get("humidity")}: $humidity %; " +
                            System.lineSeparator() +
                            "${langDict[lang]?.get("pressure")}: $pressure ${langDict[lang]?.get("hPa")}"
                }
                // Clear list
                forecastWeatherList.clear()
                swipeRefreshLayout.isRefreshing = false
                button_refresh.isEnabled = true

            } else {
                Log.e("WeatherData", "Error fetching weather data")
                swipeRefreshLayout.isRefreshing = false
                button_refresh.isEnabled = true
            }
        }
    }
}