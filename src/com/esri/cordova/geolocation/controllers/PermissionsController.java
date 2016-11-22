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
import android.os.Build;
import android.util.Log;

import org.apache.cordova.CordovaInterface;

public class PermissionsController {

    private Activity _activity;
    private static CordovaInterface _cordovaInterface;
    private static int _denyCounter = 0;
    private static final String TAG = "GeolocationPlugin";

    public final int ALLOW = 0;
    public final int DENIED = -1;
    public final int DENIED_NOASK = -2;
    public final String SHARED_PREFS_LOCATION_KEY = "LocationSettings";
    public final String SHARED_PREFS_GEO_DENIED_NOASK = "geoDeniedNoAsk";  // denied and don't ask again

    public PermissionsController(
            Activity activity,
            CordovaInterface cordovaInterface){
        _activity = activity;
        _cordovaInterface = cordovaInterface;
    }

    /**
     * Define our rules for when user is prompted for permissions access
     * @return int
     */
    public int getShowRationale(){

        int rationale = DENIED;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean fineLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            final boolean coarseLocationRationale = _activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);

            Log.d(TAG,"Counter: " + _denyCounter + ", display rationale? " + fineLocationRationale);

            // Permissions denied once
            if(fineLocationRationale && coarseLocationRationale && _denyCounter <= 1){
                Log.d(TAG,"rationale 1: user denied perms at least once");
                rationale = ALLOW;
            }
            // Start up > permissions denied twice
            else if(fineLocationRationale && coarseLocationRationale && _denyCounter > 1){
                Log.d(TAG,"rationale 2: user has denied perms more than once");
                rationale = DENIED;
            }
            // Don't ask me again check box has been set
            else if((!fineLocationRationale || !coarseLocationRationale) && _denyCounter > 1){
                Log.d(TAG,"rationale 3: user has denied perms and asked to be never asked again");
                rationale = DENIED_NOASK;
            }
            // App start up
            else if(!fineLocationRationale || !coarseLocationRationale){
                Log.d(TAG, "rationale 4: application startup - no perms are set yet");
                rationale = ALLOW;
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
     * When app is initialized we need to start tracking permissions
     */
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

    /**
     * Track when permissions requests are denied
     */
    public void handleOnRequestDenied(){
        _denyCounter++;
    }

    /**
     * Track when permissions requests are allowed
     */
    public void handleOnRequestAllowed(){
        _denyCounter = 0;
    }
}
