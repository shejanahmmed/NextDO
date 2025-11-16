package com.shejan.nextdo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

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
                String[] themeOptions = {"Light", "Dark", "System"};
                String[] themeValues = {"light", "dark", "system"};
                
                int selectedIndex = 1; // Default to dark
                for (int i = 0; i < themeValues.length; i++) {
                    if (themeValues[i].equals(currentTheme)) {
                        selectedIndex = i;
                        break;
                    }
                }
                
                new AlertDialog.Builder(this)
                        .setTitle("Choose Theme")
                        .setSingleChoiceItems(themeOptions, selectedIndex, (dialog, which) -> {
                            String selectedTheme = themeValues[which];
                            sharedPreferences.edit().putString("theme", selectedTheme).apply();
                            updateCurrentThemeText(selectedTheme);
                            dialog.dismiss();
                            recreate();
                        })
                        .show();
            });
        }
    }
    
    private void updateCurrentThemeText(String theme) {
        if (binding.currentThemeText != null) {
            String displayText = "Dark";
            if ("light".equals(theme)) displayText = "Light";
            else if ("system".equals(theme)) displayText = "System";
            binding.currentThemeText.setText(displayText);
        }
    }

    private void setupNotificationSettings() {
        if (binding.notificationsSwitch != null) {
            binding.notificationsSwitch.setChecked(sharedPreferences.getBoolean("notifications", true));
            binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.switch_animation);
                binding.notificationsSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            });
        }

        if (binding.soundSwitch != null) {
            binding.soundSwitch.setChecked(sharedPreferences.getBoolean("sound", true));
            binding.soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.switch_animation);
                binding.soundSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("sound", isChecked).apply();
            });
        }

        if (binding.vibrationSwitch != null) {
            binding.vibrationSwitch.setChecked(sharedPreferences.getBoolean("vibration", true));
            binding.vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.switch_animation);
                binding.vibrationSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("vibration", isChecked).apply();
            });
        }

        if (binding.persistentNotificationsSwitch != null) {
            binding.persistentNotificationsSwitch.setChecked(sharedPreferences.getBoolean("persistent_notifications", false));
            binding.persistentNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.switch_animation);
                binding.persistentNotificationsSwitch.startAnimation(animation);
                sharedPreferences.edit().putBoolean("persistent_notifications", isChecked).apply();
            });
        }

        if (binding.snoozeSetting != null) {
            binding.snoozeSetting.setOnClickListener(v -> {
                try {
                    String[] snoozeOptions = getResources().getStringArray(R.array.snooze_duration_entries);
                    String[] snoozeValues = getResources().getStringArray(R.array.snooze_duration_values);
                    
                    if (snoozeOptions.length == 0 || snoozeValues.length == 0) {
                        return;
                    }
                    
                    new AlertDialog.Builder(this)
                            .setTitle("Default Snooze Duration")
                            .setItems(snoozeOptions, (dialog, which) -> {
                                if (which >= 0 && which < snoozeValues.length) {
                                    String snoozeValue = snoozeValues[which];
                                    sharedPreferences.edit().putString("snooze_duration", snoozeValue).apply();
                                }
                            })
                            .show();
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
