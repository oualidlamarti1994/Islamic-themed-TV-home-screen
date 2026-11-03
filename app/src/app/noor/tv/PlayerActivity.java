package app.noor.tv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Plays one recitation or the adhan (streamed). Started only by a button the viewer pressed. */
public class PlayerActivity extends Activity {
    private MediaPlayer mp;
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView status;
    private ProgressBar bar;
    private Button toggle;

    static Intent kahf(Context c) {
        return new Intent(c, PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("url", Noor.prefs(c).getString("kahf_url", Noor.DEF_KAHF))
                .putExtra("title", "سُورَةُ الكَهْفِ")
                .putExtra("sub", Noor.prefs(c).getString("reciter", "الشيخ ماهر المعيقلي"));
    }

    static Intent adhan(Context c, String prayer) {
        return new Intent(c, PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("url", Noor.prefs(c).getString("adhan_url", Noor.DEF_ADHAN))
                .putExtra("title", "الأذان")
                .putExtra("sub", (prayer == null ? "" : "صلاة " + prayer + "  ·  ") + "الشيخ عمر القزابري");
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.addView(new IslamicView(this, true));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(Noor.text(this, getIntent().getStringExtra("title"), 54, Noor.IVORY, Noor.quran(this)));
        col.addView(Noor.text(this, getIntent().getStringExtra("sub"), 22, Noor.GOLD, Noor.amiri(this)));
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Noor.GOLD));
        bar.setMax(1000);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.4f), -2);
        bl.topMargin = 40;
        col.addView(bar, bl);
        status = Noor.text(this, "جارٍ التحميل…", 18, Noor.MUTED, Noor.amiri(this));
        col.addView(status);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        toggle = Noor.button(this, "⏸  إيقاف مؤقت · Pause", v -> toggle());
        row.addView(toggle);
        Button stop = Noor.button(this, "■  إنهاء · Stop", v -> finish());
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
        sl.leftMargin = 24;
        row.addView(stop, sl);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
        rl.topMargin = 30;
        col.addView(row, rl);
        root.addView(col, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        setContentView(root);
        toggle.requestFocus();

        mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        try {
            mp.setDataSource(getIntent().getStringExtra("url"));
            mp.setOnPreparedListener(p -> { p.start(); tick(); });
            mp.setOnCompletionListener(p -> { status.setText("صدق الله العظيم"); h.postDelayed(this::finish, 8000); });
            mp.setOnErrorListener((p, w, x) -> { status.setText("تعذّر التشغيل · check the internet connection"); return true; });
            mp.prepareAsync();
        } catch (Exception e) {
            status.setText("تعذّر التشغيل");
        }
    }

    private void toggle() {
        if (mp == null) return;
        try {
            if (mp.isPlaying()) { mp.pause(); toggle.setText("▶  متابعة · Play"); }
            else { mp.start(); toggle.setText("⏸  إيقاف مؤقت · Pause"); }
        } catch (IllegalStateException ignored) {
        }
    }

    private void tick() {
        try {
            if (mp != null && mp.getDuration() > 0) {
                bar.setProgress((int) (1000L * mp.getCurrentPosition() / mp.getDuration()));
                status.setText(Noor.arDigits(fmt(mp.getCurrentPosition()) + " / " + fmt(mp.getDuration())));
            }
        } catch (IllegalStateException ignored) {
        }
        h.postDelayed(this::tick, 1000);
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == KeyEvent.KEYCODE_MEDIA_PLAY || code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            toggle();
            return true;
        }
        if (code == KeyEvent.KEYCODE_MEDIA_STOP) { finish(); return true; }
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (mp != null) { try { mp.release(); } catch (Exception ignored) { } mp = null; }
        super.onDestroy();
    }
}
