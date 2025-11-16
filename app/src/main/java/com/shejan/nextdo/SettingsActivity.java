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
        if (binding.themeRadioGroup == null) return;
        
        String currentTheme = sharedPreferences.getString("theme", "dark");
        if ("light".equals(currentTheme)) {
            binding.themeRadioGroup.check(R.id.light_theme_button);
        } else if ("system".equals(currentTheme)) {
            binding.themeRadioGroup.check(R.id.system_theme_button);
        } else {
            binding.themeRadioGroup.check(R.id.dark_theme_button);
        }

        binding.themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            try {
                String themeValue;
                if (checkedId == R.id.light_theme_button) {
                    themeValue = "light";
                } else if (checkedId == R.id.dark_theme_button) {
                    themeValue = "dark";
                } else {
                    themeValue = "system";
                }
                sharedPreferences.edit().putString("theme", themeValue).apply();
                recreate();
            } catch (Exception e) {
                // Handle theme change failure
            }
        });
    }

    private void setupNotificationSettings() {
        if (binding.notificationsSwitch != null) {
            binding.notificationsSwitch.setChecked(sharedPreferences.getBoolean("notifications", true));
            binding.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
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
