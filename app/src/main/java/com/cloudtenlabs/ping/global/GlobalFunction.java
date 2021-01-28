package com.cloudtenlabs.ping.global;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cloudtenlabs.ping.object.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class GlobalFunction {

    private static final GlobalFunction ourInstance = new GlobalFunction();

    public static GlobalFunction getInstance() {
        return ourInstance;
    }

    private GlobalFunction() {

    }

//    public void showAlertMessage(String title, String message, Context context) {
//        AlertDialog.Builder alertDialg = new AlertDialog.Builder(context);
//        alertDialg.setMessage(message);
//        alertDialg.setTitle(title);
//        alertDialg.setPositiveButton("Ok", null);
//        alertDialg.setCancelable(true);
//        alertDialg.create().show();
//    }

    public boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public String convertCmToAFootAndInches(int cm) {
        double feet = cm/30.48;
        double inches = (cm/2.54) - ((int)feet * 12);
        long roundedInches = Math.round(inches);
        if (roundedInches == 12) {
            feet = feet + 1;
            roundedInches = 0;
        }
        return (int)feet + "'" + roundedInches + "\"";
    }

    public int convertBirthdayToAge(String birthday) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sdf.parse(birthday);
            Calendar dob = Calendar.getInstance();
            dob.setTime(date);

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)){
                age--;
            }
            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getScreenWidthInDPs(Context context){
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        return Math.round(dm.widthPixels / dm.density);
    }

//    public int pixelTodp(Context c, float pixel) {
//        float density = c.getResources().getDisplayMetrics().density;
//        float dp = pixel / density;
//        return (int)dp;
//    }

    public int dpTopixel(Context c, float dp) {
        float density = c.getResources().getDisplayMetrics().density;
        float pixel = dp * density;
        return (int)pixel;
    }

    public void sendPushNotification(User user, String message) {
        if (user.getPlayer_id() == null || user.getPlayer_id().equals("")) {
            return;
        }
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody playerIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(user.getPlayer_id()));
        RequestBody messageBody = RequestBody.create(MediaType.parse("multipart/form-data"), message);
        RequestBody vibrationBody = RequestBody.create(MediaType.parse("multipart/form-data"), user.getVibration());
        Call<JsonObject> call = apiInterface.send_message_push_notification(playerIdBody, messageBody, vibrationBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    Log.d("PUSH_MESSAGE", "Push sent");
                } else {
                    String error = result.get("error").getAsString();
                    Log.d("PUSH_MESSAGE", error);
                }
            }
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.d("PUSH_MESSAGE", "An unknown error occurred.");
            }
        });
    }

}
