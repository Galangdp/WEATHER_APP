package com.galang.weatherapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.galang.weather.BuildConfig
import com.galang.weather.databinding.ActivityMainBinding
import com.galang.weatherapp.data.response.ForecastResponse
import com.galang.weatherapp.data.response.WeatherResponse
import com.galang.weatherapp.utils.HelperFunctions.formatterDegree
import com.galang.weatherapp.utils.LOCATION_PERMISSIONS_REQ_CODE
import com.galang.weatherapp.utils.iconSizeWeather4x
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding as ActivityMainBinding

    private var _viewModel: MainViewModel? = null
    private val viewModel get() = _viewModel as MainViewModel

    private val weatherAdapter by lazy {  WeatherAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetController?.isAppearanceLightNavigationBars = true

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        searchCity()
        getWeatherByCity()
        getWeatherCurrentLocation()

    }

    private fun getWeatherByCity() {
        viewModel.getWeatherByCity().observe(this) {
            binding.tvCity.text = it.name
            binding.tvDegree.text = it.main?.temp?.let { it1 -> formatterDegree(it1).toString() }

            val icon = it.weather?.get(0)?.icon
            val iconUrl = BuildConfig.ICON_URL + icon + iconSizeWeather4x
            Glide.with(this).load(iconUrl)
                .into(binding.imgIcWeather)
        }

        viewModel.getForecastByCity().observe(this){
            weatherAdapter.setData(it.list)

            binding.rvForecastWeather.apply {
                layoutManager = LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)
                adapter = weatherAdapter
            }
        }
    }

    private fun getWeatherCurrentLocation() {
        val fusedLocationProvider : FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    LOCATION_PERMISSIONS_REQ_CODE
                )
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProvider.lastLocation.addOnSuccessListener {
            val lat = it.latitude
            val lon = it.longitude

            viewModel.weatherByCurrentLocation(lat, lon)
            viewModel.foreCastByCurrentLocation(lat, lon)
        }
            .addOnFailureListener {
                Log.e("MainActivity", "FusedLocationError: Failes getting current location.", )
            }

        viewModel.getWeatherByCurrentLocation().observe(this){
            setupView(it,null)
        }

        viewModel.getForecastByCurrentLocation().observe(this){
            setupView(null, it)
        }
    }

    private fun setupView(weather: WeatherResponse?, forecast: ForecastResponse?){
        weather?.let {
            binding.apply {
                binding.tvDegree.text = formatterDegree(weather.main?.temp)
                binding.tvCity.text = weather.name

                val icon = weather.weather?.get(0)?.icon
                val iconUrl = BuildConfig.ICON_URL + icon + iconSizeWeather4x
                Glide.with(applicationContext).load(iconUrl)
                    .into(binding.imgIcWeather)

                rvForecastWeather.apply {
                    layoutManager =
                        LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = weatherAdapter
                }
            }
        }
    }

    private fun searchCity() {
        binding.edtSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.weatherByCity(it)
                    // hide keyboard
                    viewModel.forecastByCity(it)
                }
                try {
                    val inputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error hiding keyboard: ${e.message}")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

}