package com.km.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NativeLedgerActivity extends Activity {
    private static final int BG=Color.rgb(230,221,242), INK=Color.rgb(48,38,59);
    private FirebaseAuth auth; private FirebaseFirestore store;
    private LinearLayout root, list; private TextView status, summary;
    private final List<Map<String,Object>> outlets=new ArrayList<>(), bills=new ArrayList<>(), ledger=new ArrayList<>();
    private Spinner outletSpinner; private EditText amountField,noteField;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(){GradientDrawable d=new GradientDrawable();d.setColor(Color.rgb(249,246,252));d.setCornerRadius(dp(15));d.setStroke(dp(1),Color.rgb(216,204,229));return d;}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(INK);if(bold)t.setTypeface(null,1);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(INK);b.setBackground(panel());return b;}
    private EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setInputType(type);e.setPadding(dp(12),0,dp(12),0);e.setBackground(panel());return e;}
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(218,205,232));
        try{FirebaseApp app;if(FirebaseApp.getApps(this).isEmpty()){
            FirebaseOptions o=new FirebaseOptions.Builder().setApiKey("AIzaSyAYv3-rtCaCuZPD0jK-jeYwA_3Ys7Ittyc").setApplicationId("1:1002111574436:web:16e7b339a9da923ea3e131").setProjectId("km-wholesale-7dbb4").setGcmSenderId("1002111574436").build();app=FirebaseApp.initializeApp(this,o);
        }else app=FirebaseApp.getInstance();auth=FirebaseAuth.getInstance(app);store=FirebaseFirestore.getInstance(app);
        }catch(Exception e){build();status.setText("Firebase সংযোগ শুরু করা যায়নি।");return;}
        if(auth.getCurrentUser()==null){finish();return;}build();
        store.collection("userRoles").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(this,doc->{
            if(!Boolean.TRUE.equals(doc.getBoolean("active"))||!"admin".equals(doc.getString("role"))){status.setText("Ledger পরিচালনা শুধু Admin করতে পারবেন।");return;}load();
        }).addOnFailureListener(this,e->status.setText("অ্যাকাউন্ট যাচাই করা যায়নি।"));
    }
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(18),dp(16),dp(12));root.setBackgroundColor(BG);setContentView(root);
        root.addView(text("Valuation  •  বকেয়া / Ledger",21,true));
        Button back=button("← Dashboard-এ ফিরে যান");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(9);root.addView(back,bp);back.setOnClickListener(v->finish());
        status=text("তথ্য লোড হচ্ছে…",13,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(8);root.addView(status,sp);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(1);
        LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(12),dp(12),dp(12),dp(12));form.setBackground(panel());body.addView(form);
        form.addView(text("নতুন লেনদেন",17,true));
        outletSpinner=new Spinner(this);form.addView(outletSpinner,new LinearLayout.LayoutParams(-1,dp(48)));
        amountField=field("Amount (৳)",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(49));fp.topMargin=dp(7);form.addView(amountField,fp);
        noteField=field("বিবরণ (ঐচ্ছিক)",InputType.TYPE_CLASS_TEXT);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(49));np.topMargin=dp(7);form.addView(noteField,np);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add=button("+ বকেয়া যোগ");Button receive=button("− টাকা জমা");
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1);rp.leftMargin=dp(7);actions.addView(receive,rp);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(8);form.addView(actions,ap);
        add.setOnClickListener(v->addEntry(true));receive.setOnClickListener(v->addEntry(false));
        summary=text("মোট বকেয়া: ৳0",18,true);LinearLayout.LayoutParams sm=new LinearLayout.LayoutParams(-1,-2);sm.topMargin=dp(14);body.addView(summary,sm);
        list=new LinearLayout(this);list.setOrientation(1);body.addView(list);
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    @SuppressWarnings("unchecked") private void load(){
        status.setText("Cloud থেকে তথ্য আনা হচ্ছে…");
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            Map<String,Object> p=snap.exists()&&snap.get("payload") instanceof Map?(Map<String,Object>)snap.get("payload"):new HashMap<>();
            outlets.clear();bills.clear();ledger.clear();
            copyMaps(p.get("outlets"),outlets);copyMaps(p.get("bills"),bills);copyMaps(p.get("ledger"),ledger);
            List<String> names=new ArrayList<>();for(Map<String,Object>o:outlets)names.add(String.valueOf(o.get("name")));
            outletSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
            status.setText("Ledger আপডেট হয়েছে।");render();
        }).addOnFailureListener(this,e->status.setText("Ledger লোড হয়নি: "+e.getLocalizedMessage()));
    }
    @SuppressWarnings("unchecked") private void copyMaps(Object source,List<Map<String,Object>> target){if(source instanceof List)for(Object x:(List<?>)source)if(x instanceof Map)target.add(new HashMap<>((Map<String,Object>)x));}
    private String isoNow(){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));return f.format(new Date());}
    private double num(Object x){try{return Double.parseDouble(String.valueOf(x));}catch(Exception e){return 0;}}
    private long id(Object x){try{return (long)Double.parseDouble(String.valueOf(x));}catch(Exception e){return 0;}}
    private double due(long outletId){
        double total=0;for(Map<String,Object>b:bills)if(id(b.get("outletId"))==outletId)total+=num(b.get("total"));
        for(Map<String,Object>l:ledger)if(id(l.get("outletId"))==outletId)total+=num(l.get("amount"));return total;
    }
    private String money(double x){return String.format(Locale.US,"৳%,.2f",x);}
    private void addEntry(boolean addDue){
        if(outlets.isEmpty()||outletSpinner.getSelectedItemPosition()<0){status.setText("আগে Party / Shop-এ দোকান যোগ করুন।");return;}
        double amount=num(amountField.getText().toString());if(amount<=0){status.setText("শূন্যের বেশি সঠিক Amount দিন।");return;}
        Map<String,Object> o=outlets.get(outletSpinner.getSelectedItemPosition());long outletId=id(o.get("id"));
        Map<String,Object> e=new HashMap<>();e.put("id",System.currentTimeMillis());e.put("outletId",outletId);e.put("outletName",String.valueOf(o.get("name")));e.put("date",isoNow());e.put("amount",addDue?amount:-amount);e.put("type",addDue?"Due Added":"Payment Received");e.put("note",noteField.getText().toString().trim());
        ledger.add(0,e);saveLedger(addDue?"বকেয়া যোগ হয়েছে।":"জমা গ্রহণ করা হয়েছে।");
    }
    private void saveLedger(String success){
        status.setText("সংরক্ষণ হচ্ছে…");
        store.collection("appData").document("main").get().addOnSuccessListener(this,snap->{
            @SuppressWarnings("unchecked") Map<String,Object> p=snap.exists()&&snap.get("payload") instanceof Map?new HashMap<>((Map<String,Object>)snap.get("payload")):new HashMap<>();
            List<Object> data=new ArrayList<>(ledger);p.put("ledger",data);Map<String,Object> update=new HashMap<>();update.put("payload",p);update.put("updatedAt",new Date().toString());
            store.collection("appData").document("main").set(update).addOnSuccessListener(this,v->{amountField.setText("");noteField.setText("");status.setText(success);load();}).addOnFailureListener(this,e->{status.setText("সংরক্ষণ হয়নি: "+e.getLocalizedMessage());load();});
        }).addOnFailureListener(this,e->{status.setText("Cloud ডেটা পড়া যায়নি।");load();});
    }
    private String dateInput(Object value){try{String s=String.valueOf(value);return s.length()>=10?s.substring(0,10):"";}catch(Exception e){return "";}}
    private void render(){
        list.removeAllViews();double all=0;for(Map<String,Object>o:outlets)all+=due(id(o.get("id")));summary.setText("সব দোকানের মোট বকেয়া: "+money(all));
        for(Map<String,Object>o:outlets){
            long oid=id(o.get("id"));LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(12),dp(11),dp(12),dp(11));card.setBackground(panel());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);list.addView(card,cp);
            card.addView(text(String.valueOf(o.get("name")),16,true));card.addView(text("বর্তমান বকেয়া: "+money(due(oid)),14,true));
            for(Map<String,Object>e:ledger)if(id(e.get("outletId"))==oid){
                String type=String.valueOf(e.get("type"));double a=Math.abs(num(e.get("amount")));
                TextView row=text(type+"  "+money(a)+"\n"+dateInput(e.get("date"))+"  •  "+String.valueOf(e.get("note")==null?"বিবরণ নেই":e.get("note")),13,false);
                row.setPadding(0,dp(8),0,dp(3));card.addView(row);
                Button edit=button("Edit");card.addView(edit);long entryId=id(e.get("id"));edit.setOnClickListener(v->editEntry(entryId));
            }
        }
        if(outlets.isEmpty())list.addView(text("কোনো দোকান পাওয়া যায়নি।",14,false));
    }
    private void editEntry(long entryId){
        Map<String,Object> entry=null;for(Map<String,Object>e:ledger)if(id(e.get("id"))==entryId){entry=e;break;}if(entry==null){status.setText("লেনদেনটি পাওয়া যায়নি।");return;}
        final Map<String,Object> selected=entry;LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(12),dp(8),dp(12),0);
        EditText amount=field("Amount",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setText(String.valueOf(Math.abs(num(entry.get("amount")))));box.addView(amount);
        EditText date=field("YYYY-MM-DD",InputType.TYPE_CLASS_TEXT);date.setFocusable(false);date.setText(dateInput(entry.get("date")));LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,dp(49));dp1.topMargin=dp(8);box.addView(date,dp1);
        date.setOnClickListener(v->{Calendar c=Calendar.getInstance();new DatePickerDialog(this,(view,y,m,d)->date.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();});
        EditText note=field("বিবরণ",InputType.TYPE_CLASS_TEXT);note.setText(String.valueOf(entry.get("note")==null?"":entry.get("note")));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(49));np.topMargin=dp(8);box.addView(note,np);
        new AlertDialog.Builder(this).setTitle("লেনদেন Edit").setView(box).setNegativeButton("বাতিল",null).setPositiveButton("সংরক্ষণ",(dialog,which)->{
            double a=num(amount.getText().toString());String dt=date.getText().toString().trim();
            if(a<=0||!dt.matches("\\d{4}-\\d{2}-\\d{2}")){status.setText("Amount ও Date সঠিকভাবে দিন।");return;}
            String[] parts=dt.split("-");Calendar c=Calendar.getInstance();c.set(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,Integer.parseInt(parts[2]),12,0,0);c.set(Calendar.MILLISECOND,0);
            selected.put("amount","Payment Received".equals(selected.get("type"))?-a:a);selected.put("date",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).format(c.getTime()));selected.put("note",note.getText().toString().trim());saveLedger("লেনদেন Edit হয়েছে; বকেয়া পুনরায় হিসাব করা হয়েছে।");
        }).show();
    }
}
