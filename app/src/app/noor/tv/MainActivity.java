package app.noor.tv;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The app tile: today's prayer times, Hijri date, verse of the day, recitations and the three switches. */
public class MainActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private LinearLayout times;
    private TextView hijri;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WakeService.start(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(new IslamicView(this, false));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(60, 30, 60, 20);

        col.addView(Noor.text(this, "نُور", 46, Noor.GOLD, Noor.amiriBold(this)));
        hijri = Noor.text(this, "", 19, Noor.MUTED, Noor.amiri(this));
        col.addView(hijri);

        times = new LinearLayout(this);
        times.setGravity(Gravity.CENTER);
        times.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = 22;
        col.addView(times, tl);

        JSONObject v = Noor.verseOfDay(this);
        TextView verse = Noor.text(this, v.optString("text"), v.optString("text").length() > 200 ? 22 : 28, Noor.IVORY, Noor.quran(this));
        verse.setLineSpacing(0, 1.3f);
        verse.setMaxLines(5);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.7f), -2);
        vl.topMargin = 26;
        col.addView(verse, vl);
        col.addView(Noor.text(this, "﴿ " + Noor.reference(v) + " ﴾  ·  آية اليوم", 17, Noor.GOLD, Noor.amiri(this)));

        LinearLayout row1 = row();
        Button kahf = Noor.button(this, "▶  سورة الكهف", x -> startActivity(PlayerActivity.kahf(this)));
        row1.addView(kahf, gap());
        row1.addView(Noor.button(this, "▶  الأذان", x -> startActivity(PlayerActivity.adhan(this, null))), gap());
        row1.addView(Noor.button(this, "آية جديدة · New verse", x -> startActivity(VerseActivity.show(this).putExtra("quiet", true))), gap());
        col.addView(row1, top(28));

        LinearLayout row2 = row();
        row2.addView(toggle("wake_on", "آية عند التشغيل · Verse on wake"), gap());
        row2.addView(toggle("friday_on", "سؤال الجمعة · Friday Al-Kahf"), gap());
        Button ad = Noor.button(this, adhanLabel(), null);
        ad.setOnClickListener(x -> {
            String m = Noor.prefs(this).getString("adhan_mode", "ask");
            String nm = m.equals("ask") ? "silent" : m.equals("silent") ? "off" : "ask";
            Noor.prefs(this).edit().putString("adhan_mode", nm).apply();
            ad.setText(adhanLabel());
            Prayer.refreshAndSchedule(this);
        });
        row2.addView(ad, gap());
        col.addView(row2, top(14));

        if (!Prayer.configured(this))
            col.addView(Noor.text(this, "Prayer times: set the city from the computer (tv.ps1 noor-city)", 15, Noor.MUTED, null), top(16));

        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        kahf.requestFocus();
        Prayer.refreshAndSchedule(this);
        refresh();
    }

    private String adhanLabel() {
        String m = Noor.prefs(this).getString("adhan_mode", "ask");
        return "الأذان: " + (m.equals("ask") ? "اسأل · Ask" : m.equals("silent") ? "بطاقة فقط · Card" : "إيقاف · Off");
    }

    private Button toggle(String key, String label) {
        Button b = Noor.button(this, (Noor.prefs(this).getBoolean(key, true) ? "✓ " : "✗ ") + label, null);
        b.setOnClickListener(x -> {
            boolean on = !Noor.prefs(this).getBoolean(key, true);
            Noor.prefs(this).edit().putBoolean(key, on).apply();
            b.setText((on ? "✓ " : "✗ ") + label);
        });
        return b;
    }

    /** Redraws the prayer row every 30 s (data may arrive from the network a moment later). */
    private void refresh() {
        String greg = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault()).format(new Date());
        int[] hm = Prayer.hijriMonthDay(this);
        String occ = Noor.occasion(hm[0], hm[1], Noor.isFriday());
        hijri.setText(Prayer.hijriText(this) + "   ·   " + greg + (occ.isEmpty() ? "" : "\n" + occ));
        times.removeAllViews();
        String[] t = Prayer.times(this, 0);
        long[] next = Prayer.next(this);
        if (t != null) {
            for (int i = 0; i < t.length; i++) {
                boolean isNext = next != null && next[0] == i && next[1] < System.currentTimeMillis() + 86_400_000L
                        && Prayer.at(t[i], 0) == next[1];
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(30, 12, 30, 12);
                GradientDrawable g = new GradientDrawable();
                g.setCornerRadius(24);
                g.setColor(isNext ? 0x40D9B45A : 0x22000000);
                g.setStroke(isNext ? 3 : 1, isNext ? Noor.GOLD : 0x44D9B45A);
                cell.setBackground(g);
                cell.addView(Noor.text(this, Prayer.AR[i], 20, isNext ? Noor.GOLD : Noor.IVORY, Noor.amiriBold(this)));
                cell.addView(Noor.text(this, Noor.arDigits(t[i]), 22, Noor.IVORY, Noor.amiri(this)));
                if (isNext) cell.addView(Noor.text(this, Prayer.countdown(next[1]), 14, Noor.GOLD, Noor.amiri(this)));
                LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
                l.setMargins(10, 0, 10, 0);
                times.addView(cell, l);
            }
        }
        h.postDelayed(this::refresh, 30_000);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER);
        return r;
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.setMargins(12, 0, 12, 0);
        return l;
    }

    private LinearLayout.LayoutParams top(int m) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = m;
        return l;
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
