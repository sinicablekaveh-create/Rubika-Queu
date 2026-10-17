package ir.sinicable.rubikaqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Isolated review inbox for Hooshika discoveries. Has no message sending capability. */
public final class HooshikaInboxActivity extends Activity {
    private static final String PREFS="hooshika_inbox_v1";
    private static final String KEY="review_items";
    private JSONArray incoming=new JSONArray();
    private final Set<Integer> checked=new HashSet<>();
    private LinearLayout view;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent intent=getIntent();
        if(intent != null && Intent.ACTION_SEND.equals(intent.getAction())){
            String mime=intent.getType();
            if(!HooshikaInboxPayload.MIME.equals(mime)) {
                Toast.makeText(this,"فرمت نتایج پشتیبانی نمی‌شود",Toast.LENGTH_LONG).show();
            } else {
                try {
                    incoming=HooshikaInboxPayload.decode(intent.getStringExtra(Intent.EXTRA_TEXT));
                } catch(IllegalArgumentException e) {
                    incoming=new JSONArray();
                    Toast.makeText(this,"نتایج نامعتبرند و ذخیره نشدند",Toast.LENGTH_LONG).show();
                }
            }
        }
        draw();
    }

    private TextView label(String value,int size){
        TextView t=new TextView(this);
        t.setText(value);t.setTextColor(Color.rgb(30,46,58));t.setTextSize(size);
        t.setPadding(12,14,12,14);
        return t;
    }

    private Button action(String title,Runnable work){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);
        b.setOnClickListener(v->work.run());return b;
    }

    private JSONArray saved(){
        SharedPreferences p=getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        try{return new JSONArray(p.getString(KEY,"[]"));}
        catch(Exception ignored){return new JSONArray();}
    }

    private void draw(){
        ScrollView scroll=new ScrollView(this);
        view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(16,12,16,36);scroll.addView(view);setContentView(scroll);
        view.addView(label("صندوق بررسی نتایج هوشیکا",21));
        view.addView(label("این صفحه فقط برای بازبینی اطلاعات عمومی است. واردکردن یک نتیجه، اجازه پیام‌رسانی نیست و صف فعلی را تغییر نمی‌دهد.",14));
        view.addView(label("نتایج در انتظار تأیید: "+incoming.length(),17));
        checked.clear();
        for(int i=0;i<incoming.length();i++){
            JSONObject row=incoming.optJSONObject(i);if(row==null)continue;
            final int index=i;
            CheckBox cb=new CheckBox(this);
            String kind="public_group".equals(row.optString("kind"))?"گروه/کانال":"مخاطب کسب‌وکار";
            cb.setText(kind+" | "+row.optString("name")+"\n"
                +(row.optString("phone").isEmpty()?"":row.optString("phone")+"\n")
                +row.optString("url"));
            cb.setChecked(false);
            cb.setOnCheckedChangeListener((button,yes)->{
                if(yes)checked.add(index);else checked.remove(index);
            });
            view.addView(cb);
        }
        if(incoming.length()>0) {
            view.addView(action("ذخیره موارد تیک‌خورده در صندوق بررسی",this::saveChecked));
            view.addView(action("ردکردن نتایج دریافتی",()->{
                new AlertDialog.Builder(this).setTitle("رد نتایج")
                    .setMessage("فقط موارد دریافتی فعلی کنار گذاشته شوند؟ صندوق ذخیره‌شده تغییر نمی‌کند.")
                    .setNegativeButton("لغو",null)
                    .setPositiveButton("ردکردن",(dialog,which)->{
                        incoming=new JSONArray();draw();
                    }).show();
            }));
        }
        JSONArray rows=saved();
        view.addView(label("نتایج نگهداری‌شده برای بررسی: "+rows.length(),17));
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            view.addView(label(("public_group".equals(row.optString("kind"))?"گروه/کانال":"کسب‌وکار")
               +" | "+row.optString("name")+"\n"
               +(row.optString("phone").isEmpty()?"":row.optString("phone")+"\n")
               +row.optString("url"),14));
        }
        view.addView(label("وضعیت ارسال: هیچ اقدامی انجام نشده است. هیچ شماره یا گروهی خودکار وارد صف نمی‌شود.",13));
    }

    private void saveChecked(){
        if(checked.isEmpty()){
            Toast.makeText(this,"ابتدا نتایج موردنظر را تیک بزنید",Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray selected=new JSONArray();
        for(int i=0;i<incoming.length();i++) if(checked.contains(i)) selected.put(incoming.optJSONObject(i));
        try{
            JSONArray updated=HooshikaInboxPayload.merge(saved(),selected);
            boolean ok=getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().putString(KEY,updated.toString()).commit();
            if(!ok) throw new IllegalStateException("Saving failed");
            incoming=new JSONArray();
            Toast.makeText(this,"نتایج در صندوق بررسی ذخیره شدند؛ صف فعلی تغییر نکرد",Toast.LENGTH_LONG).show();
            draw();
        }catch(Exception e){
            Toast.makeText(this,"ذخیره ناموفق بود؛ اطلاعات قبلی حفظ شدند",Toast.LENGTH_LONG).show();
        }
    }
}
