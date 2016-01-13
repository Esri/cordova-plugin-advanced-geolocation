package com.esri.cordova.geolocation.controllers;


import android.location.GpsStatus;
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
    public static final String GPS_PROVIDER = "gps";

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
        _locationManager = (LocationManager) _cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
    }

    public void run(){
        // Reference: http://developer.android.com/reference/android/os/Process.html#THREAD_PRIORITY_BACKGROUND
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        // We are running a Looper to allow the Cordova CallbackContext to be passed within the Thread as a message.
        if(Looper.myLooper() == null){
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
                Log.d(TAG, "Failing gracefully after detecting an uncaught exception on GPSController thread. "
                        + throwable.getMessage());
                }
            });

            if(_buffer) {
                _locationDataBuffer = new LocationDataBuffer(_bufferSize);
            }

            final InitStatus l2 = setLocationListenerGPSProvider();
            InitStatus l3 = new InitStatus();

            if(_returnSatelliteData){
               l3 = setGPSStatusListener();
            }

            if(!l2.success || !l3.success){
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, l2.exception + ", " + l3.exception));
            }
            else {
                // Return cache immediate if requested, otherwise wait for a location provider
                if(_returnCache){
                    final Location location = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    final String parsedLocation;

                    // If the provider is disabled or currently unavailable then null is returned
                    // Some devices will return null if the GPS is still warming up and hasn't gotten
                    // a full signal lock yet.
                    if(location == null) {
                        // Basically return all zeros as a standard practice
                        parsedLocation = "{\"provider\":\"gps\",\"latitude\":\"0.0\",\"longitude\":\"0.0\",\"altitude\":\"0.0\",\"accuracy\":\"50.0\",\"bearing\":\"0.0\",\"speed\":\"0.0\",\"timestamp\":\"0\",\"cached\":\"true\"}";
                    }
                    else {
                        parsedLocation = JSONHelper.locationJSON(LocationManager.GPS_PROVIDER, location, true);
                    }
                    sendCallback(PluginResult.Status.OK, parsedLocation);
                }
            }
        }
        else {
            Log.d(TAG, "Not starting GPSController due to thread interrupt.");
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){

        if(_locationManager != null){
            if(_locationListenerGPSProvider != null){
                _locationManager.removeUpdates(_locationListenerGPSProvider);
                _locationListenerGPSProvider = null;
            }

            if(_gpsStatusListener != null){
                _locationManager.removeGpsStatusListener(_gpsStatusListener);
                _gpsStatusListener = null;
            }

            _locationManager = null;

            // Clear all elements from the buffer
            if(_locationDataBuffer != null) {
                _locationDataBuffer.clear();
            }

            Thread.currentThread().interrupt();
        }

        Log.d(TAG, "Stopping gps geolocation");
    }

    /**
     * Callback handler for this Class
     * @param status Message status
     * @param message Any message
     */
    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.interrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }

    private static InitStatus setGPSStatusListener(){
        _gpsStatusListener = new GpsStatus.Listener() {

            @Override
            public void onGpsStatusChanged(int event) {
                Log.d(TAG, "GPS status changed.");
                sendCallback(PluginResult.Status.OK,
                        JSONHelper.satelliteDataJSON(_locationManager.getGpsStatus(null)));
            }
        };

        final InitStatus status = new InitStatus();

        _locationManager = (LocationManager) _cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
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
            status.exception = "GPS provider not enabled";
        }

        return status;
    }

    private InitStatus setLocationListenerGPSProvider(){

        _locationListenerGPSProvider = new LocationListener() {

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
                        Log.d(TAG, "Location Status Changed: GPS Out of Service");
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, "GPS out of service"));

                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "Location Status Changed: GPS Temporarily Unavailable");
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, "GPS temporarily unavailable"));
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "Status Changed: GPS Available");
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
            catch(Exception exc){
                Log.d(TAG, "Unable to start GPS provider. " + exc.getMessage());
                status.success = false;
                status.exception = exc.getMessage();
            }
        }
        else {
            //GPS not enabled
            status.success = false;
            status.exception = "GPS provider not enabled";
        }

        return status;
    }
}
