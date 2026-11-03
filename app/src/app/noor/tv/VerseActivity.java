package app.noor.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shown every time the TV wakes: a verse inside the mihrab, the Hijri date, the next prayer, and on Fridays
 *  a question whether to play Surah Al-Kahf. Nothing plays without the viewer pressing Play. */
public class VerseActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean asking;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.addView(new IslamicView(this, true));

        JSONObject v = getIntent().hasExtra("ref") ? find(getIntent().getStringExtra("ref")) : Noor.randomAyah(this);
        String text = v.optString("text");

        // top bar: Hijri + Gregorian left, next prayer right
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setPadding(dp(48), dp(24), dp(48), 0);
        String greg = new SimpleDateFormat("EEEE, d MMMM yyyy  ·  HH:mm", Locale.getDefault()).format(new Date());
        top.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
        TextView date = Noor.text(this, greg + "\n" + Prayer.hijriText(this), 16, Noor.MUTED, Noor.amiri(this));
        date.setGravity(Gravity.LEFT);
        date.setTextAlignment(android.view.View.TEXT_ALIGNMENT_GRAVITY);
        top.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(new android.view.View(this), new LinearLayout.LayoutParams(0, 1, 0.9f));
        long[] n = Prayer.next(this);
        if (n != null) {
            String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(n[1]));
            TextView np = Noor.text(this, "صلاة " + Prayer.AR[(int) n[0]] + "  " + Noor.arDigits(t) + "\n" + Prayer.countdown(n[1]),
                    16, Noor.GOLD, Noor.amiriBold(this));
            np.setGravity(Gravity.RIGHT);
            np.setTextAlignment(android.view.View.TEXT_ALIGNMENT_GRAVITY);
            top.addView(np, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        // centre column inside the arch
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int[] hm = Prayer.hijriMonthDay(this);
        String occ = Noor.occasion(hm[0], hm[1], Noor.isFriday());
        if (!occ.isEmpty()) col.addView(Noor.text(this, occ, 22, Noor.GOLD, Noor.amiriBold(this)));
        if (!v.optString("ref").startsWith("1:"))
            col.addView(Noor.text(this, Noor.basmala(this), 20, Noor.GOLD_SOFT, Noor.quran(this)), lp(0, 8));
        TextView verse = Noor.text(this, text, size(text.length()), Noor.IVORY, Noor.quran(this));
        verse.setLineSpacing(0, 1.35f);
        col.addView(verse, lp(0, 14));
        col.addView(Noor.text(this, "﴿ " + Noor.reference(v) + " ﴾", 19, Noor.GOLD, Noor.amiri(this)), lp(0, 14));
        String[] hd = Noor.randomHadith(this);
        if (hd != null) {
            TextView ht = Noor.text(this, hd[0], 17, Noor.MUTED, Noor.amiri(this));
            ht.setLineSpacing(0, 1.15f);
            col.addView(ht, lp(0, 22));
            col.addView(Noor.text(this, hd[1], 14, Noor.GOLD_SOFT, Noor.amiri(this)), lp(0, 2));
        }

        asking = Noor.isFriday() && Noor.prefs(this).getBoolean("friday_on", true)
                && !Noor.today().equals(Noor.prefs(this).getString("friday_answer", "")) && !getIntent().getBooleanExtra("quiet", false);
        if (asking) {
            col.addView(Noor.text(this, "يوم الجمعة · هل تودّ الاستماع إلى سورة الكهف؟\nبصوت الشيخ ماهر المعيقلي", 21, Noor.IVORY, Noor.amiriBold(this)), lp(0, 26));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            Button play = Noor.button(this, "▶  تشغيل · Play", x -> {
                Noor.prefs(this).edit().putString("friday_answer", Noor.today()).apply();
                startActivity(PlayerActivity.kahf(this));
                finish();
            });
            row.addView(play, btn());
            row.addView(Noor.button(this, "لاحقًا · Later", x -> finish()), btn());
            row.addView(Noor.button(this, "ليس اليوم · Not today", x -> {
                Noor.prefs(this).edit().putString("friday_answer", Noor.today()).apply();
                finish();
            }), btn());
            col.addView(row, lp(0, 14));
            play.requestFocus();
            h.postDelayed(this::finish, 90_000);
        } else {
            col.addView(Noor.text(this, "اضغط أي زر للمتابعة", 15, 0x88B9C7BF, Noor.amiri(this)), lp(0, 24));
            h.postDelayed(this::finish, Noor.prefs(this).getInt("wake_seconds", 20) * 1000L);
        }
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        cl.topMargin = dp(40);
        col.setPadding(dp(16), 0, dp(16), 0);
        root.addView(col, cl);
        col.getLayoutParams().width = (int) (getResources().getDisplayMetrics().widthPixels * 0.62f);
        setContentView(root);
        Prayer.refreshAndSchedule(this);
    }

    private JSONObject find(String ref) {
        for (int i = 0; i < Noor.verses(this).length(); i++) {
            JSONObject o = Noor.verses(this).optJSONObject(i);
            if (o.optString("ref").equals(ref)) return o;
        }
        return Noor.pickVerse(this);
    }

    private static float size(int len) {
        if (len < 70) return 34;
        if (len < 140) return 28;
        if (len < 240) return 24;
        if (len < 360) return 21;
        return 18;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private LinearLayout.LayoutParams lp(int w, int topDp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(topDp);
        return l;
    }

    private LinearLayout.LayoutParams btn() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, dp(52));
        l.setMargins(dp(10), 0, dp(10), 0);
        return l;
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (!asking && code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN
                && code != KeyEvent.KEYCODE_VOLUME_MUTE) {
            finish();
            return true;
        }
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    static Intent show(android.content.Context c) {
        return new Intent(c, VerseActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
