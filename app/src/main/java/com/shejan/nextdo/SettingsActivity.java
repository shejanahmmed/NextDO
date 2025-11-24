package com.shejan.nextdo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.shejan.nextdo.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;

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
        setupThemeSettings();
        setupBackgroundSettings();
        setupNotificationSettings();
        setupSocialLinks();
    }

    private void setupBackButton() {
        if (binding.backArrow != null) {
            binding.backArrow.setOnClickListener(v -> finish());
        }
    }

    private void setupThemeSettings() {
        String currentTheme = sharedPreferences.getString("theme", "dark");
        updateCurrentThemeText(currentTheme);

        if (binding.appearanceButton != null) {
            binding.appearanceButton.setOnClickListener(v -> {
                String[] themeOptions = { "Light", "Dark", "System" };
                String[] themeValues = { "light", "dark", "system" };

                android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
                LinearLayout container = customView.findViewById(R.id.theme_options_container);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(customView);
                builder.setNegativeButton("Cancel", null);

                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                for (int i = 0; i < themeOptions.length; i++) {
                    final int index = i;
                    android.view.View optionView = getLayoutInflater().inflate(R.layout.theme_option_item, container,
                            false);
                    TextView textView = optionView.findViewById(R.id.theme_text);
                    RadioButton radioButton = optionView.findViewById(R.id.theme_radio);

                    textView.setText(themeOptions[i]);
                    boolean isSelected = themeValues[i].equals(currentTheme);
                    radioButton.setChecked(isSelected);

                    optionView.setOnClickListener(view -> {
                        String selectedTheme = themeValues[index];
                        sharedPreferences.edit().putString("theme", selectedTheme).apply();
                        updateCurrentThemeText(selectedTheme);
                        dialog.dismiss();
                        recreate();
                    });

                    container.addView(optionView);
                }

                dialog.show();
            });
        }
    }

    private void updateCurrentThemeText(String theme) {
        if (binding.currentThemeText != null) {
            String displayText = "Dark";
            if ("light".equals(theme))
                displayText = "Light";
            else if ("system".equals(theme))
                displayText = "System";
            binding.currentThemeText.setText(displayText);
        }
    }

    private void setupBackgroundSettings() {
        String currentBackground = sharedPreferences.getString("app_background", "default");
        updateCurrentBackgroundText(currentBackground);

        if (binding.backgroundButton != null) {
            binding.backgroundButton.setOnClickListener(v -> {
                String[] backgroundNames = { "Default (Black)", "Night Cottage", "Urban Sketch", "Mystic Tree",
                        "Dark Waves" };
                String[] backgroundValues = { "default", "bg_night_cottage", "bg_urban_sketch", "bg_mystic_tree",
                        "bg_dark_waves" };

                android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
                LinearLayout container = customView.findViewById(R.id.theme_options_container);
                TextView title = customView.findViewById(R.id.dialog_title);
                if (title != null)
                    title.setText("Choose Background");

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(customView);
                builder.setNegativeButton("Cancel", null);

                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

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
                        sharedPreferences.edit().putString("app_background", selectedBackground).apply();
                        updateCurrentBackgroundText(selectedBackground);
                        dialog.dismiss();
                        // Optional: Show toast or just let user go back to see change
                    });

                    container.addView(optionView);
                }

                dialog.show();
            });
        }
    }

    private void updateCurrentBackgroundText(String background) {
        if (binding.currentBackgroundText != null) {
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
            }
            binding.currentBackgroundText.setText(displayText);
        }
    }

    private void setupNotificationSettings() {
        if (binding.notificationsSwitch != null) {
            binding.notificationsSwitch.setChecked(sharedPreferences.getBoolean("notifications", true));
            binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this,
                        R.anim.switch_animation);
                binding.notificationsSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            });
        }

        if (binding.persistentNotificationsSwitch != null) {
            binding.persistentNotificationsSwitch
                    .setChecked(sharedPreferences.getBoolean("persistent_notifications", false));
            binding.persistentNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this,
                        R.anim.switch_animation);
                binding.persistentNotificationsSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("persistent_notifications", isChecked).apply();
            });
        }

        if (binding.snoozeSetting != null) {
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
                    dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

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

    private void setupSocialLinks() {
        if (binding.githubIcon != null) {
            binding.githubIcon.setOnClickListener(v -> openUrl("https://github.com/shejanahmmed"));
        }
        if (binding.instagramIcon != null) {
            binding.instagramIcon.setOnClickListener(v -> openUrl("https://www.instagram.com/iamshejan/"));
        }
        if (binding.linkedinIcon != null) {
            binding.linkedinIcon.setOnClickListener(v -> openUrl("https://www.linkedin.com/in/farjan-ahmmed/"));
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            // Handle URL opening failure
        }
    }
}
