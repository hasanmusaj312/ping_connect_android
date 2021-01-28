package com.cloudtenlabs.ping.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.App;
import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.signin.LoginActivity;
import com.cloudtenlabs.ping.fragment.AllUsersFragment;
import com.cloudtenlabs.ping.fragment.ContactUsFragment;
import com.cloudtenlabs.ping.fragment.EditProfileFragment;
import com.cloudtenlabs.ping.fragment.FaqFragment;
import com.cloudtenlabs.ping.fragment.FavoritesFragment;
import com.cloudtenlabs.ping.fragment.HomeFragment;
import com.cloudtenlabs.ping.fragment.MessagesFragment;
import com.cloudtenlabs.ping.fragment.PrivacyPolicyFragment;
import com.cloudtenlabs.ping.fragment.SettingsFragment;
import com.cloudtenlabs.ping.fragment.TermsConditionsFragment;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.login.LoginManager;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.onesignal.OneSignal;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.apptik.widget.MultiSlider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ConnectionListener, QBChatDialogMessageListener {

    private static final String TAG_HOME = "home";
    private static final String TAG_MY_FAVORITES = "my_favorites";
    private static final String TAG_MESSAGES = "messages";
    private static final String TAG_FAQ = "faq";
    private static final String TAG_EDIT_PROFILE = "edit_profile";
    private static final String TAG_TERMS_CONDITIONS = "terms_conditions";
    private static final String TAG_PRIVACY_POLICY = "privacy_policy";
    private static final String TAG_CONTACT_US = "contact_us";
    private static final String TAG_ALL_USERS = "all_users";
    private static final String TAG_SETTINGS = "settings";

    private static String CURRENT_TAG = TAG_HOME;
    private DrawerLayout drawerLayout;
    private Handler mHandler;

    private LabeledSwitch labeledSwitch;
    private MultiSlider multiSlider;

    private TextView radiusTextView, descriptionTextView, badgeTextView;
    private Context context;

    private Socket socket;

    private QBIncomingMessagesManager incomingMessagesManager;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        context = this;

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mHandler = new Handler();

        OneSignal.clearOneSignalNotifications();

        LinearLayout allUsersLayout = findViewById(R.id.allUsersLayout);
        if (GlobalVariable.getInstance().loggedInUser.getIs_test().equals("1")) {
            allUsersLayout.setVisibility(View.VISIBLE);
        } else {
            allUsersLayout.setVisibility(View.GONE);
        }
        allUsersLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_ALL_USERS;
            loadHomeFragment();
        });

        badgeTextView = findViewById(R.id.badgeTextView);
        badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
        badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);

        LocalBroadcastManager.getInstance(this).registerReceiver(updateUnreadMessageBroadcastReceiver, new IntentFilter("UpdateUnreadCount"));

        if (GlobalVariable.getInstance().loggedInUser != null) {
            IO.Options options = new IO.Options();
            options.query = "user_id=" + GlobalVariable.getInstance().loggedInUser.getId();
            try {
                socket = IO.socket( GlobalVariable.getInstance().SERVER_URL + ":8080", options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            socket.connect();
//            socket.on("message_arrived", args -> runOnUiThread(() -> {
//                Gson gson = new Gson();
//                JsonParser jsonParser = new JsonParser();
//                JsonObject jsonObject = jsonParser.parse(( args[0]).toString()).getAsJsonObject();
//                Message message = gson.fromJson(jsonObject, Message.class);
//
//                if (message.getConversation_id() != GlobalVariable.getInstance().conversationId) {
//                    String sender = message.getSender().getFirst_name();
//                    String content = "";
//                    switch (message.getMessage_type()) {
//                        case "TEXT":
//                            content = sender + ": " + message.getMessage();
//                            break;
//                        case "IMAGE":
//                            content = sender + " sent you a picture";
//                            break;
//                        case "VIDEO":
//                            content = sender + " sent you a video";
//                            break;
//                    }
//                    if (message.getSender().getId() != GlobalVariable.getInstance().loggedInUser.getId()) {
//                        View v = findViewById(R.id.app_content);
//                        Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_SHORT);
//                        Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();
//                        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)snackbar.getView().getLayoutParams();
//                        params.gravity = Gravity.TOP;
//
//                        View customView = getLayoutInflater().inflate(R.layout.snack_bar_custom_view, null);
//
//                        CircleImageView userPhotoImageView = customView.findViewById(R.id.userPhotoImageView);
//                        userPhotoImageView.setVisibility(View.GONE);
//
//                        TextView titleTextView = customView.findViewById(R.id.titleTextView);
//                        titleTextView.setVisibility(View.GONE);
//
//                        TextView bodyTextView = customView.findViewById(R.id.bodyTextView);
//                        bodyTextView.setText(content);
//
//                        snackbarView.addView(customView, 0);
//                        snackbar.show();
//
//                        playSound();
//
//                        if (message.getIs_read() == 0) {
//                            ChatHelper.getInstance().totalUnreadMessageCount = ChatHelper.getInstance().totalUnreadMessageCount + 1;
//                            Intent intent = new Intent("UpdateUnreadCount");
//                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//                        }
//                    }
//                }
//
//                Intent intent = new Intent("MessageArrived");
//                intent.putExtra("message", message);
//                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//            }));
            socket.on("ping_arrived", args -> runOnUiThread(() -> {
                JSONObject ping = (JSONObject) args[0];
                try {
                    View v = findViewById(R.id.app_content);
                    Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_SHORT);
                    Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)snackbar.getView().getLayoutParams();
                    params.gravity = Gravity.TOP;

                    String user_id = ping.getString("user_id");
                    View customView = getLayoutInflater().inflate(R.layout.snack_bar_custom_view, null);
                    CircleImageView userPhotoImageView = customView.findViewById(R.id.userPhotoImageView);
//                            Picasso.get().load(Uri.parse(ping.getString("photo"))).into(userPhotoImageView);
                    Glide.with(context).load(ping.getString("photo")).into(userPhotoImageView);

                    TextView titleTextView = customView.findViewById(R.id.titleTextView);
                    titleTextView.setVisibility(View.GONE);

                    TextView bodyTextView = customView.findViewById(R.id.bodyTextView);
                    bodyTextView.setText(ping.getString("first_name"));

                    snackbarView.addView(customView, 0);
                    snackbar.show();

                    playSound();

                    customView.setOnClickListener(v1 -> {
                        Intent intent = new Intent("ShowPingedUser");
                        intent.putExtra("user_id", user_id);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    });

                    if (ping.getString("vibration").equals("1")) {
                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(500);
                        }
                    }
                } catch (JSONException ignored) {

                }
            }));
        }

        Fresco.initialize(this);

        labeledSwitch = findViewById(R.id.labeledSwitch);
        labeledSwitch.setOnToggledListener((toggleableView, isOn) -> {
            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Updating...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "push_notification");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), labeledSwitch.isOn() ? "1" : "0");

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        GlobalVariable.getInstance().loggedInUser.setPush_notification(labeledSwitch.isOn() ? "1" : "0");
                    } else {
                        labeledSwitch.setOn(GlobalVariable.getInstance().loggedInUser.getPush_notification().equals("1"));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    progressDialog.dismiss();
                    labeledSwitch.setOn(GlobalVariable.getInstance().loggedInUser.getPush_notification().equals("1"));

                }
            });
        });
        labeledSwitch.setOn(GlobalVariable.getInstance().loggedInUser.getPush_notification().equals("1"));

        descriptionTextView = findViewById(R.id.descriptionTextView);
        radiusTextView = findViewById(R.id.radiusTextView);
        multiSlider = findViewById(R.id.multiSlider);
        multiSlider.setMax(30270/*15270*/);
        multiSlider.setMin(150);


        int preferredRadius = GlobalVariable.getInstance().loggedInUser.getPreferred_radius();
        if (preferredRadius < 5280) {
            multiSlider.getThumb(0).setValue(preferredRadius);
            radiusTextView.setText(String.format("%d'", preferredRadius));
        } else {
            double miles = preferredRadius/5280.0F;
            radiusTextView.setText(String.format("%d mi", (int)(miles)));
            multiSlider.getThumb(0).setValue(((int) miles - 1) * 10 + 5280);
        }
        descriptionTextView.setText(String.format("Radius Set at %s. Widen Your Range as Needed", radiusTextView.getText().toString()));

        multiSlider.setOnThumbValueChangeListener((multiSlider, thumb, thumbIndex, value) -> {
            if (thumbIndex == 0) {
                if (value < 5280) {
                    radiusTextView.setText(String.format("%d'", value));
                } else {
                    double miles = (value - 5280.0F) / 10.0F + 1;
                    radiusTextView.setText(String.format("%d mi", (int)miles));
                }
                descriptionTextView.setText(String.format("Radius Set at %s. Widen Your Range as Needed", radiusTextView.getText().toString()));
            }
        });
        multiSlider.setOnTrackingChangeListener(new MultiSlider.OnTrackingChangeListener() {
            @Override
            public void onStartTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value) {

            }

            @Override
            public void onStopTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value) {
                final int radiusInFeet = multiSlider.getThumb(0).getValue() < 5280 ? multiSlider.getThumb(0).getValue() : ((multiSlider.getThumb(0).getValue() - 5280) / 10 + 1) * 5280;
                final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Updating...", true, false);
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "preferred_radius");
                RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(radiusInFeet));

                Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        progressDialog.dismiss();

                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();

                        if (success == 1) {
                            GlobalVariable.getInstance().loggedInUser.setPreferred_radius(radiusInFeet);
                        } else {
                            refreshData();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        refreshData();
                    }
                });
            }
        });

        LinearLayout homeLayout = findViewById(R.id.homeLayout);
        homeLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_HOME;
            loadHomeFragment();
        });

        LinearLayout settingsLayout = findViewById(R.id.settingsLayout);
        settingsLayout.setOnClickListener(view -> {
            CURRENT_TAG = TAG_SETTINGS;
            loadHomeFragment();
        });

        LinearLayout myFavoritesLayout = findViewById(R.id.myFavoritesLayout);
        myFavoritesLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_MY_FAVORITES;
            loadHomeFragment();
        });

        LinearLayout messagesLayout = findViewById(R.id.messagesLayout);
        messagesLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_MESSAGES;
            loadHomeFragment();
        });

        LinearLayout faqLayout = findViewById(R.id.faqLayout);
        faqLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_FAQ;
            loadHomeFragment();
        });

        LinearLayout editProfileLayout = findViewById(R.id.editProfileLayout);
        editProfileLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_EDIT_PROFILE;
            loadHomeFragment();
        });

        LinearLayout termsConditionsLayout = findViewById(R.id.termsConditionsLayout);
        termsConditionsLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_TERMS_CONDITIONS;
            loadHomeFragment();
        });

        LinearLayout privacyPolicyLayout = findViewById(R.id.privacyPolicyLayout);
        privacyPolicyLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_PRIVACY_POLICY;
            loadHomeFragment();
        });

        LinearLayout contactUsLayout = findViewById(R.id.contactUsLayout);
        contactUsLayout.setOnClickListener(v -> {
            CURRENT_TAG = TAG_CONTACT_US;
            loadHomeFragment();
        });

        LinearLayout pushNotificationLayout = findViewById(R.id.pushNotificationLayout);
        pushNotificationLayout.setOnClickListener(v -> {

        });

        LinearLayout deleteAccountLayout = findViewById(R.id.deleteAccountLayout);
        deleteAccountLayout.setOnClickListener(v -> {
            AlertDialog.Builder alertDialg = new AlertDialog.Builder(context);
            alertDialg.setMessage("Are you sure you want to delete account?");
            alertDialg.setTitle("Confirmation");
            alertDialg.setPositiveButton("Yes", (dialogInterface, i) -> {
                final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Deleting...", true, false);
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));

                Call<JsonObject> call = apiInterface.delete_account(userIdBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        progressDialog.dismiss();

                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();
                        if (success == 1) {
                            SharedPreferences sharedPreferences = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove("email");
                            editor.remove("password");
                            editor.remove("user_registered");
                            editor.remove("facebook_id");
                            editor.apply();

                            LoginManager.getInstance().logOut();
                            try {
                                QBChatService.getInstance().logout();
                                QBUsers.deleteUser(GlobalVariable.getInstance().loggedInUser.getQb_user().getId());

                                Intent intent = new Intent(context, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } catch (SmackException.NotConnectedException e) {
                                QBUsers.deleteUser(GlobalVariable.getInstance().loggedInUser.getQb_user().getId());

                                Intent intent = new Intent(context, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            String error = result.get("error").getAsString();
                            Toast.makeText(context, error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            alertDialg.setNegativeButton("No", null);
            alertDialg.setCancelable(true);
            alertDialg.create().show();
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            CURRENT_TAG = TAG_HOME;
            loadHomeFragment();
        }

        if (QBChatService.getInstance().isLoggedIn()) {
            ChatHelper.getInstance().loadDialogs(context);
        }

        if (GlobalVariable.getInstance().currentLocation != null) {
            if (GlobalVariable.getInstance().loggedInUser != null) {
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "latitude,longitude");
                RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), GlobalVariable.getInstance().currentLocation.getLatitude() + "," + GlobalVariable.getInstance().currentLocation.getLongitude());

                Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        Intent intent = new Intent("LocationUpdated");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {

                    }
                });
            }
        }
        if (GlobalVariable.getInstance().playerId != null) {
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

        if (!GlobalVariable.getInstance().mTracking) {
            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            GlobalVariable.getInstance().gpsService.startTracking();
                            GlobalVariable.getInstance().mTracking = true;

                            updateLocationPermissionStatus("A_Granted");
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            if (response.isPermanentlyDenied()) {
                                updateLocationPermissionStatus("A_PermanentlyDenied");

                                Intent intent = new Intent();
                                intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } else {
                                updateLocationPermissionStatus("A_Denied");
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        } else {
            updateLocationPermissionStatus("A_Granted");
        }
        QBChatService.getInstance().addConnectionListener(this);
        registerQbChatListeners();

        fabricLogUser();
        LocalBroadcastManager.getInstance(this).registerReceiver(showPingedUserBroadcastReceiver, new IntentFilter("ShowPingedUser"));
    }

    private void registerQbChatListeners() {
        try {
            incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
        } catch (Exception e) {
            reloginToChat();
            return;
        }
        if (incomingMessagesManager == null) {
            reloginToChat();
            return;
        }
        incomingMessagesManager.addDialogMessageListener(this);
    }

    private void unregisterQbChatListeners() {
        if (incomingMessagesManager != null) {
            incomingMessagesManager.removeDialogMessageListrener(this);
            incomingMessagesManager = null;
        }
    }

    private void reloginToChat() {
        QBChatService.getInstance().login(GlobalVariable.getInstance().loggedInUser.getQb_user(), new QBEntityCallback() {
            @Override
            public void onSuccess(Object o, Bundle bundle) {
                registerQbChatListeners();
                ChatHelper.getInstance().loadDialogs(context);
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (incomingMessagesManager == null) {
            registerQbChatListeners();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(showPingedUserBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateUnreadMessageBroadcastReceiver);
        socket.disconnect();
        QBChatService.getInstance().removeConnectionListener(this);
        unregisterQbChatListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        QBChatService.getInstance().removeConnectionListener(this);
        unregisterQbChatListeners();
    }

    private void updateLocationPermissionStatus(String status) {
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "location_permission");
        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), status);

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

    private void playSound() {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.graceful);
        mp.start();
    }

    private void fabricLogUser() {
//        Crashlytics.setUserIdentifier(String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
//        Crashlytics.setUserEmail(String.valueOf(GlobalVariable.getInstance().loggedInUser.getEmail()));
//        Crashlytics.setUserName(String.valueOf(GlobalVariable.getInstance().loggedInUser.getFirst_name()));
    }

    private BroadcastReceiver showPingedUserBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CURRENT_TAG = TAG_HOME;
            loadHomeFragment();
        }
    };

    @SuppressLint("DefaultLocale")
    public void refreshData() {
        int preferredRadius = GlobalVariable.getInstance().loggedInUser.getPreferred_radius();
        if (preferredRadius < 5280) {
            multiSlider.getThumb(0).setValue(preferredRadius/10);
            radiusTextView.setText(String.format("%d'", preferredRadius));
        } else {
            double miles = preferredRadius/528.0;
//            if (miles == 1000) {
//                radiusTextView.setText("Any");
//            } else {
                radiusTextView.setText(String.format("%d mi", (int)(miles/10.0)));
//            }
        }
        descriptionTextView.setText(String.format("Radius Set at %s. Widen Your Range as Needed", radiusTextView.getText().toString()));
        labeledSwitch.setOn(GlobalVariable.getInstance().loggedInUser.getPush_notification().equals("1"));
    }

    private BroadcastReceiver updateUnreadMessageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
            badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);
        }
    };

    public void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void loadHomeFragment() {
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_TAG) != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        Runnable mPendingRunnable = () -> {
            // update the main content by replacing fragments
            Fragment fragment = getHomeFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out);
            fragmentTransaction.replace(R.id.frame, fragment, CURRENT_TAG);
            fragmentTransaction.commitAllowingStateLoss();
        };

        mHandler.post(mPendingRunnable);

        drawerLayout.closeDrawer(GravityCompat.START);
        invalidateOptionsMenu();
    }

    private Fragment getHomeFragment() {
        switch (CURRENT_TAG) {
            case TAG_SETTINGS:
                return new SettingsFragment();
            case TAG_MY_FAVORITES:
                return new FavoritesFragment();
            case TAG_MESSAGES:
                return new MessagesFragment();
            case TAG_FAQ:
                return new FaqFragment();
            case TAG_EDIT_PROFILE:
                return new EditProfileFragment();
            case TAG_TERMS_CONDITIONS:
                return new TermsConditionsFragment();
            case TAG_PRIVACY_POLICY:
                return new PrivacyPolicyFragment();
            case TAG_CONTACT_US:
                return new ContactUsFragment();
            case TAG_ALL_USERS:
                return new AllUsersFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Quickblox connection listener
    @Override
    public void connected(XMPPConnection xmppConnection) {
        ChatHelper.getInstance().loadDialogs(context);
    }

    @Override
    public void authenticated(XMPPConnection xmppConnection, boolean b) {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {

    }

    @Override
    public void reconnectionSuccessful() {
        ChatHelper.getInstance().loadDialogs(context);
    }

    @Override
    public void reconnectingIn(int i) {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }

    //Quickblox DialogMessageListener
    @Override
    public void processMessage(String dialogId, QBChatMessage qbChatMessage, Integer senderId) {
        int dialogIndex = ChatHelper.getInstance().dialogIndex(dialogId);
        if (dialogIndex == -1) {
            QBRestChatService.getChatDialogById(dialogId).performAsync(new QBEntityCallback<QBChatDialog>() {
                @Override
                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                    QBPagedRequestBuilder pagedRequestBuilder = new QBPagedRequestBuilder();
                    pagedRequestBuilder.setPage(0);
                    pagedRequestBuilder.setPerPage(100);
                    QBUsers.getUsersByIDs(qbChatDialog.getOccupants(), pagedRequestBuilder).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
                        @Override
                        public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                            int dialogIndex = ChatHelper.getInstance().dialogIndex(qbChatDialog.getDialogId());
                            if (dialogIndex == -1) {
                                ChatHelper.getInstance().dialogs.add(qbChatDialog);
                                for (int i = 0; i < qbUsers.size(); i++) {
                                    int userIndex = ChatHelper.getInstance().userIndex(qbUsers.get(i).getId());
                                    if (userIndex != -1) {
                                        ChatHelper.getInstance().dialogsUsers.set(userIndex, qbUsers.get(i));
                                    } else {
                                        ChatHelper.getInstance().dialogsUsers.add(qbUsers.get(i));
                                    }
                                }
                            } else {
                                ChatHelper.getInstance().dialogs.set(dialogIndex, qbChatDialog);
                            }

                            Intent intent = new Intent("UpdateDialogs");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                            ChatHelper.getInstance().totalUnreadMessageCount(context);
                            showMessageBanner(qbChatMessage, dialogId);
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        } else {
            QBRestChatService.getChatDialogById(dialogId).performAsync(new QBEntityCallback<QBChatDialog>() {
                @Override
                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                    ChatHelper.getInstance().dialogs.set(dialogIndex, qbChatDialog);

                    Intent intent = new Intent("UpdateDialogs");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    ChatHelper.getInstance().totalUnreadMessageCount(context);
                    showMessageBanner(qbChatMessage, dialogId);
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }

    private void showMessageBanner(QBChatMessage qbChatMessage, String dialogId) {
        if (!ChatHelper.getInstance().currentDialogID.equals(dialogId) && !qbChatMessage.getSenderId().equals(GlobalVariable.getInstance().loggedInUser.getQb_id())) {
            int userIndex = ChatHelper.getInstance().userIndex(qbChatMessage.getSenderId());
            if (userIndex != -1) {
                QBUser qbUser = ChatHelper.getInstance().dialogsUsers.get(userIndex);

                String content = qbUser.getFullName() + ": " + qbChatMessage.getBody();
                View v = findViewById(R.id.app_content);
                Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_SHORT);
                Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)snackbar.getView().getLayoutParams();
                params.gravity = Gravity.TOP;

                View customView = getLayoutInflater().inflate(R.layout.snack_bar_custom_view, null);

                CircleImageView userPhotoImageView = customView.findViewById(R.id.userPhotoImageView);
                userPhotoImageView.setVisibility(View.GONE);

                TextView titleTextView = customView.findViewById(R.id.titleTextView);
                titleTextView.setVisibility(View.GONE);

                TextView bodyTextView = customView.findViewById(R.id.bodyTextView);
                bodyTextView.setText(content);

                snackbarView.addView(customView, 0);
                snackbar.show();

                playSound();
            }
        }
    }

    @Override
    public void processError(String dialogId, QBChatException e, QBChatMessage qbChatMessage, Integer senderId) {

    }

}