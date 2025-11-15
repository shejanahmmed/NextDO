package com.shejan.nextdo;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.shejan.nextdo.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements TaskListAdapter.OnTaskInteractionListener {

    private ActivityMainBinding binding;
    private TaskViewModel taskViewModel;

    private final ActivityResultLauncher<Intent> newTaskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String title = data.getStringExtra(NewTaskActivity.EXTRA_TITLE);
                    String description = data.getStringExtra(NewTaskActivity.EXTRA_DESCRIPTION);
                    String priority = data.getStringExtra(NewTaskActivity.EXTRA_PRIORITY);
                    long reminderTime = data.getLongExtra(NewTaskActivity.EXTRA_REMINDER_TIME, 0);
                    String repeat = data.getStringExtra(NewTaskActivity.EXTRA_REPEAT);

                    Task task = new Task();
                    task.title = Objects.requireNonNull(title);
                    task.description = description != null ? description : "";
                    task.priority = priority != null ? priority : "";
                    task.reminderTime = reminderTime;
                    task.repeat = repeat != null ? repeat : "";

                    taskViewModel.insert(task);
                } else {
                    // No toast message on cancel
                }
            });

    private final ActivityResultLauncher<Intent> editTaskActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int id = data.getIntExtra(NewTaskActivity.EXTRA_ID, -1);
                    if (id == -1) {
                        Toast.makeText(this, "Task can\'t be updated", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String title = data.getStringExtra(NewTaskActivity.EXTRA_TITLE);
                    String description = data.getStringExtra(NewTaskActivity.EXTRA_DESCRIPTION);
                    String priority = data.getStringExtra(NewTaskActivity.EXTRA_PRIORITY);
                    long reminderTime = data.getLongExtra(NewTaskActivity.EXTRA_REMINDER_TIME, 0);
                    String repeat = data.getStringExtra(NewTaskActivity.EXTRA_REPEAT);

                    Task task = new Task();
                    task.id = id;
                    task.title = Objects.requireNonNull(title);
                    task.description = description != null ? description : "";
                    task.priority = priority != null ? priority : "";
                    task.reminderTime = reminderTime;
                    task.repeat = repeat != null ? repeat : "";

                    taskViewModel.update(task);
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
                } else {
                    // No toast message on cancel
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        final TaskListAdapter adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(), this);
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, tasks -> {
            adapter.submitList(tasks);
            if (tasks.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerview.setVisibility(View.GONE);
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerview.setVisibility(View.VISIBLE);
            }
        });

        binding.fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
            newTaskActivityLauncher.launch(intent);
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
                    if (taskToDelete != null) {
                        taskViewModel.delete(taskToDelete);

                        Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo", v -> taskViewModel.insert(taskToDelete))
                                .show();
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (dX < 0) { // Swiping to the left
                    // Draw red background
                    background.setColor(Color.RED);
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    // Prepare paint for text
                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(getResources().getDimension(R.dimen.swipe_text_size));
                    textPaint.setAntiAlias(true);
                    textPaint.setTextAlign(Paint.Align.RIGHT);

                    // Calculate position and draw text
                    String deleteText = "Delete";
                    float textMargin = getResources().getDimension(R.dimen.swipe_text_margin);
                    float textX = itemView.getRight() - textMargin;
                    float textY = itemView.getTop() + (itemView.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);

                    c.drawText(deleteText, textX, textY, textPaint);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(binding.recyclerview);
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
    }

    @Override
    public void onTaskClicked(Task task) {
        Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
        intent.putExtra(NewTaskActivity.EXTRA_ID, task.id);
        intent.putExtra(NewTaskActivity.EXTRA_TITLE, task.title);
        intent.putExtra(NewTaskActivity.EXTRA_DESCRIPTION, task.description);
        intent.putExtra(NewTaskActivity.EXTRA_PRIORITY, task.priority);
        intent.putExtra(NewTaskActivity.EXTRA_REMINDER_TIME, task.reminderTime);
        intent.putExtra(NewTaskActivity.EXTRA_REPEAT, task.repeat);
        editTaskActivityLauncher.launch(intent);
    }
}
