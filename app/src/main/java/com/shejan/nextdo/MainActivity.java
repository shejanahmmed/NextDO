package com.shejan.nextdo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
                    Toast.makeText(
                            getApplicationContext(),
                            R.string.empty_not_saved,
                            Toast.LENGTH_LONG).show();
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
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            R.string.empty_not_saved,
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            ThemeManager.applyTheme(this);
        } catch (Exception e) {
            // Continue with default theme if theme application fails
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (binding.toolbar != null) {
            setSupportActionBar(binding.toolbar);
        }

        final TaskListAdapter adapter = new TaskListAdapter(new TaskListAdapter.TaskDiff(), this);
        if (binding.recyclerview != null) {
            binding.recyclerview.setAdapter(adapter);
            binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
        }

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                adapter.submitList(tasks);
            }
        });

        if (binding.fab != null) {
            binding.fab.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
                newTaskActivityLauncher.launch(intent);
            });
        }

        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Task myTask = adapter.getTaskAt(position);
                            if (myTask != null) {
                                taskViewModel.delete(myTask);
                            }
                        }
                    }
                });
        if (binding.recyclerview != null) {
            helper.attachToRecyclerView(binding.recyclerview);
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
