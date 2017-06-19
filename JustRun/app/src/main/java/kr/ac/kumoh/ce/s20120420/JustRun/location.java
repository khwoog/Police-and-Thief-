package kr.ac.kumoh.ce.s20120420.JustRun;

import java.io.Serializable;

/**
 * Created by woong on 2017-06-06.
 */
public class location implements Serializable {
    double latitude;
    double longitude;

    public location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}