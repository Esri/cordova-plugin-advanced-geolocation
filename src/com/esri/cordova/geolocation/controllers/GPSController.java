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
import android.location.GpsStatus;
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
import com.esri.cordova.geolocation.utils.ErrorMessages;
import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

public final class GPSController implements Runnable {

    private static LocationManager _locationManager = null;
    private static LocationListener _locationListenerGPSProvider = null;
    private static GpsStatus.Listener _gpsStatusListener = null;

    private static CallbackContext _callbackContext; // Threadsafe
    private static CordovaInterface _cordova;

    private static long _minDistance = 0;
    private static long _minTime = 0;
    private static boolean _buffer = false;
    private static int _bufferSize = 0;
    private static boolean _returnCache = false;
    private static boolean _returnSatelliteData = false;
    private static LocationDataBuffer _locationDataBuffer = null;

    private static final String TAG = "GeolocationPlugin";

    public GPSController(
            CordovaInterface cordova,
            CallbackContext callbackContext,
            long minDistance,
            long minTime,
            boolean returnCache,
            boolean returnSatelliteData,
            boolean buffer,
            int bufferSize
    ){
        _cordova = cordova;
        _callbackContext = callbackContext;
        _minDistance = minDistance;
        _minTime = minTime;
        _returnCache = returnCache;
        _returnSatelliteData = returnSatelliteData;
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
            Log.i(TAG,"Available location providers: " + _locationManager.getAllProviders().toString());

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    Log.e(TAG, "Failing gracefully after detecting an uncaught exception on GPSController thread. "
                        + throwable.getMessage());

                    sendCallback(PluginResult.Status.ERROR,
                            JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.UNCAUGHT_THREAD_EXCEPTION()));
                    stopLocation();
                }
            });

            if(_buffer) {
                _locationDataBuffer = new LocationDataBuffer(_bufferSize);
            }

            final InitStatus gpsListener = setLocationListenerGPSProvider();
            InitStatus satelliteListener = new InitStatus();

            if(_returnSatelliteData){
               satelliteListener = setGPSStatusListener();
            }

            if(!gpsListener.success || !satelliteListener.success){
                if(gpsListener.exception == null){
                    // Handle custom error messages
                    sendCallback(PluginResult.Status.ERROR,
                            JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, gpsListener.error));
                }
                else {
                    // Handle system exceptions
                    sendCallback(PluginResult.Status.ERROR,
                            JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, gpsListener.exception));
                }
            }
            else {
                // Return cache immediate if requested, otherwise wait for a location provider
                if(_returnCache){

                    Location location = null;

                    try {
                        location = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    catch(SecurityException exc){
                        Log.e(TAG, exc.getMessage());
                    }

                    final String parsedLocation;

                    // If the provider is disabled or currently unavailable then null is returned
                    // Some devices will return null if the GPS is still warming up and hasn't gotten
                    // a full signal lock yet.
                    if(location != null) {
                        parsedLocation = JSONHelper.locationJSON(LocationManager.GPS_PROVIDER, location, true);
                        sendCallback(PluginResult.Status.OK, parsedLocation);
                    }
                }
            }
        }
        else {
            Log.e(TAG, "Not starting GPSController due to thread interrupt.");
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){

        if(_locationManager != null){
            Log.d(TAG, "Attempting to stop gps geolocation");

            if(_gpsStatusListener != null){
                _locationManager.removeGpsStatusListener(_gpsStatusListener);
                _gpsStatusListener = null;
            }

            if(_locationListenerGPSProvider != null){

                try {
                    _locationManager.removeUpdates(_locationListenerGPSProvider);
                }
                catch(SecurityException exc){
                    Log.e(TAG, exc.getMessage());
                }

                _locationListenerGPSProvider = null;
            }

            _locationManager = null;

            // Clear all elements from the buffer
            if(_locationDataBuffer != null) {
                _locationDataBuffer.clear();
            }

            try {
                Thread.currentThread().interrupt();
            }
            catch(SecurityException exc){
                Log.e(TAG, exc.getMessage());
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.FAILED_THREAD_INTERRUPT()));
            }
        }
        else{
            Log.d(TAG, "GPS location already stopped");
        }
    }

    /**
     * Callback handler for this Class
     * @param status Message status
     * @param message Any message
     */
    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.currentThread().isInterrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }

    private static InitStatus setGPSStatusListener(){

        // IMPORTANT: The GpsStatus.Listener Interface is deprecated at API 24.
        // Reference: https://developer.android.com/reference/android/location/package-summary.html
        _gpsStatusListener = new GpsStatus.Listener() {

            @Override
            public void onGpsStatusChanged(int event) {
                Log.d(TAG, "GPS status changed.");

                // Ignore if GPS_EVENT_STARTED or GPS_EVENT_STOPPED
                if(!Thread.currentThread().isInterrupted() &&
                        (event == GpsStatus.GPS_EVENT_FIRST_FIX ||
                                event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) &&
                                        _locationManager != null){
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.satelliteDataJSON(_locationManager.getGpsStatus(null)));
                }
            }
        };

        final InitStatus status = new InitStatus();

        final Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(gpsProviderEnabled){
            try{
                _locationManager.addGpsStatusListener(_gpsStatusListener);
            }
            // if the ACCESS_FINE_LOCATION permission is not present
            catch(SecurityException exc){
                status.success = false;
                status.exception = exc.getMessage();
            }
        }
        else {
            //GPS not enabled
            status.success = false;
            status.error = ErrorMessages.GPS_UNAVAILABLE();
        }

        return status;
    }

    private InitStatus setLocationListenerGPSProvider(){

        _locationListenerGPSProvider = new LocationListener() {

            public void onLocationChanged(Location location) {
                if(_buffer && !Thread.currentThread().isInterrupted()){
                    final Coordinate coordinate = new Coordinate();
                    coordinate.latitude = location.getLatitude();
                    coordinate.longitude = location.getLongitude();
                    coordinate.accuracy = location.getAccuracy();

                    // Get the size of the buffer
                    final int size = _locationDataBuffer.add(coordinate);

                    final Coordinate center = _locationDataBuffer.getGeographicCenter();

                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.locationJSON(
                                    LocationManager.GPS_PROVIDER,
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
                            JSONHelper.locationJSON(LocationManager.GPS_PROVIDER, location, false));
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        // Reference: https://developer.android.com/reference/android/location/LocationProvider.html#OUT_OF_SERVICE
                        Log.d(TAG, "Location Status Changed: " + ErrorMessages.GPS_OUT_OF_SERVICE().message);
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.GPS_OUT_OF_SERVICE()));

                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "Location Status Changed: " + ErrorMessages.GPS_UNAVAILABLE().message);
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.GPS_UNAVAILABLE()));
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "Location Status Changed: GPS Available");
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
        final Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(gpsProviderEnabled){

            try{
                Log.d(TAG, "Starting LocationManager.GPS_PROVIDER");
                // Register the listener with the Location Manager to receive location updates
                _locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, _minTime, _minDistance, _locationListenerGPSProvider);
            }
            catch(SecurityException exc){
                Log.e(TAG, "Unable to start GPS provider. " + exc.getMessage());
                status.success = false;
                status.exception = exc.getMessage();
            }
        }
        else {
            Log.w(TAG, ErrorMessages.GPS_UNAVAILABLE().message);
            //GPS not enabled
            status.success = false;
            status.error = ErrorMessages.GPS_UNAVAILABLE();
        }

        return status;
    }
}
