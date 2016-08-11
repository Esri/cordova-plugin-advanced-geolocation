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
    private static int _rationaleCounter = 0;
    private static boolean _requestPendingFlag = false; //if a perms request is pending or not

    private static final String SHARED_PREFS_LOCATION = "LocationSettings";
    private static final String SHARED_PREFS_FIRST_RUN = "firstRun";
    private static final String SHARED_PREFS_GEO_DENIED = "geoDenied";             // basic denied
    private static final String SHARED_PREFS_GEO_DENIED_NOASK = "geoDeniedNoAsk";  // denied and don't ask again
    private static final String SHARED_PREFS_GEO_GRANTED = "geoGranted";
    private static final String SHARED_PREFS_DENIED_COUNTER = "deniedCounter";
    private static final String TAG = "GeolocationPlugin";
    private static final int REQUEST_LOCATION_PERMS_CODE = 10;

    public final int ALLOW = 0;
    public final int DENIED = -1;
    public final int DENIED_NOASK = -2;

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
//        final boolean rationale = getShowRationale();
        final boolean firstRun = getIsFirstRun();
    }

    public int getShowRationale(){

        int rationale = DENIED;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean fineLocation = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            final boolean coarseLocation = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
Log.d(TAG,"Counter: " + _denyCounter);
            if(fineLocation && coarseLocation && _denyCounter <= 1){
                Log.d(TAG,"rationale 1");
                rationale = ALLOW;
            }
            if(fineLocation && coarseLocation && _denyCounter > 1){
                Log.d(TAG,"rationale 2");
                rationale = DENIED;
            }
            if(!fineLocation || !coarseLocation){
                rationale = DENIED_NOASK;
            }
        }

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
        return getSharedPreferences(SHARED_PREFS_LOCATION).equals(SHARED_PREFS_GEO_DENIED_NOASK);
    }

    public boolean getAllowRationale(){
        boolean allow = false;

        if(getShowRationale() == ALLOW && _denyCounter < 1){
            allow = true;
        }

        return allow;
    }

    public boolean getAllowRequestPermissions(){
        _requestPendingFlag = true;
        boolean allow = false;

        if(_denyCounter == 0){
            allow = true;
        }

        return allow;
    }

    public void handleOnPause(){
        // Requesting permissions triggers an onPause event
        // Since we can't distinguish between this forced event and device-related events
        // we use a pending flag as a hack to prevent false positives.
        if(!_requestPendingFlag) {
            _denyCounter = 0; //reset the deny counter
        }
    }

    public void handleOnRequestDenied(){
        _requestPendingFlag = false;
        _denyCounter++;
    }

    public void handleOnRequestAllowed(){
        _requestPendingFlag = false;
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
