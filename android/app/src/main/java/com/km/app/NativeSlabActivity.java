package com.km.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeSlabActivity extends Activity {
    private static final int BG=Color.rgb(230,221,242), INK=Color.rgb(48,38,59);
    private FirebaseAuth auth; private FirebaseFirestore store;
    private LinearLayout root, form, list; private TextView status;
    private Spinner outletSpinner; private final List<Map<String,Object>> outlets=new ArrayList<>();
    private final List<Map<String,Object>> slabs=new ArrayList<>();
    private EditText startField,endField,targetField,giftNameField,giftValueField;

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(){GradientDrawable d=new GradientDrawable();d.setColor(Color.rgb(249,246,252));d.setCornerRadius(dp(15));d.setStroke(dp(1),Color.rgb(216,204,229));return d;}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(INK);if(bold)t.setTypeface(null,1);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setBackground(panel());return b;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setInputType(type);e.setPadding(dp(12),0,dp(12),0);e.setBackground(panel());return e;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(218,205,232));
        try{
            FirebaseApp app;
            if(FirebaseApp.getApps(this).isEmpty()){
                FirebaseOptions o=new FirebaseOptions.Builder().setApiKey("AIzaSyAYv3-rtCaCuZPD0jK-jeYwA_3Ys7Ittyc")
                    .setApplicationId("1:1002111574436:web:16e7b339a9da923ea3e131").setProjectId("km-wholesale-7dbb4")
                    .setGcmSenderId("1002111574436").build();app=FirebaseApp.initializeApp(this,o);
            }else app=FirebaseApp.getInstance();
            auth=FirebaseAuth.getInstance(app);store=FirebaseFirestore.getInstance(app);
        }catch(Exception e){build();status.setText("Firebase সংযোগ শুরু করা যায়নি।");return;}
        if(auth.getCurrentUser()==null){finish();return;}
        build();
        store.collection("userRoles").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(this,doc->{
            if(!Boolean.TRUE.equals(doc.getBoolean("active"))||!"admin".equals(doc.getString("role"))){status.setText("Slab পরিচালনা শুধু Admin করতে পারবেন।");return;}
            loadData();
        }).addOnFailureListener(this,e->status.setText("অ্যাকাউন্ট যাচাই করা যায়নি।"));
    }
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(18),dp(16),dp(14));root.setBackgroundColor(BG);setContentView(root);
        root.addView(text("Valuation  •  Slab",22,true));
        Button back=button("← Dashboard-এ ফিরে যান");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(10);root.addView(back,bp);back.setOnClickListener(v->finish());
        status=text("তথ্য লোড হচ্ছে…",13,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(8);root.addView(status,sp);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(1);
        form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(12),dp(12),dp(12),dp(12));form.setBackground(panel());body.addView(form);
        body.addView(text("Slab Progress",18,true));list=new LinearLayout(this);list.setOrientation(1);body.addView(list);
        scroll.addView(body);LinearLayout.LayoutParams sc=new LinearLayout.LayoutParams(-1,0,1);sc.topMargin=dp(12);root.addView(scroll,sc);
    }
    private void addField(String hint,EditText e){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(49));p.topMargin=dp(7);form.addView(e,p);}
    private void dateField(String label,EditText e){
        e.setFocusable(false);e.setClickable(true);e.setHint(label);e.setBackground(panel());e.setPadding(dp(12),0,dp(12),0);
        e.setOnClickListener(v->{Calendar c=Calendar.getInstance();new DatePickerDialog(this,(view,y,m,d)->e.setText(String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();});
        addField(label,e);
    }
    private void loadData(){
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> p=snap.exists()&&snap.get("payload") instanceof Map?(Map<String,Object>)snap.get("payload"):new HashMap<>();
            outlets.clear();slabs.clear();
            if(p.get("outlets") instanceof List)for(Object x:(List<?>)p.get("outlets"))if(x instanceof Map)outlets.add(new HashMap<>((Map<String,Object>)x));
            if(p.get("slabs") instanceof List)for(Object x:(List<?>)p.get("slabs"))if(x instanceof Map)slabs.add(new HashMap<>((Map<String,Object>)x));
            buildForm();renderSlabs();status.setText("Cloud থেকে তথ্য এসেছে।");
        }).addOnFailureListener(this,e->status.setText("Slab তথ্য লোড হয়নি: "+e.getLocalizedMessage()));
    }
    private void buildForm(){
        form.removeAllViews();form.addView(text("নতুন Gift Slab তৈরি করুন",17,true));
        outletSpinner=new Spinner(this);List<String> names=new ArrayList<>();for(Map<String,Object> o:outlets)names.add(String.valueOf(o.get("name"))+"  •  "+String.valueOf(o.get("id")));
        outletSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        form.addView(outletSpinner,new LinearLayout.LayoutParams(-1,dp(48)));
        startField=field("শুরুর তারিখ",InputType.TYPE_CLASS_TEXT);dateField("Slab Start Date",startField);
        endField=field("শেষ তারিখ",InputType.TYPE_CLASS_TEXT);dateField("Slab End Date",endField);
        targetField=field("Target Amount (৳)",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);addField("Target Amount",targetField);
        giftNameField=field("Gift Name",InputType.TYPE_CLASS_TEXT);addField("Gift Name",giftNameField);
        giftValueField=field("Gift Value (৳)",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);addField("Gift Value",giftValueField);
        Button save=button("Slab সংরক্ষণ করুন");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(10);form.addView(save,bp);save.setOnClickListener(v->saveSlab(save));
        if(outlets.isEmpty()){save.setEnabled(false);status.setText("আগে Party / Shop-এ দোকান যোগ করুন।");}
    }
    @SuppressWarnings("unchecked") private void saveSlab(Button save){
        if(outletSpinner.getSelectedItemPosition()<0||outlets.isEmpty()){status.setText("আগে একটি Outlet যোগ করুন।");return;}
        String start=startField.getText().toString().trim(),end=endField.getText().toString().trim(),gn=giftNameField.getText().toString().trim();
        double target=number(targetField.getText().toString()),gift=number(giftValueField.getText().toString());
        if(start.isEmpty()||end.isEmpty()||gn.isEmpty()||target<=0||giftValueField.getText().toString().trim().isEmpty()){status.setText("সব তথ্য সঠিকভাবে পূরণ করুন।");return;}
        if(end.compareTo(start)<0){status.setText("শেষ তারিখ শুরুর তারিখের আগে হতে পারবে না।");return;}
        Map<String,Object> outlet=outlets.get(outletSpinner.getSelectedItemPosition());long outletId=numberLong(outlet.get("id"));
        for(Map<String,Object> old:slabs)if(numberLong(old.get("outletId"))==outletId){
            String a=String.valueOf(old.get("startDate")==null?"":old.get("startDate")),b=String.valueOf(old.get("endDate")==null?"":old.get("endDate"));
            if(!a.isEmpty()&&!b.isEmpty()&&start.compareTo(b)<=0&&end.compareTo(a)>=0){status.setText("এই দোকানের অন্য Slab-এর সময়ের সঙ্গে তারিখ মিলে যাচ্ছে।");return;}
        }
        save.setEnabled(false);status.setText("সংরক্ষণ হচ্ছে…");
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> payload=snap.exists()&&snap.get("payload") instanceof Map?new HashMap<>((Map<String,Object>)snap.get("payload")):new HashMap<>();
            List<Object> current=payload.get("slabs") instanceof List?new ArrayList<>((List<Object>)payload.get("slabs")):new ArrayList<>();
            Map<String,Object> slab=new HashMap<>();slab.put("id",System.currentTimeMillis());slab.put("outletId",outletId);slab.put("target",target);slab.put("gift",gift);slab.put("giftName",gn);slab.put("startDate",start);slab.put("endDate",end);slab.put("progress",0);slab.put("createdAt",new Date().toString());
            current.add(slab);payload.put("slabs",current);Map<String,Object> update=new HashMap<>();update.put("payload",payload);update.put("updatedAt",new Date().toString());
            store.collection("appData").document("main").set(update).addOnSuccessListener(this,u->{save.setEnabled(true);status.setText("নতুন Slab সংরক্ষিত হয়েছে।");loadData();})
                .addOnFailureListener(this,e->{save.setEnabled(true);status.setText("সংরক্ষণ হয়নি: "+e.getLocalizedMessage());});
        }).addOnFailureListener(this,e->{save.setEnabled(true);status.setText("Cloud ডেটা পড়া যায়নি।");});
    }
    private double number(Object v){try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){return 0;}}
    private long numberLong(Object v){try{return (long)Double.parseDouble(String.valueOf(v));}catch(Exception e){return 0;}}
    private String dateOnly(Object value){if(value==null)return "";String s=String.valueOf(value);return s.length()>=10?s.substring(0,10):s;}
    private double progress(Map<String,Object> slab,Map<String,Object> payload){
        String start=String.valueOf(slab.get("startDate")),end=String.valueOf(slab.get("endDate"));long outletId=numberLong(slab.get("outletId"));double total=0;
        if(payload.get("bills") instanceof List)for(Object x:(List<?>)payload.get("bills"))if(x instanceof Map){
            Map<String,Object>b=(Map<String,Object>)x;if(numberLong(b.get("outletId"))==outletId){String d=dateOnly(b.get("date"));if(d.compareTo(start)>=0&&d.compareTo(end)<=0)total+=Math.max(0,number(b.get("total"))-number(b.get("extra")));}
        }
        if(payload.get("ledger") instanceof List)for(Object x:(List<?>)payload.get("ledger"))if(x instanceof Map){
            Map<String,Object>l=(Map<String,Object>)x;if(numberLong(l.get("outletId"))==outletId&&"Due Added".equals(String.valueOf(l.get("type")))&&number(l.get("amount"))>0){String d=dateOnly(l.get("date"));if(d.compareTo(start)>=0&&d.compareTo(end)<=0)total+=number(l.get("amount"));}
        }
        return total;
    }
    @SuppressWarnings("unchecked") private void renderSlabs(){
        list.removeAllViews();
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> payload=snap.exists()&&snap.get("payload") instanceof Map?(Map<String,Object>)snap.get("payload"):new HashMap<>();
            int count=0;for(Map<String,Object>s:slabs){
                long oid=numberLong(s.get("outletId"));String shop="দোকান";for(Map<String,Object>o:outlets)if(numberLong(o.get("id"))==oid)shop=String.valueOf(o.get("name"));
                double current=progress(s,payload),target=number(s.get("target")),gift=number(s.get("gift"));int pct=target>0?(int)Math.min(100,Math.round(current*100/target)):0;
                LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(13),dp(12),dp(13),dp(12));card.setBackground(panel());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(9);list.addView(card,cp);
                card.addView(text(shop+"  •  "+String.valueOf(s.get("giftName")),16,true));
                card.addView(text("সময়: "+s.get("startDate")+" থেকে "+s.get("endDate"),12,false));
                card.addView(text("অগ্রগতি: ৳"+Math.round(current)+" / ৳"+Math.round(target)+"  ("+pct+"%)",14,true));
                android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(pct);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(9));bp.topMargin=dp(8);card.addView(bar,bp);
                final long id=numberLong(s.get("id"));
                Button edit=button("Target / Gift Value পরিবর্তন");card.addView(edit);edit.setOnClickListener(v->editSlab(id,target,gift));
                Button del=button("Slab মুছুন");card.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("এই Slab মুছে ফেলবেন? দোকান, Bill ও Ledger অপরিবর্তিত থাকবে।").setNegativeButton("না",null).setPositiveButton("হ্যাঁ", (a,b)->deleteSlab(id)).show());count++;
            }
            if(count==0)list.addView(text("এখনো কোনো Slab তৈরি করা হয়নি।",14,false));
        }).addOnFailureListener(this,e->status.setText("Progress হিসাব করা যায়নি।"));
    }
    private void editSlab(long id,double oldTarget,double oldGift){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(12),dp(8),dp(12),0);
        EditText target=field("Target Amount",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);target.setText(String.valueOf(oldTarget));box.addView(target);
        EditText gift=field("Gift Value",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);gift.setText(String.valueOf(oldGift));LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(49));gp.topMargin=dp(8);box.addView(gift,gp);
        new AlertDialog.Builder(this).setTitle("Slab Amount Edit").setView(box).setNegativeButton("বাতিল",null).setPositiveButton("সংরক্ষণ",(dialog,which)->{
            double t=number(target.getText().toString()),g=number(gift.getText().toString());
            if(t<=0||gift.getText().toString().trim().isEmpty()){status.setText("Target ও Gift Value সঠিক দিন।");return;}
            updateSlabAmounts(id,t,g);
        }).show();
    }
    @SuppressWarnings("unchecked") private void updateSlabAmounts(long id,double target,double gift){
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> payload=snap.exists()&&snap.get("payload") instanceof Map?new HashMap<>((Map<String,Object>)snap.get("payload")):new HashMap<>();
            List<Object> current=payload.get("slabs") instanceof List?new ArrayList<>((List<Object>)payload.get("slabs")):new ArrayList<>();
            boolean found=false;
            for(Object x:current)if(x instanceof Map&&numberLong(((Map<String,Object>)x).get("id"))==id){Map<String,Object> item=new HashMap<>((Map<String,Object>)x);item.put("target",target);item.put("gift",gift);current.set(current.indexOf(x),item);found=true;break;}
            if(!found){status.setText("Slab পাওয়া যায়নি।");return;}
            payload.put("slabs",current);Map<String,Object> update=new HashMap<>();update.put("payload",payload);update.put("updatedAt",new Date().toString());
            store.collection("appData").document("main").set(update).addOnSuccessListener(this,u->{status.setText("Slab-এর Target ও Gift Value আপডেট হয়েছে।");loadData();}).addOnFailureListener(this,e->status.setText("আপডেট হয়নি: "+e.getLocalizedMessage()));
        }).addOnFailureListener(this,e->status.setText("Cloud ডেটা পড়া যায়নি।"));
    }
    @SuppressWarnings("unchecked") private void deleteSlab(long id){
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> payload=snap.exists()&&snap.get("payload") instanceof Map?new HashMap<>((Map<String,Object>)snap.get("payload")):new HashMap<>();
            List<Object> current=payload.get("slabs") instanceof List?new ArrayList<>((List<Object>)payload.get("slabs")):new ArrayList<>();
            List<Object> kept=new ArrayList<>();for(Object x:current)if(!(x instanceof Map)||numberLong(((Map<String,Object>)x).get("id"))!=id)kept.add(x);
            payload.put("slabs",kept);Map<String,Object> update=new HashMap<>();update.put("payload",payload);update.put("updatedAt",new Date().toString());
            store.collection("appData").document("main").set(update).addOnSuccessListener(this,u->{status.setText("Slab মুছে ফেলা হয়েছে।");loadData();}).addOnFailureListener(this,e->status.setText("মুছতে সমস্যা হয়েছে।"));
        });
    }
}
