package com.esri.cordova.geolocation.utils;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Threadsafe class for converting location data into JSON
 */
public final class JSONHelper {

    public static final String SATELLITE_PROVIDER = "satellite";
    private static final String TAG = "GeolocationPlugin";

    /**
     * Converts location data into a JSON form that can be consumed within a JavaScript application
     * @param provider Indicates if this location is coming from gps or network provider
     * @param location The android Location
     * @param cached Indicates if the value was pulled from the device cache or not
     * @return Location data. Note: this library returns 0 rather than null to avoid nullPointExceptions
     */
    public static String locationJSON(String provider, Location location, boolean cached) {

        final JSONObject json = new JSONObject();

        if(location != null){
            try {

                json.put("provider", provider);
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
                json.put("altitude", location.getAltitude());
                json.put("accuracy", location.getAccuracy());
                json.put("bearing", location.getBearing());
                json.put("speed", location.getSpeed());
                json.put("timestamp", location.getTime());
                json.put("cached", cached);
            }
            catch (JSONException exc) {
                Log.d(TAG, exc.getMessage());
            }
        }

        return json.toString();
    }

    /**
     * Converts location data into a JSON form that can be consumed within a JavaScript application
     * @param provider Indicates if this location is coming from gps or network provider
     * @param location The android Location
     * @param cached Indicates if the value was pulled from the device cache or not
     * @param buffer Boolean indicates whether or not buffering is activated
     * @param bufferLat The buffer's geometric latitudinal center.
     * @param bufferedLon The buffer's geometric longitudinal center.
     * @param bufferedAccuracy The buffer's average accuracy.
     * @param bufferSize The number of elements within the buffer
     * @return Location data. Note: this library returns 0 rather than null to avoid nullPointExceptions
     */
    public static String locationJSON(
            String provider,
            Location location,
            boolean cached,
            boolean buffer,
            double bufferLat,
            double bufferedLon,
            float bufferedAccuracy,
            int bufferSize) {

        final JSONObject json = new JSONObject();

        if(location != null){
            try {

                json.put("provider", provider);
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
                json.put("altitude", location.getAltitude());
                json.put("accuracy", location.getAccuracy());
                json.put("bearing", location.getBearing());
                json.put("speed", location.getSpeed());
                json.put("timestamp", location.getTime());
                json.put("cached", cached);
                json.put("buffer", buffer);
                json.put("bufferSize", bufferSize);
                json.put("bufferedLatitude", bufferLat);
                json.put("bufferedLongitude", bufferedLon);
                json.put("bufferedAccuracy", bufferedAccuracy);
            }
            catch (JSONException exc) {
                Log.d(TAG, exc.getMessage());
            }
        }

        return json.toString();
    }

    /**
     * Converts GpsStatus into JSON.
     * @param gpsStatus Send a GpsStatus whenever the GPS fires
     * @return JSON representation of the satellite data
     */
    public static String satelliteDataJSON(GpsStatus gpsStatus){

        final Calendar calendar = Calendar.getInstance();
        final JSONObject json = new JSONObject();

        try {
            json.put("provider", SATELLITE_PROVIDER);
            json.put("timestamp", calendar.getTimeInMillis());

            if(gpsStatus.getSatellites() != null) {
                int count = 0;
                final int timeToFirstFix = gpsStatus.getTimeToFirstFix();

                for(GpsSatellite sat: gpsStatus.getSatellites() ){
                    final JSONObject satelliteInfo = new JSONObject();

                    satelliteInfo.put("PRN", sat.getPrn());
                    satelliteInfo.put("timeToFirstFix", timeToFirstFix);
                    satelliteInfo.put("usedInFix", sat.usedInFix());
                    satelliteInfo.put("azimuth", sat.getAzimuth());
                    satelliteInfo.put("elevation", sat.getElevation());
                    satelliteInfo.put("hasEphemeris", sat.hasEphemeris());
                    satelliteInfo.put("hasAlmanac", sat.hasAlmanac());
                    satelliteInfo.put("SNR", sat.getSnr());

                    json.put(Integer.toString(count), satelliteInfo);

                    count++;
                }
            }
        }
        catch (JSONException exc){
            Log.d(TAG, exc.getMessage());
        }

        return json.toString();
    }

    /**
     * Helper method for reporting errors coming off a location provider
     * @param provider Indicates if this error is coming from gps or network provider
     * @param error The actual error being thrown by the provider
     * @return Error string
     */
    public static String errorJSON(String provider, String error) {

        final JSONObject json = new JSONObject();

        try {
            json.put("provider", provider);
            json.put("error", error);
        }
        catch (JSONException exc) {
            Log.d(TAG, exc.getMessage());
        }

        return json.toString();
    }
}
