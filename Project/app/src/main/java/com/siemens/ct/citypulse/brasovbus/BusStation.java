package com.siemens.ct.citypulse.brasovbus;

import citypulse.commons.data.Coordinate;

/**
 * Created by z003jn8y on 26.09.2016.
 */
public class BusStation {

    private String busStationName;
    private Coordinate coordinate;
    private String uuid;

    public BusStation() {
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBusStationName() {
        return busStationName;
    }

    public void setBusStationName(String busStationName) {
        this.busStationName = busStationName;
    }
}
