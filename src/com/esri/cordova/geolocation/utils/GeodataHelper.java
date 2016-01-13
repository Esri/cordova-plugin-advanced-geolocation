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

import com.esri.cordova.geolocation.model.Coordinate;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GeodataHelper {

    private final static int _radiusKM = 6367; // earth's radius km's

    public static double getMean(double[] data){
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/data.length;
    }

    public static double getVariance(double[] data)
    {
        final double mean = getMean(data);
        double temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/data.length;
    }

    public static double getStdDev(double[] data)
    {
        return Math.sqrt(getVariance(data));
    }

    /**
     * Calculate the average geometric center of a Queue that contains cartesian coordinates
     * Reference: http://stackoverflow.com/questions/6671183/calculate-the-center-point-of-multiple-latitude-longitude-coordinate-pairs
     * Reference: http://stackoverflow.com/questions/1185408/converting-from-longitude-latitude-to-cartesian-coordinates
     * Reference: http://en.wikipedia.org/wiki/Spherical_coordinate_system
     * @param queue The location buffer queue
     * @return Returns a Coordinate object
     */
    public static Coordinate getGeographicCenter(final ConcurrentLinkedQueue<Coordinate> queue){
        double x = 0;
        double y = 0;
        double z = 0;
        float accuracy = 0;

        for(final Coordinate coordinate : queue){
            accuracy += coordinate.accuracy;

            // Convert latitude and longitude to radians
            final double latRad = Math.PI * coordinate.latitude / 180;
            final double lonRad = Math.PI * coordinate.longitude / 180;

            // Convert to cartesian coords
            x += _radiusKM * Math.cos(latRad) * Math.cos(lonRad);
            y += _radiusKM * Math.cos(latRad) * Math.sin(lonRad);
            z += _radiusKM * Math.sin(latRad);
        }

        // Get our averages
        final double xAvg = x / queue.size();
        final double yAvg = y / queue.size();
        final double zAvg = z / queue.size();
        final float accuracyAvg = accuracy / queue.size();

        // Convert cartesian back to radians
        final double sphericalLatRads = Math.asin(zAvg / _radiusKM);
        final double sphericalLonRads = Math.atan2(yAvg, xAvg);

        final Coordinate centerPoint = new Coordinate();
        centerPoint.latitude = sphericalLatRads * (180 / Math.PI);
        centerPoint.longitude = sphericalLonRads * (180 / Math.PI);
        centerPoint.accuracy = accuracyAvg;

        return centerPoint;
    }
}
