package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private Button btnLogout;

    // Optional views (add these IDs to your layout if you want)
    private TextView tierBadge;
    private View btnPrintLabel;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar     = findViewById(R.id.progressBar);
        btnLogout       = findViewById(R.id.btnLogout);
        tierBadge       = findViewById(R.id.tierBadge);       // may be null if not in layout
        btnPrintLabel   = findViewById(R.id.btnPrintLabel);   // may be null if not in layout
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Show tier badge / apply gating
        applyTierUi();

        // Simulate loading process
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        }, 2000);

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void applyTierUi() {
        // Show a quick toast so you can verify the active membership tier at runtime
        SecurePrefs prefs = SecurePrefs.get(this);
        String planLabel = TierUtils.planLabel(prefs);
        Toast.makeText(this, planLabel + " plan active", Toast.LENGTH_SHORT).show();

        // Tier badge (optional). If you add a TextView with id=tierBadge, this will populate it.
        if (tierBadge != null) {
            tierBadge.setText(planLabel);
            tierBadge.setVisibility(View.VISIBLE);
        }

        // Example premium-only button (e.g., "Print Label")
        if (btnPrintLabel != null) {
            btnPrintLabel.setVisibility(View.VISIBLE);
            btnPrintLabel.setOnClickListener(v -> {
                String feature = getString(R.string.print_label);
                String upgradeUrl = UpgradeLinks.getUrlForUpgrade(prefs);
                if (FeatureGate.requirePremiumOrDemo(this, prefs, feature, upgradeUrl)) {
                    Toast.makeText(this, getString(R.string.printing_label), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.apply();

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, MembershipLoginActivity.class));
        finish();
    }
}
