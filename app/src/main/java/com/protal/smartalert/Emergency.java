package com.protal.smartalert;

public class Emergency {
    double longitude, latitude;
    long timestamp;
    String state;
    public Emergency() {

    }

    public Emergency(String state,double longitude, double latitude, long timestamp) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.state = state;
        this.timestamp=timestamp;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp; }
}

