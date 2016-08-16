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

import com.esri.cordova.geolocation.model.Error;

/**
 * This is a central repository for managing error messages and reducing duplication.
 */
public class ErrorMessages {

    // Location request errors are 100 series
    public static final String NETWORK_UNAVAILABLE = "{\"error\": \"101\", \"msg\":\"Network location requested but network is not available\"}";

    // Configuration errors are 900 series
    public static final String INCORRECT_CONFIG_ARGS = "{\"error\": \"901\", \"msg\": \"There was a problem with the optional configuration arguments\"}";

    public static Error CELL_DATA_NOT_AVAILABLE(){
        final Error err = new Error();
        err.number = "102";
        err.message = "Cell data requested but unavailable. Check internet connection";

        return err;
    }

    public static Error CELL_DATA_NOT_ALLOWED(){
        final Error err = new Error();
        err.number = "103";
        err.message = "Cell Data option is not available on Android API versions < 18";

        return err;
    }

    public static Error LOCATION_SERVICES_UNAVAILABLE(){
        final Error err = new Error();
        err.number = "105";
        err.message = "Neither GPS nor network location is available";

        return err;
    }

    public static Error LOCATION_SERVICES_DENIED_NOASK(){
        final Error err = new Error();
        err.number = "106";
        err.message = "Location services were denied by user with the flag to never ask again";

        return err;
    }

    public static Error LOCATION_SERVICES_DENIED(){
        final Error err = new Error();
        err.number = "107";
        err.message = "Location services were denied by user";

        return err;
    }

    public static Error GPS_UNAVAILABLE(){
        final Error err = new Error();
        err.number = "110";
        err.message = "GPS location requested but GPS is not available";

        return err;
    }

    public static Error GPS_OUT_OF_SERVICE(){
        final Error err = new Error();
        err.number = "111";
        err.message = "GPS is out of service";

        return err;
    }

    public static Error GPS_THREAD_EXCEPTION(){
        final Error err = new Error();
        err.number = "112";
        err.message = "Library failure: uncaught exception in GPS Controller.";

        return err;
    }
}