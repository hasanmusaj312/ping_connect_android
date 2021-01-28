package com.cloudtenlabs.ping.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactUsFragment extends Fragment {

    private EditText subjectTextField, descriptionTextField;
    private TextView badgeTextView;
    private DrawerActivity mActivity;
    private Context mContext;

    public ContactUsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contact_us, container, false);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view1 -> mActivity.toggleDrawer());

        subjectTextField = view.findViewById(R.id.subjectTextField);
        descriptionTextField = view.findViewById(R.id.descriptionTextField);

        TextView emailButton = view.findViewById(R.id.emailButton);
        emailButton.setOnClickListener(view12 -> {
            String subject = subjectTextField.getText().toString();
            String description = descriptionTextField.getText().toString();

            if (subject.equals("")) {
                Toast.makeText(getContext(), "Please enter an subject", Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.equals("")) {
                Toast.makeText(getContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
                return;
            }

            final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Sending...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

            RequestBody emailFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), GlobalVariable.getInstance().loggedInUser.getEmail());
            RequestBody emailToBody = RequestBody.create(MediaType.parse("multipart/form-data"), "pingconnect@gmail.com");
            RequestBody subjectBody = RequestBody.create(MediaType.parse("multipart/form-data"), subject);
            RequestBody descriptionBody = RequestBody.create(MediaType.parse("multipart/form-data"), description);

            Call<JsonObject> call = apiInterface.contact_us(emailFromBody, emailToBody, subjectBody, descriptionBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();

                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();

                    if (success == 1) {
                        subjectTextField.setText("");
                        descriptionTextField.setText("");

                        Toast.makeText(getContext(), "We appreciate you contacting us. We will get back to you shortly.", Toast.LENGTH_SHORT).show();
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

        badgeTextView = view.findViewById(R.id.badgeTextView);
        badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
        badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(updateUnreadMessageBroadcastReceiver, new IntentFilter("UpdateUnreadCount"));

        return view;
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
