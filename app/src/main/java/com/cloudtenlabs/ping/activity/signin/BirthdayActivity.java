package com.cloudtenlabs.ping.activity.signin;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.apptik.widget.MultiSlider;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BirthdayActivity extends AppCompatActivity {

    private Context context;

    private EditText birthdayTextField, cityTextField, emailTextField;
    private TextView heightTextView;

    private int height = 153;
    private String birthday = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday);

        context = this;

        LinearLayout emailLayout = findViewById(R.id.emailLayout);
        emailTextField = findViewById(R.id.emailTextField);

        if(GlobalVariable.getInstance().loggedInUser.getEmail() == null || GlobalVariable.getInstance().loggedInUser.getEmail().equals("")) {
            emailLayout.setVisibility(View.VISIBLE);
            if (AccessToken.isCurrentAccessTokenActive()) {
                GraphRequest request = GraphRequest.newMeRequest(
                        AccessToken.getCurrentAccessToken(),
                        (object, response) -> {
                            try {
                                String email = object.getString("email");
                                emailTextField.setText(email);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();
            }
        } else {
            emailTextField.setText(GlobalVariable.getInstance().loggedInUser.getEmail());
            emailLayout.setVisibility(View.GONE);
        }

        final Calendar calendar = Calendar.getInstance();
        birthdayTextField = findViewById(R.id.birthdayTextField);
        birthdayTextField.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_datepicker);

            DatePicker datePicker = dialog.findViewById(R.id.datePicker);
            if (birthday.equals("")) {
                datePicker.updateDate(calendar.get(Calendar.YEAR) - 18, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            } else {
                String dateFormat = "yyyy-MM-dd";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                try {
                    Date selectedDate = simpleDateFormat.parse(birthday);
                    calendar.setTime(selectedDate);
                    datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            TextView okButton = dialog.findViewById(R.id.okButton);
            okButton.setOnClickListener(view -> {
                String dateFormat = "yyyy-MM-dd";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                try {
                    birthday = datePicker.getYear() + "-" + (datePicker.getMonth() + 1) + "-" + datePicker.getDayOfMonth();
                    Date selectedDate = simpleDateFormat.parse(birthday);

                    dateFormat = "MMM, dd yyyy";
                    simpleDateFormat.applyPattern(dateFormat);
                    birthdayTextField.setText(simpleDateFormat.format(selectedDate));

                    dialog.hide();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            TextView cancelButton = dialog.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(view -> dialog.hide());

            dialog.show();
        });

        heightTextView = findViewById(R.id.heightTextView);

        MultiSlider heightSlider = findViewById(R.id.heightSlider);
        heightSlider.setMax(226, true);
        heightSlider.setMin(153, true);
        heightSlider.setOnThumbValueChangeListener((multiSlider, thumb, thumbIndex, value) -> {
            if (thumbIndex == 0) {
                height = value;
                heightTextView.setText(GlobalFunction.getInstance().convertCmToAFootAndInches(height));
                Log.d("HEIGHT_CHANGED", String.valueOf(height));
            }
        });

        cityTextField = findViewById(R.id.cityTextField);

        TextView nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            if (birthday.equals("")) {
                Toast.makeText(context, "Please choose your birthday", Toast.LENGTH_SHORT).show();
                return;
            }
            String city = cityTextField.getText().toString();
            if (city.equals("")) {
                Toast.makeText(context, "Please enter your city", Toast.LENGTH_SHORT).show();
                return;
            }
            String email = emailTextField.getText().toString();
            if (email.equals("")) {
                Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show();
                return;
            }

            final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Registering...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "birthday,height,city,email");
            RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), birthday + "," + height + "," + city + "," + email);

            Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        GlobalVariable.getInstance().loggedInUser.setBirthday(birthday);
                        GlobalVariable.getInstance().loggedInUser.setHeight(height);
                        GlobalVariable.getInstance().loggedInUser.setCity(city);

                        Intent intent = new Intent(context, GenderActivity.class);
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
    }
}
