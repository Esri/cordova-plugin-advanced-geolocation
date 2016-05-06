/**
 * @author Andy Gup
 *
 * Copyright 2016 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.​
 */
package com.esri.cordova.geolocation.controllers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.esri.cordova.geolocation.model.Coordinate;
import com.esri.cordova.geolocation.model.InitStatus;
import com.esri.cordova.geolocation.model.LocationDataBuffer;
import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

public final class NetworkLocationController implements Runnable {

    private static LocationManager _locationManager = null;
    private static LocationListener _locationListenerNetworkProvider = null;

    private static CallbackContext _callbackContext; // Threadsafe
    private static CordovaInterface _cordova;

    private static long _minDistance = 0;
    private static long _minTime = 0;
    private static boolean _buffer = false;
    private static int _bufferSize = 0;
    private static boolean _returnCache = false;
    private static LocationDataBuffer _locationDataBuffer = null;

    private static final String TAG = "GeolocationPlugin";
    public static final String SATELLITE_PROVIDER = "satellite";

    public NetworkLocationController(
            CordovaInterface cordova,
            CallbackContext callbackContext,
            long minDistance,
            long minTime,
            boolean returnCache,
            boolean buffer,
            int bufferSize
    ){
        _cordova = cordova;
        _callbackContext = callbackContext;
        _minDistance = minDistance;
        _minTime = minTime;
        _returnCache = returnCache;
        _buffer = buffer;
        _bufferSize = bufferSize;
    }

    public void run(){
        // Reference: http://developer.android.com/reference/android/os/Process.html#THREAD_PRIORITY_BACKGROUND
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        // We are running a Looper to allow the Cordova CallbackContext to be passed within the Thread as a message.
        if(Looper.myLooper() == null){
            _locationManager = (LocationManager) _cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
            Looper.prepare();
            startLocation();
            Looper.loop();
        }
    }

    public void startLocation(){

        if(!Thread.currentThread().isInterrupted()){

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    Log.d(TAG, "Failing gracefully after detecting an uncaught exception on NetworkLocationController thread.");
                }
            });

            if(_buffer) {
                _locationDataBuffer = new LocationDataBuffer(_bufferSize);
            }

            final InitStatus l2 = setLocationListenerNetworkProvider();

            if(!l2.success){
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, l2.exception));
            }
            else {

                // Return cache immediate if requested, otherwise wait for a location provider
                if(_returnCache){
                    final Location location = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    final String parsedLocation;

                    // If the provider is disabled or currently unavailable then null may be returned on some devices
                    if(location != null) {
                        parsedLocation = JSONHelper.locationJSON(LocationManager.NETWORK_PROVIDER, location, true);
                        sendCallback(PluginResult.Status.OK, parsedLocation);
                    }
                }
            }
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){

        if(_locationManager != null){
            if(_locationListenerNetworkProvider != null){
                _locationManager.removeUpdates(_locationListenerNetworkProvider);
                _locationListenerNetworkProvider = null;
            }

            _locationManager = null;

            // Clear all elements from the buffer
            if(_locationDataBuffer != null) {
                _locationDataBuffer.clear();
            }

            Thread.currentThread().interrupt();
        }

        Log.d(TAG, "Stopping network geolocation");
    }

    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.interrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }

    private InitStatus setLocationListenerNetworkProvider() {

        _locationListenerNetworkProvider = new LocationListener() {

            public void onLocationChanged(Location location) {

                if(_buffer){
                    final Coordinate coordinate = new Coordinate();
                    coordinate.latitude = location.getLatitude();
                    coordinate.longitude = location.getLongitude();
                    coordinate.accuracy = location.getAccuracy();

                    // Get the size of the buffer
                    final int size = _locationDataBuffer.add(coordinate);

                    final Coordinate center = _locationDataBuffer.getGeographicCenter();

                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.locationJSON(
                                    LocationManager.NETWORK_PROVIDER,
                                    location,
                                    false,
                                    _buffer,
                                    center.latitude,
                                    center.longitude,
                                    center.accuracy,
                                    size)
                    );
                }
                else {
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.locationJSON(LocationManager.NETWORK_PROVIDER, location, false));
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.d(TAG, "Location Status Changed: Network Provider Out of Service");
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.NETWORK_PROVIDER, "Network provider out of service"));
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "Location Status Changed: Network Provider Temporarily Unavailable");
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.NETWORK_PROVIDER, "Network provider temporarily unavailable"));
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "Location Status Changed: Network Provider Available");
                        break;
                }
            }

            public void onProviderEnabled(String provider) {
                startLocation();
            }

            public void onProviderDisabled(String provider) {
                stopLocation();
            }
        };

        final InitStatus status = new InitStatus();
        final Boolean networkProviderEnabled = _locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(networkProviderEnabled){
            try {
                Log.d(TAG, "Starting LocationManager.NETWORK_PROVIDER");
                // Register the listener with the Location Manager to receive location updates
                _locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, _minTime, _minDistance, _locationListenerNetworkProvider);

            } catch (Exception exc) {
                Log.d(TAG, "Unable to start network provider. " + exc.getMessage());
                status.success = false;
                status.exception = exc.getMessage();
            }
        }
        else {
            status.success = false;
        }

        return status;
    }
}