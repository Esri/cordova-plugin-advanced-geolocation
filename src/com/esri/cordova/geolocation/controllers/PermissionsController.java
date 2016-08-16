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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class PermissionsController {

    private static Activity _activity;
    private static CordovaPlugin _cordovaPlugin;
    private static CordovaInterface _cordovaInterface;
    private static SharedPreferences _sharedPreferences;
    private static CallbackContext _callbackContext;
    private static int _denyCounter = 0;

    private static final String SHARED_PREFS_FIRST_RUN = "firstRun";
    private static final String SHARED_PREFS_GEO_DENIED = "geoDenied";             // basic denied
    private static final String SHARED_PREFS_GEO_GRANTED = "geoGranted";
    private static final String SHARED_PREFS_DENIED_COUNTER = "deniedCounter";
    private static final String TAG = "GeolocationPlugin";
    private static final int REQUEST_LOCATION_PERMS_CODE = 10;

    public final int ALLOW = 0;
    public final int ALLOW_ONCE = 1;
    public final int DENIED = -1;
    public final int DENIED_NOASK = -2;
    public final String SHARED_PREFS_ONSTOP_KEY = "onstop";
    public final boolean SHARED_PREFS_ONSTOP_TRUE = true;
    public final boolean SHARED_PREFS_ONSTOP_FALSE = false;
    public final String SHARED_PREFS_LOCATION = "LocationSettings";
    public final String SHARED_PREFS_GEO_DENIED_NOASK = "geoDeniedNoAsk";  // denied and don't ask again

    public PermissionsController(
            Activity activity,
            CordovaPlugin cordovaPlugin,
            CordovaInterface cordovaInterface){
        _activity = activity;
        _cordovaPlugin = cordovaPlugin;
        _cordovaInterface = cordovaInterface;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_activity);
    }

    public int getShowRationale(){

        int rationale = DENIED;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean fineLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            final boolean coarseLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
            final boolean didMinimize = _sharedPreferences.getBoolean(SHARED_PREFS_ONSTOP_KEY, false);

Log.d(TAG,"Counter: " + _denyCounter + ", didMin = " + didMinimize + ", showRationale: " + fineLocationRationale +", sharedPrefs: " + getNoAsk());
            // Minimized app
//            if(fineLocationRationale && coarseLocationRationale && _denyCounter == 0){
//                Log.d(TAG,"rationale #0");
//                rationale = ALLOW_ONCE;
//            }
//            // Reinstall after having denied permissions in the previous install
//            else if(!fineLocationRationale || !coarseLocationRationale && _denyCounter > 0 && didMinimize == false){
//                Log.d(TAG,"rationale #1");
//                rationale = ALLOW_ONCE;
//            }
            // Denied at least once
            if(fineLocationRationale && coarseLocationRationale && _denyCounter <= 1){
                Log.d(TAG,"rationale #2");
                rationale = ALLOW;
            }
            // Start up > Denied twice
            else if(fineLocationRationale && coarseLocationRationale && _denyCounter > 1){
                Log.d(TAG,"rationale #3");
                rationale = DENIED;
            }
            // Don't ask me again check box
            else if((!fineLocationRationale || !coarseLocationRationale) && _denyCounter > 1){
                Log.d(TAG,"rationale #4");
                rationale = DENIED_NOASK;
            }
            else if(!fineLocationRationale || !coarseLocationRationale){
                Log.d(TAG, "ALLOW !!! " + getAppPermissions());
                rationale = ALLOW;
            }
        }
Log.d(TAG,"Rationale = " + rationale);

        return rationale;
    }

    /**
     * We only have a finite number of permissions for this plugin: ACCESS_FINE_LOCATION and
     * ACCESS_COARSE_LOCATION.
     * @return boolean
     */
    public boolean getAppPermissions(){
        return _cordovaInterface.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                _cordovaInterface.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Whether or not the user wants to be asked about permissions again.
     * @return boolean true indicates they don't want to be asked again.
     */
    public boolean getNoAsk(){

        boolean noAsk = false;
Log.w(TAG, "GET NO ASK: " + getSharedPreferences(SHARED_PREFS_LOCATION));
        if(getSharedPreferences(SHARED_PREFS_LOCATION).equals(SHARED_PREFS_GEO_DENIED_NOASK)){
            noAsk = true;
        }

        return noAsk;
    }

    public void handleOnPause(){
        //TODO
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void handleOnInitialize(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean fineLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            final boolean coarseLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);

            if(fineLocationRationale || coarseLocationRationale){
                _denyCounter++;
            }
            else {
                _denyCounter = 0;
            }
        }
    }

    public void handleOnRequestDenied(){
        _denyCounter++;
    }

    public void handleOnRequestAllowed(){
        setSharedPreferences(SHARED_PREFS_LOCATION, SHARED_PREFS_GEO_GRANTED);
        _denyCounter = 0;
    }

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

    private boolean getIsFirstRun(){
        boolean firstRun = _sharedPreferences.getBoolean(SHARED_PREFS_FIRST_RUN, true);

        if (firstRun) {
            _sharedPreferences.edit().putBoolean(SHARED_PREFS_FIRST_RUN, false).apply();
        }

        return firstRun;
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
}
