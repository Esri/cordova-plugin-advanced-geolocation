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

/**
 * IMPORTANT: This Class is only compatible with API Level 17 or greater
 * Reference: https://developer.android.com/reference/android/telephony/CellInfo.html
 */

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import java.util.List;

public final class CellLocationController implements Runnable{

    public static final String CELLINFO_PROVIDER = "cell";
    private static final String TAG = "GeolocationPlugin";
    private static final int MIN_BUILD_VER = 21;
    private static CallbackContext _callbackContext; // Threadsafe
    private static TelephonyManager _telephonyManager = null;
    private static PhoneStateListener _phoneStateListener = null;
    private static CordovaInterface _cordova;
    private static boolean _isConnected = false;

    public CellLocationController(
            boolean isConnected,
            CordovaInterface cordova,
            CallbackContext callbackContext
    ){
        _isConnected = isConnected;
        _cordova = cordova;
        _callbackContext = callbackContext;
    }

    public void run(){

        // There are minimum OS version requirements
        if(versionCheck()){
            // Reference: http://developer.android.com/reference/android/os/Process.html#THREAD_PRIORITY_BACKGROUND
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            // We are running a Looper to allow the Cordova CallbackContext to be passed within the Thread as a message.
            if(Looper.myLooper() == null){
                _telephonyManager = (TelephonyManager) _cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                Looper.prepare();
                startLocation();
                Looper.loop();
            }
        }
    }

    public void startLocation(){

        if(!Thread.currentThread().isInterrupted()){

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                Log.d(TAG, "Failing gracefully after detecting an uncaught exception on CellLocationController thread. "
                        + throwable.getMessage());
                stopLocation();
                }
            });

            if(_isConnected){
                // Set up a change listener
                setPhoneStateListener();

                // Return a snapshot of all cell info
                getAllCellInfos();

                Log.d(TAG, "Starting CellLocationController");
            }
            else {
                Log.e(TAG, "Unable to start CellLocationController: no internet connection.");
            }
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){

        if(_phoneStateListener != null && _telephonyManager != null){
            _telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
            _phoneStateListener = null;
            _telephonyManager = null;
            Thread.currentThread().interrupt();
            Log.d(TAG, "Stopping PhoneStateListener");
        }
    }

    /**
     * Returns all observed cell information from all radios on the device including the primary and
     * neighboring cells. Calling this method does not trigger a call to onCellInfoChanged(), or change
     * the rate at which onCellInfoChanged() is called.
     */
    private void getAllCellInfos(){
        if(_telephonyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final List<CellInfo> cellInfos = _telephonyManager.getAllCellInfo();
            processCellInfos(cellInfos);
        }
        else {
            Log.w(TAG, "Unable to provide cell info due to version restriction");
        }
    }

    private void setPhoneStateListener(){
        _phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCellLocationChanged(CellLocation location){
                if(location instanceof CdmaCellLocation){
                    final CdmaCellLocation cellLocationCdma = (CdmaCellLocation) location;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cdmaCellLocationJSON(cellLocationCdma));
                }
                if(location instanceof GsmCellLocation){
                    final GsmCellLocation cellLocationGsm = (GsmCellLocation) location;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.gsmCellLocationJSON(cellLocationGsm));
                }
            }
        };

        _telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    private static void processCellInfos(List<CellInfo> cellInfos){
        if(cellInfos != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            for(CellInfo cellInfo : cellInfos){

                if(cellInfo instanceof  CellInfoWcdma){
                    final CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoWCDMAJSON(cellInfoWcdma));
                }
                if(cellInfo instanceof CellInfoGsm){
                    final CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoGSMJSON(cellInfoGsm));
                }
                if(cellInfo instanceof  CellInfoCdma){
                    final CellInfoCdma cellIdentityCdma = (CellInfoCdma) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoCDMAJSON(cellIdentityCdma));
                }
                if(cellInfo instanceof  CellInfoLte){
                    final CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoLTEJSON(cellInfoLte));
                }

                Log.d(TAG,cellInfo.toString());
            }
        }
        else {
            Log.e(TAG, "CellInfoLocation returning null. Is it supported on this phone?");

            // There are several reasons as to why cell location data would be null.
            // * could be an older device running an unsupported version of the Android OS
            // * could be a device that doesn't support this capability.
            // * could be incorrect permissions: ACCESS_COARSE_LOCATION
            sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, "Cell location data is returning as null"));
        }
    }

    /**
     * This Class will not work correctly on older versions of the Android SDK
     * Reference: http://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
     */
    private static boolean versionCheck(){
        boolean verified = true;
        final int version = Build.VERSION.SDK_INT;
        if(version < MIN_BUILD_VER){
            Log.e(TAG, "WARNING: A minimum SDK v17 is required for CellLocation to work, and  minimum SDK v21 is REQUIRED for this library.");
            verified = false;
        }

        return verified;
    }

    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.interrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }

}
