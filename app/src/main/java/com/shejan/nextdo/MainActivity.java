package com.shejan.nextdo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.shejan.nextdo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements TaskListAdapter.OnTaskInteractionListener {

    private ActivityMainBinding binding;
    private TaskViewModel taskViewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications will not be shown.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> taskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, 0);

                    Task task = new Task();
                    if (id != 0) { // Existing task
                        task.id = id;
                        task.alarmId = data.getIntExtra(NewTaskActivity.EXTRA_ALARM_ID, 0);
                    } else {
                        task.alarmId = (int) System.currentTimeMillis();
                    }
                    task.title = data.getStringExtra(NewTaskActivity.EXTRA_TITLE);
                    task.description = data.getStringExtra(NewTaskActivity.EXTRA_DESCRIPTION);
                    task.priority = data.getStringExtra(NewTaskActivity.EXTRA_PRIORITY);
                    task.reminderTime = data.getLongExtra(NewTaskActivity.EXTRA_REMINDER_TIME, 0);
                    task.repeat = data.getStringExtra(NewTaskActivity.EXTRA_REPEAT);

                    if (id != 0) {
                        taskViewModel.update(task);
                    } else {
                        taskViewModel.insert(task);
                    }
                } else if (result.getResultCode() == NewTaskActivity.RESULT_DELETE && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, -1);
                    if (id == -1) {
                        Toast.makeText(this, "Task can\'t be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Task task = new Task();
                    task.id = id;
                    taskViewModel.delete(task);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Remove toolbar for Nothing theme
        setupSettingsButton();

        askNotificationPermission();

        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        final TaskListAdapter adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(), this);
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));

        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.submitList(tasks);
                if (binding.emptyView != null && binding.recyclerview != null) {
                    if (tasks.isEmpty()) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.recyclerview.setVisibility(View.GONE);
                    } else {
                        binding.emptyView.setVisibility(View.GONE);
                        binding.recyclerview.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        if (binding.fab != null) {
            // Start floating animation
            android.view.animation.Animation floatAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fab_float_animation);
            binding.fab.startAnimation(floatAnimation);
            
            binding.fab.setOnClickListener(view -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fab_click_animation);
                binding.fab.startAnimation(animation);
                Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
                taskActivityLauncher.launch(intent);
            });
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
            
            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }
            private final Paint textPaint = new Paint();
            private final ColorDrawable background = new ColorDrawable();

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task taskToDelete = adapter.getTaskAt(position);
                    taskViewModel.delete(taskToDelete);

                    Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> taskViewModel.insert(taskToDelete))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (dX < 0) { // Swiping to the left
                    background.setColor(Color.RED);
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(getResources().getDimension(R.dimen.swipe_text_size));
                    textPaint.setAntiAlias(true);
                    textPaint.setTextAlign(Paint.Align.RIGHT);

                    String deleteText = "Delete";
                    float textMargin = getResources().getDimension(R.dimen.swipe_text_margin);
                    float textX = itemView.getRight() - textMargin;
                    float textY = itemView.getTop() + (itemView.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);

                    c.drawText(deleteText, textX, textY, textPaint);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.recyclerview);
    }

    private void setupSettingsButton() {
        try {
            findViewById(R.id.settings_icon).setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        } catch (Exception e) {
            // Handle settings button setup failure
        }
    }

    private void askNotificationPermission() {
        // This is only necessary for API level 33 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Launch the permission request
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskCompleted(Task task, boolean isCompleted) {
        task.isCompleted = isCompleted;
        taskViewModel.update(task);
        
        if (isCompleted) {
            androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(this);
            notificationManager.cancel(task.id);
        }
    }

    @Override
    public void onTaskClicked(Task task) {
        Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
        intent.putExtra(NewTaskActivity.EXTRA_ID, task.id);
        intent.putExtra(NewTaskActivity.EXTRA_ALARM_ID, task.alarmId);
        intent.putExtra(NewTaskActivity.EXTRA_TITLE, task.title);
        intent.putExtra(NewTaskActivity.EXTRA_DESCRIPTION, task.description);
        intent.putExtra(NewTaskActivity.EXTRA_PRIORITY, task.priority);
        intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TIME, task.reminderTime);
        intent.putExtra(NewTaskActivity.EXTRA_REPEAT, task.repeat);
        taskActivityLauncher.launch(intent);
    }

    @Override
    public void onTaskLongClicked(Task task) {
        showTaskContextMenu(task);
    }

    private void showTaskContextMenu(Task task) {
        if (task == null) return;
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_task_options, null);
        builder.setView(customView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        customView.findViewById(R.id.edit_option).setOnClickListener(v -> {
            dialog.dismiss();
            onTaskClicked(task);
        });
        
        customView.findViewById(R.id.delete_option).setOnClickListener(v -> {
            dialog.dismiss();
            taskViewModel.delete(task);
            Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", view -> taskViewModel.insert(task))
                    .show();
        });
        
        dialog.show();
    }
}
