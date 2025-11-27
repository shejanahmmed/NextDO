package com.shejan.nextdo;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class UpcomingTasksWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new UpcomingTasksRemoteViewsFactory(this.getApplicationContext());
    }
}
