package com.cloudtenlabs.ping;

import android.content.Context;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;
import com.onesignal.OneSignal;

import org.json.JSONException;

import me.leolin.shortcutbadger.ShortcutBadgeException;
import me.leolin.shortcutbadger.ShortcutBadger;

public class NotificationExtenderBareBonesExample extends NotificationExtenderService {

    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult notification) {
        try {
            OneSignal.clearOneSignalNotifications();
            String badge = notification.payload.additionalData.getString("badge_count");
            setBadgeCount(badge, getApplicationContext());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setBadgeCount(String badge, Context ctx){
        try {
            int badgeCount = 0;
            if(badge != null && !badge.isEmpty()) {
                badgeCount = tryParseInt(badge);
            }
            if(badgeCount > 0) {
                ShortcutBadger. applyCountOrThrow(ctx, badgeCount);
            }
        } catch (ShortcutBadgeException e) {
            e.printStackTrace();
        }
    }
    static int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}