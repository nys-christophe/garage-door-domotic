package com.isodroid.portedegarage

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.isodroid.portedegarage.service.LocationService
import com.getlocationbackground.util.Util
import com.github.kittinunf.fuel.Fuel
import com.ncorti.slidetoact.SlideToActView
import com.ncorti.slidetoact.SlideToActView.OnSlideCompleteListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*
import java.lang.reflect.Method
import java.util.*


class MainActivity : AppCompatActivity() {

    var resultStr = ""

    var mLocationService: LocationService = LocationService()
    lateinit var mServiceIntent: Intent
    lateinit var mActivity: Activity

    companion object {
        var restart = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mActivity = this@MainActivity

        setContentView(R.layout.activity_main2)

        if (!Util.isLocationEnabledOrNot(mActivity)) {
            Util.showAlertLocation(
                mActivity,
                getString(R.string.gps_enable),
                getString(R.string.please_turn_on_gps),
                getString(
                    R.string.ok
                )
            )
        }

        requestPermissionsSafely(
            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION), 200
        )


        slide.onSlideCompleteListener =
            object : OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    triggerDoor(BuildConfig.URL_OPEN)
                    slide.resetSlider()
                }
            }

        slide2.onSlideCompleteListener =
            object : OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    triggerDoor(BuildConfig.URL_OPEN_CLOSE)
                    slide2.resetSlider()
                }
            }

        slide3.onSlideCompleteListener =
            object : OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    mLocationService = LocationService()
                    mServiceIntent = Intent(this@MainActivity, mLocationService.javaClass)
                    if (Util.isMyServiceRunning(mLocationService.javaClass, mActivity)) {
                        Toast.makeText(
                            mActivity,
                            getString(R.string.service_shutdown),
                            Toast.LENGTH_SHORT
                        ).show()
                        restart = false
                        stopService(mServiceIntent)
                    } else {
                        Toast.makeText(
                            mActivity,
                            getString(R.string.service_start_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        LocationService.outside = false
                        restart = true
                        startService(mServiceIntent)
                    }
                    slide3.resetSlider()
                }
            }

    }


    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsSafely(
        permissions: Array<String>,
        requestCode: Int
    ) {
        requestPermissions(permissions, requestCode)
    }

    override fun onDestroy() {
        if (::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        super.onDestroy()
    }


    private fun triggerDoor(url: String) {
        Fuel.get(url)
            .responseString { request, response, result ->
                when (result) {
                    is com.github.kittinunf.result.Result.Failure -> {
                        val ex = result.getException()
                        Toast.makeText(this@MainActivity, ex.toString(), Toast.LENGTH_LONG).show()
                    }
                    is com.github.kittinunf.result.Result.Success -> {
                        val data = result.get()
                        if (data == "Open/Close") {
                            Toast.makeText(this@MainActivity, "OK", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "KO !!!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }


    fun setMobileDataState(mobileDataEnabled: Boolean) {
        try {
            val telephonyService = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val setMobileDataEnabledMethod: Method =
                Objects.requireNonNull(telephonyService).javaClass.getDeclaredMethod(
                    "setDataEnabled",
                    Boolean::class.javaPrimitiveType
                )
            setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e("MainActivity", "Error setting mobile data state", ex)
        }
    }

    private fun tap(s: String) {
        resultStr = "${resultStr}$s"
        result.text = "${result.text}*"
        if (resultStr.length == 5) {
            if (resultStr == "31519")
                openCloseGarageDoor()
            else finish()
        }
    }

    private fun openCloseGarageDoor() {
        val wifiManager =
            getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            showPowerButton()
        } else {
            wifiManager.isWifiEnabled = true
            // Attends un peu
            SystemClock.sleep(3000)
            showPowerButton()
        }
    }

    private fun showPowerButton() {
        result.text = ""
        button1.visibility = View.GONE
        button2.visibility = View.GONE
        button3.visibility = View.GONE
        button4.visibility = View.GONE
        button5.visibility = View.GONE
        button6.visibility = View.GONE
        button7.visibility = View.GONE
        button8.visibility = View.GONE
        button9.visibility = View.GONE
        result.visibility = View.GONE

        power.visibility = View.VISIBLE
    }
}
