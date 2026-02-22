package com.shejan.nextdo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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
        setupDataManagementSettings();
        setupMoreInfoSettings();

    }

    private void setupBackButton() {
        binding.backArrow.setOnClickListener(v -> finish());
    }

    private void setupThemeSettings() {
        String currentTheme = sharedPreferences.getString("app_theme", "light");
        updateCurrentThemeText(currentTheme);

        binding.themeButton.setOnClickListener(v -> showThemePicker(currentTheme));
    }

    private void showThemePicker(String currentTheme) {
        String[] themeNames = { "Auto (System Default)", "Light", "Dark" };
        String[] themeValues = { "auto", "light", "dark" };

        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_theme_choice, null);
        LinearLayout container = customView.findViewById(R.id.theme_options_container);
        TextView title = customView.findViewById(R.id.dialog_title);
        if (title != null)
            title.setText("Choose Theme");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);
        builder.setNegativeButton("Cancel", null);
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
            RadioButton radioButton = optionView.findViewById(R.id.theme_radio);
            android.view.View colorCircle = optionView.findViewById(R.id.color_circle);

            textView.setText(themeNames[i]);
            radioButton.setChecked(themeValues[i].equals(currentTheme));

            // Hide color circle for theme options
            if (colorCircle != null) {
                colorCircle.setVisibility(android.view.View.GONE);
            }

            optionView.setOnClickListener(view -> {
                sharedPreferences.edit().putString("app_theme", themeValues[index]).apply();
                updateCurrentThemeText(themeValues[index]);

                // Apply theme immediately
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
                builder.setNegativeButton("Cancel", null);

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
