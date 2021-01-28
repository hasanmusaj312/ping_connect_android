package com.cloudtenlabs.ping.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.fragment.AllUsersFragment;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<User> users;
    private AllUsersFragment fragment;
    private boolean showLoadMore;

    private final int VIEW_ITEM = 0;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView usernameTextView;
        CircleImageView userPhotoImageView;

        ItemViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            userPhotoImageView = itemView.findViewById(R.id.userPhotoImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {

        ProgressBar progressBar;

        FooterViewHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    public AllUsersListAdapter(List<User> users, AllUsersFragment fragment, boolean showLoadMore){
        this.users = users;
        this.fragment = fragment;
        this.showLoadMore = showLoadMore;
    }

    @Override
    public int getItemCount() {
        return showLoadMore ? users.size() + 1 : users.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == users.size()) {
            return 1;
        } else {
            return VIEW_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_all_users, viewGroup, false);
            return new ItemViewHolder(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_load_more, viewGroup, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof ItemViewHolder) {
            AllUsersListAdapter.ItemViewHolder itemViewHolder = (AllUsersListAdapter.ItemViewHolder) viewHolder;
            User user = users.get(i);

            Glide.with(fragment.getView()).load(GlobalVariable.getInstance().SERVER_IMAGE_URL + user.getPhoto()).into(itemViewHolder.userPhotoImageView);
            itemViewHolder.usernameTextView.setText(user.getFirst_name());
        } else {
            AllUsersListAdapter.FooterViewHolder footerViewHolder = (AllUsersListAdapter.FooterViewHolder) viewHolder;
            footerViewHolder.itemView.setOnClickListener(view -> {
                footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                fragment.loadUsers(users.size(), fragment.loadCount, false);
            });
            footerViewHolder.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

}