package com.mishrashubhamkumar786.weatherapplication

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mishrashubhamkumar786.weatherapplication.models.Weather
import com.mishrashubhamkumar786.weatherapplication.models.WeatherResponse
import com.mishrashubhamkumar786.weatherapplication.network.WeatherService
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        if(!isLocationEnabled()){
            Toast.makeText(this, "your location provider is turned off, please turn it on, We are redirecting you to desired settings", Toast.LENGTH_LONG).show()
            Timer().schedule(4000) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

        }else{
            //Toast.makeText(this,"Hurry! Your Location is already turned On", Toast.LENGTH_LONG).show()
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                                requestLocationData()
                        }


                        if (report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity,"You have denied some most important perqmissions",Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                            showRelationalDialogForPermissions()
                        }
                    }).onSameThread()
                    .check()

        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
                Looper.myLooper()
        )
    }


    private val mLocationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {

        val mLastLocation:Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")
            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")
            val let = "Lat"+" "+latitude.toString()
            tv_let.setText(let)
            val lon = "Lon"+" "+longitude.toString()
            tv_lon.setText(lon)
            getLocationWeatherDetails(latitude,longitude)

        }
    }

    private fun getLocationWeatherDetails(latitude:Double, longitude:Double){
        if(Constants.isNetworkAvailable(this)){
            //Toast.makeText(this@MainActivity,"Yuhuuuu!!! you are connected to internet and are eligible to make a API Call", Toast.LENGTH_SHORT).show()
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service : WeatherService = retrofit
                .create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude , longitude , Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog() //for calling Progress Dialog just before downloading the content
            listCall.enqueue(object: Callback<WeatherResponse>{

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Error",t!!.message.toString())
                    hideProgressDialog() //if data loading gets failed then the progress dialog get hidden
                }



                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response!!.isSuccessful){
                        //time to close the Progress Dialog bar
                        hideProgressDialog()

                        val weatherList: WeatherResponse = response.body()!!
                        setupUI(weatherList)
                        Log.i("Response Result","$weatherList")
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 -> Log.e("Error 400","Bad Connection")
                            404 -> Log.e("Error 404", "Not Found")
                            else -> Log.e("Error","Generic Error")
                        }
                    }
                }

            })

        }else{
            Toast.makeText(this@MainActivity,"No Internet Connection!!!",Toast.LENGTH_SHORT).show()
        }
    }




    private fun showRelationalDialogForPermissions(){
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have Turned off permission required for this feature. It need to be enabled under Application settings")
            .setPositiveButton(
                "Go to settings"
            ){ _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                } catch(e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("cancel"){ dialog,
                                          _->
                dialog.dismiss()
            }.show()
    }


    //for checking the location is enable or not

    private fun isLocationEnabled():Boolean{
        // this provides access to the system location services

        val locationManager: LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    //for showing Progress Dialog
    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocationData()
                true
            }else->  super.onOptionsItemSelected(item)
        }
    }



    //for hiding Progress Dialog
    private fun hideProgressDialog(){
        if(mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }


    //for setting up the UI
    private fun setupUI(weatherList: WeatherResponse){
        for(i in weatherList.weather.indices){
            tv_daytype.text = weatherList.weather[i].main
            weather_desc.text = weatherList.weather[i].description
            tv_temperature.text = weatherList.main.temp_max.toString()
            tv_windandspeed.text = weatherList.wind.speed.toString()
            tv_windandspeed.append("miles/hour")
            tv_temperature.append("\u00B0 C")
            min_temp.append("min")
            min_temp.text = weatherList.main.temp_min.toString()
            min_temp.append("\u00B0 C")
            tv_cityName.text = weatherList.name
            tv_country.text = weatherList.sys.country
            tv_sunrise.text = unixTime(weatherList.sys.sunrise)
            tv_sunset.text = unixTime(weatherList.sys.sunset)
        }
    }


    private fun unixTime(timex:Long):String?{
        val date = Date(timex *1000L)
        val sdf=
            SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }
}
