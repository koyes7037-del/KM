package com.km.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class NativeDashboardActivity extends Activity {
    private static final int BG = Color.rgb(230, 221, 242);
    private static final int TILE = Color.rgb(240, 234, 247);
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
        showDashboard();
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(48, 38, 59));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(16, 22, 16, 16);
        setContentView(root);
        root.addView(text("Valuation", 25, true));
        TextView heading = text(title, 19, true);
        heading.setPadding(0, 18, 0, 14);
        root.addView(heading);
    }

    private void showDashboard() {
        currentPage = "Dashboard";
        base("সব ফিচার");
        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(true);
        for (String[] item : modules) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackgroundColor(TILE);
            tile.setPadding(5, 18, 5, 18);
            TextView icon = text(item[0], 27, false);
            TextView label = text(item[1], 12, true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(2, 7, 2, 0);
            tile.addView(icon);
            tile.addView(label);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = getResources().getDisplayMetrics().widthPixels / 3 - 25;
            lp.height = 112;
            tile.setLayoutParams(lp);
            tile.setOnClickListener(v -> openModule(item[1]));
            grid.addView(tile);
        }
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void openModule(String name) {
        if ("Dashboard".equals(name)) return;
        currentPage = name;
        base(name);
        Button back = new Button(this);
        back.setText("← Dashboard-এ ফিরে যান");
        back.setOnClickListener(v -> showDashboard());
        root.addView(back);
        TextView note = text(name + " পেজের Native ফিচার স্থানান্তর পরবর্তী ধাপে যুক্ত হবে।\n\nএই প্রোটোটাইপে পুরোনো ডেটা বা Firebase পরিবর্তন করা হচ্ছে না।", 15, false);
        note.setPadding(0, 24, 0, 0);
        root.addView(note);
    }

    @Override public void onBackPressed() {
        if (!"Dashboard".equals(currentPage)) showDashboard();
        else super.onBackPressed();
    }
}
