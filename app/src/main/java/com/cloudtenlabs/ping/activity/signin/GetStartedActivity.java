package com.cloudtenlabs.ping.activity.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetStartedActivity extends AppCompatActivity {

    private Context context;
    private CallbackManager callbackManager = CallbackManager.Factory.create();
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        context = this;
        SharedPreferences sharedPreferences = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        TextView startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, AgreementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();

                        if (success == 1 && result.has("user")) {
                            Gson gson = new Gson();
                            GlobalVariable.getInstance().loggedInUser = gson.fromJson(result.getAsJsonObject("user"), User.class);

                            editor.putInt("user_id", GlobalVariable.getInstance().loggedInUser.getId());
                            editor.putString("facebook_id", loginResult.getAccessToken().getUserId());
                            editor.putBoolean("user_registered", true);
                            editor.remove("email");
                            editor.remove("password");
                            editor.apply();

                            loginAction();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

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
                // App code
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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
            finish();
        } else {
            Intent intent = new Intent(context, DrawerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

}
