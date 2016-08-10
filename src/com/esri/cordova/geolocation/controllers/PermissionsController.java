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
    private static final String SHARED_PREFS_GEO_DENIED_NOASK = "geoDeniedNoAsk";  // denied and don't ask again
    private static final String SHARED_PREFS_GEO_GRANTED = "geoGranted";
    private static final String SHARED_PREFS_DENIED_COUNTER = "deniedCounter";
    private static final String TAG = "GeolocationPlugin";
    private static final int REQUEST_LOCATION_PERMS_CODE = 10;

    public boolean onPause = false;

    public PermissionsController(
            Activity activity,
            CordovaPlugin cordovaPlugin,
            CordovaInterface cordovaInterface){
        _activity = activity;
        _cordovaPlugin = cordovaPlugin;
        _cordovaInterface = cordovaInterface;
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_activity);
    }

    public void validatePermissions(){
        final boolean permissions = getAppPermissions();
        final boolean rationale = getShowRationale();
        final boolean firstRun = getIsFirstRun();
    }

    public boolean getShowRationale(){

        boolean showRationale = false;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean fineLocation = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            final boolean coarseLocation = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);

            if(fineLocation || coarseLocation){
                showRationale = true;
            }
        }

        return showRationale;
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

    public void incrementDenyCounter(){
        _denyCounter++;
    }

    private void requestPermissions(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            _cordovaInterface.requestPermissions(_cordovaPlugin, REQUEST_LOCATION_PERMS_CODE, perms);
        }
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
