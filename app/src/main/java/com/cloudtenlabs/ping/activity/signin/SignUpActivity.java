package com.cloudtenlabs.ping.activity.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.App;
import com.cloudtenlabs.ping.BuildConfig;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
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

public class SignUpActivity extends AppCompatActivity {

    private Context context;
    private SharedPreferences.Editor editor;
    private EditText emailTextField, passwordTextField, confirmPasswordTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        context = this;

        emailTextField = findViewById(R.id.emailTextField);
        passwordTextField = findViewById(R.id.passwordTextField);
        confirmPasswordTextField = findViewById(R.id.confirmPasswordTextField);

        SharedPreferences sharedPreferences = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            String email = emailTextField.getText().toString();
            String password = passwordTextField.getText().toString();
            String confirm = confirmPasswordTextField.getText().toString();

            if (email.equals("")) {
                Toast.makeText(context, "Please enter an email address", Toast.LENGTH_SHORT).show();
            } else if (!GlobalFunction.getInstance().isValidEmail(email)) {
                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirm)) {
                Toast.makeText(context, "Password and confirm password does not match", Toast.LENGTH_SHORT).show();
            } else {
                final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
                APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                RequestBody emailBody = RequestBody.create(MediaType.parse("multipart/form-data"), email);
                RequestBody phoneBody = RequestBody.create(MediaType.parse("multipart/form-data"), "");
                RequestBody passwordBody = RequestBody.create(MediaType.parse("multipart/form-data"), password);
                RequestBody osBody = RequestBody.create(MediaType.parse("multipart/form-data"), "android");
                RequestBody versionBody = RequestBody.create(MediaType.parse("multipart/form-data"), BuildConfig.VERSION_NAME);

                Call<JsonObject> call = apiInterface.register(emailBody, phoneBody, passwordBody, osBody, versionBody);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        JsonObject result = response.body();
                        assert result != null;
                        int success = result.get("success").getAsInt();
                        if (success == 1) {
                            Gson gson = new Gson();
                            User user = gson.fromJson(result.getAsJsonObject("user"), User.class);

                            QBUser qbUser = new QBUser();
                            qbUser.setLogin(String.valueOf(user.getId()));
//                            qbUser.setEmail(user.getEmail());
                            qbUser.setPassword(App.USER_DEFAULT_PASSWORD);
                            qbUser.setCustomData(user.getPhoto());

                            QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                                @Override
                                public void onSuccess(QBUser qbUser1, Bundle bundle) {
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

                                                                Intent intent = new Intent(context, FirstNameActivity.class);
                                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                startActivity(intent);
                                                                finish();
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
                        } else {
                            progressDialog.dismiss();
                            String error = result.get("error").getAsString();
                            Toast.makeText(context,  error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
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

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
}
