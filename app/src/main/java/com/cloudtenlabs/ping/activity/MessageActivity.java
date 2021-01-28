package com.cloudtenlabs.ping.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.chat.DemoMessagesActivity;
import com.cloudtenlabs.ping.chat.model.Message;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalFunction;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends DemoMessagesActivity
        implements MessagesListAdapter.OnMessageLongClickListener<Message>,
        MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessagesListAdapter.OnLoadMoreListener, QBChatDialogMessageListener {

    private QBChatDialog dialog;
    public User opponent;
    public BottomSheetDialog bsDialog;

    private TextView blockUserButton;
    private Context context;

    private MessageInput messageInput;
    private MessagesList messagesList;

    public static void open(Context context, QBChatDialog dialog, User user) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra("user", user);
        intent.putExtra("dialog", dialog);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        context = this;

        this.opponent = (User)getIntent().getSerializableExtra("user");
        this.dialog = (QBChatDialog) getIntent().getSerializableExtra("dialog");

        dialog.initForChat(QBChatService.getInstance());
        dialog.addMessageListener(this);

        messagesList = findViewById(R.id.messagesList);
        initAdapter();

        messageInput = findViewById(R.id.input);
        messageInput.setInputListener(this);
        messageInput.setAttachmentsListener(this);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            finish();
        });

        CircleImageView userPhotoImageView = findViewById(R.id.userPhotoImageView);
        Glide.with(context).load(GlobalVariable.getInstance().SERVER_IMAGE_URL + opponent.getPhoto()).into(userPhotoImageView);
        userPhotoImageView.setOnClickListener(view -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("user", opponent);
            startActivity(intent);
        });

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        usernameTextView.setText(opponent.getFirst_name());

        blockUserButton = findViewById(R.id.blockUserButton);
        blockUserButton.setText(opponent.getIs_blocked().equals("1") ? "UNBLOCK USER" : "Block/Report");
        blockUserButton.setOnClickListener(v -> {
            if (opponent.getIs_blocked().equals("1")) {
                AlertDialog.Builder alertDialg = new AlertDialog.Builder(context);
                alertDialg.setMessage("Are you sure you want to unblock this user?");
                alertDialg.setTitle("Confirmation");
                alertDialg.setPositiveButton("Unblock", (dialogInterface, i) -> {
                    this.blockUser();
                });
                alertDialg.setNegativeButton("Cancel", null);
                alertDialg.setCancelable(true);
                alertDialg.create().show();
            } else {
                this.openDialog();
            }
        });

        ChatHelper.getInstance().currentDialogID = dialog.getDialogId();
    }
    private void openDialog() {
        View view = getLayoutInflater().inflate(R.layout.sheet_main, null);
        bsDialog = new BottomSheetDialog(this);
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
//                takePhotoFromCamera();
                blockUser();
                bsDialog.dismiss();
            }
        });
        report_sel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                takePhotoFromGallery();
                bsDialog.dismiss();
                AlertDialog.Builder alertDialg = new AlertDialog.Builder(context);
                alertDialg.setMessage("You've successfully reported this user.");
                alertDialg.setTitle("Report");
                alertDialg.setNegativeButton("Ok", null);
                alertDialg.setCancelable(true);
                alertDialg.create().show();
            }
        });
        bsDialog.show();
    }
    void blockUser() {
        final String is_block = opponent.getIs_blocked().equals("1") ? "0" : "1";

        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Loading...", true, false);
        APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);

        RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
        RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(opponent.getId()));
        RequestBody isBlockedBody = RequestBody.create(MediaType.parse("multipart/form-data"), is_block);

        Call<JsonObject> call = apiInterface.block_user(userIdFromBody, userIdToBody, isBlockedBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressDialog.dismiss();
                opponent.setIs_blocked(is_block);
                blockUserButton.setText(opponent.getIs_blocked().equals("1") ? "UNBLOCK USER" : "Block/Report");
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dialog.removeMessageListrener(this);

        ChatHelper.getInstance().currentDialogID = "";
        ChatHelper.getInstance().loadDialogs(context);

        //Mark all messages read
//        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
//        messageGetBuilder.setLimit(messagesAdapter.getMessagesCount());
//        messageGetBuilder.setSkip(0);
//        QBRestChatService.getDialogMessages(dialog, messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
//            @Override
//            public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
//
//            }
//
//            @Override
//            public void onError(QBResponseException e) {
//
//            }
//        });
    }

    @Override
    public boolean onSubmit(CharSequence input) {
        messageInput.getButton().setClickable(false);

        QBChatMessage qbChatMessage = new QBChatMessage();
        qbChatMessage.setBody(input.toString());
        qbChatMessage.setSaveToHistory(true);
        qbChatMessage.setDateSent((new Date()).getTime() / 1000);

        dialog.sendMessage(qbChatMessage, new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                messageInput.getButton().setClickable(true);

                com.cloudtenlabs.ping.chat.model.User sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()), String.valueOf(GlobalVariable.getInstance().loggedInUser.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + GlobalVariable.getInstance().loggedInUser.getPhoto(), true);
                Message message = new Message(qbChatMessage.getId(), sender, qbChatMessage.getBody());

                messagesAdapter.addToStart(message, true);

                GlobalFunction.getInstance().sendPushNotification(opponent, input.toString());
            }

            @Override
            public void onError(QBResponseException e) {
                messageInput.getButton().setClickable(true);
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    @Override
    public void onAddAttachments() {
//        messagesAdapter.addToStart(MessagesFixtures.getImageMessage(), true);
    }

    @Override
    public void onMessageLongClick(Message message) {
        if (Integer.parseInt(message.getUser().getId()) == GlobalVariable.getInstance().loggedInUser.getId()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setMessage("Are you sure you want to remove this message?");
            alertDialog.setTitle("Remove Message");
            alertDialog.setPositiveButton("Remove", (dialogInterface, i) -> QBRestChatService.deleteMessage(message.getId(), false).performAsync(new QBEntityCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid, Bundle bundle) {
                    messagesAdapter.delete(message);
                }

                @Override
                public void onError(QBResponseException e) {
                    Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                }
            }));
            alertDialog.setNegativeButton("Cancel", null);
            alertDialog.setCancelable(true);
            alertDialog.create().show();
        }
    }

    private void initAdapter() {
        MessageHolders holdersConfig = new MessageHolders()
                .setIncomingTextLayout(R.layout.item_custom_incoming_text_message)
                .setOutcomingTextLayout(R.layout.item_custom_outcoming_text_message)
                .setIncomingImageLayout(R.layout.item_custom_incoming_image_message)
                .setOutcomingImageLayout(R.layout.item_custom_outcoming_image_message);

        super.messagesAdapter = new MessagesListAdapter<>(super.senderId, holdersConfig, super.imageLoader);
        super.messagesAdapter.setOnMessageLongClickListener(this);
        super.messagesAdapter.setLoadMoreListener(this);
        messagesList.setAdapter(super.messagesAdapter);
    }

    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        super.onLoadMore(page, totalItemsCount);

        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(10);
        messageGetBuilder.setSkip(messagesAdapter.getMessagesCount());
        messageGetBuilder.sortDesc("date_sent");

        QBRestChatService.getDialogMessages(dialog, messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
            @Override
            public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                ArrayList<Message> messages = new ArrayList<>();
                for (int i = 0; i < qbChatMessages.size(); i++) {
                    QBChatMessage qbChatMessage = qbChatMessages.get(i);
                    com.cloudtenlabs.ping.chat.model.User sender;
                    if (qbChatMessage.getSenderId().equals(opponent.getQb_id())) {
                        sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(opponent.getId()), String.valueOf(opponent.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + opponent.getPhoto(), true);
                    } else {
                        sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()), String.valueOf(GlobalVariable.getInstance().loggedInUser.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + GlobalVariable.getInstance().loggedInUser.getPhoto(), true);
                    }
                    Message message = new Message(qbChatMessage.getId(), sender, qbChatMessage.getBody());

                    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                    calendar.setTimeInMillis(dialog.getLastMessageDateSent() * 1000);
                    message.setCreatedAt(calendar.getTime());

                    messages.add(message);
                }
                messagesAdapter.addToEnd(messages, false);
            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void loadMessages() {
        super.loadMessages();

        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(10);
        messageGetBuilder.sortDesc("date_sent");

        QBRestChatService.getDialogMessages(dialog, messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
            @Override
            public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                messagesAdapter.clear();
                ArrayList<Message> messages = new ArrayList<>();
                for (int i = 0; i < qbChatMessages.size(); i++) {
                    QBChatMessage qbChatMessage = qbChatMessages.get(i);
                    com.cloudtenlabs.ping.chat.model.User sender;
                    if (qbChatMessage.getSenderId().equals(opponent.getQb_id())) {
                        sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(opponent.getId()), String.valueOf(opponent.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + opponent.getPhoto(), true);
                    } else {
                        sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()), String.valueOf(GlobalVariable.getInstance().loggedInUser.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + GlobalVariable.getInstance().loggedInUser.getPhoto(), true);
                    }
                    Message message = new Message(qbChatMessage.getId(), sender, qbChatMessage.getBody());

                    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                    calendar.setTimeInMillis(dialog.getLastMessageDateSent() * 1000);
                    message.setCreatedAt(calendar.getTime());

                    messages.add(message);
                }
                messagesAdapter.addToEnd(messages, false);
            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        com.cloudtenlabs.ping.chat.model.User sender = new com.cloudtenlabs.ping.chat.model.User(String.valueOf(opponent.getId()), String.valueOf(opponent.getFirst_name()), GlobalVariable.getInstance().SERVER_IMAGE_URL + opponent.getPhoto(), true);
        Message message = new Message(qbChatMessage.getId(), sender, qbChatMessage.getBody());

        messagesAdapter.addToStart(message, true);

        dialog.readMessage(qbChatMessage, new QBEntityCallback() {
            @Override
            public void onSuccess(Object o, Bundle bundle) {

            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

    }

}
