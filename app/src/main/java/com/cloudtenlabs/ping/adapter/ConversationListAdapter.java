package com.cloudtenlabs.ping.adapter;

import android.app.ProgressDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choota.dev.ctimeago.TimeAgo;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.MessageActivity;
import com.cloudtenlabs.ping.fragment.MessagesFragment;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.Conversation;
import com.cloudtenlabs.ping.object.User;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<QBChatDialog> dialogs;
    private MessagesFragment fragment;
    private boolean isEditMode;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView usernameTextView, messageTextView, dateTextView, badgeTextView;
        CircleImageView userPhotoImageView;
        ImageView selectImageView;

        ItemViewHolder(View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            userPhotoImageView = itemView.findViewById(R.id.userPhotoImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            badgeTextView = itemView.findViewById(R.id.badgeTextView);
            selectImageView = itemView.findViewById(R.id.selectImageView);
        }
    }

    public ConversationListAdapter(List<QBChatDialog> dialogs, MessagesFragment fragment, boolean isEditMode){
        this.dialogs = dialogs;
        this.fragment = fragment;
        this.isEditMode = isEditMode;
    }

    @Override
    public int getItemCount() {
        return dialogs.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_conversation, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {
        ConversationListAdapter.ItemViewHolder itemViewHolder = (ConversationListAdapter.ItemViewHolder) viewHolder;
        QBChatDialog dialog = dialogs.get(i);

        itemViewHolder.selectImageView.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        itemViewHolder.selectImageView.setOnClickListener(view -> {
            fragment.addRemoveIndex(i);
            itemViewHolder.selectImageView.setImageResource(fragment.removeIndexes.contains(i) ? R.drawable.ic_radio_black_selected : R.drawable.ic_radio_black_normal);
        });

        int tmp1 = dialog.getOccupants().get(0).equals(GlobalVariable.getInstance().loggedInUser.getQb_id()) ? 1 : 0;
        int tmp2 = dialog.getOccupants().get( tmp1 );
        int tmp3 = ChatHelper.getInstance().userIndex( tmp2);
        if(tmp3 == -1) {
            itemViewHolder.messageTextView.setVisibility(View.GONE);
            itemViewHolder.dateTextView.setVisibility(View.GONE);
            itemViewHolder.badgeTextView.setVisibility(View.GONE);
            return;
        }
        QBUser opponent = ChatHelper.getInstance().dialogsUsers.get(tmp3);

        Glide.with(fragment.getView()).load(GlobalVariable.getInstance().SERVER_IMAGE_URL + opponent.getCustomData()).into(itemViewHolder.userPhotoImageView);
        itemViewHolder.usernameTextView.setText(opponent.getFullName());
        if (dialog.getLastMessage() == null || dialog.getLastMessage().isEmpty()) {
            itemViewHolder.messageTextView.setVisibility(View.GONE);
            itemViewHolder.dateTextView.setVisibility(View.GONE);
            itemViewHolder.badgeTextView.setVisibility(View.GONE);
        } else {
            itemViewHolder.messageTextView.setVisibility(View.VISIBLE);
            itemViewHolder.dateTextView.setVisibility(View.VISIBLE);

            String content = dialog.getLastMessageUserId().equals(GlobalVariable.getInstance().loggedInUser.getQb_id()) ? "You" : opponent.getFullName().substring(0, 1).toUpperCase() + opponent.getFullName().substring(1);
            content = content + ": " + dialog.getLastMessage();

            itemViewHolder.messageTextView.setText(content);

            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
            calendar.setTimeInMillis(dialog.getLastMessageDateSent() * 1000);

            TimeAgo timeAgo = new TimeAgo().locale(fragment.getContext());
            itemViewHolder.dateTextView.setText(timeAgo.getTimeAgo(calendar.getTime()));

            itemViewHolder.badgeTextView.setVisibility(dialog.getUnreadMessageCount() == 0 ? View.GONE : View.VISIBLE);
            itemViewHolder.badgeTextView.setText(String.valueOf(dialog.getUnreadMessageCount()));
        }

        itemViewHolder.cardView.setOnClickListener(v -> {
            final ProgressDialog progressDialog = ProgressDialog.show(fragment.getContext(), "", "Loading...", true, false);
            APIInterface apiInterface = APIClient.getPHPClient().create(APIInterface.class);
            RequestBody userIdFromBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(GlobalVariable.getInstance().loggedInUser.getId()));
            RequestBody userIdToBody = RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(opponent.getLogin()));
            Call<JsonObject> call = apiInterface.load_user(userIdFromBody, userIdToBody);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressDialog.dismiss();
                    JsonObject result = response.body();
                    assert result != null;
                    int success = result.get("success").getAsInt();
                    if (success == 1) {
                        Gson gson = new Gson();
                        User user = gson.fromJson(result.get("user").getAsJsonObject(), User.class);
                        MessageActivity.open(fragment.getContext(), dialog, user);
                    } else {
                        String error = result.get("error").getAsString();
                        if (error.equals("")) {
                            Toast.makeText(fragment.getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(fragment.getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(fragment.getContext(), "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

}