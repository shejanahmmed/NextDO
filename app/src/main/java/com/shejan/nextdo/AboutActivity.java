package com.shejan.nextdo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        // Edge-to-edge so gradient header fills behind status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_about);

        // Back navigation handled by system back button

        // Version text
        TextView versionText = findViewById(R.id.version_text);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionText.setText(getString(R.string.version_template, version));
        } catch (PackageManager.NameNotFoundException e) {
            android.util.Log.e("AboutActivity", "Package name not found", e);
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
}
