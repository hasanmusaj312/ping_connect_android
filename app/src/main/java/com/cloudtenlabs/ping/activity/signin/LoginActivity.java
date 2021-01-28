package com.cloudtenlabs.ping.activity.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.cloudtenlabs.ping.App;
import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.DeviceUtil;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private Context context;

    private EditText emailTextField, passwordTextField;

    private SharedPreferences.Editor editor;

    private CallbackManager callbackManager = CallbackManager.Factory.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        context = this;

        SharedPreferences sharedPreferences = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        emailTextField = findViewById(R.id.emailTextField);
        passwordTextField = findViewById(R.id.passwordTextField);

        CardView loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(view -> {
            String email = emailTextField.getText().toString();
            String password = passwordTextField.getText().toString();

            if (email.equals("")) {
                Toast.makeText(context, "Please enter an email address", Toast.LENGTH_SHORT).show();
            } else if (!GlobalFunction.getInstance().isValidEmail(email)) {
                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Logging...", true, false);
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
                        if (success == 1) {
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
                                                progressDialog.dismiss();
                                                GlobalVariable.getInstance().loggedInUser = user;

                                                editor.putInt("user_id", GlobalVariable.getInstance().loggedInUser.getId());
                                                editor.putString("email", GlobalVariable.getInstance().loggedInUser.getEmail());
                                                editor.putString("password", password);
                                                editor.putBoolean("user_registered", true);
                                                editor.remove("facebook_id");
                                                editor.apply();

                                                loginAction();
                                            }

                                            @Override
                                            public void onError(QBResponseException e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                QBUser qbUser = new QBUser();
                                qbUser.setLogin(String.valueOf(user.getId()));
                                qbUser.setPassword(App.USER_DEFAULT_PASSWORD);
                                qbUser.setFullName(user.getFirst_name());
//                                qbUser.setEmail(user.getEmail());
                                qbUser.setCustomData(user.getPhoto());

                                QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                    @Override
                                    public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                        user.setQb_user(qbUser1);

                                        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(user.getId()));
                                        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "qb_id");
                                        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(qbUser1.getId()));

                                        Call<JsonObject> call1 = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                                        call1.enqueue(new Callback<JsonObject>() {
                                            @Override
                                            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                                                JsonObject result = response.body();
                                                assert result != null;
                                                int success = result.get("success").getAsInt();

                                                if (success == 1) {
                                                    QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                                        @Override
                                                        public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                                            user.setQb_user(qbUser1);
                                                            QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                                                                @Override
                                                                public void onSuccess(Object o, Bundle bundle) {
                                                                    progressDialog.dismiss();
                                                                    GlobalVariable.getInstance().loggedInUser = user;

                                                                    editor.putInt("user_id", GlobalVariable.getInstance().loggedInUser.getId());
                                                                    editor.putString("email", GlobalVariable.getInstance().loggedInUser.getEmail());
                                                                    editor.putString("password", password);
                                                                    editor.putBoolean("user_registered", true);
                                                                    editor.remove("facebook_id");
                                                                    editor.apply();

                                                                    loginAction();
                                                                }

                                                                @Override
                                                                public void onError(QBResponseException e) {
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }

                                                        @Override
                                                        public void onError(QBResponseException e) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                } else {
                                                    progressDialog.dismiss();
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
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            progressDialog.dismiss();
                            String error = result.get("error").getAsString();
                            Toast.makeText(context, error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                        call.cancel();
                    }
                });
            }
        });

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Logging...", true, false);
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody facebookIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), loginResult.getAccessToken().getUserId());
                RequestBody osBody = RequestBody.create(MediaType.parse("multipart/form-data"), "android");
                RequestBody versionBody = RequestBody.create(MediaType.parse("multipart/form-data"), BuildConfig.VERSION_NAME);

                Call<JsonObject> call = apiInterface.login_fb(facebookIdBody, osBody, versionBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {


                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();

                        if (success == 1) {
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
                                                progressDialog.dismiss();
                                                GlobalVariable.getInstance().loggedInUser = user;

                                                editor.putInt("user_id", GlobalVariable.getInstance().loggedInUser.getId());
                                                editor.putString("facebook_id", loginResult.getAccessToken().getUserId());
                                                editor.putBoolean("user_registered", true);
                                                editor.remove("email");
                                                editor.remove("password");
                                                editor.commit();

                                                loginAction();
                                            }

                                            @Override
                                            public void onError(QBResponseException e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                QBUser qbUser = new QBUser();
                                qbUser.setLogin(String.valueOf(user.getId()));
                                qbUser.setPassword(App.USER_DEFAULT_PASSWORD);
                                qbUser.setFullName(user.getFirst_name());
//                                qbUser.setEmail(user.getEmail());
                                qbUser.setCustomData(user.getPhoto());

                                QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                    @Override
                                    public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                        user.setQb_user(qbUser1);

                                        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(user.getId()));
                                        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "qb_id");
                                        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(qbUser1.getId()));

                                        Call<JsonObject> call1 = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                                        call1.enqueue(new Callback<JsonObject>() {
                                            @Override
                                            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                                                JsonObject result = response.body();
                                                assert result != null;
                                                int success = result.get("success").getAsInt();

                                                if (success == 1) {
                                                    QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                                        @Override
                                                        public void onSuccess(QBUser qbUser1, Bundle bundle) {
                                                            user.setQb_user(qbUser1);
                                                            QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                                                                @Override
                                                                public void onSuccess(Object o, Bundle bundle) {
                                                                    progressDialog.dismiss();
                                                                    GlobalVariable.getInstance().loggedInUser = user;

                                                                    editor.putInt("user_id", GlobalVariable.getInstance().loggedInUser.getId());
                                                                    editor.putString("facebook_id", loginResult.getAccessToken().getUserId());
                                                                    editor.putBoolean("user_registered", true);
                                                                    editor.remove("email");
                                                                    editor.remove("password");
                                                                    editor.commit();

                                                                    loginAction();
                                                                }

                                                                @Override
                                                                public void onError(QBResponseException e) {
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }

                                                        @Override
                                                        public void onError(QBResponseException e) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                } else {
                                                    progressDialog.dismiss();
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
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            progressDialog.dismiss();
                            String error = result.get("error").getAsString();
                            Toast.makeText(context, error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                        call.cancel();
                    }
                });
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
            }
        });

        CardView facebookButton = findViewById(R.id.facebookButton);
        facebookButton.setOnClickListener(view -> LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "email")));

        TextView signupButton = findViewById(R.id.signupButton);
        signupButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        TextView forgotButton = findViewById(R.id.forgotButton);
        forgotButton.setOnClickListener(view -> startActivity(new Intent(context, ForgotPasswordActivity.class)));
    }

    private void loginAction() {
        if (GlobalVariable.getInstance().loggedInUser.getFirst_name() == null) {
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
        } else {
            Intent intent = new Intent(context, DrawerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

}