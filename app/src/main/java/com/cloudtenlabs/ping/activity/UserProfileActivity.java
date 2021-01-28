package com.cloudtenlabs.ping.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.Conversation;
import com.cloudtenlabs.ping.object.Photo;
import com.cloudtenlabs.ping.object.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;

import java.util.ArrayList;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout photosContainerLayout;
    private TextView nameTextView, heightTextView, aboutTextView;
    private ImageView replyOneCheckbox, replyTwoCheckbox, replyThreeCheckbox, replyFourCheckbox;
    private EditText messageTextField;

    private Context context;
    private User user;

    private int replySelectedIndex = -1;
    private String[] replyMessages = {
            "Hi...",
            "Sure!",
            "Yes, you can message me",
            "Thank you, but I can't right now."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        context = this;

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

        user = (User)getIntent().getSerializableExtra("user");

        photosContainerLayout = findViewById(R.id.photosContainerLayout);
        nameTextView = findViewById(R.id.nameTextView);
        heightTextView = findViewById(R.id.heightTextView);
        aboutTextView = findViewById(R.id.aboutTextView);

        messageTextField = findViewById(R.id.messageTextField);
        ImageView messageSendButton = findViewById(R.id.messageSendButton);
        messageSendButton.setOnClickListener(this);

        replyOneCheckbox = findViewById(R.id.replyOneCheckbox);
        replyTwoCheckbox = findViewById(R.id.replyTwoCheckbox);
        replyThreeCheckbox = findViewById(R.id.replyThreeCheckbox);
        replyFourCheckbox = findViewById(R.id.replyFourCheckbox);

        replyOneCheckbox.setOnClickListener(this);
        replyTwoCheckbox.setOnClickListener(this);
        replyThreeCheckbox.setOnClickListener(this);
        replyFourCheckbox.setOnClickListener(this);

        CardView replyButton = findViewById(R.id.replyButton);

        replyButton.setOnClickListener(this);

        loadData();
    }

    @SuppressLint("DefaultLocale")
    private void loadData() {
        photosContainerLayout.removeAllViews();
        for (Photo photo:
                user.getPhotos()) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(GlobalFunction.getInstance().dpTopixel(context, GlobalFunction.getInstance().getScreenWidthInDPs(context)), GlobalFunction.getInstance().dpTopixel(context, 250)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//            Picasso.get().load(GlobalVariable.getInstance().SERVER_IMAGE_URL + photo.getPhoto()).into(imageView);
            Glide.with(this)
                    .load(GlobalVariable.getInstance().SERVER_IMAGE_URL + photo.getPhoto())
//                    .override(GlobalFunction.getInstance().getScreenWidthInDPs(getContext()), GlobalFunction.getInstance().dpTopixel(getContext(), 250))
                    .into(imageView);
            photosContainerLayout.addView(imageView);
        }

        nameTextView.setText(String.format("%s %d", user.getFirst_name(), GlobalFunction.getInstance().convertBirthdayToAge(user.getBirthday())));
        heightTextView.setText(String.format("HEIGHT %s", GlobalFunction.getInstance().convertCmToAFootAndInches(user.getHeight())));
        aboutTextView.setText(user.getAbout());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.replyOneCheckbox || view.getId() == R.id.replyTwoCheckbox || view.getId() == R.id.replyThreeCheckbox || view.getId() == R.id.replyFourCheckbox) {
            replyOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            switch(view.getId()) {
                case R.id.replyOneCheckbox:
                    replySelectedIndex = 0;
                    replyOneCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.replyTwoCheckbox:
                    replySelectedIndex = 1;
                    replyTwoCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.replyThreeCheckbox:
                    replySelectedIndex = 2;
                    replyThreeCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.replyFourCheckbox:
                    replySelectedIndex = 3;
                    replyFourCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                default:
                    break;
            }
        }

        if (view.getId() == R.id.replyButton) {
            if (replySelectedIndex == -1) {
                Toast.makeText(context, "Please select a message to reply.", Toast.LENGTH_SHORT).show();
                return;
            }
            String message = replyMessages[replySelectedIndex];

            replyOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            replySelectedIndex = -1;

            sendMessage(message);
        }

        if (view.getId() == R.id.messageSendButton) {
            String message = messageTextField.getText().toString();
            if (message.isEmpty()) {
                Toast.makeText(context, "Please input a message to send.", Toast.LENGTH_SHORT).show();
                return;
            }

            replyOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            replyFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            replySelectedIndex = -1;

            messageTextField.setText("");
            sendMessage(message);
        }
    }

    private void sendMessage(String message) {
        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(user.getId()));
        Call<JsonObject> call = apiInterface.load_user(userIdFromBody, userIdToBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    Gson gson = new Gson();
                    User user = gson.fromJson(result.get("user").getAsJsonObject(), User.class);

                    ArrayList<Integer> occupantIdsList = new ArrayList<>();
                    occupantIdsList.add(user.getQb_id());

                    QBChatDialog dialog = new QBChatDialog();
                    dialog.setType(QBDialogType.PRIVATE);
                    dialog.setOccupantsIds(occupantIdsList);

                    QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                        @Override
                        public void onSuccess(QBChatDialog dialog, Bundle bundle) {
                            QBChatMessage qbChatMessage = new QBChatMessage();
                            qbChatMessage.setBody(message);
                            qbChatMessage.setSaveToHistory(true);
                            qbChatMessage.setDateSent((new Date()).getTime() / 1000);

                            dialog.sendMessage(qbChatMessage, new QBEntityCallback<Void>() {
                                @Override
                                public void onSuccess(Void aVoid, Bundle bundle) {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Message sent successfully.", Toast.LENGTH_SHORT).show();

                                    GlobalFunction.getInstance().sendPushNotification(user, message);
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
