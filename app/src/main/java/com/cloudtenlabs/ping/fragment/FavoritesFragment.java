package com.cloudtenlabs.ping.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.activity.MessageActivity;
import com.cloudtenlabs.ping.custom.TinderCard2;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.Conversation;
import com.cloudtenlabs.ping.object.Photo;
import com.cloudtenlabs.ping.object.User;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mindorks.placeholderview.SwipeDecor;
import com.mindorks.placeholderview.SwipePlaceHolderView;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.stfalcon.frescoimageviewer.ImageViewer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesFragment extends Fragment implements TinderCard2.CardCallback, View.OnClickListener {

    public BottomSheetDialog bsDialog;

    private SwipePlaceHolderView mSwipeView;
    private ScrollView mScrollView;
    private TextView emptyTextView;
    private LinearLayout informationLayout, cardViewContainerLayout;

    private TextView blockUserButton, heightTextView, aboutTextView, badgeTextView;

    private ArrayList<User> users = new ArrayList<>();
    private int selectedIndex = -1;

    private ImageView sendOneCheckbox, sendTwoCheckbox, sendThreeCheckbox, sendFourCheckbox;
    private ImageView replyOneCheckbox, replyTwoCheckbox, replyThreeCheckbox, replyFourCheckbox;
    private CardView downArrow;

    private int sendSelectedIndex = -1, replySelectedIndex = -1;
    private String[] sendMessages = {
            "Hi...",
            "Can I join you?",
            "Can I buy you a coffee/drinks?",
            "Can I message you?"
    };
    private String[] replyMessages = {
            "Hi...",
            "Sure!",
            "Yes, you can message me",
            "Thank you, but I can't right now."
    };

    private EditText messageTextField;

    private Context mContext;
    private DrawerActivity mActivity;

    public FavoritesFragment() {
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
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        mSwipeView = view.findViewById(R.id.swipeView);
        mSwipeView.getBuilder()
                .setSwipeType(SwipePlaceHolderView.SWIPE_TYPE_DEFAULT)
                .setDisplayViewCount(3)
                .setSwipeDecor(new SwipeDecor()
                        .setPaddingTop(20)
                        .setRelativeScale(0.01f));
//                        .setSwipeInMsgLayoutId(R.layout.tinder_swipe_in_msg_view)
//                        .setSwipeOutMsgLayoutId(R.layout.tinder_swipe_out_msg_view));

        cardViewContainerLayout = view.findViewById(R.id.cardViewContainerLayout);
        mScrollView = view.findViewById(R.id.scrollView);
        mScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardViewContainerLayout.getLayoutParams();
            params.setMargins(0, -mScrollView.getScrollY(), 0, 0);
            cardViewContainerLayout.setLayoutParams(params);
            downArrow.setVisibility(mScrollView.getScrollY() == 0 ? View.VISIBLE : View.GONE);
        });

        messageTextField = view.findViewById(R.id.messageTextField);
        ImageView messageSendButton = view.findViewById(R.id.messageSendButton);
        messageSendButton.setOnClickListener(this);

        informationLayout = view.findViewById(R.id.informationLayout);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view1 -> mActivity.toggleDrawer());

        TextView chatButton = view.findViewById(R.id.chatButton);
        chatButton.setOnClickListener(view12 -> {
            final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Loading...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
            RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(users.get(selectedIndex).getId()));
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
                                progressDialog.dismiss();
                                MessageActivity.open(mContext, dialog, user);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                progressDialog.dismiss();
                                Toast.makeText(mContext, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
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
                    Toast.makeText(getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        informationLayout.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        blockUserButton = view.findViewById(R.id.blockUserButton);
        aboutTextView = view.findViewById(R.id.aboutTextView);
        heightTextView = view.findViewById(R.id.heightTextView);

        loadFavoriteUsers();

        badgeTextView = view.findViewById(R.id.badgeTextView);
        badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
        badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(updateUnreadMessageBroadcastReceiver, new IntentFilter("UpdateUnreadCount"));

        sendOneCheckbox = view.findViewById(R.id.sendOneCheckbox);
        sendTwoCheckbox = view.findViewById(R.id.sendTwoCheckbox);
        sendThreeCheckbox = view.findViewById(R.id.sendThreeCheckbox);
        sendFourCheckbox = view.findViewById(R.id.sendFourCheckbox);

        replyOneCheckbox = view.findViewById(R.id.replyOneCheckbox);
        replyTwoCheckbox = view.findViewById(R.id.replyTwoCheckbox);
        replyThreeCheckbox = view.findViewById(R.id.replyThreeCheckbox);
        replyFourCheckbox = view.findViewById(R.id.replyFourCheckbox);

        sendOneCheckbox.setOnClickListener(this);
        sendTwoCheckbox.setOnClickListener(this);
        sendThreeCheckbox.setOnClickListener(this);
        sendFourCheckbox.setOnClickListener(this);
        replyOneCheckbox.setOnClickListener(this);
        replyTwoCheckbox.setOnClickListener(this);
        replyThreeCheckbox.setOnClickListener(this);
        replyFourCheckbox.setOnClickListener(this);

        CardView sendButton = view.findViewById(R.id.sendButton);
        CardView replyButton = view.findViewById(R.id.replyButton);
        downArrow = view.findViewById(R.id.downArrow);

        sendButton.setOnClickListener(this);
        replyButton.setOnClickListener(this);

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

    private void loadFavoriteUsers() {
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));

        Call<JsonObject> call = apiInterface.load_favorite_users(userIdBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    users.clear();
                    selectedIndex = -1;
                    for (JsonElement jsonObject :
                            result.getAsJsonArray("users")) {
                        Gson gson = new Gson();
                        users.add(gson.fromJson(jsonObject.getAsJsonObject(), User.class));
                    }
                    emptyTextView.setVisibility(users.size() == 0 ? View.VISIBLE : View.GONE);
                    downArrow.setVisibility((mScrollView.getScrollY() == 0 && users.size() > 0) ? View.VISIBLE : View.GONE);
                    loadSwipeView();
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

    private void loadSwipeView() {
        mSwipeView.removeAllViews();
        if (users.size() > 0) {
            setInformation(users.get(0));
            selectedIndex = 0;
            informationLayout.setVisibility(View.VISIBLE);
        } else {
            informationLayout.setVisibility(View.GONE);
            selectedIndex = -1;
        }
        for (User user :
                users) {
            mSwipeView.addView(new TinderCard2(this, user, users.indexOf(user), getContext()));
        }
    }

    @SuppressLint("SetTextI18n")
    private void setInformation(User user) {
        heightTextView.setText("HEIGHT " + GlobalFunction.getInstance().convertCmToAFootAndInches(user.getHeight()));
        aboutTextView.setText(user.getAbout());
        blockUserButton.setText(user.getIs_blocked().equals("1") ? "UNBLOCK USER" : "Block/Report");
        blockUserButton.setOnClickListener(view -> {
            if (user.getIs_blocked().equals("1")) {
                AlertDialog.Builder alertDialg = new AlertDialog.Builder(mContext);
                alertDialg.setMessage("Are you sure you want to unblock this user?");
                alertDialg.setTitle("Confirmation");
                alertDialg.setPositiveButton("Block", (dialogInterface, i) -> {
                    blockUser(user);
                });
                alertDialg.setNegativeButton("Cancel", null);
                alertDialg.setCancelable(true);
                alertDialg.create().show();
            } else {
                openDialog(user);
            }
        });
    }
    private void openDialog(User user) {
        View view = getLayoutInflater().inflate(R.layout.sheet_main, null);

        bsDialog = new BottomSheetDialog(getContext());
        bsDialog.setContentView(view);
        TextView block_sel = (TextView) view.findViewById(R.id.block);
        TextView report_sel = (TextView) view.findViewById(R.id.report);
        TextView cancel_sel = (TextView) view.findViewById(R.id.sheet_cancel);
        cancel_sel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsDialog.dismiss();
            }
        });
        block_sel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockUser(user);
                bsDialog.dismiss();
            }
        });
        report_sel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsDialog.dismiss();
                AlertDialog.Builder alertDialg = new AlertDialog.Builder(getContext());
                alertDialg.setMessage("You've successfully reported this user.");
                alertDialg.setTitle("Report");
                alertDialg.setNegativeButton("Ok", null);
                alertDialg.setCancelable(true);
                alertDialg.create().show();
            }
        });
        bsDialog.show();
    }
    public void blockUser(User user){
        final String is_block = user.getIs_blocked().equals("1") ? "0" : "1";

        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(user.getId()));
        RequestBody isBlockeBody = RequestBody.create(MediaType.parse("multipart/form-data"), is_block);

        Call<JsonObject> call = apiInterface.block_user(userIdFromBody, userIdToBody, isBlockeBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    blockUserButton.setText(is_block.equals("1") ? "UNBLOCK USER" : "Block/Report");

                    int index = users.indexOf(user);
                    user.setIs_blocked(is_block);
                    users.set(index, user);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
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

    @Override
    public void onSwiping() {

    }

    @Override
    public void onSwipingEnd(int index) {

    }

    @Override
    public void onSwipedRight(int index) {
        if (index == users.size() - 1) {
            informationLayout.setVisibility(View.GONE);
            selectedIndex = -1;
            final Handler handler = new Handler();
            handler.postDelayed(this::loadSwipeView, 300);
        } else {
            setInformation(users.get(index + 1));
            selectedIndex = index + 1;
        }
    }

    @Override
    public void onSwipedLeft(int index) {
//        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
//
//        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
//        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(users.get(index).getId()));
//        RequestBody isLikeBody = RequestBody.create(MediaType.parse("multipart/form-data"), "0");
//
//        Call<JsonObject> call = apiInterface.swipe_user(userIdFromBody, userIdToBody, isLikeBody);
//        call.enqueue(new Callback<JsonObject>() {
//            @Override
//            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
//            }
//        });

        if (index == users.size() - 1) {
            informationLayout.setVisibility(View.GONE);
            selectedIndex = -1;
            final Handler handler = new Handler();
            handler.postDelayed(this::loadSwipeView, 300);
        } else {
            setInformation(users.get(index + 1));
            selectedIndex = index + 1;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onCardTapped(int index, TinderCard2 card) {
        List<String> urls = new ArrayList<>();
        for (Photo photo:
                card.user.getPhotos()) {
            urls.add(GlobalVariable.getInstance().SERVER_IMAGE_URL + photo.getPhoto());
        }

        TextView tv = new TextView(getContext());
        tv.setText("1/" + urls.size());
        tv.setTextColor(Color.parseColor("#ffffff"));
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(0 ,50, 0, 0);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTextSize(16);

        new ImageViewer.Builder<>(mContext, urls).setStartPosition(0).setImageChangeListener(position -> tv.setText((position + 1) + "/" + urls.size())).setOverlayView(tv).setFormatter((ImageViewer.Formatter<String>) url -> url).show();
    }

    @Override
    public void onFavoriteTapped(int index, TinderCard2 card) {
        final String is_favorite = users.get(index).getIs_favorited().equals("1") ? "0" : "1";
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(users.get(index).getId()));
        RequestBody isFavoriteBody = RequestBody.create(MediaType.parse("multipart/form-data"), is_favorite);

        Call<JsonObject> call = apiInterface.favorite_user(userIdFromBody, userIdToBody, isFavoriteBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();

                JsonObject result = response.body();
                assert result != null;
                int success = result.get("success").getAsInt();
                if (success == 1) {
                    card.favoriteButton.setImageResource(is_favorite.equals("1") ? R.drawable.ic_favorite_selected : R.drawable.ic_favorite_normal);

                    User user = users.get(index);
                    user.setIs_favorited(is_favorite);
                    users.set(index, user);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sendOneCheckbox || view.getId() == R.id.sendTwoCheckbox || view.getId() == R.id.sendThreeCheckbox || view.getId() == R.id.sendFourCheckbox) {
            sendOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            switch(view.getId()) {
                case R.id.sendOneCheckbox:
                    sendSelectedIndex = 0;
                    sendOneCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.sendTwoCheckbox:
                    sendSelectedIndex = 1;
                    sendTwoCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.sendThreeCheckbox:
                    sendSelectedIndex = 2;
                    sendThreeCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                case R.id.sendFourCheckbox:
                    sendSelectedIndex = 3;
                    sendFourCheckbox.setImageResource(R.drawable.ic_radio_black_selected);
                    break;
                default:
                    break;
            }
        }

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

        if (view.getId() == R.id.sendButton) {
            if (sendSelectedIndex == -1) {
                Toast.makeText(getContext(), "Please select a message to send.", Toast.LENGTH_SHORT).show();
                return;
            }
            String message = sendMessages[sendSelectedIndex];

            sendOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            sendSelectedIndex = -1;

            sendMessage(message);
        }

        if (view.getId() == R.id.replyButton) {
            if (replySelectedIndex == -1) {
                Toast.makeText(getContext(), "Please select a message to reply.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Please input a message to send.", Toast.LENGTH_SHORT).show();
                return;
            }

            sendOneCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendTwoCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendThreeCheckbox.setImageResource(R.drawable.ic_radio_black_normal);
            sendFourCheckbox.setImageResource(R.drawable.ic_radio_black_normal);

            sendSelectedIndex = -1;

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
        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(users.get(selectedIndex).getId()));
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
                                    Toast.makeText(getContext(), "Message sent successfully.", Toast.LENGTH_SHORT).show();

                                    GlobalFunction.getInstance().sendPushNotification(user, message);
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(mContext, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            progressDialog.dismiss();
                            Toast.makeText(mContext, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    String error = result.get("error").getAsString();
                    Toast.makeText(mContext,  error.equals("") ? "An unknown error occurred." : error, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(mContext, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
