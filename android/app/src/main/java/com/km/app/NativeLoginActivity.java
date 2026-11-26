package com.km.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NativeLoginActivity extends Activity {
    private static final int BG = Color.rgb(230, 221, 242);
    private static final int INK = Color.rgb(48, 38, 59);
    private FirebaseAuth auth;
    private FirebaseFirestore store;
    private TextView status;
    private Button login;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(218, 205, 232));
        getWindow().getDecorView().setSystemUiVisibility( android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR );
        try {
            FirebaseApp app;
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAYv3-rtCaCuZPD0jK-jeYwA_3Ys7Ittyc")
                    .setApplicationId("1:1002111574436:web:16e7b339a9da923ea3e131")
                    .setProjectId("km-wholesale-7dbb4")
                    .setGcmSenderId("1002111574436")
                    .build();
                app = FirebaseApp.initializeApp(this, options);
            } else app = FirebaseApp.getInstance();
            auth = FirebaseAuth.getInstance(app);
            store = FirebaseFirestore.getInstance(app);
        } catch (Exception e) {
            showMessage("Firebase সংযোগ শুরু করা যায়নি।");
            return;
        }
        if (auth.getCurrentUser() != null) {
            verifyRole(auth.getCurrentUser().getUid());
            return;
        }
        buildForm();
    }

    private GradientDrawable panel() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.rgb(248, 245, 251));
        d.setCornerRadius(22 * getResources().getDisplayMetrics().density);
        d.setStroke(dp(1), Color.rgb(216, 204, 229));
        return d;
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private void buildForm() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(24));
        root.setBackgroundColor(BG);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Valuation");
        title.setTextSize(28);
        title.setTextColor(INK);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("অ্যাকাউন্টে লগইন");
        sub.setTextSize(18);
        sub.setTextColor(INK);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(18);
        subLp.bottomMargin = dp(18);
        root.addView(sub, subLp);

        EditText email = field("ইমেইল", 33);
        root.addView(email, new LinearLayout.LayoutParams(-1, dp(54)));
        EditText password = field("পাসওয়ার্ড", 129);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.topMargin = dp(12);
        root.addView(password, p);

        login = new Button(this);
        login.setText("লগইন");
        login.setAllCaps(false);
        login.setTextColor(INK);
        login.setBackground(panel());
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(-1, dp(52));
        b.topMargin = dp(16);
        root.addView(login, b);

        status = new TextView(this);
        status.setTextColor(INK);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.topMargin = dp(12);
        root.addView(status, st);

        login.setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String pw = password.getText().toString();
            if (e.isEmpty() || pw.isEmpty()) {
                status.setText("ইমেইল ও পাসওয়ার্ড লিখুন।");
                return;
            }
            login.setEnabled(false);
            status.setText("লগইন যাচাই হচ্ছে…");
            auth.signInWithEmailAndPassword(e, pw).addOnCompleteListener(this, task -> {
                login.setEnabled(true);
                if (task.isSuccessful() && auth.getCurrentUser() != null)
                    verifyRole(auth.getCurrentUser().getUid());
                else status.setText("লগইন হয়নি। ইমেইল ও পাসওয়ার্ড যাচাই করুন।");
            });
        });
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setInputType(type);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(panel());
        return e;
    }

    private void verifyRole(String uid) {
        showMessage("অ্যাকাউন্টের অনুমতি যাচাই হচ্ছে…");
        store.collection("userRoles").document(uid).get()
            .addOnSuccessListener(this, doc -> {
                boolean active = Boolean.TRUE.equals(doc.getBoolean("active"));
                String role = doc.getString("role");
                if (active && ("admin".equals(role) || "employee".equals(role))) {
                    startActivity(new Intent(this, NativeDashboardActivity.class));
                    finish();
                } else {
                    auth.signOut();
                    buildForm();
                    status.setText("এই অ্যাকাউন্ট সক্রিয় নয় অথবা অনুমতি নেই।");
                }
            })
            .addOnFailureListener(this, e -> {
                auth.signOut();
                buildForm();
                status.setText("অনুমতি যাচাই করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।");
            });
    }

    private void showMessage(String message) {
        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);
        TextView t = new TextView(this);
        t.setText(message);
        t.setTextColor(INK);
        t.setTextSize(16);
        root.addView(t);
        setContentView(root);
    }
}
