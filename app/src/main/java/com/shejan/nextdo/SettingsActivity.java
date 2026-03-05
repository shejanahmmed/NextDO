package com.shejan.nextdo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
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
        // Draw content behind status bar so gradient header is truly edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(binding.getRoot());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setupThemeSettings();

        setupNotificationSettings();
        setupDataManagementSettings();
        setupMoreInfoSettings();

    }

    private void setupThemeSettings() {
        String currentTheme = sharedPreferences.getString("app_theme", "light");
        updateCurrentThemeText(currentTheme);

        binding.themeButton.setOnClickListener(v -> showThemePicker(currentTheme));
    }

    private void showThemePicker(String currentTheme) {
        String[] themeNames = { "Auto (System Default)", "Light", "Dark" };
        String[] themeValues = { "auto", "light", "dark" };
        String[] themeSubtitles = {
                "Follows your device setting",
                "Always bright & warm",
                "Easy on the eyes at night"
        };

        // Icon resource per theme: phone (auto), sun (light), moon (dark)
        int[] themeIcons = {
                R.drawable.ic_theme_auto,
                R.drawable.ic_theme_light,
                R.drawable.ic_theme_dark
        };
        // Tint colour per theme icon
        int[] iconTints = {
                android.graphics.Color.parseColor("#FF3B82F6"), // auto → teal
                android.graphics.Color.parseColor("#FFF59E0B"), // light → warm amber (sun)
                android.graphics.Color.parseColor("#FF6366F1") // dark → purple (moon)
        };
        // Background bubble per theme
        int[] iconBgs = {
                R.drawable.bg_settings_icon_teal,
                R.drawable.bg_settings_icon_orange,
                R.drawable.bg_settings_icon_purple
        };

        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
        LinearLayout container = customView.findViewById(R.id.theme_options_container);
        TextView title = customView.findViewById(R.id.dialog_title);
        if (title != null)
            title.setText("Choose Theme");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            binding.getRoot().setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(10f, 10f, android.graphics.Shader.TileMode.MIRROR));
        }

        dialog.setOnDismissListener(d -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                binding.getRoot().setRenderEffect(null);
            }
        });

        for (int i = 0; i < themeNames.length; i++) {
            final int index = i;
            android.view.View optionView = getLayoutInflater().inflate(R.layout.theme_option_item, container, false);

            TextView textView = optionView.findViewById(R.id.theme_text);
            TextView subtitleView = optionView.findViewById(R.id.theme_subtitle);
            RadioButton radioButton = optionView.findViewById(R.id.theme_radio);
            android.view.View colorCircle = optionView.findViewById(R.id.color_circle);
            android.view.View checkIndicator = optionView.findViewById(R.id.check_indicator);

            textView.setText(themeNames[i]);
            if (subtitleView != null)
                subtitleView.setText(themeSubtitles[i]);

            // Set theme icon, tint, and background bubble
            android.widget.ImageView themeIcon = optionView.findViewById(R.id.theme_icon);
            android.view.View iconBgView = optionView.findViewById(R.id.theme_icon_bg);
            if (themeIcon != null) {
                themeIcon.setImageResource(themeIcons[i]);
                themeIcon.setColorFilter(iconTints[i]);
            }
            if (iconBgView != null) {
                iconBgView.setBackgroundResource(iconBgs[i]);
            }

            boolean isSelected = themeValues[i].equals(currentTheme);
            if (radioButton != null)
                radioButton.setChecked(isSelected);

            // Show/hide check indicator and update border
            if (checkIndicator != null) {
                checkIndicator.setVisibility(isSelected ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            optionView.setBackground(getDrawable(
                    isSelected
                            ? R.drawable.bg_theme_option_selected
                            : R.drawable.bg_theme_option_unselected));

            optionView.setOnClickListener(view -> {
                sharedPreferences.edit().putString("app_theme", themeValues[index]).apply();
                updateCurrentThemeText(themeValues[index]);
                applyTheme(themeValues[index]);
                dialog.dismiss();
                Toast.makeText(this, "Theme changed to " + themeNames[index], Toast.LENGTH_SHORT).show();
            });
            container.addView(optionView);
        }
        dialog.show();
    }

    private void applyTheme(String theme) {
        int nightMode;
        switch (theme) {
            case "light":
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "auto":
            default:
                nightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private void updateCurrentThemeText(String theme) {
        String displayText;
        switch (theme) {
            case "light":
                displayText = "Light";
                break;
            case "dark":
                displayText = "Dark";
                break;
            case "auto":
            default:
                displayText = "Auto";
                break;
        }
        binding.currentThemeText.setText(displayText);
    }

    private void setupNotificationSettings() {

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

                AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    binding.getRoot().setRenderEffect(android.graphics.RenderEffect.createBlurEffect(10f, 10f,
                            android.graphics.Shader.TileMode.MIRROR));
                }

                dialog.setOnDismissListener(d -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        binding.getRoot().setRenderEffect(null);
                    }
                });

                // Build subtitles from values (convert ms → friendly description)
                String[] snoozeSubtitles = new String[snoozeValues.length];
                for (int j = 0; j < snoozeValues.length; j++) {
                    try {
                        long ms = Long.parseLong(snoozeValues[j]);
                        long mins = ms / 60000;
                        if (mins < 60) {
                            snoozeSubtitles[j] = "Snooze alarm for " + mins + " minute" + (mins == 1 ? "" : "s");
                        } else {
                            long hrs = mins / 60;
                            snoozeSubtitles[j] = "Snooze alarm for " + hrs + " hour" + (hrs == 1 ? "" : "s");
                        }
                    } catch (NumberFormatException ex) {
                        snoozeSubtitles[j] = "";
                    }
                }

                for (int i = 0; i < snoozeOptions.length; i++) {
                    final int index = i;
                    android.view.View optionView = getLayoutInflater().inflate(R.layout.snooze_option_item,
                            container, false);
                    TextView textView = optionView.findViewById(R.id.snooze_text);
                    TextView subtitleView = optionView.findViewById(R.id.snooze_subtitle);
                    RadioButton radioButton = optionView.findViewById(R.id.snooze_radio);
                    android.view.View checkIndicator = optionView.findViewById(R.id.snooze_check_indicator);

                    textView.setText(snoozeOptions[i]);
                    if (subtitleView != null)
                        subtitleView.setText(snoozeSubtitles[i]);

                    boolean isSelected = snoozeValues[i].equals(currentSnooze);
                    if (radioButton != null)
                        radioButton.setChecked(isSelected);

                    if (checkIndicator != null) {
                        checkIndicator.setVisibility(isSelected ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                    optionView.setBackground(getDrawable(
                            isSelected
                                    ? R.drawable.bg_theme_option_selected
                                    : R.drawable.bg_theme_option_unselected));

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

    private void setupDataManagementSettings() {
        binding.settingRecycleBin.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, RecycleBinActivity.class);
            startActivity(intent);
        });
    }

    private void setupMoreInfoSettings() {
        binding.settingAbout.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, AboutActivity.class);
            startActivity(intent);
        });

        binding.settingReleases.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://github.com/Farjan-Ahmmed/NextDO/releases"));
            startActivity(intent);
        });

        binding.settingLicense.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://github.com/Farjan-Ahmmed/NextDO/blob/main/LICENSE"));
            startActivity(intent);
        });

        binding.settingHelp.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, HelpFAQActivity.class);
            startActivity(intent);
        });
    }

}
