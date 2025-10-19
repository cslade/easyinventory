package com.kinvo.easyinventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;   // 👈 NEW
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private Button btnLogout;

    // Optional views (add these IDs to your layout if you want)
    private TextView tierBadge;         // 👈 Optional badge
    private View btnPrintLabel;         // 👈 Example premium-only entry point

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

        // 🎫 Show tier badge / apply gating
        applyTierUi();

        // Simulate loading process
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        }, 2000);

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void applyTierUi() {
        // Show a quick toast so you can verify the active flavor during dev
        if (BuildConfig.IS_PREMIUM) {
            Toast.makeText(this, "Premium features enabled", Toast.LENGTH_SHORT).show();
        } else if (BuildConfig.IS_DEMO) {
            Toast.makeText(this, "Demo mode: limited features", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Basic plan active", Toast.LENGTH_SHORT).show();
        }

        // 🏷️ Tier badge (optional). If you add a TextView with id=tierBadge, this will populate it.
        if (tierBadge != null) {
            // Use flavor overlay string `plan_name` if you’ve created it (Demo/Basic/Premium)
            tierBadge.setText(getString(R.string.plan_name));
            tierBadge.setVisibility(View.VISIBLE);
        }

        // 🔒 Example premium-only button (e.g., “Print Label”)
        if (btnPrintLabel != null) {
            btnPrintLabel.setVisibility(BuildConfig.IS_PREMIUM ? View.VISIBLE : View.GONE);
            if (!BuildConfig.IS_PREMIUM) {
                btnPrintLabel.setOnClickListener(v ->
                        Toast.makeText(this, getString(R.string.upgrade_required), Toast.LENGTH_SHORT).show()
                );
            }
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
