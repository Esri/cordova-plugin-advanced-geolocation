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
package com.esri.cordova.geolocation.utils;

/**
 * This is a central repository for managing error messages and reducing duplication.
 */
public class ErrorMessages {

    // Location request errors are 100 series
    public static final String NETWORK_UNAVAILABLE = "{\"err\": \"101\", \"msg\":\"Network location requested but network is not available\"}";
    public static final String GPS_UNAVAILABLE = "{\"err\": \"102\", \"msg\":\"GPS location requested but GPS is not available\"}";
    public static final String CELLDATA_UNAVALABLE = "{\"err\": \"103\", \"msg\":\"Cell data requested but unavailable. Check internet connection\"}";
    public static final String CELLDATA_NOT_ALLOWED = "{\"err\": \"104\", \"msg\":\"Cell Data option is not available on Android API versions < 18\"}";
    public static final String LOCATION_SERVICES_UNAVAILABLE = "{\"err\": \"105\",\"msg\": \"Neither GPS nor network location is available\"}";
    public static final String LOCATION_SERVICES_DENIED_NOASK = "{\"err\": \"106\",\"msg\": \"Location services were denied by user with the flag to never ask again.\"}";
    public static final String LOCATION_SERVICES_DENIED = "{\"err\": \"107\",\"msg\": \"Location services were denied by user.\"}";

    // Configuration errors are 900 series
    public static final String INCORRECT_CONFIG_ARGS = "{\"err\": \"901\", \"msg\": \"There was a problem with the optional configuration arguments\"}";
}
