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
