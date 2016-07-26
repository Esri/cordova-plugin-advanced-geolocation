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
package com.esri.cordova.geolocation;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.esri.cordova.geolocation.controllers.CellLocationController;
import com.esri.cordova.geolocation.controllers.GPSController;
import com.esri.cordova.geolocation.controllers.NetworkLocationController;
import com.esri.cordova.geolocation.fragments.GPSAlertDialogFragment;
import com.esri.cordova.geolocation.fragments.NetworkUnavailableDialogFragment;
import com.esri.cordova.geolocation.utils.ErrorMessages;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


public class AdvancedGeolocation extends CordovaPlugin {

    public static final String PROVIDERS_ALL = "all";
    public static final String PROVIDERS_SOME = "some";
    public static final String PROVIDERS_GPS = "gps";
    public static final String PROVIDERS_NETWORK = "network";
    public static final String PROVIDERS_CELL = "cell";

    private static final String TAG = "GeolocationPlugin";
    private static final String SHARED_PREFS_KEY = "LocationSettings";
    private static final String SHARED_PREFS_ACTION = "action";
    private static final int MIN_API_LEVEL = 18;
    private static final int REQUEST_LOCATION_PERMS_CODE = 10;

    private static long _minDistance = 0;
    private static long _minTime = 0;
    private static boolean _noWarn;
    private static String _providers;
    private static boolean _useCache;
    private static boolean _returnSatelliteData = false;
    private static boolean _buffer = false;
    private static int _bufferSize = 0;

    private static GPSController _gpsController = null;
    private static NetworkLocationController _networkLocationController = null;
    private static CellLocationController _cellLocationController = null;
    private static LocationManager _locationManager;
    private static CordovaInterface _cordova;
    private static Activity _cordovaActivity;
    private static CallbackContext _callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        _cordova = cordova;
        _cordovaActivity = cordova.getActivity();
        removeActionPreferences();
        Log.d(TAG, "Initialized");
    }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException{
        _callbackContext = callbackContext;

        Log.d(TAG, "Action = " + action);

        // Save this action so we can refer to it when the app restarts
        setSharedPreferences(SHARED_PREFS_KEY, action);

        if (args != null) {
            parseArgs(args);
        }

        return runAction(action);
    }

    private boolean runAction(final String action){

        // NOTE: LocationManager.isProviderEnabled only verifies if the provider is enabled in the Settings menu!
        _locationManager = (LocationManager) _cordovaActivity.getSystemService(Context.LOCATION_SERVICE);
        final boolean networkLocationEnabled = _locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        final boolean gpsEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = isInternetConnected(_cordovaActivity.getApplicationContext());

//        final String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
//
//        final PreferencesHelper preferencesHelper = new PreferencesHelper(_cordova, this);
//        Log.d(TAG, "MANIFEST: " + preferencesHelper.checkManifest().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Reference: Permission Groups https://developer.android.com/guide/topics/security/permissions.html#normal-dangerous
            if(_cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || _cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){
                //TODO
            }
            else {
                final String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                _cordova.requestPermissions(this, REQUEST_LOCATION_PERMS_CODE, perms);
                return true;
            }
        }
        else {
            // If warnings are disabled then skip initializing alert dialog fragments
            if(!_noWarn && (!networkLocationEnabled || !gpsEnabled || !networkEnabled)){
                alertDialog(gpsEnabled, networkLocationEnabled, networkEnabled);
                return true;
            }
        }

        return handleStartActions(action);
    }

    private boolean handleStartActions(final String action){
        if(action.equals("start")){
            startLocation();
            return true;
        }
        if(action.equals("stop")){
            stopLocation();
            return true;
        }
        if(action.equals("kill")){
            onDestroy();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Cordova callback when querying for permissions
     * FYI: you can verify device permissions when in debug mode using:
     * <code>adb shell pm list permissions -d -g</code>
     * @param requestCode The request code we assign - it's basically a token
     * @param permissions The requested permissions - never null
     * @param grantResults <code>PERMISSION_GRANTED</code> or <code>PERMISSION_DENIED</code> - never null
     * @throws JSONException
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        if(requestCode == REQUEST_LOCATION_PERMS_CODE){
            Log.d(TAG,"YES");
        }
    }

    private void startLocation(){

        // Misc. note: If you see the message "Attempted to send a second callback for ID:" then you need
        // to make sure to set pluginResult.setKeepCallback(true);

        final boolean networkEnabled = isInternetConnected(_cordovaActivity.getApplicationContext());

        if(_providers.equalsIgnoreCase(PROVIDERS_ALL)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_gpsController);

            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_networkLocationController);

            // Reference: https://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
            // Reference: https://developer.android.com/reference/android/telephony/CellIdentityWcdma.html (added at API 18)
            if (Build.VERSION.SDK_INT < MIN_API_LEVEL){
                Log.w(TAG, ErrorMessages.CELLDATA_NOT_ALLOWED);
            }
            else {
                _cellLocationController = new CellLocationController(networkEnabled,_cordova,_callbackContext);
                cordova.getThreadPool().execute(_cellLocationController);
            }
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_SOME)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_gpsController);

            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_networkLocationController);

        }
        if(_providers.equalsIgnoreCase(PROVIDERS_GPS)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_gpsController);
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_NETWORK)){
            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            cordova.getThreadPool().execute(_networkLocationController);
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_CELL)){

            // Reference: https://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
            // Reference: https://developer.android.com/reference/android/telephony/CellIdentityWcdma.html
            if (Build.VERSION.SDK_INT < MIN_API_LEVEL){
                Log.w(TAG, ErrorMessages.CELLDATA_NOT_ALLOWED);
                sendCallback(PluginResult.Status.ERROR, ErrorMessages.CELLDATA_NOT_ALLOWED);
            }
            else {
                _cellLocationController = new CellLocationController(networkEnabled,_cordova,_callbackContext);
                cordova.getThreadPool().execute(_cellLocationController);
            }
        }
    }

    /**
     * Halt any active providers.
     */
    private void stopLocation(){

        if(_locationManager != null){
            if(_gpsController != null){
                // Gracefully attempt to stop location
                _gpsController.stopLocation();

                // make sure there are no references
                _gpsController = null;
            }
            if(_networkLocationController != null){
                // Gracefully attempt to stop location
                _networkLocationController.stopLocation();

                // make sure there are no references
                _networkLocationController = null;
            }

            // make sure there are no references
            _locationManager = null;
        }

        // CellLocationController does not require LocationManager
        if(_cellLocationController != null){
            // Gracefully attempt to stop location
            _cellLocationController.stopLocation();

            // make sure there are no references
            _cellLocationController = null;
        }

        Log.d(TAG, "Stopping geolocation");
    }

    /**
     * Retrieves shared preferences to find out what action was requested when the app
     * originally launched. Resumes based on that last action.
     * @param multitasking Unused in this API. Flag indicating if multitasking is turned on for app
     * and is inherited from Cordova.
     */
    public void onResume(boolean multitasking){
        Log.d(TAG, "onResume");
        if(_locationManager != null){
            startLocation();
        }
        else {
            final SharedPreferences preferences = _cordovaActivity.getSharedPreferences(SHARED_PREFS_KEY,0);
            final String action = preferences.getString(SHARED_PREFS_ACTION,"");
            if(!action.equals("")){
                runAction(action);
            }
        }
    }

    public void onStart(){
        Log.d(TAG, "onStart");
        if(_locationManager != null){
            startLocation();
        }
    }

    public void onPause(boolean multitasking){
        stopLocation();
        Log.d(TAG, "onPause");
    }

    public void onStop(){
        stopLocation();
        Log.d(TAG, "onStop");
    }

    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        if(_cordova.getThreadPool() != null){
            stopLocation();
            removeActionPreferences();
            shutdownAndAwaitTermination(_cordova.getThreadPool());
            _cordovaActivity.finish();
        }
    }

    /**
     * Shutdown cordova thread pool. This assumes we are in control of all tasks running
     * in the thread pool.
     * Additional info: http://developer.android.com/reference/java/util/concurrent/ExecutorService.html
     * @param pool Cordova application's thread pool
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        Log.d(TAG,"Attempting to shutdown cordova threadpool");
        if(!pool.isShutdown()){
            try {
                // Disable new tasks from being submitted
                pool.shutdown();
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                        System.err.println("Cordova thread pool did not terminate.");
                    }
                }
            }
            catch (InterruptedException ie) {
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Callback handler for this Class
     * @param status Message status
     * @param message Any message
     */
    private static void sendCallback(PluginResult.Status status, String message){
        final PluginResult result = new PluginResult(status, message);
        result.setKeepCallback(true);
        _callbackContext.sendPluginResult(result);
    }

    private void alertDialog(boolean gpsEnabled, boolean networkLocationEnabled, boolean celllularEnabled){

        if(!gpsEnabled || !networkLocationEnabled){
            sendCallback(PluginResult.Status.ERROR, ErrorMessages.LOCATION_SERVICES_UNAVAILABLE);

            final DialogFragment gpsFragment = new GPSAlertDialogFragment();
            gpsFragment.show(_cordovaActivity.getFragmentManager(), "GPSAlert");
        }

        if(!celllularEnabled){
            sendCallback(PluginResult.Status.ERROR, ErrorMessages.CELLDATA_UNAVALABLE);

            final DialogFragment networkUnavailableFragment = new NetworkUnavailableDialogFragment();
            networkUnavailableFragment.show(_cordovaActivity.getFragmentManager(), "NetworkUnavailableAlert");
        }
    }

    /**
     * Check for <code>Network</code> connection.
     * Checks for generic Exceptions and writes them to logcat as <code>CheckConnectivity Exception</code>.
     * Make sure AndroidManifest.xml has appropriate permissions.
     * @param con Application context
     * @return Boolean
     */
    private Boolean isInternetConnected(Context con){

        Boolean connected = false;

        try{
            final ConnectivityManager connectivityManager = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if(networkInfo.isConnected()){
                connected = true;
            }
        }
        catch(Exception e){
            Log.e(TAG,"CheckConnectivity Exception: " + e.getMessage());
        }

        return connected;
    }

    private void removeActionPreferences(){
        _cordovaActivity.getSharedPreferences(SHARED_PREFS_KEY,0).edit().remove(SHARED_PREFS_ACTION).commit();
    }

    /**
     * Stores shared preferences so they can be retrieved after the app
     * is minimized then resumed.
     * @param value String
     * @param key String
     */
    private void setSharedPreferences(String key, String value){
        SharedPreferences settings = _cordovaActivity.getSharedPreferences(key, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);

        // Use apply() since it runs in the background rather than commit()
        editor.apply();
    }

    private void parseArgs(JSONArray args){
        Log.d(TAG,"Execute args: " + args.toString());
        if(args.length() > 0){
            try {
                final JSONObject obj = args.getJSONObject(0);
                _minTime = obj.getLong("minTime");
                _minDistance = obj.getLong("minDistance");
                _noWarn = obj.getBoolean("noWarn");
                _providers = obj.getString("providers");
                _useCache = obj.getBoolean("useCache");
                _returnSatelliteData = obj.getBoolean("satelliteData");
                _buffer = obj.getBoolean("buffer");
                _bufferSize = obj.getInt("bufferSize");

            }
            catch (Exception exc){
                Log.d(TAG, ErrorMessages.INCORRECT_CONFIG_ARGS + ", " + exc.getMessage());
                sendCallback(PluginResult.Status.ERROR, ErrorMessages.INCORRECT_CONFIG_ARGS + ", " + exc.getMessage());
            }
        }
    }
}
