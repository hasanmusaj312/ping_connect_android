package com.cloudtenlabs.ping.manager;

import android.os.Bundle;
import android.util.Log;

import com.cloudtenlabs.ping.util.chat.ChatHelper;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class BackgroundListener implements LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void onBackground() {
//        QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid, Bundle bundle) {
//                QBChatService.getInstance().destroy();
//            }
//
//            @Override
//            public void onError(QBResponseException e) {
//
//            }
//        });
    }
}