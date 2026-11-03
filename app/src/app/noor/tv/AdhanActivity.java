package app.noor.tv;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small card in the top corner when a prayer time arrives. The adhan plays only if the viewer presses Play. */
public class AdhanActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int idx = getIntent().getIntExtra("idx", 0);
        long at = getIntent().getLongExtra("at", System.currentTimeMillis());
        String name = Prayer.AR[idx];
        getWindow().setGravity(Gravity.TOP | Gravity.END);
        WindowManager.LayoutParams wl = getWindow().getAttributes();
        wl.x = 40;
        wl.y = 40;
        getWindow().setAttributes(wl);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(48, 28, 48, 28);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Noor.EMERALD, Noor.NIGHT});
        bg.setCornerRadius(36);
        bg.setStroke(3, Noor.GOLD);
        card.setBackground(bg);
        card.addView(Noor.text(this, "حان الآن وقت صلاة " + name, 26, Noor.IVORY, Noor.amiriBold(this)));
        String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(at));
        card.addView(Noor.text(this, Prayer.KEYS[idx] + "  ·  " + Noor.arDigits(t), 18, Noor.GOLD, Noor.amiri(this)));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button first;
        if ("ask".equals(Noor.prefs(this).getString("adhan_mode", "ask"))) {
            first = Noor.button(this, "▶  الأذان · Play adhan", v -> { startActivity(PlayerActivity.adhan(this, name)); finish(); });
            row.addView(first);
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
            l.leftMargin = 18;
            row.addView(Noor.button(this, "إغلاق · Close", v -> finish()), l);
        } else {
            first = Noor.button(this, "إغلاق · Close", v -> finish());
            row.addView(first);
        }
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
        rl.topMargin = 18;
        card.addView(row, rl);
        setContentView(card);
        first.requestFocus();
        h.postDelayed(this::finish, 60_000);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
