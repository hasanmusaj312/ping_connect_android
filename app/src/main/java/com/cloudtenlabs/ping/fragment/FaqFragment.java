package com.cloudtenlabs.ping.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.activity.DrawerActivity;
import com.cloudtenlabs.ping.global.GlobalVariable;
import com.cloudtenlabs.ping.util.chat.ChatHelper;


public class FaqFragment extends Fragment {

    private TextView badgeTextView;

    private Context mContext;
    private DrawerActivity mActivity;

    public FaqFragment() {
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
        View view = inflater.inflate(R.layout.fragment_faq, container, false);

        ImageView menuButton = view.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view1 -> mActivity.toggleDrawer());

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
