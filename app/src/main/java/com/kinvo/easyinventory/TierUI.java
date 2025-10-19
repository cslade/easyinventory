package com.kinvo.easyinventory;

import android.view.View;
import android.widget.TextView;

public final class TierUI {
    private TierUI() {}

    public static void bindBanner(View bannerRoot, Tier tier) {
        if (bannerRoot == null) return;

        TextView tvName = bannerRoot.findViewById(R.id.tvTierName);
        TextView tvMsg  = bannerRoot.findViewById(R.id.tvTierMessage);
        if (tvName == null || tvMsg == null) {
            // Layout didn’t include the expected IDs — nothing to bind
            return;
        }

        switch (tier) {
            case DEMO:
                tvName.setText("Demo");
                tvMsg.setText("Sandbox data. Some features disabled.");
                break;
            case BASIC:
                tvName.setText("Basic");
                tvMsg.setText("Upgrade to Premium for advanced features.");
                break;
            case PREMIUM:
            default:
                tvName.setText("Premium");
                tvMsg.setText("All features unlocked.");
                break;
        }
        bannerRoot.setVisibility(View.VISIBLE);
    }
}
