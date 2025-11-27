package com.shejan.nextdo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class UpcomingTasksWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent intent = new Intent(context, UpcomingTasksWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_upcoming_tasks);
        rv.setRemoteAdapter(R.id.widget_list_view, intent);
        rv.setEmptyView(R.id.widget_list_view, R.id.empty_view);

        // Set up pending intent for template (if we want individual item clicks to open
        // the app)
        // For now, let's just make the title open the app
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    public static void sendRefreshBroadcast(Context context) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(new android.content.ComponentName(context, UpcomingTasksWidgetProvider.class));
        context.sendBroadcast(intent);

        Intent intentLight = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intentLight.setComponent(new android.content.ComponentName(context, UpcomingTasksLightWidgetProvider.class));
        context.sendBroadcast(intentLight);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            // Refresh all widgets
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            android.content.ComponentName cn = new android.content.ComponentName(context,
                    UpcomingTasksWidgetProvider.class);
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widget_list_view);
        }
        super.onReceive(context, intent);
    }
}
