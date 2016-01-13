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
package com.esri.cordova.geolocation.model;

import com.esri.cordova.geolocation.utils.GeodataHelper;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FIFO queue for storing and manipulating cartesian coordinates.
 */
public class LocationDataBuffer {

    private ConcurrentLinkedQueue<Coordinate> queue = new ConcurrentLinkedQueue<Coordinate>();
    private int _maxBufferSize;

    public LocationDataBuffer(int maxBufferSize){
        _maxBufferSize = maxBufferSize;
    }

    /**
     * Takes a Coordinate and adds it to the queue.
     * @param coordinate Any valid coordinate object.
     * @return Size of the queue.
     */
    public int add(Coordinate coordinate){
        queue.add(coordinate);
        final int size = queue.size();

        // Trim the queue to the maxBufferSize
        if(size == _maxBufferSize){
            queue.poll();
        }

        return size;
    }

    /**
     * Returns the average geometric center of the queue.
     * @return Coordinate
     */
    public Coordinate getGeographicCenter(){
        return GeodataHelper.getGeographicCenter(queue);
    }

    /**
     * Remove all elements from the queue.
     */
    public void clear(){
        queue.clear();
    }

    /**
     * Returns <code>true</code> if queue contains no elements.
     * @return boolean
     */
    public boolean isEmpty(){
        return queue.isEmpty();
    }
}
