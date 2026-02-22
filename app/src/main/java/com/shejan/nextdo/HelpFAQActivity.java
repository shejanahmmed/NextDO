package com.shejan.nextdo;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

public class HelpFAQActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_help_faq);

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set navigation icon color based on theme
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowLightStatusBar, typedValue, true);
        boolean isLightMode = typedValue.data != 0;

        // Get the navigation icon and tint it
        android.graphics.drawable.Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            if (isLightMode) {
                // Light mode - use black icon
                navigationIcon.setTint(0xFF000000);
            } else {
                // Dark mode - use white icon
                navigationIcon.setTint(0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
