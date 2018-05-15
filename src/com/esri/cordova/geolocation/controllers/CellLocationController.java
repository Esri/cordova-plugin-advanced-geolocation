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
 * Provides information derived from the cellular service of the device. Not all functionality
 * is available on all devices and it also depends on the cellular providers capabilities.
 *
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
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.esri.cordova.geolocation.listeners.SignalStrengthListener;
import com.esri.cordova.geolocation.model.StrengthChange;
import com.esri.cordova.geolocation.utils.ErrorMessages;
import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import java.util.List;

public final class CellLocationController implements Runnable{

    public static final String CELLINFO_PROVIDER = "cell";
    private static final String TAG = "GeolocationPlugin";
    private static CallbackContext _callbackContext; // Threadsafe
    private static TelephonyManager _telephonyManager = null;
    private static PhoneStateListener _phoneStateListener = null;
    private static SignalStrengthListener _signalStrengthListener = null;
    private static CordovaInterface _cordova;
    private static boolean _isConnected = false;
    private static boolean _returnSignalStrength = false;

    public CellLocationController(
            boolean isConnected,
            boolean returnSignalStrength,
            CordovaInterface cordova,
            CallbackContext callbackContext
    ){
        _isConnected = isConnected;
        _cordova = cordova;
        _callbackContext = callbackContext;
        _returnSignalStrength = returnSignalStrength;
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
        else {
            Log.e(TAG, ErrorMessages.CELL_DATA_MIN_VERSION().message);
            sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, ErrorMessages.CELL_DATA_MIN_VERSION()));
        }
    }

    public void startLocation(){

        if(!Thread.currentThread().isInterrupted()){

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Failing gracefully after detecting an uncaught exception on CellLocationController thread. "
                        + throwable.getMessage());
                sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, ErrorMessages.UNCAUGHT_THREAD_EXCEPTION()));
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
                sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, ErrorMessages.CELL_DATA_NOT_AVAILABLE()));
            }
        }
        else {
            Log.e(TAG, "Not starting CellLocationController due to thread interrupt.");
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){
        if(_phoneStateListener != null && _telephonyManager != null){
            _telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        if(_signalStrengthListener != null && _telephonyManager != null){
            _telephonyManager.listen(_signalStrengthListener, PhoneStateListener.LISTEN_NONE);
        }

        _signalStrengthListener = null;
        _phoneStateListener = null;
        _telephonyManager = null;

        try {
            Thread.currentThread().interrupt();
        }
        catch(SecurityException exc){
            Log.e(TAG, exc.getMessage());
            sendCallback(PluginResult.Status.ERROR,
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, ErrorMessages.FAILED_THREAD_INTERRUPT()));
        }

        Log.d(TAG, "Stopping cell location listeners");
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

        if(_returnSignalStrength){
            _signalStrengthListener = new SignalStrengthListener();
            _signalStrengthListener.setListener(new StrengthChange() {
                @Override
                public SignalStrength onSignalStrengthChanged(SignalStrength signalStrength) {

                    if(!Thread.currentThread().isInterrupted()){
                        sendCallback(PluginResult.Status.OK,
                                JSONHelper.signalStrengthJSON(signalStrength));
                    }

                    return null;
                }
            });

            _telephonyManager.listen(_signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        _phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCellLocationChanged(CellLocation location){

                if(!Thread.currentThread().isInterrupted()){
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
            }

            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo){
                if(!Thread.currentThread().isInterrupted()){
                    processCellInfos(cellInfo);
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
                            JSONHelper.cellInfoWCDMAJSON(cellInfoWcdma, _returnSignalStrength));
                }
                if(cellInfo instanceof CellInfoGsm){
                    final CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoGSMJSON(cellInfoGsm, _returnSignalStrength));
                }
                if(cellInfo instanceof  CellInfoCdma){
                    final CellInfoCdma cellIdentityCdma = (CellInfoCdma) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoCDMAJSON(cellIdentityCdma, _returnSignalStrength));
                }
                if(cellInfo instanceof  CellInfoLte){
                    final CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                    sendCallback(PluginResult.Status.OK,
                            JSONHelper.cellInfoLTEJSON(cellInfoLte, _returnSignalStrength));
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
                    JSONHelper.errorJSON(CELLINFO_PROVIDER, ErrorMessages.CELL_DATA_IS_NULL()));
        }
    }

    /**
     * This Class will not work correctly on older versions of the Android SDK
     * Reference: http://developer.android.com/reference/android/telephony/TelephonyManager.html#getAllCellInfo()
     */
    private static boolean versionCheck(){
        boolean verified = true;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            verified = false;
        }

        return verified;
    }

    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.currentThread().isInterrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }
}
