package com.km.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class NativePartyActivity extends Activity {
    private static final int BG = Color.rgb(230,221,242);
    private static final int INK = Color.rgb(48,38,59);
    private FirebaseAuth auth;
    private FirebaseFirestore store;
    private LinearLayout root, listBox, formBox;
    private TextView status;
    private String role = "", employeeName = "", employeeRoute = "";

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable panel() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.rgb(249,246,252));
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), Color.rgb(216,204,229));
        return d;
    }
    private TextView label(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(INK); if (bold) t.setTypeface(null,1); return t;
    }
    private EditText field(String hint, int type) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint);
        e.setInputType(type); e.setPadding(dp(12),0,dp(12),0); e.setBackground(panel()); return e;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false);
        b.setTextColor(INK); b.setBackground(panel()); return b;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(218,205,232));
        try {
            FirebaseApp app;
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAYv3-rtCaCuZPD0jK-jeYwA_3Ys7Ittyc")
                    .setApplicationId("1:1002111574436:web:16e7b339a9da923ea3e131")
                    .setProjectId("km-wholesale-7dbb4")
                    .setGcmSenderId("1002111574436").build();
                app = FirebaseApp.initializeApp(this, options);
            } else app = FirebaseApp.getInstance();
            auth = FirebaseAuth.getInstance(app); store = FirebaseFirestore.getInstance(app);
        } catch (Exception e) { build("Firebase সংযোগ শুরু করা যায়নি।"); return; }
        if (auth.getCurrentUser() == null) { finish(); return; }
        build("Party / Shop");
        store.collection("userRoles").document(auth.getCurrentUser().getUid()).get()
            .addOnSuccessListener(this, doc -> {
                role = doc.getString("role");
                employeeName = doc.getString("name");
                Object r = doc.get("route"); employeeRoute = r == null ? "" : String.valueOf(r);
                if (!Boolean.TRUE.equals(doc.getBoolean("active")) ||
                    !("admin".equals(role) || "employee".equals(role))) {
                    auth.signOut(); finish(); return;
                }
                if ("admin".equals(role)) showAdminForm();
                loadOutlets();
            }).addOnFailureListener(this, e -> status.setText("অ্যাকাউন্ট যাচাই করা যায়নি।"));
    }

    private void build(String title) {
        root = new LinearLayout(this); root.setOrientation(1);
        root.setPadding(dp(16),dp(18),dp(16),dp(16)); root.setBackgroundColor(BG);
        setContentView(root);
        root.addView(label("Valuation  •  "+title,22,true));
        Button back = button("← Dashboard-এ ফিরে যান");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,dp(48)); bp.topMargin=dp(12);
        root.addView(back,bp); back.setOnClickListener(v -> finish());
        status = label("তথ্য লোড হচ্ছে…",13,false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(10); root.addView(status,sp);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(1);
        formBox = new LinearLayout(this); formBox.setOrientation(1); content.addView(formBox);
        content.addView(label("সংরক্ষিত দোকানসমূহ",18,true));
        listBox = new LinearLayout(this); listBox.setOrientation(1);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8); content.addView(listBox,lp);
        scroll.addView(content); LinearLayout.LayoutParams sc = new LinearLayout.LayoutParams(-1,0,1); sc.topMargin=dp(12); root.addView(scroll,sc);
    }

    private void showAdminForm() {
        formBox.removeAllViews();
        formBox.addView(label("নতুন Party / Shop যোগ করুন",17,true));
        EditText name=field("দোকানের নাম",1), owner=field("মালিকের নাম",1),
            phone=field("মোবাইল নম্বর",3), address=field("ঠিকানা",1), route=field("Route / এলাকা",1);
        addField(formBox,name); addField(formBox,owner); addField(formBox,phone);
        addField(formBox,address); addField(formBox,route);
        Button save=button("দোকান সংরক্ষণ করুন");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48)); lp.topMargin=dp(8); formBox.addView(save,lp);
        save.setOnClickListener(v -> {
            String n=name.getText().toString().trim(), o=owner.getText().toString().trim();
            if(n.isEmpty()||o.isEmpty()){status.setText("দোকান ও মালিকের নাম লিখুন।");return;}
            saveOutlet(n,o,phone.getText().toString().trim(),address.getText().toString().trim(),route.getText().toString().trim(),save);
        });
    }
    private void addField(LinearLayout box, EditText e) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50)); lp.topMargin=dp(8); box.addView(e,lp);
    }

    @SuppressWarnings("unchecked")
    private void saveOutlet(String name,String owner,String phone,String address,String route,Button save) {
        save.setEnabled(false); status.setText("সংরক্ষণ হচ্ছে…");
        store.collection("appData").document("main").get().addOnSuccessListener(this, snap -> {
            Map<String,Object> payload = snap.exists() && snap.get("payload") instanceof Map
                ? new HashMap<>((Map<String,Object>)snap.get("payload")) : new HashMap<>();
            List<Object> outlets = payload.get("outlets") instanceof List
                ? new ArrayList<>((List<Object>)payload.get("outlets")) : new ArrayList<>();
            Map<String,Object> outlet = new HashMap<>();
            outlet.put("id", System.currentTimeMillis()); outlet.put("name",name); outlet.put("owner",owner);
            outlet.put("phone",phone); outlet.put("address",address);
            outlet.put("route",route.isEmpty()?"None":route); outlet.put("enteredBy","");
            outlets.add(outlet); payload.put("outlets",outlets);
            Map<String,Object> update = new HashMap<>(); update.put("payload",payload);
            update.put("updatedAt",new Date().toString());
            store.collection("appData").document("main").set(update)
                .addOnSuccessListener(this, unused -> publishEmployeeViews(payload, save))
                .addOnFailureListener(this,e -> {
                    save.setEnabled(true); status.setText("সংরক্ষণ হয়নি: "+e.getLocalizedMessage());
                });
        }).addOnFailureListener(this,e -> {
            save.setEnabled(true); status.setText("Cloud ডেটা পড়া যায়নি।");
        });
    }

    @SuppressWarnings("unchecked")
    private void publishEmployeeViews(Map<String,Object> payload, Button save) {
        List<Object> allOutlets = payload.get("outlets") instanceof List
            ? (List<Object>)payload.get("outlets") : new ArrayList<>();
        List<Object> products = payload.get("products") instanceof List
            ? (List<Object>)payload.get("products") : new ArrayList<>();
        List<Object> allEmployees = payload.get("employees") instanceof List
            ? (List<Object>)payload.get("employees") : new ArrayList<>();
        List<Object> allBills = payload.get("bills") instanceof List
            ? (List<Object>)payload.get("bills") : new ArrayList<>();
        List<Object> safeBills = new ArrayList<>();
        Map<String,Object> allAssignments = new HashMap<>();
        for (Object item : allBills) {
            if (!(item instanceof Map)) continue;
            Map<String,Object> bill = (Map<String,Object>)item;
            Map<String,Object> safe = new HashMap<>();
            for (String key : new String[]{"id","invoiceNo","date","outletId","outletName","employee","deliveryStatus","orderId","deliveredAt"})
                if (bill.containsKey(key)) safe.put(key,bill.get(key));
            if (!safe.containsKey("invoiceNo")) safe.put("invoiceNo","");
            if (!safe.containsKey("date")) safe.put("date","");
            if (!safe.containsKey("outletName")) safe.put("outletName","");
            if (!safe.containsKey("employee")) safe.put("employee","");
            if (!safe.containsKey("deliveryStatus")) safe.put("deliveryStatus","Pending");
            if (!safe.containsKey("orderId")) safe.put("orderId",null);
            if (!safe.containsKey("deliveredAt")) safe.put("deliveredAt",null);
            safeBills.add(safe);
            String employee = String.valueOf(bill.get("employee") == null ? "" : bill.get("employee"));
            String delivery = String.valueOf(bill.get("deliveryStatus") == null ? "Pending" : bill.get("deliveryStatus"));
            if (!employee.isEmpty() && !"Delivered".equals(delivery))
                allAssignments.put(String.valueOf(bill.get("id")), employee);
        }
        List<Object> safeEmployees = new ArrayList<>();
        for (Object item : allEmployees) {
            if (!(item instanceof Map)) continue;
            Map<String,Object> employee = (Map<String,Object>)item;
            Map<String,Object> safe = new HashMap<>();
            for (String key : new String[]{"id","name","route","target"})
                if (employee.containsKey(key)) safe.put(key,employee.get(key));
            safeEmployees.add(safe);
        }
        Map<String,Object> shared = new HashMap<>();
        shared.put("outlets",allOutlets); shared.put("products",products);
        shared.put("employees",safeEmployees); shared.put("bills",safeBills);
        shared.put("deliveryAssignments",allAssignments); shared.put("shop","KM");
        shared.put("language",payload.get("language") == null ? "en" : payload.get("language"));
        Map<String,Object> sharedDoc = new HashMap<>();
        sharedDoc.put("payload",shared); sharedDoc.put("updatedAt",new Date().toString());

        java.util.Set<String> routes = new java.util.LinkedHashSet<>();
        for (Object item : allOutlets) if (item instanceof Map) {
            Object route = ((Map<String,Object>)item).get("route");
            if (route != null && !String.valueOf(route).isEmpty()) routes.add(String.valueOf(route));
        }
        for (Object item : allEmployees) if (item instanceof Map) {
            Object route = ((Map<String,Object>)item).get("route");
            if (route != null && !String.valueOf(route).isEmpty()) routes.add(String.valueOf(route));
        }
        List<com.google.android.gms.tasks.Task<Void>> writes = new ArrayList<>();
        writes.add(store.collection("appData").document("shared").set(sharedDoc));
        for (String route : routes) {
            List<Object> routeOutlets = new ArrayList<>(), routeEmployees = new ArrayList<>(), routeBills = new ArrayList<>();
            java.util.Set<String> employeeNames = new java.util.HashSet<>();
            for (Object item : allOutlets) if (item instanceof Map) {
                Map<String,Object> o = (Map<String,Object>)item;
                if (route.equals(String.valueOf(o.get("route")))) routeOutlets.add(o);
            }
            for (Object item : allEmployees) if (item instanceof Map) {
                Map<String,Object> e = (Map<String,Object>)item;
                if (route.equals(String.valueOf(e.get("route")))) {
                    routeEmployees.add(e);
                    if (e.get("name") != null) employeeNames.add(String.valueOf(e.get("name")));
                }
            }
            Map<String,Object> assignments = new HashMap<>();
            for (Object item : safeBills) if (item instanceof Map) {
                Map<String,Object> bill = (Map<String,Object>)item;
                String employee = String.valueOf(bill.get("employee"));
                if (employeeNames.contains(employee)) {
                    routeBills.add(bill);
                    if (!"Delivered".equals(String.valueOf(bill.get("deliveryStatus"))))
                        assignments.put(String.valueOf(bill.get("id")),employee);
                }
            }
            Map<String,Object> routePayload = new HashMap<>();
            routePayload.put("route",route); routePayload.put("outlets",routeOutlets);
            routePayload.put("products",products); routePayload.put("employees",routeEmployees);
            routePayload.put("bills",routeBills); routePayload.put("deliveryAssignments",assignments);
            routePayload.put("shop","KM"); routePayload.put("language",shared.get("language"));
            Map<String,Object> routeDoc = new HashMap<>();
            routeDoc.put("route",route); routeDoc.put("payload",routePayload); routeDoc.put("updatedAt",new Date().toString());
            writes.add(store.collection("employeeData").document(route).set(routeDoc));
        }
        com.google.android.gms.tasks.Tasks.whenAll(writes)
            .addOnSuccessListener(this, result -> {
                save.setEnabled(true); status.setText("দোকান সংরক্ষিত ও Route-এ Sync হয়েছে।");
                loadOutlets();
            }).addOnFailureListener(this, error -> {
                save.setEnabled(true); status.setText("দোকান সংরক্ষিত, তবে Route Sync অসম্পূর্ণ: "+error.getLocalizedMessage());
            });
    }

    @SuppressWarnings("unchecked")
    private void loadOutlets() {
        if ("admin".equals(role)) {
            store.collection("appData").document("main").get().addOnSuccessListener(this, snap -> {
                Map<String,Object> payload = snap.exists() && snap.get("payload") instanceof Map
                    ? (Map<String,Object>)snap.get("payload") : new HashMap<>();
                renderList(payload.get("outlets") instanceof List ? (List<Object>)payload.get("outlets") : new ArrayList<>());
                status.setText("Cloud থেকে তথ্য এসেছে।");
            }).addOnFailureListener(this,e -> status.setText("দোকানের তথ্য লোড হয়নি।"));
        } else if (!employeeRoute.isEmpty()) {
            store.collection("employeeData").document(employeeRoute).get().addOnSuccessListener(this, snap -> {
                Map<String,Object> payload = snap.exists() && snap.get("payload") instanceof Map
                    ? (Map<String,Object>)snap.get("payload") : new HashMap<>();
                renderList(payload.get("outlets") instanceof List ? (List<Object>)payload.get("outlets") : new ArrayList<>());
                status.setText("আপনার Route-এর দোকানসমূহ।");
            }).addOnFailureListener(this,e -> status.setText("দোকানের তথ্য লোড হয়নি।"));
        } else status.setText("Employee অ্যাকাউন্টে Route নির্ধারিত নেই।");
    }

    @SuppressWarnings("unchecked")
    private void renderList(List<Object> outlets) {
        listBox.removeAllViews();
        int count=0;
        for(Object item:outlets) {
            if(!(item instanceof Map)) continue;
            Map<String,Object> o=(Map<String,Object>)item;
            String route=String.valueOf(o.get("route")==null?"None":o.get("route"));
            if("employee".equals(role) && !employeeRoute.equals(route)) continue;
            String info=String.valueOf(o.get("name")==null?"দোকান":o.get("name"))+
                "\nমালিক: "+String.valueOf(o.get("owner")==null?"—":o.get("owner"))+
                "\nমোবাইল: "+String.valueOf(o.get("phone")==null?"—":o.get("phone"))+
                "\nঠিকানা: "+String.valueOf(o.get("address")==null?"—":o.get("address"))+
                "\nRoute: "+route;
            TextView card=label(info,14,false); card.setPadding(dp(14),dp(12),dp(14),dp(12));
            card.setBackground(panel()); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.topMargin=dp(8); listBox.addView(card,lp); count++;
        }
        if(count==0) listBox.addView(label("এখনো কোনো দোকান যোগ করা হয়নি।",14,false));
        status.setText("মোট দোকান: "+count);
    }
}
