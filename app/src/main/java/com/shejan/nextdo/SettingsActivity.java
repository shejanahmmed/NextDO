package com.shejan.nextdo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.shejan.nextdo.databinding.ActivitySettingsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        File file = new File(getFilesDir(), "custom_background.jpg");
                        FileOutputStream outputStream = new FileOutputStream(file);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((inputStream != null) && ((length = inputStream.read(buffer)) > 0)) {
                            outputStream.write(buffer, 0, length);
                        }
                        if (inputStream != null)
                            inputStream.close();
                        outputStream.close();

                        sharedPreferences.edit().putString("app_background", "custom").apply();
                        updateCurrentBackgroundText("custom");
                        Toast.makeText(this, "Background set", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to set background", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ThemeManager.applyTheme(this);
        } catch (Exception e) {
            // Continue with default theme
        }

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setupBackButton();
        setupAccentColorSettings();
        setupBackgroundSettings();
        setupNotificationSettings();

    }

    private void setupBackButton() {
        binding.backArrow.setOnClickListener(v -> finish());
    }

    private void setupAccentColorSettings() {
        int currentColor = sharedPreferences.getInt("accent_color", 0xFF34C759);
        updateColorPreview(currentColor);

        binding.accentColorButton.setOnClickListener(v -> showColorPicker(currentColor));
    }

    private void showColorPicker(int currentColor) {
        int[] colors = { 0xFF34C759, 0xFF007AFF, 0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFFAF52DE, 0xFFFF2D55,
                0xFF5AC8FA };
        String[] colorNames = { "Green", "Blue", "Red", "Orange", "Yellow", "Purple", "Pink", "Cyan" };

        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
        LinearLayout container = customView.findViewById(R.id.theme_options_container);
        TextView title = customView.findViewById(R.id.dialog_title);
        if (title != null)
            title.setText(R.string.choose_accent_color);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        for (int i = 0; i < colors.length; i++) {
            final int color = colors[i];
            android.view.View optionView = getLayoutInflater().inflate(R.layout.theme_option_item, container, false);
            TextView textView = optionView.findViewById(R.id.theme_text);
            RadioButton radioButton = optionView.findViewById(R.id.theme_radio);
            android.view.View colorCircle = optionView.findViewById(R.id.color_circle);

            textView.setText(colorNames[i]);
            radioButton.setChecked(color == currentColor);
            radioButton.setButtonTintList(android.content.res.ColorStateList.valueOf(color));

            // Show and color the preview circle
            if (colorCircle != null) {
                colorCircle.setVisibility(android.view.View.VISIBLE);
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                drawable.setColor(color);
                colorCircle.setBackground(drawable);
            }

            optionView.setOnClickListener(view -> {
                sharedPreferences.edit().putInt("accent_color", color).apply();
                updateColorPreview(color);

                // Apply color to notification switch immediately
                // Apply color to notification switch immediately
                applySwitchColors(binding.notificationsSwitch, color);
                applySwitchColors(binding.persistentNotificationsSwitch, color);

                dialog.dismiss();
                Toast.makeText(this, "Accent color changed", Toast.LENGTH_SHORT).show();
            });
            container.addView(optionView);
        }
        dialog.show();
    }

    private void updateColorPreview(int color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        binding.colorPreview.setBackground(drawable);
    }

    private void setupBackgroundSettings() {
        String currentBackground = sharedPreferences.getString("app_background", "default");
        updateCurrentBackgroundText(currentBackground);

        binding.backgroundButton.setOnClickListener(v -> {
            String[] backgroundNames = { "Default (Black)", "Night Cottage", "Urban Sketch", "Mystic Tree",
                    "Dark Waves", "Choose from Gallery" };
            String[] backgroundValues = { "default", "bg_night_cottage", "bg_urban_sketch", "bg_mystic_tree",
                    "bg_dark_waves", "custom" };

            android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
            LinearLayout container = customView.findViewById(R.id.theme_options_container);
            TextView title = customView.findViewById(R.id.dialog_title);
            if (title != null)
                title.setText(R.string.choose_background);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(customView);
            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            for (int i = 0; i < backgroundNames.length; i++) {
                final int index = i;
                android.view.View optionView = getLayoutInflater().inflate(R.layout.theme_option_item, container,
                        false);
                TextView textView = optionView.findViewById(R.id.theme_text);
                RadioButton radioButton = optionView.findViewById(R.id.theme_radio);

                textView.setText(backgroundNames[i]);
                boolean isSelected = backgroundValues[i].equals(currentBackground);
                radioButton.setChecked(isSelected);

                optionView.setOnClickListener(view -> {
                    String selectedBackground = backgroundValues[index];
                    if ("custom".equals(selectedBackground)) {
                        imagePickerLauncher.launch("image/*");
                        dialog.dismiss();
                    } else {
                        sharedPreferences.edit().putString("app_background", selectedBackground).apply();
                        updateCurrentBackgroundText(selectedBackground);
                        dialog.dismiss();
                    }
                });

                container.addView(optionView);
            }

            dialog.show();
        });
    }

    private void updateCurrentBackgroundText(String background) {
        String displayText = "Default";
        switch (background) {
            case "bg_night_cottage":
                displayText = "Night Cottage";
                break;
            case "bg_urban_sketch":
                displayText = "Urban Sketch";
                break;
            case "bg_mystic_tree":
                displayText = "Mystic Tree";
                break;
            case "bg_dark_waves":
                displayText = "Dark Waves";
                break;
            case "custom":
                displayText = "Custom Image";
                break;
        }
        binding.currentBackgroundText.setText(displayText);
    }

    private void applySwitchColors(com.google.android.material.switchmaterial.SwitchMaterial switchView,
            int accentColor) {
        if (switchView == null)
            return;

        // Set thumb to white
        switchView.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));

        // Set track to accent color when checked, gray when unchecked
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
                accentColor,
                0x4DFFFFFF // 30% white when unchecked
        };
        switchView.setTrackTintList(new android.content.res.ColorStateList(states, colors));
    }

    private void setupNotificationSettings() {
        int accentColor = sharedPreferences.getInt("accent_color", 0xFF34C759);

        applySwitchColors(binding.notificationsSwitch, accentColor);
        binding.notificationsSwitch.setChecked(sharedPreferences.getBoolean("notifications", true));
        binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this,
                    R.anim.switch_animation);
            binding.notificationsSwitch.startAnimation(animation);
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
        });

        applySwitchColors(binding.persistentNotificationsSwitch, accentColor);
        binding.persistentNotificationsSwitch
                .setChecked(sharedPreferences.getBoolean("persistent_notifications", false));
        binding.persistentNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this,
                    R.anim.switch_animation);
            binding.persistentNotificationsSwitch.startAnimation(animation);
            sharedPreferences.edit().putBoolean("persistent_notifications", isChecked).apply();
        });

        binding.snoozeSetting.setOnClickListener(v -> {
            try {
                String[] snoozeOptions = getResources().getStringArray(R.array.snooze_duration_entries);
                String[] snoozeValues = getResources().getStringArray(R.array.snooze_duration_values);
                String currentSnooze = sharedPreferences.getString("snooze_duration", "300000");

                if (snoozeOptions.length == 0 || snoozeValues.length == 0) {
                    return;
                }

                android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_snooze_duration, null);
                LinearLayout container = customView.findViewById(R.id.snooze_options_container);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(customView);
                builder.setNegativeButton("Cancel", null);

                AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                for (int i = 0; i < snoozeOptions.length; i++) {
                    final int index = i;
                    android.view.View optionView = getLayoutInflater().inflate(R.layout.snooze_option_item,
                            container, false);
                    TextView textView = optionView.findViewById(R.id.snooze_text);
                    RadioButton radioButton = optionView.findViewById(R.id.snooze_radio);

                    textView.setText(snoozeOptions[i]);
                    boolean isSelected = snoozeValues[i].equals(currentSnooze);
                    radioButton.setChecked(isSelected);

                    optionView.setOnClickListener(view -> {
                        sharedPreferences.edit().putString("snooze_duration", snoozeValues[index]).apply();
                        dialog.dismiss();
                    });

                    container.addView(optionView);
                }

                dialog.show();
            } catch (Exception e) {
                // Handle dialog creation failure
            }
        });
    }

}
