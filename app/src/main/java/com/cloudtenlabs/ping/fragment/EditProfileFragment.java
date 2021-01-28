package com.cloudtenlabs.ping.fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.activity.EditPhotosActivity;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.Photo;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.google.gson.JsonObject;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

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

public class EditProfileFragment extends Fragment {

    private LinearLayout photosContainerLayout;

    private TextView nameTextView, heightTextView, aboutTextView, ageDescTextView;
    private TextView maleButton, femaleButton, preferredMaleButton, preferredFemaleButton;
    private TextView badgeTextView;

    private EditText currentPasswordTextField, newPasswordTextField, confirmPasswordTextField;
    private int preferred_age_min = 18, preferred_age_max = 80;

    private MultiSlider ageSlider;
    private DrawerActivity mActivity;
    private Context mContext;

    public EditProfileFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view12 -> mActivity.toggleDrawer());

        TextView uploadNowButton = view.findViewById(R.id.uploadNowButton);
        uploadNowButton.setOnClickListener(view1 -> mContext.startActivity(new Intent(mContext, EditPhotosActivity.class)));

        photosContainerLayout = view.findViewById(R.id.photosContainerLayout);
        nameTextView = view.findViewById(R.id.nameTextView);
        heightTextView = view.findViewById(R.id.heightTextView);
        aboutTextView = view.findViewById(R.id.aboutTextView);
        maleButton = view.findViewById(R.id.maleButton);
        femaleButton = view.findViewById(R.id.femaleButton);
        preferredMaleButton = view.findViewById(R.id.preferredMaleButton);
        preferredFemaleButton = view.findViewById(R.id.preferredFemaleButton);
        TextView updatePasswordButton = view.findViewById(R.id.updatePasswordButton);
        currentPasswordTextField = view.findViewById(R.id.currentPasswordTextField);
        newPasswordTextField = view.findViewById(R.id.newPasswordTextField);
        confirmPasswordTextField = view.findViewById(R.id.confirmPasswordTextField);
        ageDescTextView = view.findViewById(R.id.ageDescTextView);

        ageSlider = view.findViewById(R.id.ageSlider);
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

        ageSlider.setOnTrackingChangeListener(new MultiSlider.OnTrackingChangeListener() {
            @Override
            public void onStartTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value) {

            }
            @Override
            public void onStopTrackingTouch(MultiSlider multiSlider, MultiSlider.Thumb thumb, int value) {
                savePreferredAge();
            }
        });

        heightTextView.setOnClickListener(view19 -> {
            final Dialog dialog = new Dialog(mContext, R.style.UpdateOverlayTheme);
            dialog.setContentView(R.layout.dialog_height_popup);

            EditText dlgNameTextField = dialog.findViewById(R.id.firstNameTextField);
            EditText dlgAboutTextField = dialog.findViewById(R.id.aboutTextField);
            TextView dlgHeightTextView = dialog.findViewById(R.id.heightTextView);
            EditText dlgBirthdayTextField = dialog.findViewById(R.id.birthdayTextField);
            MultiSlider dlgHeightSlider = dialog.findViewById(R.id.heightSlider);

            TextView updateButton = dialog.findViewById(R.id.updateButton);
            updateButton.setOnClickListener(view191 -> {
                String name = dlgNameTextField.getText().toString();
                if (name.equals("")) {
                    Toast.makeText(getContext(), "Please enter your first name", Toast.LENGTH_SHORT).show();
                    return;
                }
                String bio = dlgAboutTextField.getText().toString();
                if (bio.equals("")) {
                    Toast.makeText(getContext(), "Please add a description about yourself", Toast.LENGTH_SHORT).show();
                    return;
                }
                String birthday = dlgBirthdayTextField.getText().toString();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                try {
                    dialog.dismiss();

                    Date dtBirthday = simpleDateFormat.parse(birthday);
                    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    final String formatted_birthday = simpleDateFormat.format(dtBirthday);

                    final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Updating...", true, false);
                    APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

                    RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
                    RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "first_name,birthday,height,about");
                    RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), name + "," + formatted_birthday + "," + dlgHeightSlider.getThumb(0).getValue() + "," + bio);

                    Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                            progressDialog.dismiss();

                            JsonObject result = response.body();
                            assert result != null;
                            int success = result.get("success").getAsInt();

                            if (success == 1) {
                                QBUser qbUser = GlobalVariable.getInstance().loggedInUser.getQb_user();
                                qbUser.setFullName(name);
                                QBUsers.updateUser(qbUser);

                                GlobalVariable.getInstance().loggedInUser.setFirst_name(name);
                                GlobalVariable.getInstance().loggedInUser.setBirthday(formatted_birthday);
                                GlobalVariable.getInstance().loggedInUser.setHeight(dlgHeightSlider.getThumb(0).getValue());
                                GlobalVariable.getInstance().loggedInUser.setAbout(bio);

                                loadData();
                            } else {
                                String error = result.get("error").getAsString();
                                if (error.equals("")) {
                                    Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                            progressDialog.dismiss();

                            Toast.makeText(getContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                            call.cancel();
                        }
                    });
                } catch (ParseException e) {
                    Toast.makeText(getContext(), "Please choose your birthday", Toast.LENGTH_SHORT).show();
                }
            });
            TextView cancelButton = dialog.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(view14 -> dialog.dismiss());

            final Calendar calendar = Calendar.getInstance();
            dlgBirthdayTextField.setOnClickListener(view13 -> new DatePickerDialog(mContext, (datePicker, year, month, day) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                String dateFormat = "dd MMM yyyy";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);

                dlgBirthdayTextField.setText(simpleDateFormat.format(calendar.getTime()));
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show());

            dlgHeightSlider.setMax(226, true);
            dlgHeightSlider.setMin(153, true);
            dlgHeightSlider.setOnThumbValueChangeListener((multiSlider, thumb, thumbIndex, value) -> {
                if (thumbIndex == 0) {
                    dlgHeightTextView.setText(GlobalFunction.getInstance().convertCmToAFootAndInches(value));
                }
            });
            dlgHeightSlider.getThumb(0).setValue(GlobalVariable.getInstance().loggedInUser.getHeight());

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            try {
                Date dtBirthday = simpleDateFormat.parse(GlobalVariable.getInstance().loggedInUser.getBirthday());
                simpleDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                dlgBirthdayTextField.setText(simpleDateFormat.format(dtBirthday));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            dlgNameTextField.setText(GlobalVariable.getInstance().loggedInUser.getFirst_name());
            dlgHeightTextView.setText(GlobalFunction.getInstance().convertCmToAFootAndInches(GlobalVariable.getInstance().loggedInUser.getHeight()));
            dlgAboutTextField.setText(GlobalVariable.getInstance().loggedInUser.getAbout());

            dialog.show();
        });
        maleButton.setOnClickListener(view18 -> setGender("M"));
        femaleButton.setOnClickListener(view110 -> setGender("F"));

        preferredMaleButton.setOnClickListener(view15 -> setPreferredGender("M"));
        preferredFemaleButton.setOnClickListener(view17 -> setPreferredGender("F"));

        updatePasswordButton.setOnClickListener(view16 -> {
            String current_password = currentPasswordTextField.getText().toString();
            String new_password = newPasswordTextField.getText().toString();
            String confirm_password = confirmPasswordTextField.getText().toString();

            if (current_password.equals("")) {
                Toast.makeText(getContext(), "Please enter your current password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (new_password.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!confirm_password.equals(new_password)) {
                Toast.makeText(getContext(), "New password and confirm password does not match", Toast.LENGTH_SHORT).show();
                return;
            }

            final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Updating...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody oldPasswordBody = RequestBody.create(MediaType.parse("multipart/form-data"), current_password);
            RequestBody newPasswordBody = RequestBody.create(MediaType.parse("multipart/form-data"), new_password);

            Call<JsonObject> call = apiInterface.change_password(userIdBody, oldPasswordBody, newPasswordBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        currentPasswordTextField.setText("");
                        newPasswordTextField.setText("");
                        confirmPasswordTextField.setText("");

                        Toast.makeText(getContext(), "Your password has been reset successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        String error = result.get("error").getAsString();
                        if (error.equals("")) {
                            Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    progressDialog.dismiss();

                    Toast.makeText(getContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                    call.cancel();
                }
            });
        });

        loadData();

        badgeTextView = view.findViewById(R.id.badgeTextView);
        badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
        badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);

        assert getContext() != null;
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(updateUnreadMessageBroadcastReceiver, new IntentFilter("UpdateUnreadCount"));

        return view;
    }

    private void savePreferredAge() {
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Saving...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "preferred_age_min,preferred_age_max");
        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(preferred_age_min) + ',' + preferred_age_max);

        Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();

                if (success == 1) {
                    GlobalVariable.getInstance().loggedInUser.setPreferred_age_min(preferred_age_min);
                    GlobalVariable.getInstance().loggedInUser.setPreferred_age_max(preferred_age_max);
                } else {
                    String error = result.get("error").getAsString();
                    if (error.equals("")) {
                        Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();

                Toast.makeText(getContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }

    private BroadcastReceiver updateUnreadMessageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
            badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(updateUnreadMessageBroadcastReceiver);
    }

    private void setGender(String gender) {
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Updating...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "gender");
        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), gender);

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
                    loadData();
                } else {
                    String error = result.get("error").getAsString();
                    if (error.equals("")) {
                        Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();

                Toast.makeText(getContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }

    private void setPreferredGender(String gender) {
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Updating...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody keysBody = RequestBody.create(MediaType.parse("multipart/form-data"), "preferred_gender");
        RequestBody valuesBody = RequestBody.create(MediaType.parse("multipart/form-data"), gender);

        Call<JsonObject> call = apiInterface.update_user(userIdBody, keysBody, valuesBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();

                if (success == 1) {
                    GlobalVariable.getInstance().loggedInUser.setPreferred_gender(gender);
                    loadData();
                } else {
                    String error = result.get("error").getAsString();
                    if (error.equals("")) {
                        Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();

                Toast.makeText(getContext(), "An unknown error occurred", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadData() {
        photosContainerLayout.removeAllViews();
        for (Photo photo:
                GlobalVariable.getInstance().loggedInUser.getPhotos()) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(GlobalFunction.getInstance().dpTopixel(mContext, GlobalFunction.getInstance().getScreenWidthInDPs(mContext)), GlobalFunction.getInstance().dpTopixel(mContext, 250)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(mContext)
                    .load(GlobalVariable.getInstance().SERVER_IMAGE_URL + photo.getPhoto())
                    .into(imageView);
            photosContainerLayout.addView(imageView);
        }

        nameTextView.setText(GlobalVariable.getInstance().loggedInUser.getFirst_name() + " " + GlobalFunction.getInstance().convertBirthdayToAge(GlobalVariable.getInstance().loggedInUser.getBirthday()));
        heightTextView.setText("HEIGHT " + GlobalFunction.getInstance().convertCmToAFootAndInches(GlobalVariable.getInstance().loggedInUser.getHeight()));
        aboutTextView.setText(GlobalVariable.getInstance().loggedInUser.getAbout());

        maleButton.setTextColor(Color.parseColor(GlobalVariable.getInstance().loggedInUser.getGender().equals("M") ? "#FD3F53" : "#999999"));
        femaleButton.setTextColor(Color.parseColor(GlobalVariable.getInstance().loggedInUser.getGender().equals("M") ? "#999999" : "#FD3F53"));

        preferredMaleButton.setTextColor(Color.parseColor(GlobalVariable.getInstance().loggedInUser.getPreferred_gender().equals("M") ? "#FD3F53" : "#999999"));
        preferredFemaleButton.setTextColor(Color.parseColor(GlobalVariable.getInstance().loggedInUser.getPreferred_gender().equals("M") ? "#999999" : "#FD3F53"));

        preferred_age_min = GlobalVariable.getInstance().loggedInUser.getPreferred_age_min();
        preferred_age_max = GlobalVariable.getInstance().loggedInUser.getPreferred_age_max();

        ageSlider.getThumb(0).setValue(preferred_age_min);
        ageSlider.getThumb(1).setValue(preferred_age_max);

        ageDescTextView.setText("BETWEEN " + preferred_age_min + " AND " + preferred_age_max);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        this.mContext = context;
        this.mActivity = (DrawerActivity)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
