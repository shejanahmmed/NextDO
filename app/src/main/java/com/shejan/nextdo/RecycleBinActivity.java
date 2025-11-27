package com.shejan.nextdo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.shejan.nextdo.databinding.ActivityRecycleBinBinding;

public class RecycleBinActivity extends AppCompatActivity {

    private ActivityRecycleBinBinding binding;
    private TaskViewModel taskViewModel;
    private RecycleBinAdapter adapter;

    private android.view.MenuItem deleteAllItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        binding = ActivityRecycleBinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets systemBars = windowInsets
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Apply top inset to toolbar (padding)
            binding.toolbar.setPadding(
                    binding.toolbar.getPaddingLeft(),
                    systemBars.top,
                    binding.toolbar.getPaddingRight(),
                    binding.toolbar.getPaddingBottom());

            // Apply bottom inset to RecyclerView (padding)
            binding.recyclerviewRecycleBin.setPadding(
                    binding.recyclerviewRecycleBin.getPaddingLeft(),
                    binding.recyclerviewRecycleBin.getPaddingTop(),
                    binding.recyclerviewRecycleBin.getPaddingRight(),
                    16 + systemBars.bottom // Original 16dp + inset
            );

            return windowInsets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize ViewModel
        TaskViewModelFactory factory = new TaskViewModelFactory(getApplication());
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        // Setup RecyclerView
        adapter = new RecycleBinAdapter(new RecycleBinAdapter.TaskDiff(),
                new RecycleBinAdapter.OnTaskActionListener() {
                    @Override
                    public void onRestore(Task task) {
                        taskViewModel.restore(task);
                        Snackbar.make(binding.getRoot(), "Task restored", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDelete(Task task) {
                        taskViewModel.deletePermanently(task);
                        Snackbar.make(binding.getRoot(), "Task deleted permanently", Snackbar.LENGTH_SHORT).show();
                    }
                });
        binding.recyclerviewRecycleBin.setAdapter(adapter);
        binding.recyclerviewRecycleBin.setLayoutManager(new LinearLayoutManager(this));

        // Observe deleted tasks
        taskViewModel.getDeletedTasks().observe(this, tasks -> {
            adapter.submitList(tasks);
            boolean isEmpty = tasks.isEmpty();
            if (isEmpty) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerviewRecycleBin.setVisibility(View.GONE);
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerviewRecycleBin.setVisibility(View.VISIBLE);
            }

            // Update menu item state
            if (deleteAllItem != null) {
                deleteAllItem.setEnabled(!isEmpty);

                String title = deleteAllItem.getTitle().toString();
                android.text.SpannableString s = new android.text.SpannableString(title);

                if (isEmpty) {
                    // Gray out
                    s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY), 0, s.length(),
                            0);
                } else {
                    // Red for danger
                    s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#EF5350")),
                            0, s.length(), 0);
                }
                deleteAllItem.setTitle(s);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recycle_bin, menu);
        deleteAllItem = menu.findItem(R.id.action_delete_all);

        // Initial state check if data is already loaded
        boolean isEmpty = adapter.getCurrentList().isEmpty();
        deleteAllItem.setEnabled(!isEmpty);

        String title = deleteAllItem.getTitle().toString();
        android.text.SpannableString s = new android.text.SpannableString(title);

        if (isEmpty) {
            s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY), 0, s.length(), 0);
        } else {
            s.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#EF5350")), 0,
                    s.length(), 0);
        }
        deleteAllItem.setTitle(s);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            showDeleteAllConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteAllConfirmationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
            taskViewModel.deleteAllDeletedTasks();
            Snackbar.make(binding.getRoot(), "Recycle Bin emptied", Snackbar.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();

        // Force width to 330dp
        if (dialog.getWindow() != null) {
            float density = getResources().getDisplayMetrics().density;
            int widthPx = (int) (330 * density);
            dialog.getWindow().setLayout(widthPx, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for expired tasks (30 days)
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        long threshold = System.currentTimeMillis() - thirtyDaysInMillis;
        taskViewModel.deleteOldTasks(threshold);
    }

}
