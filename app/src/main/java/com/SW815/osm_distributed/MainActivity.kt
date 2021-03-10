/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.SW815.osm_distributed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import org.osmdroid.config.Configuration
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MainActivity : AppCompatActivity(){

  private val REQUEST_PERMISSIONS_REQUEST_CODE = 1;

  private lateinit var map : MapView;

  val REQUEST_CHECK_SETTINGS = 1

  lateinit var locationManager : LocationManager;

  lateinit var locationRequest : LocationRequest;

  lateinit var task: Task<LocationSettingsResponse>;


  lateinit var fusedLocationClient : FusedLocationProviderClient;
  lateinit var locationCallback: LocationCallback;
  var wayLatitude : Double = 0.0;
  var wayLongitude : Double = 0.0;
  private lateinit var myLocationOverlay: MyLocationNewOverlay


  override fun onCreate(savedInstanceState: Bundle?) {
    //map.getInstance(this, "pk.eyJ1IjoiY2tyZWpsMTciLCJhIjoiY2tsczA2dzE5MXJueDJ2bjNwNjZzcG80ciJ9.Hgs5VQhjkfkuOppslUuW4Q")

    var firstRun : Boolean = true;
    super.onCreate(savedInstanceState)
    getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    setContentView(R.layout.activity_main)

    createLocationRequest()

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

    val client: SettingsClient = LocationServices.getSettingsClient(this)
    task = client.checkLocationSettings(builder.build()) as Task<LocationSettingsResponse>


    task.addOnSuccessListener { locationSettingsResponse ->
      startLocationUpdates()
    }

    task.addOnFailureListener { exception ->
      println(exception.message)
      if (exception is ResolvableApiException){

        // Location settings are not satisfied, but this can be fixed
        // by showing the user a dialog.
        try {
          // Show the dialog by calling startResolutionForResult(),
          // and check the result in onActivityResult().
          exception.startResolutionForResult(this@MainActivity,
                  REQUEST_CHECK_SETTINGS)
        } catch (sendEx: IntentSender.SendIntentException) {
          finish()
        }
      }
    }


    map = findViewById<MapView>(R.id.map)
    map.setTileSource(TileSourceFactory.MAPNIK)
    Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)
    val mapController = map.controller
    mapController.setZoom(9.5)
    var startPoint = GeoPoint(wayLatitude, wayLongitude);
    mapController.setCenter(startPoint);





    locationCallback = object : LocationCallback() {
      @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
      override fun onLocationResult(locationResult: LocationResult?) {
        locationResult ?: return
        for (location in locationResult.locations){
          wayLatitude = location.latitude
          wayLongitude = location.longitude
          startPoint = GeoPoint(wayLatitude, wayLongitude);

          if(firstRun) {
            mapController.setCenter(startPoint);
            firstRun = false;
          }
          locationMarker(startPoint)
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CHECK_SETTINGS) {
      if (resultCode == Activity.RESULT_OK) {
        startLocationUpdates()
      } else if (resultCode == Activity.RESULT_CANCELED) {
        finish()
      }
    }
  }

  @SuppressWarnings("MissingPermission")
  override fun onStart() {
    super.onStart()

  }

  override fun onResume() {
    super.onResume()
    //startLocationUpdates()

    //map.onResume()
  }

  override fun onPause() {
    super.onPause()
    //map.onPause()
  }


  //Location
  fun createLocationRequest() {
    locationRequest = LocationRequest.create().apply {
      interval = 10000
      fastestInterval = 1000
      priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
  }

  @SuppressWarnings("MissingPermission")
  private fun startLocationUpdates() {

    fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  private fun locationMarker(startPoint : GeoPoint){


    val newItem = OverlayItem("Here", "You are here", startPoint)

    var items : List<OverlayItem> = mutableListOf(newItem);

    val myLocOverlay = ItemizedIconOverlay<OverlayItem>(items,
            resources.getDrawable(R.drawable.ic_menu_mylocation, null),
            object : OnItemGestureListener<OverlayItem?> {
              override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                return false
              }

              override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return false
              }
            },
            this)
    map.overlays.add(myLocOverlay)
  }


}
