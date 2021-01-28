package com.cloudtenlabs.ping.chat.ui.custom.holder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cloudtenlabs.ping.chat.DemoDialogsActivity;
import com.cloudtenlabs.ping.chat.model.Dialog;
import com.cloudtenlabs.ping.chat.data.DialogsFixtures;
import com.cloudtenlabs.ping.R;
import com.cloudtenlabs.ping.chat.ui.custom.holder.holders.dialogs.CustomDialogViewHolder;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;


public class CustomHolderDialogsActivity extends DemoDialogsActivity {

    public static void open(Context context) {
        context.startActivity(new Intent(context, CustomHolderDialogsActivity.class));
    }

    private DialogsList dialogsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_holder_dialogs);

        dialogsList = (DialogsList) findViewById(R.id.dialogsList);
        initAdapter();
    }

    @Override
    public void onDialogClick(Dialog dialog) {
        CustomHolderMessagesActivity.open(this);
    }

    private void initAdapter() {
        super.dialogsAdapter = new DialogsListAdapter<>(
                R.layout.item_custom_dialog_view_holder,
                CustomDialogViewHolder.class,
                super.imageLoader);

        super.dialogsAdapter.setItems(DialogsFixtures.getDialogs());

        super.dialogsAdapter.setOnDialogClickListener(this);
        super.dialogsAdapter.setOnDialogLongClickListener(this);

        dialogsList.setAdapter(super.dialogsAdapter);
    }
}
