package com.shejan.nextdo;

import android.content.Context;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpcomingTasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final List<Task> upcomingTasks = new ArrayList<>();
    private final TaskDao taskDao;
    private final String theme;

    public UpcomingTasksRemoteViewsFactory(Context context, android.content.Intent intent) {
        this.context = context;
        AppDatabase db = AppDatabase.getDatabase(context);
        this.taskDao = db.taskDao();
        this.theme = intent.getStringExtra("THEME");
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
            upcomingTasks.sort(java.util.Comparator.comparingLong(t -> t.reminderTime));
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
        int layoutId = R.layout.widget_item_task;
        if ("LIGHT".equals(theme)) {
            layoutId = R.layout.widget_item_task_light;
        }
        RemoteViews rv = new RemoteViews(context.getPackageName(), layoutId);

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
