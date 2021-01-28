package com.cloudtenlabs.ping;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.manager.BackgroundListener;
import com.google.gson.JsonObject;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OSPermissionObserver;
import com.onesignal.OSPermissionStateChanges;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;
import com.quickblox.auth.session.QBSettings;

import org.json.JSONException;
import org.json.JSONObject;

import me.leolin.shortcutbadger.ShortcutBadgeException;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.cloudtenlabs.ping.util.*;

public class App extends Application implements OSSubscriptionObserver, OneSignal.NotificationOpenedHandler, OSPermissionObserver, OneSignal.NotificationReceivedHandler {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!

    public static final String USER_DEFAULT_PASSWORD = "qb_ping_password!@#";
    public static final int CHAT_PORT = 5223;
    public static final int SOCKET_TIMEOUT = 300;
    public static final boolean KEEP_ALIVE = true;
    public static final boolean USE_TLS = true;
    public static final boolean AUTO_JOIN = false;
    public static final boolean AUTO_MARK_DELIVERED = true;
    public static final boolean RECONNECTION_ALLOWED = true;
    public static final boolean ALLOW_LISTEN_NETWORK = true;

    private static final String APPLICATION_ID = GlobalVariable.getInstance().IS_TESTING_MODE ? "82062" : "82041";
    private static final String AUTH_KEY = GlobalVariable.getInstance().IS_TESTING_MODE ? "6BKxF6WaRqHENRc" : "Pck4VVAAQ5fFRZc";
    private static final String AUTH_SECRET = GlobalVariable.getInstance().IS_TESTING_MODE ? "gYUBQMPCRGV4bfz" : "6qhcF3jJdFz97Sf";
    private static final String ACCOUNT_KEY = GlobalVariable.getInstance().IS_TESTING_MODE ? "8T_gyZDpz7R2vxtyXscN" : "8T_gyZDpz7R2vxtyXscN";

    private static final int MAX_PORT_VALUE = 65535;
    private static final int MIN_PORT_VALUE = 1000;
    private static final int MIN_SOCKET_TIMEOUT = 300;
    private static final int MAX_SOCKET_TIMEOUT = 60000;

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Required initialization logic here!
        initApplication();
        ActivityLifecycle.init(this);
        checkAppCredentials();
        checkChatSettings();
        initCredentials();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new BackgroundListener());

        final Intent intent = new Intent(this, BackgroundService.class);
        this.startService(intent);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        OneSignal.startInit(this)
                .setNotificationReceivedHandler(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.None)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
//        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.ERROR, OneSignal.LOG_LEVEL.ERROR);
        OneSignal.idsAvailable((userId, registrationId) -> {
            if (registrationId != null) {
                GlobalVariable.getInstance().playerId = userId;
                updatePlayerId();
            }
        });
        OneSignal.addSubscriptionObserver(this);
        OneSignal.addPermissionObserver(this);

    }

    private void initApplication() {
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }

    private void checkAppCredentials() {
        if (APPLICATION_ID.isEmpty() || AUTH_KEY.isEmpty() || AUTH_SECRET.isEmpty() || ACCOUNT_KEY.isEmpty()) {
            throw new AssertionError(getString(R.string.error_qb_credentials_empty));
        }
    }

    private void checkChatSettings() {
        if (USER_DEFAULT_PASSWORD.isEmpty() || (CHAT_PORT < MIN_PORT_VALUE || CHAT_PORT > MAX_PORT_VALUE)
                || (SOCKET_TIMEOUT < MIN_SOCKET_TIMEOUT || SOCKET_TIMEOUT > MAX_SOCKET_TIMEOUT)) {
            throw new AssertionError(getString(R.string.error_chat_credentails_empty));
        }
    }

    private void initCredentials() {
        QBSettings.getInstance().init(getApplicationContext(), APPLICATION_ID, AUTH_KEY, AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
        //
        // QBSettings.getInstance().setEndpoints("https://your_api_endpoint.com", "your_chat_endpoint", ServiceZone.PRODUCTION);
        // QBSettings.getInstance().setZone(ServiceZone.PRODUCTION);
    }

    @Override
    public void notificationReceived(OSNotification notification) {
        try {
//            OneSignal.clearOneSignalNotifications();
            String badge = notification.payload.additionalData.getString("badge_count");
            setBadgeCount(badge, getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setBadgeCount(String badge, Context ctx){
        try {
            int badgeCount = 0;
            if(badge != null && !badge.isEmpty()) {
                badgeCount = tryParseInt(badge);
            }
            if(badgeCount > 0) {
                ShortcutBadger. applyCountOrThrow(ctx, badgeCount);
            }
        } catch (ShortcutBadgeException e) {
            e.printStackTrace();
        }
    }
    static int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (!stateChanges.getFrom().getSubscribed() && stateChanges.getTo().getSubscribed()) {
            GlobalVariable.getInstance().playerId = stateChanges.getTo().getUserId();
            updatePlayerId();
        }
    }

    @Override
    public void onOSPermissionChanged(OSPermissionStateChanges stateChanges) {
        Log.d("ONESIGNAL", stateChanges.toString());
    }

    private void updatePlayerId() {
        if (GlobalVariable.getInstance().loggedInUser != null) {
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "player_id");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), GlobalVariable.getInstance().playerId);

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {

                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {

                }
            });
        }
    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundService")) {
                GlobalVariable.getInstance().gpsService = ((BackgroundService.LocationServiceBinder) service).getService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundService")) {
                GlobalVariable.getInstance().gpsService = null;
            }
        }
    };

    @Override
    public void notificationOpened(OSNotificationOpenResult result) {
        JSONObject data = result.notification.payload.additionalData;
        try {
            if (data.getString("type").equals("ping")) {
                Intent intent = new Intent("ShowPingedUser");
                intent.putExtra("user_id", data.getString("user_id"));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        } catch (JSONException ignored) {

        }
    }

}