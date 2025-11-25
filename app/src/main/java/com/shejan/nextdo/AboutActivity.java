package com.shejan.nextdo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_about);

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        TextView versionText = findViewById(R.id.version_text);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionText.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        setupSocialLinks();
    }

    private void setupSocialLinks() {
        findViewById(R.id.github_icon).setOnClickListener(v -> openUrl("https://github.com/shejanahmmed"));
        findViewById(R.id.instagram_icon).setOnClickListener(v -> openUrl("https://www.instagram.com/iamshejan/"));
        findViewById(R.id.linkedin_icon).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/farjan-ahmmed/"));
    }

    private void openUrl(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            // Handle URL opening failure
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
