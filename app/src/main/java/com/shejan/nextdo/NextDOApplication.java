package com.shejan.nextdo;

import android.app.Application;

public class NextDOApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            NotificationHelper.createNotificationChannel(this);
        } catch (Exception e) {
            // Continue app startup even if notification channel creation fails
        }
    }
}
