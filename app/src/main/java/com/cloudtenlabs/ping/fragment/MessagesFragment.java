package com.cloudtenlabs.ping.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cloudtenlabs.ping.App;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.activity.signin.LoginActivity;
import com.cloudtenlabs.ping.adapter.ConversationListAdapter;
import com.cloudtenlabs.ping.global.APIClient;
import com.cloudtenlabs.ping.global.APIInterface;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.object.Conversation;
import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.facebook.login.LoginManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesFragment extends Fragment {

    private RecyclerView recyclerView;

    private TextView badgeTextView;
    private ImageView removeButton, editButton, confirmButton;

    public ArrayList<Integer> removeIndexes = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    private Context mContext;
    private DrawerActivity mActivity;

    public MessagesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void addRemoveIndex(Integer removeIndex) {
        if (removeIndexes.contains(removeIndex)) {
            removeIndexes.remove(removeIndex);
        } else {
            removeIndexes.add(removeIndex);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::reloadDialogs);

        removeButton = view.findViewById(R.id.removeButton);
        removeButton.setOnClickListener(view13 -> {
            if (removeIndexes.size() > 0) {
                StringifyArrayList<String> dialogIDs = new StringifyArrayList<>();
                for (Integer removeIndex:
                        removeIndexes) {
                    dialogIDs.add(String.valueOf(ChatHelper.getInstance().dialogs.get(removeIndex).getDialogId()));
                }
                AlertDialog.Builder alertDialg = new AlertDialog.Builder(mContext);
                alertDialg.setMessage("Are you sure you want to delete selected conversations?");
                alertDialg.setTitle("Delete Messages");
                alertDialg.setPositiveButton("Yes", (dialogInterface, i) -> {
                    final ProgressDialog progressDialog = ProgressDialog.show(getContext(), "", "Deleting...", true, false);
                    QBRestChatService.deleteDialogs(dialogIDs, false, null).performAsync(new QBEntityCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> strings, Bundle bundle) {
                            progressDialog.dismiss();
                            confirmButton.callOnClick();
                            reloadDialogs();
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "An unknown error occurred. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                alertDialg.setNegativeButton("No", null);
                alertDialg.setCancelable(true);
                alertDialg.create().show();
            }
        });
        editButton = view.findViewById(R.id.editButton);
        editButton.setOnClickListener(view1 -> {
            editButton.setVisibility(View.GONE);
            removeButton.setVisibility(View.VISIBLE);
            confirmButton.setVisibility(View.VISIBLE);
            refreshRecyclerView(true);
        });
        confirmButton = view.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(view12 -> {
            editButton.setVisibility(View.VISIBLE);
            removeButton.setVisibility(View.GONE);
            confirmButton.setVisibility(View.GONE);
            removeIndexes.clear();
            refreshRecyclerView(false);
        });

        badgeTextView = view.findViewById(R.id.badgeTextView);
        badgeTextView.setText(String.valueOf(ChatHelper.getInstance().totalUnreadMessageCount));
        badgeTextView.setVisibility(ChatHelper.getInstance().totalUnreadMessageCount == 0 ? View.GONE : View.VISIBLE);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        refreshRecyclerView(false);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view14 -> mActivity.toggleDrawer());

        CardView shareButton = view.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(view15 -> {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "ping connects is the new and exciting dating app. Download it today!\", URL(string: \"https://pingconnects.com/";
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        });

        CardView logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view16 -> {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("saved_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("email");
            editor.remove("password");
            editor.remove("facebook_id");
            editor.apply();

            LoginManager.getInstance().logOut();
            try {
                QBChatService.getInstance().logout();
                QBUsers.signOut();

                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                mActivity.finish();
            } catch (SmackException.NotConnectedException e) {
                QBUsers.signOut();

                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                mActivity.finish();
            }
        });

        LocalBroadcastManager.getInstance(mContext).registerReceiver(refreshChatBroadcastReceiver, new IntentFilter("UpdateDialogs"));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(updateUnreadMessageBroadcastReceiver, new IntentFilter("UpdateUnreadCount"));

        return view;
    }

    private BroadcastReceiver refreshChatBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshRecyclerView(false);
//            reloadDialogs();
        }
    };

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
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(refreshChatBroadcastReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(updateUnreadMessageBroadcastReceiver);
    }

    private void refreshRecyclerView(boolean isEditMode) {
        int i;
        ArrayList<QBChatDialog>  arrDlg = new ArrayList<>();
        for(i=0;i<ChatHelper.getInstance().dialogs.size();i++) {
            QBChatDialog dialog = ChatHelper.getInstance().dialogs.get(i);
            int tmp1 = dialog.getOccupants().get(0).equals(GlobalVariable.getInstance().loggedInUser.getQb_id()) ? 1 : 0;
            int tmp2 = dialog.getOccupants().get( tmp1 );
            int tmp3 = ChatHelper.getInstance().userIndex( tmp2);
            if(tmp3 != -1) {
                arrDlg.add(dialog);
            }
        }

        ConversationListAdapter adapter = new ConversationListAdapter(arrDlg/*ChatHelper.getInstance().dialogs*/, this, isEditMode);
        recyclerView.setAdapter(adapter);
    }

    private void reloadDialogs() {
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.setLimit(100);
        requestBuilder.setSkip(0);

        QBRestChatService.getChatDialogs(QBDialogType.PRIVATE, requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {
                ArrayList<Integer> usersIDs = new ArrayList<>();
                for(int i = 0; i < qbChatDialogs.size(); i++) {
                    if (ChatHelper.getInstance().dialogIndex(qbChatDialogs.get(i).getDialogId()) == -1) {
                        List<Integer> occupants = qbChatDialogs.get(i).getOccupants();
                        for (int j = 0; j < occupants.size(); j++) {
                            Integer occupant = occupants.get(j);
                            if (!usersIDs.contains(occupant)) {
                                usersIDs.add(occupant);
                            }
                        }
                    }
                }
                if (usersIDs.size() > 0) {
                    QBPagedRequestBuilder pagedRequestBuilder = new QBPagedRequestBuilder();
                    pagedRequestBuilder.setPage(0);
                    pagedRequestBuilder.setPerPage(100);
                    QBUsers.getUsersByIDs(usersIDs, pagedRequestBuilder).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
                        @Override
                        public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                            swipeRefreshLayout.setRefreshing(false);

                            ChatHelper.getInstance().dialogs = qbChatDialogs;
                            ChatHelper.getInstance().dialogsUsers = qbUsers;
                            ChatHelper.getInstance().totalUnreadMessageCount(mContext);

                            refreshRecyclerView(false);
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    swipeRefreshLayout.setRefreshing(false);

                    ChatHelper.getInstance().dialogs = qbChatDialogs;
                    ChatHelper.getInstance().totalUnreadMessageCount(mContext);

                    refreshRecyclerView(false);
                }
            }

            @Override
            public void onError(QBResponseException e) {
                swipeRefreshLayout.setRefreshing(false);
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

}
