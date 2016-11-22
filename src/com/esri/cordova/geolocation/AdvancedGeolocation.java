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
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.esri.cordova.geolocation.controllers.CellLocationController;
import com.esri.cordova.geolocation.controllers.GPSController;
import com.esri.cordova.geolocation.controllers.NetworkLocationController;
import com.esri.cordova.geolocation.controllers.PermissionsController;
import com.esri.cordova.geolocation.fragments.GPSAlertDialogFragment;
import com.esri.cordova.geolocation.fragments.NetworkUnavailableDialogFragment;
import com.esri.cordova.geolocation.model.StopLocation;
import com.esri.cordova.geolocation.utils.ErrorMessages;
import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class AdvancedGeolocation extends CordovaPlugin{

    public static final String PROVIDERS_ALL = "all";
    public static final String PROVIDERS_SOME = "some";
    public static final String PROVIDERS_GPS = "gps";
    public static final String PROVIDERS_NETWORK = "network";
    public static final String PROVIDERS_CELL = "cell";
    public static final String PROVIDER_PRIMARY = "application"; // references this main controller and not tied to a sensor

    private static final String TAG = "GeolocationPlugin";
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
    private static boolean _signalStrength = false;
    private static int _bufferSize = 0;

    private static GPSController _gpsController = null;
    private static NetworkLocationController _networkLocationController = null;
    private static CellLocationController _cellLocationController = null;
    private static CordovaInterface _cordova;
    private Activity _cordovaActivity;
    private static CallbackContext _callbackContext;
    private static SharedPreferences _sharedPreferences;
    private PermissionsController _permissionsController;

    // For managing termination of ExecutorService
    private static Future _gpsFuture = null;
    private static Future _networkFuture = null;
    private static Future _cellularFuture = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        _cordova = cordova;
        _cordovaActivity = cordova.getActivity();
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_cordovaActivity);
        _permissionsController = new PermissionsController(_cordovaActivity, _cordova);
        _permissionsController.handleOnInitialize();
        removeActionPreferences();
        Log.d(TAG, "Initialized");
    }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException{
        _callbackContext = callbackContext;

        Log.d(TAG, "Action = " + action);

        // Save this action so we can refer to it when the app restarts
        setSharedPreferences(SHARED_PREFS_ACTION, action);

        if (args != null) {
            parseArgs(args);
        }

        return runAction(action);
    }

    private boolean runAction(final String action){

        if(action.equals("start")){
            validatePermissions();
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
     * Overrides in CordovaPlugin
     * FYI: you can verify device permissions using:
     * <code>adb shell pm list permissions -d -g</code>
     * Reference: http://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
     * @param requestCode The request code we assign - it's basically a token
     * @param permissions The requested permissions - never null
     * @param grantResults <code>PERMISSION_GRANTED</code> or <code>PERMISSION_DENIED</code> - never null
     * @throws JSONException
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        // 1st Try - ALLOW or DENY. There is no don't ask again check box.
        // If ALLOW the proceed and all is good
        // If DENY then retry with NEVER ASK AGAIN prompt
        // If ALLOW then proceed and all is good
        // If DENY again with never ask again checked, then lock down the app and don't ask again
        // If DENY again without checking never ask again, then recheck on next app launch
        // have to remember to manually reactivate
        //
        // IMPORTANT! When this event completes the onResume event will fire!

        // TEST CASES
        // Start -> Allow -> minimize -> open app
        // Start -> Allow -> minimize -> Change perms to deny geo -> open app
        // Start -> Deny -> Deny -> minimize -> open app
        // Start -> Deny -> Deny -> minimize -> Change perms to allow geo -> open app
        // Start -> Deny -> Deny and check no ask -> minimize -> open app
        // Start -> Deny -> Deny and check no ask -> minimize -> Change perms to allow geo -> open app
        // Repeat test cases except shut off device geo permissions
        //
        // Reference for Permission Denied Workflow: https://material.google.com/patterns/permissions.html#permissions-denied-permissions

        if(requestCode == REQUEST_LOCATION_PERMS_CODE && grantResults.length > 1){

            // If permission was granted then go ahead
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG,"GEO PERMISSIONS GRANTED.");
                _permissionsController.handleOnRequestAllowed();
            }
            // If permission was denied then we can't run geolocation - permission DISABLED
            else{
                Log.w(TAG,"GEO PERMISSIONS DENIED.");
                _permissionsController.handleOnRequestDenied();

                // User doesn't want to see any more preference-related dialog boxes
                if(_permissionsController.getShowRationale() == _permissionsController.DENIED_NOASK){
                    Log.w(TAG, "requestPermissions() Callback: " + ErrorMessages.LOCATION_SERVICES_DENIED_NOASK().message);
                    setSharedPreferences(_permissionsController.SHARED_PREFS_LOCATION_KEY, _permissionsController.SHARED_PREFS_GEO_DENIED_NOASK);
                }
            }
        }
    }

    private void validatePermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Reference: Permission Groups https://developer.android.com/guide/topics/security/permissions.html#normal-dangerous
            // As of July 2016 - ACCESS_WIFI_STATE and ACCESS_NETWORK_STATE are not considered dangerous permissions
            Log.d(TAG, "validatePermissions()");

            final int showRationale = _permissionsController.getShowRationale();

            if(_permissionsController.getAppPermissions()){
                startLocation();
            }
            // The user has said to never ask again about activating location services
            else if(showRationale == _permissionsController.DENIED_NOASK){
                Log.w(TAG, ErrorMessages.LOCATION_SERVICES_DENIED_NOASK().message);
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(PROVIDER_PRIMARY, ErrorMessages.LOCATION_SERVICES_DENIED_NOASK()));
            }
            else if(showRationale == _permissionsController.ALLOW) {
                requestPermissions();
            }
            else if(showRationale == _permissionsController.DENIED) {
                Log.w(TAG, "Rationale already shown, geolocation denied twice");
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(PROVIDER_PRIMARY, ErrorMessages.LOCATION_SERVICES_DENIED()));
            }
        }
        else {
            final LocationManager _locationManager = (LocationManager) _cordovaActivity.getSystemService(Context.LOCATION_SERVICE);
            final boolean networkLocationEnabled = _locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            final boolean gpsEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            final boolean networkEnabled = isInternetConnected(_cordovaActivity.getApplicationContext());

            // If warnings are disabled then skip initializing alert dialog fragments
            if(!_noWarn && (!networkLocationEnabled || !gpsEnabled || !networkEnabled)){
                alertDialog(gpsEnabled, networkLocationEnabled, networkEnabled);
            }
            else {
                startLocation();
            }
        }
    }

    private void startLocation(){

        // Misc. note: If you see the message "Attempted to send a second callback for ID:" then you need
        // to make sure to set pluginResult.setKeepCallback(true);

        // We want to prevent multiple instances of controllers from running!
        if(_gpsController != null || _networkLocationController != null || _cellLocationController != null){
            stopLocation();
        }

        final boolean networkEnabled = isInternetConnected(_cordovaActivity.getApplicationContext());
        ExecutorService threadPool = cordova.getThreadPool();

        if(_providers.equalsIgnoreCase(PROVIDERS_ALL)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            _gpsFuture = threadPool.submit(_gpsController);

            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            _networkFuture = threadPool.submit(_networkLocationController);

            // Reference: https://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
            // Reference: https://developer.android.com/reference/android/telephony/CellIdentityWcdma.html (added at API 18)
            if (Build.VERSION.SDK_INT < MIN_API_LEVEL){
                cellDataNotAllowed();
            }
            else {
                _cellLocationController = new CellLocationController(networkEnabled, _signalStrength, _cordova,_callbackContext);
                _cellularFuture = threadPool.submit(_cellLocationController);
            }
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_SOME)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            _gpsFuture = threadPool.submit(_gpsController);

            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            _networkFuture = threadPool.submit(_networkLocationController);

        }
        if(_providers.equalsIgnoreCase(PROVIDERS_GPS)){
            _gpsController = new GPSController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _returnSatelliteData, _buffer, _bufferSize);
            _gpsFuture = threadPool.submit(_gpsController);
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_NETWORK)){
            _networkLocationController = new NetworkLocationController(
                    _cordova, _callbackContext, _minDistance, _minTime, _useCache, _buffer, _bufferSize);
            _networkFuture = threadPool.submit(_networkLocationController);
        }
        if(_providers.equalsIgnoreCase(PROVIDERS_CELL)){

            // Reference: https://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
            // Reference: https://developer.android.com/reference/android/telephony/CellIdentityWcdma.html
            if (Build.VERSION.SDK_INT < MIN_API_LEVEL){
                cellDataNotAllowed();
            }
            else {
                _cellLocationController = new CellLocationController(networkEnabled,_signalStrength ,_cordova,_callbackContext);
                _cellularFuture = threadPool.submit(_cellLocationController);
            }
        }
    }

    /**
     * Halt any active providers.
     */
    private void stopLocation(){

        List<StopLocation> providers = new ArrayList<StopLocation>();

        if(_gpsController != null){
            // Gracefully attempt to stop location
            _gpsController.stopLocation();

            // make sure there are no references
            _gpsController = null;

            // Cancel the threadpool execution of this task
            if(_gpsFuture != null){
                StopLocation sl = new StopLocation();
                sl.provider = PROVIDERS_GPS;
                sl.success = _gpsFuture.cancel(true);
                providers.add(sl);
            }
        }
        if(_networkLocationController != null){
            // Gracefully attempt to stop location
            _networkLocationController.stopLocation();

            // make sure there are no references
            _networkLocationController = null;

            // Cancel the threadpool execution of this task
            if(_networkFuture != null){
                StopLocation sl = new StopLocation();
                sl.provider = PROVIDERS_NETWORK;
                sl.success = _networkFuture.cancel(true);
                providers.add(sl);
            }
        }

        // CellLocationController does not require LocationManager
        if(_cellLocationController != null){
            // Gracefully attempt to stop location
            _cellLocationController.stopLocation();

            // make sure there are no references
            _cellLocationController = null;

            // Cancel the threadpool execution of this task
            if(_cellularFuture != null){
                StopLocation sl = new StopLocation();
                sl.provider = PROVIDERS_CELL;
                sl.success = _cellularFuture.cancel(true);
                providers.add(sl);
            }
        }

        if(providers.size() > 0){
            sendCallback(PluginResult.Status.OK,
                    JSONHelper.stopLocationJSON(providers));
        }

        Log.d(TAG, "Stopping geolocation");
    }

    //
    //
    // PREFERENCES
    //
    //

    private String getSharedPreferences(String key){
        return _sharedPreferences.getString(key,"");
    }

    /**
     * Stores shared preferences so they can be retrieved after the app
     * is minimized then resumed.
     * @param value String
     * @param key String
     */
    private void setSharedPreferences(String key, String value){
        _sharedPreferences.edit().putString(key, value).apply();
        Log.d(TAG, "prefs: " + key + ", " + _sharedPreferences.getString(key,""));
    }

    private void removeActionPreferences(){
        _sharedPreferences.edit().remove(SHARED_PREFS_ACTION).apply();
    }

    private void requestPermissions(){
        final String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        _cordova.requestPermissions(this, REQUEST_LOCATION_PERMS_CODE, perms);
    }

    private void cellDataNotAllowed(){
        Log.w(TAG, ErrorMessages.CELL_DATA_NOT_ALLOWED().message);
        sendCallback(PluginResult.Status.ERROR,
                JSONHelper.errorJSON(PROVIDER_PRIMARY, ErrorMessages.CELL_DATA_NOT_ALLOWED()));
    }

    //
    //
    // EVENTS
    //
    //

    /**
     * Retrieves shared preferences to find out what action was requested when the app
     * originally launched. Resumes based on that last action.
     * @param multitasking Unused in this API. Flag indicating if multitasking is turned on for app
     * and is inherited from Cordova.
     */
    public void onResume(boolean multitasking){
        Log.d(TAG, "onResume");

        final String action = getSharedPreferences(SHARED_PREFS_ACTION);
        if(!action.equals("")) {
            runAction(action);
        }
    }

    public void onStart(){
        Log.d(TAG, "onStart");
    }

    public void onPause(boolean multitasking){
        Log.d(TAG, "onPause");
        stopLocation();
    }

    public void onStop(){
        Log.d(TAG, "onStop");
        stopLocation();
    }

    public void onDestroy(){
        Log.d(TAG, "onDestroy");
        if(_cordova.getThreadPool() != null){
            stopLocation();
            removeActionPreferences();
            shutdownAndAwaitTermination(_cordova.getThreadPool());
            _cordovaActivity.finish();
        }

        sendCallback(PluginResult.Status.OK,
                JSONHelper.killLocationJSON());
    }


    //
    //
    // UTILITY METHODS
    //
    //

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

    /**
     * For working with pre-Android M security permissions
     * @param gpsEnabled If the cacheManifest and system allow gps
     * @param networkLocationEnabled If the cacheManifest and system allow network location access
     * @param cellularEnabled If the cacheManifest and system allow cellular data access
     */
    private void alertDialog(boolean gpsEnabled, boolean networkLocationEnabled, boolean cellularEnabled){

        if(!gpsEnabled || !networkLocationEnabled){
            sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(PROVIDER_PRIMARY, ErrorMessages.LOCATION_SERVICES_UNAVAILABLE()));

            final DialogFragment gpsFragment = new GPSAlertDialogFragment();
            gpsFragment.show(_cordovaActivity.getFragmentManager(), "GPSAlert");
        }

        if(!cellularEnabled){
            sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(PROVIDER_PRIMARY, ErrorMessages.CELL_DATA_NOT_AVAILABLE()));

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
                _signalStrength = obj.getBoolean("signalStrength");
                _bufferSize = obj.getInt("bufferSize");

            }
            catch (Exception exc){
                Log.d(TAG, ErrorMessages.INCORRECT_CONFIG_ARGS + ", " + exc.getMessage());
                sendCallback(PluginResult.Status.ERROR, ErrorMessages.INCORRECT_CONFIG_ARGS + ", " + exc.getMessage());
            }
        }
    }
}
