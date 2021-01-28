package com.cloudtenlabs.ping.activity.signin;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.google.gson.JsonObject;

import io.apptik.widget.MultiSlider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenderActivity extends AppCompatActivity {

    private Context context;

    private TextView maleButton, femaleButton, preferredMaleButton, preferredFemaleButton, ageDescTextView;

    private String gender = "F", preferred_gender = "M";
    private int preferred_age_min = 18, preferred_age_max = 80;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gender);

        context = this;

        maleButton = findViewById(R.id.maleButton);
        femaleButton = findViewById(R.id.femaleButton);
        preferredMaleButton = findViewById(R.id.preferredMaleButton);
        preferredFemaleButton = findViewById(R.id.preferredFemaleButton);
        ageDescTextView = findViewById(R.id.ageDescTextView);

        maleButton.setOnClickListener(view -> {
            gender = "M";
            maleButton.setTextColor(Color.parseColor("#FD3F53"));
            femaleButton.setTextColor(Color.parseColor("#999999"));
        });

        femaleButton.setOnClickListener(view -> {
            gender = "F";
            maleButton.setTextColor(Color.parseColor("#999999"));
            femaleButton.setTextColor(Color.parseColor("#FD3F53"));
        });

        preferredMaleButton.setOnClickListener(view -> {
            preferred_gender = "M";
            preferredMaleButton.setTextColor(Color.parseColor("#FD3F53"));
            preferredFemaleButton.setTextColor(Color.parseColor("#999999"));
        });

        preferredFemaleButton.setOnClickListener(view -> {
            preferred_gender = "F";
            preferredMaleButton.setTextColor(Color.parseColor("#999999"));
            preferredFemaleButton.setTextColor(Color.parseColor("#FD3F53"));
        });

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "gender,preferred_gender,preferred_age_min,preferred_age_max");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), gender + "," + preferred_gender + "," + preferred_age_min + ',' + preferred_age_max);

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        GlobalVariable.getInstance().loggedInUser.setGender(gender);
                        GlobalVariable.getInstance().loggedInUser.setPreferred_gender(preferred_gender);
                        GlobalVariable.getInstance().loggedInUser.setPreferred_age_min(18 /*preferred_age_min*/);
                        GlobalVariable.getInstance().loggedInUser.setPreferred_age_max(80 /*preferred_age_max*/);
                        GlobalVariable.getInstance().loggedInUser.setPreferred_radius(13200000);

                        Intent intent = new Intent(context, BioActivity.class);
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

        MultiSlider ageSlider = findViewById(R.id.ageSlider);
        ageSlider.setMin(18, true);
        ageSlider.setMax(80, true);
        ageSlider.setOnThumbValueChangeListener((multiSlider, thumb, thumbIndex, value) -> {
            if (thumbIndex == 0) {
                preferred_age_min = value;
            } else if (thumbIndex == 1) {
                preferred_age_max = value;
            }
            ageDescTextView.setText("BETWEEN " + preferred_age_min + " AND " + preferred_age_max);
        });
    }
}
