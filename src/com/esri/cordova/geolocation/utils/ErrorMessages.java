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
    public static final String NETWORK_UNAVAILABLE = "ERROR 101: Network location requested but network is not available";
    public static final String GPS_UNAVAILABLE = "ERROR 102: GPS location requested but GPS is not available";
    public static final String CELLDATA_UNAVALABLE = "ERROR 103: Cell data requested but unavailable. Check internet connection";
    public static final String LOCATIONSERVICES_UNAVAILABLE = "ERROR 104: Neither GPS nor network location is available";

    // Configuration errors are 900 series
    public static final String INCORRECT_CONFIG_ARGS = "ERROR 901: There was a problem with the optional configuration arguments";
}
