package com.km.app;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class NativeDashboardActivity extends Activity {
    private static final int BG = Color.rgb(230, 221, 242);
    private static final int TILE = Color.rgb(247, 243, 251);
    private static final int INK = Color.rgb(48, 38, 59);
    private LinearLayout root;
    private String currentPage = "Dashboard";

    private final String[][] modules = {
        {"🏠", "Dashboard"}, {"🏪", "Party / Shop"}, {"🎁", "Slab"},
        {"📦", "Products"}, {"🧾", "Bill Create"}, {"📚", "Bill History"},
        {"💳", "বকেয়া / Ledger"}, {"🛒", "Orders"}, {"🚚", "Delivery"},
        {"🎯", "Targets"}, {"🗺️", "Routes"}, {"📊", "Reports"},
        {"👥", "Employees"}, {"⚙️", "Settings"}, {"☁️", "Backup & Sync"}
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(218, 205, 232));
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showDashboard();
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(INK);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(22 * getResources().getDisplayMetrics().density);
        d.setStroke((int)(1 * getResources().getDisplayMetrics().density), stroke);
        return d;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        int pad = dp(16);
        root.setPadding(pad, dp(18), pad, dp(12));
        setContentView(root);

        TextView brand = text("Valuation", 25, true);
        root.addView(brand);
        TextView sub = text("ব্যবসা ব্যবস্থাপনা", 12, false);
        sub.setTextColor(Color.rgb(105, 91, 119));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(2);
        root.addView(sub, subLp);

        TextView heading = text(title, 19, true);
        heading.setPadding(0, dp(20), 0, dp(12));
        root.addView(heading);
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showDashboard() {
        currentPage = "Dashboard";
        base("সব ফিচার");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int tileWidth = (screenWidth - dp(32) - dp(24)) / 4;

        int animationIndex = 0;
        for (String[] item : modules) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            tile.setBackgroundColor(Color.TRANSPARENT);
            tile.setPadding(dp(2), dp(3), dp(2), dp(5));

            LinearLayout iconCard = new LinearLayout(this);
            iconCard.setGravity(Gravity.CENTER);
            iconCard.setBackground(shape(Color.rgb(232, 241, 248), Color.rgb(218, 230, 239)));
            iconCard.setElevation(dp(2));
            TextView icon = text(item[0], 34, false);
            icon.setGravity(Gravity.CENTER);
            iconCard.addView(icon, new LinearLayout.LayoutParams(-1, -1));
            tile.addView(iconCard, new LinearLayout.LayoutParams(-1, dp(78)));
            TextView label = text(item[1], 12, true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(dp(2), dp(8), dp(2), 0);
            tile.addView(icon, new LinearLayout.LayoutParams(-1, -2));

            // Keep the tile and layout fixed; only the icon gently floats.
            ObjectAnimator floatIcon = ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, 0f, -dp(4), 0f);
            floatIcon.setDuration(2100);
            floatIcon.setStartDelay(animationIndex * 110L);
            floatIcon.setRepeatCount(ValueAnimator.INFINITE);
            floatIcon.setInterpolator(new AccelerateDecelerateInterpolator());
            floatIcon.start();
            animationIndex++;
            tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = tileWidth;
            lp.height = dp(122);
            lp.setMargins(dp(3), dp(4), dp(3), dp(7));
            tile.setLayoutParams(lp);
            tile.setOnClickListener(v -> {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
                v.postDelayed(() -> openModule(item[1]), 90);
            });
            grid.addView(tile);
        }
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void openModule(String name) {
        if ("Dashboard".equals(name)) return;
        if ("Party / Shop".equals(name)) {
            startActivity(new Intent(this, NativePartyActivity.class));
            return;
        }
        if ("Slab".equals(name)) {
            startActivity(new Intent(this, NativeSlabActivity.class));
            return;
        }
        if ("বকেয়া / Ledger".equals(name)) {
            startActivity(new Intent(this, NativeLedgerActivity.class));
            return;
        }
        currentPage = name;
        base(name);

        Button back = new Button(this);
        back.setText("←  Dashboard-এ ফিরে যান");
        back.setAllCaps(false);
        back.setTextColor(INK);
        back.setBackground(shape(TILE, Color.rgb(216, 204, 229)));
        back.setOnClickListener(v -> showDashboard());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView note = text(name + " মডিউলটি Native সংস্করণে স্থানান্তরের কাজ বাকি আছে।\n\nএই প্রাথমিক স্ক্রিনে পুরোনো ডেটা বা Firebase পরিবর্তন করা হয় না।", 15, false);
        note.setPadding(dp(4), dp(24), dp(4), 0);
        root.addView(note);
    }

    @Override public void onBackPressed() {
        if (!"Dashboard".equals(currentPage)) showDashboard();
        else super.onBackPressed();
    }
}