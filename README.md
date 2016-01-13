#cordova-plugin-advanced-geolocation (Android-only)

Highly configurable native interface to both [GPS](http://developer.android.com/reference/android/location/LocationManager.html#GPS_PROVIDER) and [NETWORK](http://developer.android.com/reference/android/location/LocationManager.html#NETWORK_PROVIDER) on-device [location providers](http://developer.android.com/reference/android/location/LocationProvider.html). It will return any location data registered by the on-device providers including real-time [satellite info](http://developer.android.com/reference/android/location/GpsSatellite.html).

In comparison to the W3C HTML Geolocation API, this plugin provides you with significantly greater control and more information to make better decisions with geolocation data.

##Supported Platforms

**Android-only.** This plugin is designed for Android API 5.0.0 (Lollipop / API Level 21) and greater. Cordova supports the following android [releases](https://github.com/apache/cordova-android/releases).

##Quick Start!

Here are the cliff notes for getting started. More details on the Cordova CLI can be found [here](https://cordova.apache.org/docs/en/latest/guide/cli/index.html).

`cordova create cordova-advanced-geolocation com.esri.geo GeoTest`

`cd cordova-advanced-geolocation`

`cordova platform add android`

`cordova platform update android@5.0.0`

`cordova plugin add https://github.com/esri/cordova-plugin-advanced-geolocation.git`

In `config.xml` modify the following: ` <content src="sample-map.html" />`

`cordova build`

Plug in your phone and run: `cordova run android`, or in Android Studio select `^D`.

##IMPORTANT!

This API does **NOT** conform to the coding patterns described in the W3C Geolocation API Specification. 

Use this plugin to gain significantly greater control and insight into which location provider submitted geolocation data. 

This plugin won't increase device accuracy. Accuracy is heavily dependent on the quality of the GPS antenna and the GPS processor. Most smartphones and tablets use consumer-priced GPS chipsets and micro-strip antennas which under perfect conditions typically return between 3 - 10 meters accuracy, at best. For more information check out this series of [blog posts](http://www.andygup.net/android-gps/). 

External consumer GPS devices may help provide better accuracy, faster location acquisition and less location fluctuation. Here's a good article on [Why Buy an External GPS Antenna?](http://www.ebay.com/gds/Why-Buy-an-External-GPS-Antenna-/10000000177631439/g.html).

If your requirements specifically call for less than 3 meter accuracy then consider using a commercial, external high-accuracy GPS such as the Trimble R1 and slave it to your device via Bluetooth.  

**PRIVACY WARNING** Keep in mind the [W3C security and privacy considerations](http://dev.w3.org/geo/api/spec-source.html#security). This plugin uses native geolocation functionality only. Users will not automatically see a W3C Geolocation prompt, they will only get native Android prompts. The plugin requires the following Android User Permissions: [ACCESS_COARSE_LOCATION](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION), [ACCESS_FINE_LOCATION](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION), [ACCESS_NETWORK_STATE](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_NETWORK_STATE), [ACCESS_WIFI_STATE](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_WIFI_STATE) and [INTERNET](http://developer.android.com/reference/android/Manifest.permission.html#INTERNET).


##Example Usage

```javaScript
    
    // Implement this in `deviceready` event callback
    AdvancedGeolocation.start(function(success){

        try{
            var jsonObject = JSON.parse(success);

            switch(jsonObject.provider){
                case "gps":
					 //TODO
                    break;

                case "network":
					 //TODO
                    break;

                case "satellite":
					 //TODO
                    break;
            }
        }
        catch(exc){
            console.log("Invalid JSON: " + exc);
        }
    },
    function(error){
        console.log("ERROR! " + JSON.stringify(error));
    },
    ////////////////////////////////////////////
    //
    // REQUIRED:
    // These are required Configuration options!
    // See API Reference for additional details.
    //
    ////////////////////////////////////////////
    {
        "minTime":500,         // Min time interval between updates (ms)
        "minDistance":1,       // Min distance between updates (meters)
        "noWarn":true,         // Native location provider warnings
        "providers":"all",     // Return GPS and NETWORK locations
        "useCache":true,       // Return GPS and NETWORK cached locations
        "satelliteData":false, // Return of GPS satellite info
        "buffer":false,        // Buffer location data
        "bufferSize":0         // Max elements in buffer
    });


```

##Use Cases

Here are example use cases for the different ways location providers can be set in the configuration options:

* **`"gps"`** Activates only the GPS provider. Best accuracy where device has an unobstructed view of the sky.
* **`"network"`** Activates only the Network provider. Best accuracy indoors and urban/downtown areas with tall buildings where device does not have an unobstructed view of the sky and cellular service is available and/or WiFi. 
* **`"all"`** Activates both GPS and Network providers. Allows you to take advantage of network providers to establish initial location and then use GPS to finalize a more accurate location. Typically the device will provide the network location first before the GPS warms up. After the GPS warms up, and if the accuracy is good enough for your requirements, then you would switch to using the GPS locations.


##Geolocation Data Description

The following geolocation data may be exposed and accessible by this API if the on-device provider is available and enabled:
* Real-time GPS location
* Cached GPS location
* GPS satellites meta data
* Real-time Network location triangulation
* Cached Network location

#API Reference

This geolocation API is multi-threaded for maximum efficiency and reduced impact on user experience.

##Methods
Method | Description
--- | ---
`start` | Starts any location providers that were specified in the configuration options. 
`stop` | Stops all location processes. This will also automatically occur when the app is placed in the background. The app will continue to consume memory.
`kill` | Shuts down all location activities, stops all threads and destroys the application instance. Can be used to hard stop a runaway GPS process, for example, or to simply close the application and stop all processes.

##Configuration Options (Required)

Option | Type | Description
--- | --- | ---
`minTime` | integer | The minimum time interval between location updates in milliseconds. Smaller numbers increase battery usage.
`minDistance` | integer | The minimum distance between location updates in meters. Smaller numbers increase battery usage.
`noWarn` | boolean | Display native warning popup dialog if GPS or Network is disabled.
`providers` | String | Acceptable values to specify location providers are: `"gps"`, `"network"`, or `"all"`. Network provider may return locations if WiFi or cellular internet is enabled.
`useCache` | boolean | Will return cached values from any active location provider. While not gauranteed, both GPS and NETWORK providers have a cache.
`satelliteData` | boolean | If `true` it returns all available satellite data from the GPS receiver. Requires that the `gps` provider is also enabled. <br><br>**CAUTION:** Activating satellite data will increase CPU and memory usage. 
`buffer` | boolean | If `true` it will start a buffer that returns the averaged geometric center. Use this when requirements call for determining a single, best location. The buffer uses a FIFO ordering, so new values added and old values are removed. 
`bufferSize` | integer | The maximum number of elements allowed within the buffer. It's strongly recommended to use as small of a buffer size as possible to minimize memory usage and garbage collection. Experiment to see what works best This property will be ignored if `buffer` is set to `false`. Buffers larger than 30 elements may not be necessary.<br><br>**CAUTION:** Increasing the buffer size will increase CPU and memory usage. 

## GPS and Network Data

Whenever a location event is successful, this plugin will return the following location data in the form of a JSON payload. This section provides a description of the attribute/value pairs that are returned.

Example:

```javascript

	{
    "provider":"gps",
    "latitude":"42.05991886",
    "longitude":"-105.00000",
    "altitude":"1565.0",
    "accuracy":"9.0",
    "bearing":"0.0",
    "speed":"1.0",
    "timestamp":"1449876957000",
    "cached":"false"
    }

```

Property | Type |  Value | Description
--- | --- | --- | ---
`provider` | String | `"gps"` or `"network"` | Let's you determine where this data is coming from.
`latitude` | String | number | Latitude in degrees. Be sure to check for `0.0`. values if no location was returned. `0.0` is a valid [WGS 84 location off the coast of Africa](https://en.wikipedia.org/wiki/Null_Island). 
`longitude` | String | number | Longitude in degrees. Be sure to check for `0.0`. values if no location was returned.
`altitude` | String | number | Altitude if available, in meters above the WGS 84 reference ellipsoid. If this location does not have an altitude then `0.0` is returned.
`accuracy` | String | number | Get the estimated accuracy of this location, in meters. <br><br>Android defines accuracy as the radius of 68% confidence. In other words, if you draw a circle centered at this location's latitude and longitude, and with a radius equal to the accuracy, then there is a 68% probability that the true location is inside the circle.<br><br>In statistical terms, it is assumed that location errors are random with a normal distribution, so the 68% confidence circle represents one standard deviation. Note that in practice, location errors do not always follow such a simple distribution.<br><br>This accuracy estimation is only concerned with horizontal accuracy, and does not indicate the accuracy of bearing, velocity or altitude if those are included in this Location.<br><br>If this location does not have an accuracy, then `0.0` is returned. All locations generated by the LocationManager include an accuracy.
`bearing` | String | number | The bearing, in degrees. Bearing is the horizontal direction of travel of this device, and is not related to the device orientation. It is guaranteed to be in the range (0.0, 360.0] if the device has a bearing.<br><br>If a location does not have a bearing then `0.0` is returned.
`speed` | String | number | The speed if it is available, in meters/second over ground. If this location does not have a speed then `0.0` is returned.
`timestamp` | String | number | Return the UTC time of this fix, in milliseconds since January 1, 1970.<br><br>Note that the UTC time on a device is not monotonic: it can jump forwards or backwards unpredictably.<br><br>All locations generated by the LocationManager are guaranteed to have a valid UTC time, however remember that the system time may have changed since the location was generated.
`cached` | String | boolean | Whether or not the location data, either GPS or Network, was returned from the device cache.<br><br>Note: cached data can be very unpredictable, use with caution and make sure to compare the timestamp with current time to establish the age of the cached location.

## Buffered GPS and Network Data

If you set the `buffer` configuration option to `true` this will enable new attribute/value pairs to be included in the return payload that are in addition to the GPS and Network elements listed above.

Buffering is typically used in commercial and government applications that require greater accuracy for determining static locations. Using the buffer will minimize location fluctuations by providing an averaged geometric center of cartesian coordinates.

Best practices for buffer requires the user to hold the device still in one location until the desired accuracy level is reached. An all-JavaScript (non-native Android) sample app can be found in the html5-geolocation-tool-js [Field Location Template](https://github.com/Esri/html5-geolocation-tool-js).

Note that cached location data is not buffered since there will only one cached location per provider.

Example:

```javascript

    {
    "provider":"gps",
    "latitude":39.91974497,
    "longitude":-105.11730789,
    "altitude":1651,
    "accuracy":48,
    "bearing":0,
    "speed":0,
    "timestamp":1452634769000,
    "cached":false,
    "buffer":true,
    "bufferSize":10,
    "bufferedLatitude":39.919744632857146,
    "bufferedLongitude":-105.11730871142859,
    "bufferedAccuracy":48
    }

```

Property | Type |  Value | Description
--- | --- | --- | ---
`buffer` | String | boolean | Indicates whether or not buffering has been activated.
`bufferSize` | String | integer | Indicates the number of elements within the buffer. You can compare this value against the `maxBufferSize` as set in the configuration options.
`bufferedLatitude` | String | number | The buffer's geometric latitudinal center. Value is latitude in degrees. Be sure to check for `0.0` values indicating that no latitude data was provided.
`bufferedLongitude` | String | number | The buffer's geometric longitudinal center. Value is longitude in degrees. Be sure to check for `0.0` values indicating that no longitude data was provided.
`bufferedAccuracy` | String | number | The buffer's average horizontal accuracy in meters. It may be possible to have a buffered accuracy equal to `0.0`.

##Satellite Data

If you have the Configuration option `satelliteData` to `true`, then for each satellite detected by the GPS the following data will be returned as JSON in the callback. This section provides a description of the attribute/value pairs that are returned. 

Note: it's up to the GPS to determine whether or not any values will be provided for each property, this is especially true as the GPS warms up.

The plugin assigns a number to each satellites simply to make it easy to iterate through the JSON file. These numbers infer no actual relationship to a particular satellite. The `PRN` number is the actual satellite identifier.

Property | Type |  Value | Description
--- | --- | --- | ---
`provider` | String | `"satellite"` | Let's you determine where this data is coming from.
`timestamp` | number | milliseconds | Time right now based on the Calendar whose locale is determined by system settings. Gregorian calendar assumes counting begins at the start of the epoch: i.e., YEAR = 1970, MONTH = JANUARY, DATE = 1, etc. For more info see [java.util.Calendar](http://developer.android.com/reference/java/util/Calendar.html).
number | String | number | Each satellite is assigned a sequential number for assisting with iterating the JSON. The numbers are simply a matter of convienence and have no other relationship with the satellite data.
`PRN` | String | integer | Returns the PRN (pseudo-random number) for the satellite. For more info see this [wikipedia article](https://en.wikipedia.org/wiki/List_of_GPS_satellites). Numbers 01 thru 32 represent GPS satellites. GLONASS uses higher numbers. 
`timeToFirstFix` | String | milliseconds | Returns the time required to receive the first fix since the most recent restart of the GPS engine.
`usedInFix` | String | boolean | Indicates whether or not a satellite was used to determine a GPS fix.
`azimuth` | String | number | Returns the azimuth of the satellite in degrees. The azimuth can vary between 0 and 360.
`elevation` | String | number | Returns the elevation of the satellite above the horizon in degrees. The elevation can vary between 0 and 90. 
`hasEphemeris` | String | boolean | Returns true if the GPS engine has ephemeris data for the satellite. 
`hasAlmanac` | String | boolean | Returns true if the GPS engine has almanac data for the satellite. 
`SNR` | String | number | Returns the signal to noise ratio for the satellite.  

## FAQ

* **Which location providers does this plugin use?** The plugin can be configured to use both [GPS](http://developer.android.com/reference/android/location/LocationManager.html#GPS_PROVIDER) and [NETWORK](http://developer.android.com/reference/android/location/LocationManager.html#NETWORK_PROVIDER) location providers. NETWORK location providers require access to the internet whether it's via cellular or WiFi connection. The plugin does not use [PASSIVE](http://developer.android.com/reference/android/location/LocationManager.html#PASSIVE_PROVIDER) location providers because you have no direct control over those.
* **Will this library work as a background process?** No. This library is **not** designed to be used while minimized. Because of its potential to consume large amounts of memory and CPU cycles it will only provide locations, by default, while the application is in the foreground and active.
* **I got a plugin not supported error, what do I do?** If you get the following error `Plugin doesn't support this project's cordova-android version. cordova-android: 4.1.1, failed version requirement: >=5.0.0
Skipping 'cordova-plugin-advanced-geolocation' for android`, then you most likely need to upgrade your version of cordova-android. You can explicitly upgrade by running the following command in your cordova project directory `cordova platform update android@5.0.0`. 
* **How come this plug-in does not support iOS?** iOS does not give you the same granular-level control over the location manager as does Android. 
* **Does the plugin store location data?** No. The only information it stores intentionally is the `action` setting in the Configuration options. The reason that is stored is so the application can automatically restart after being paused or placed in the background.

## Sample Mapping App

Included with the plugin is a sample demonstration mapping app called `sample-map.html`. To use it simply change the following line in your `config.xml` to point to the app's location, for example:

```javscript

   <content src="sample-map.html" />

```

![Sample Mapping App](sample_mapping_app.png)

##Licensing

Copyright 2016 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [license.txt]( license.txt) file.

[](Esri Tags: JavaScript HTML5 GPS Test ArcGIS Location Tools)
[](Esri Language: JavaScript)


