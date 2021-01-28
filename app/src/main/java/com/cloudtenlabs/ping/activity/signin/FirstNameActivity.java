package com.cloudtenlabs.ping.activity.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.google.gson.JsonObject;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstNameActivity extends AppCompatActivity {

    private Context context;

    private EditText firstNameTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_name);

        context = this;

        firstNameTextField = findViewById(R.id.firstNameTextField);

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            String firstName = firstNameTextField.getText().toString();
            if (firstName.equals("")) {
                Toast.makeText(context, "Please enter your first name", Toast.LENGTH_SHORT).show();
                return;
            }

            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "first_name");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), firstName);

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();
                    if (success == 1) {
//                        QBUser qbUser = new QBUser();
                        QBUser qbUser = GlobalVariable.getInstance().loggedInUser.getQb_user();
                        qbUser.setFullName(firstName);
//                        qbUser.setId(GlobalVariable.getInstance().loggedInUser.getQb_id());

                        QBUsers.updateUser(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                            @Override
                            public void onSuccess(QBUser qbUser, Bundle bundle) {
                                progressDialog.dismiss();

                                GlobalVariable.getInstance().loggedInUser.setQb_user(qbUser);
                                GlobalVariable.getInstance().loggedInUser.setFirst_name(firstName);

                                Intent intent = new Intent(context, BirthdayActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
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
                    Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        });

    }
}
