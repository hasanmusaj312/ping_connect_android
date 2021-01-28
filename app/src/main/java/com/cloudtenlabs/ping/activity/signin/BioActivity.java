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

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BioActivity extends AppCompatActivity {

    private Context context;

    private EditText aboutTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bio);

        context = this;

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            String about = aboutTextField.getText().toString();
            if (about.equals("")) {
                Toast.makeText(context, "Please add a description about yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "about");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), about);

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        GlobalVariable.getInstance().loggedInUser.setAbout(about);

                        Intent intent = new Intent(context, UploadPhotoActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        String error = result.get("error").getAsString();
                        if (error.equals("")) {
                            Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
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

        aboutTextField = findViewById(R.id.aboutTextField);

    }
}