package com.cloudtenlabs.ping.global;

import android.location.Location;

import com.cloudtenlabs.ping.BackgroundService;
import com.cloudtenlabs.ping.object.User;

public class GlobalVariable {

    public boolean IS_TESTING_MODE = false;

    public String SERVER_URL = IS_TESTING_MODE ? "http://10.0.3.2" : "http://3.137.9.15";
    public String SERVER_IMAGE_URL = SERVER_URL + "/yolo/api/upload/";

    public User loggedInUser = null;
    public Location currentLocation = null;
    public String playerId = null;

    public BackgroundService gpsService;
    public boolean mTracking = false;

    private static final GlobalVariable ourInstance = new GlobalVariable();

    public static GlobalVariable getInstance() {
        return ourInstance;
    }

    private GlobalVariable() {

    }

}