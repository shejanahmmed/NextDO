package com.shejan.nextdo;

import android.content.Context;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingTasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private List<Task> upcomingTasks = new ArrayList<>();
    private final TaskDao taskDao;

    public UpcomingTasksRemoteViewsFactory(Context context) {
        this.context = context;
        AppDatabase db = AppDatabase.getDatabase(context);
        this.taskDao = db.taskDao();
    }

    @Override
    public void onCreate() {
        // Data initialization is done in onDataSetChanged
    }

    @Override
    public void onDataSetChanged() {
        // This is called when the widget is updated
        List<Task> allTasks = taskDao.getAllTasksSync();
        upcomingTasks.clear();
        long currentTime = System.currentTimeMillis();

        if (allTasks != null) {
            for (Task task : allTasks) {
                // Filter for upcoming tasks (reminderTime > current time) and not completed
                if (!task.isCompleted && task.reminderTime > currentTime) {
                    upcomingTasks.add(task);
                }
            }
            // Sort by reminder time ascending
            Collections.sort(upcomingTasks, (t1, t2) -> Long.compare(t1.reminderTime, t2.reminderTime));
        }
    }

    @Override
    public void onDestroy() {
        upcomingTasks.clear();
    }

    @Override
    public int getCount() {
        return upcomingTasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= upcomingTasks.size()) {
            return null;
        }

        Task task = upcomingTasks.get(position);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item_task);

        rv.setTextViewText(R.id.widget_item_title, task.title);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String timeStr = sdf.format(new Date(task.reminderTime));
        rv.setTextViewText(R.id.widget_item_time, timeStr);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
