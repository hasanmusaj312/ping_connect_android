package com.cloudtenlabs.ping;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by roberto on 9/29/16.
 */

public class BackgroundService extends Service {

    private final LocationServiceBinder binder = new LocationServiceBinder();
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;

    public static final String NOTIFICATION_CHANNEL_ID = "10002";

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener
    {
        private Location mLastLocation;

        LocationListener(String provider)
        {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            mLastLocation = location;
//            Log.i(TAG, "LocationChanged: "+location);
            GlobalVariable.getInstance().currentLocation = mLastLocation;
            if (GlobalVariable.getInstance().loggedInUser != null) {
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "latitude,longitude");
                RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());

                Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        Intent intent = new Intent("LocationUpdated");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {

                    }
                });
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
//            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
//            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
//            Log.e(TAG, "onStatusChanged: " + status);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
//        return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(12345678, getNotification());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ignored) {

            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            int LOCATION_DISTANCE = 5;
            int LOCATION_INTERVAL = 500;
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener );
        } catch (java.lang.SecurityException ex) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification() {
        NotificationChannel channel;
        channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setAutoCancel(true);
        return builder.build();
    }


    class LocationServiceBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

}