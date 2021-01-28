package com.cloudtenlabs.ping.activity.signin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.App;
import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.DeviceUtil;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.facebook.AccessToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartActivity extends AppCompatActivity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        context = this;
        SharedPreferences sharedPreferences = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean("user_registered", false)) {
            String email = sharedPreferences.getString("email", "");
            String password = sharedPreferences.getString("password", "");
            String facebook_id = sharedPreferences.getString("facebook_id", "");
            assert password != null;
            assert email != null;

            if (!email.equals("") && !password.equals("")) {
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody emailBody = RequestBody.create(MediaType.parse("multipart/form-data"), email);
                RequestBody passwordBody = RequestBody.create(MediaType.parse("multipart/form-data"), password);
                RequestBody osBody = RequestBody.create(MediaType.parse("multipart/form-data"), "android");
                RequestBody versionBody = RequestBody.create(MediaType.parse("multipart/form-data"), BuildConfig.VERSION_NAME);
                RequestBody isSimulatorBody = RequestBody.create(MediaType.parse("multipart/form-data"), DeviceUtil.isEmulator() ? "1" : "0");

                Call<JsonObject> call = apiInterface.login(emailBody, passwordBody, osBody, versionBody, isSimulatorBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();
                        if (success == 1 && result.has("user")) {
                            Gson gson = new Gson();
                            User user = gson.fromJson(result.getAsJsonObject("user"), User.class);
                            if (user.getQb_id() != null) {
                                QBUser qbUser = new QBUser();
                                qbUser.setLogin(String.valueOf(user.getId()));
                                qbUser.setPassword(App.USER_DEFAULT_PASSWORD);
                                QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                    @Override
                                    public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                        user.setQb_user(qbUser1);
                                        QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                                            @Override
                                            public void onSuccess(Object o, Bundle bundle) {
                                                GlobalVariable.getInstance().loggedInUser = user;
                                                loginAction();
                                            }

                                            @Override
                                            public void onError(QBResponseException e) {
                                                loginAction();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        loginAction();
                                    }
                                });
                            } else {
                                loginAction();
                            }
                        } else {
                            loginAction();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        loginAction();
                        call.cancel();
                    }
                });
            } else {
                assert facebook_id != null;
                if (!facebook_id.equals("")) {
                    AccessToken accessToken = AccessToken.getCurrentAccessToken();
                    boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
                    if (isLoggedIn) {
                        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
                        RequestBody facebookIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), accessToken.getUserId());
                        RequestBody osBody = RequestBody.create(MediaType.parse("multipart/form-data"), "android");
                        RequestBody versionBody = RequestBody.create(MediaType.parse("multipart/form-data"), BuildConfig.VERSION_NAME);

                        Call<JsonObject> call = apiInterface.login_fb_background(facebookIdBody, osBody, versionBody);
                        call.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                                JsonObject result = response.body();
                                assert result != null;
                                int success = result.get("success").getAsInt();

                                if (success == 1 && result.has("user")) {
                                    Gson gson = new Gson();
                                    User user = gson.fromJson(result.getAsJsonObject("user"), User.class);
                                    if (user.getQb_id() != null) {
                                        QBUser qbUser = new QBUser();
                                        qbUser.setLogin(String.valueOf(user.getId()));
                                        qbUser.setPassword(App.USER_DEFAULT_PASSWORD);
                                        QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                            @Override
                                            public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                                user.setQb_user(qbUser1);
                                                QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                                                    @Override
                                                    public void onSuccess(Object o, Bundle bundle) {
                                                        GlobalVariable.getInstance().loggedInUser = user;
                                                        loginAction();
                                                    }

                                                    @Override
                                                    public void onError(QBResponseException e) {
                                                        loginAction();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(QBResponseException e) {
                                                loginAction();
                                            }
                                        });
                                    } else {
                                        loginAction();
                                    }
                                } else {
                                    loginAction();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                                loginAction();
                                call.cancel();
                            }
                        });
                    }
                } else {
                    Intent intent = new Intent(context, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        } else {
            Intent intent = new Intent(context, GetStartedActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loginAction() {
        if (GlobalVariable.getInstance().loggedInUser == null) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (GlobalVariable.getInstance().loggedInUser.getFirst_name() == null) {
            Intent intent = new Intent(context, FirstNameActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (GlobalVariable.getInstance().loggedInUser.getBirthday() == null) {
            Intent intent = new Intent(context, BirthdayActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (GlobalVariable.getInstance().loggedInUser.getGender() == null) {
            Intent intent = new Intent(context, GenderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (GlobalVariable.getInstance().loggedInUser.getAbout() == null) {
            Intent intent = new Intent(context, BioActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (GlobalVariable.getInstance().loggedInUser.getPhotos().size() == 0) {
            Intent intent = new Intent(context, UploadPhotoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(context, DrawerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

}