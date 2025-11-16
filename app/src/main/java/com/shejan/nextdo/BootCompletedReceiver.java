package com.shejan.nextdo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // This is where we would re-schedule alarms for all tasks.
            // For this to work, we need to access the database, which is not
            // straightforward from a BroadcastReceiver. A better approach is to
            // start a service or use WorkManager to handle the rescheduling.
            // However, to keep things simple for now, we will add this later if needed.
        }
    }
}
