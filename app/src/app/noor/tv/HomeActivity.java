package app.noor.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Noor Home: an ad-free Islamic home screen. Clock, Hijri date, prayer times, verse of the day,
 *  then Favourites, Games and All apps. Hold OK on an app to add it to / remove it from Favourites. */
public class HomeActivity extends Activity {
    static final String DEF_FAV = "com.google.android.youtube.tv,com.netflix.ninja,com.amazon.amazonvideo.livingroom,"
            + "com.disney.disneyplus,app.noor.tv,com.tcl.tv";
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView clock, dates, nextPrayer;
    private LinearLayout prayers, rows;
    private ScrollView scroll;
    private TextView quote, quoteRef;
    private boolean showHadith;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WakeService.start(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(new IslamicView(this, false));
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(48), dp(26), dp(48), dp(30));

        // header: clock + dates left, next prayer right
        LinearLayout head = new LinearLayout(this);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        clock = Noor.text(this, "", 44, Noor.IVORY, null);
        clock.setGravity(Gravity.LEFT);
        clock.setShadowLayer(dp(10), 0, 0, 0xAAD9B45A);
        left.addView(clock);
        dates = Noor.text(this, "", 17, Noor.MUTED, Noor.amiri(this));
        dates.setGravity(Gravity.LEFT);
        dates.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        left.addView(dates);
        head.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        TextView salam = Noor.text(this, "السلام عليكم", 30, Noor.GOLD, Noor.amiriBold(this));
        salam.setShadowLayer(dp(12), 0, 0, 0x99D9B45A);
        right.addView(salam);
        nextPrayer = Noor.text(this, "", 18, Noor.IVORY, Noor.amiri(this));
        nextPrayer.setGravity(Gravity.RIGHT);
        nextPrayer.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        right.addView(nextPrayer);
        head.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        col.addView(head);

        prayers = new LinearLayout(this);
        prayers.setGravity(Gravity.CENTER);
        prayers.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        col.addView(prayers, top(14));

        quote = Noor.text(this, "", 22, Noor.IVORY, Noor.quran(this));
        quote.setMaxLines(3);
        quote.setEllipsize(android.text.TextUtils.TruncateAt.END);
        quote.setLineSpacing(0, 1.2f);
        quote.setMinHeight(dp(70));
        col.addView(quote, top(16));
        quoteRef = Noor.text(this, "", 15, Noor.GOLD_SOFT, Noor.amiri(this));
        col.addView(quoteRef);

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        col.addView(rows, top(10));

        scroll.addView(col);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildRows();
        tick();
        rotate();
        Prayer.refreshAndSchedule(this);
    }

    @Override
    protected void onPause() {
        h.removeCallbacksAndMessages(null);
        super.onPause();
    }

    private void tick() {
        Date now = new Date();
        clock.setText(new SimpleDateFormat("HH:mm", Locale.US).format(now));
        int[] hm = Prayer.hijriMonthDay(this);
        String occ = Noor.occasion(hm[0], hm[1], Noor.isFriday());
        dates.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(now) + "\n"
                + Prayer.hijriText(this) + (occ.isEmpty() ? "" : "  ·  " + occ));
        long[] n = Prayer.next(this);
        nextPrayer.setText(n == null ? "" : "صلاة " + Prayer.AR[(int) n[0]] + "  "
                + Noor.arDigits(new SimpleDateFormat("HH:mm", Locale.US).format(new Date(n[1]))) + "  ·  " + Prayer.countdown(n[1]));
        prayers.removeAllViews();
        String[] t = Prayer.times(this, 0);
        if (t != null) for (int i = 0; i < t.length; i++) {
            boolean isNext = n != null && n[0] == i && Prayer.at(t[i], 0) == n[1];
            LinearLayout cell = new LinearLayout(this);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(16), dp(5), dp(16), dp(5));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(18));
            g.setColor(isNext ? 0x40D9B45A : 0x26000000);
            g.setStroke(isNext ? 3 : 1, isNext ? Noor.GOLD : 0x44D9B45A);
            cell.setBackground(g);
            cell.addView(Noor.text(this, Prayer.AR[i] + "  " + Noor.arDigits(t[i]), 16, isNext ? Noor.GOLD : Noor.IVORY, Noor.amiri(this)));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
            l.setMargins(dp(6), 0, dp(6), 0);
            prayers.addView(cell, l);
        }
        h.postDelayed(this::tick, 20_000);
    }

    /** Every 30 s: a new random ayah from the whole Quran, then a sahih hadith, with a soft fade. */
    private void rotate() {
        quote.animate().alpha(0f).setDuration(450).withEndAction(() -> {
            String[] hd = showHadith ? Noor.randomHadith(this) : null;
            if (hd != null) {
                quote.setTypeface(Noor.amiri(this));
                quote.setTextSize(21);
                quote.setText(hd[0]);
                quoteRef.setText(hd[1]);
            } else {
                JSONObject v = Noor.randomAyah(this);
                quote.setTypeface(Noor.quran(this));
                quote.setTextSize(22);
                quote.setText(v.optString("text"));
                quoteRef.setText("﴿ " + Noor.reference(v) + " ﴾");
            }
            showHadith = !showHadith;
            quote.animate().alpha(1f).setDuration(600).start();
        }).start();
        h.postDelayed(this::rotate, 30_000);
    }

    // ---------------------------------------------------------------- apps

    private static class App {
        String pkg, label;
        Intent launch;
        Drawable banner, icon;
        boolean game;
    }

    private Map<String, App> loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, App> apps = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
                ActivityInfo ai = r.activityInfo;
                if (apps.containsKey(ai.packageName) || ai.packageName.equals("com.android.tv.settings")) continue;
                App a = new App();
                a.pkg = ai.packageName;
                a.label = String.valueOf(r.loadLabel(pm));
                a.launch = new Intent(Intent.ACTION_MAIN).addCategory(cat).setClassName(ai.packageName, ai.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                a.banner = ai.loadBanner(pm);
                if (a.banner == null) a.banner = ai.applicationInfo.loadBanner(pm);
                a.icon = r.loadIcon(pm);
                a.game = ai.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
                        || (ai.applicationInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0
                        || a.pkg.equals("com.retroarch") || a.pkg.startsWith("com.retroarch.");
                apps.put(a.pkg, a);
            }
        }
        return apps;
    }

    private List<String> favourites() {
        return new ArrayList<>(Arrays.asList(Noor.prefs(this).getString("favourites", DEF_FAV).split(",")));
    }

    private void buildRows() {
        Map<String, App> apps = loadApps();
        rows.removeAllViews();
        View firstCard = null;

        List<App> fav = new ArrayList<>();
        for (String p : favourites()) if (apps.containsKey(p)) fav.add(apps.get(p));
        if (!fav.isEmpty()) firstCard = row("المفضلة · Favourites", fav);

        List<App> games = new ArrayList<>();
        for (App a : apps.values()) if (a.game) games.add(a);
        if (!games.isEmpty()) row("الألعاب · Games", games);

        List<App> all = new ArrayList<>(apps.values());
        Collections.sort(all, (x, y) -> x.label.compareToIgnoreCase(y.label));
        View f2 = row("كل التطبيقات · All apps", all);
        if (firstCard == null) firstCard = f2;

        TextView qt = Noor.text(this, "اختصارات · Quick", 20, Noor.GOLD, Noor.amiriBold(this));
        qt.setGravity(Gravity.LEFT);
        qt.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        rows.addView(qt, top(18));
        LinearLayout quick = new LinearLayout(this);
        quick.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        quick.setPadding(dp(8), dp(10), dp(8), dp(10));
        quick.addView(tile("settings", "الإعدادات", "Settings", new Intent(Settings.ACTION_SETTINGS)), tileLp());
        quick.addView(tile("wifi", "الشبكة", "Wi-Fi", new Intent(Settings.ACTION_WIFI_SETTINGS)), tileLp());
        quick.addView(tile("bluetooth", "بلوتوث", "Bluetooth", new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)), tileLp());
        quick.addView(tile("display", "الشاشة", "Display", new Intent(Settings.ACTION_DISPLAY_SETTINGS)), tileLp());
        quick.addView(tile("kahf", "سورة الكهف", "Al-Kahf", PlayerActivity.kahf(this)), tileLp());
        quick.addView(tile("verse", "آية وحديث", "Verse", VerseActivity.show(this).putExtra("quiet", true)), tileLp());
        HorizontalScrollView qs = new HorizontalScrollView(this);
        qs.setHorizontalScrollBarEnabled(false);
        qs.addView(quick);
        rows.addView(qs);
        if (firstCard != null) firstCard.requestFocus();
        scroll.postDelayed(() -> scroll.scrollTo(0, 0), 350);
    }

    /** One titled, horizontally scrolling row of app cards; returns its first card. */
    private View row(String title, List<App> list) {
        TextView t = Noor.text(this, title, 20, Noor.GOLD, Noor.amiriBold(this));
        t.setGravity(Gravity.LEFT);
        t.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        rows.addView(t, top(18));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        LinearLayout line = new LinearLayout(this);
        line.setPadding(dp(8), dp(10), dp(8), dp(10));
        line.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        View first = null;
        for (App a : list) {
            View c = card(a);
            if (first == null) first = c;
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(128), dp(72));
            l.setMargins(dp(8), 0, dp(8), 0);
            line.addView(c, l);
        }
        hs.addView(line);
        rows.addView(hs);
        return first;
    }

    private View card(App a) {
        FrameLayout c = new FrameLayout(this);
        c.setFocusable(true);
        c.setCameraDistance(8000 * getResources().getDisplayMetrics().density);
        c.setElevation(dp(2));
        if (android.os.Build.VERSION.SDK_INT >= 28) { c.setOutlineSpotShadowColor(Noor.GOLD); c.setOutlineAmbientShadowColor(Noor.GOLD); }
        c.setClipToOutline(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFF0B3A30);
        bg.setStroke(dp(1), 0x55D9B45A);
        c.setBackground(bg);
        ImageView img = new ImageView(this);
        if (a.banner != null) {
            img.setImageDrawable(a.banner);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            c.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else {
            img.setImageDrawable(a.icon);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            il.topMargin = dp(12);
            c.addView(img, il);
            TextView name = Noor.text(this, a.label, 13, Noor.IVORY, null);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            nl.bottomMargin = dp(8);
            c.addView(name, nl);
        }
        c.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.14f : 1f).scaleY(has ? 1.14f : 1f).rotationX(has ? -6f : 0f).translationZ(has ? dp(14) : 0).setDuration(180).start();
            bg.setStroke(has ? dp(3) : dp(1), has ? Noor.GOLD : 0x55D9B45A);
        });
        c.setOnClickListener(v -> safeStart(a.launch));
        c.setOnLongClickListener(v -> {
            List<String> f = favourites();
            boolean had = f.remove(a.pkg);
            if (!had) f.add(a.pkg);
            Noor.prefs(this).edit().putString("favourites", String.join(",", f)).apply();
            Toast.makeText(this, (had ? "Removed from favourites: " : "Added to favourites: ") + a.label, Toast.LENGTH_SHORT).show();
            buildRows();
            return true;
        });
        return c;
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(128), dp(72));
        l.setMargins(dp(8), 0, dp(8), 0);
        return l;
    }

    /** Quick tile in the same style as the app cards: gold icon on emerald glass, Arabic + English label. */
    private View tile(String icon, String ar, String en, Intent go) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setFocusable(true);
        c.setCameraDistance(8000 * getResources().getDisplayMetrics().density);
        c.setElevation(dp(2));
        if (android.os.Build.VERSION.SDK_INT >= 28) { c.setOutlineSpotShadowColor(Noor.GOLD); c.setOutlineAmbientShadowColor(Noor.GOLD); }
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0F4A3D, 0xFF06231D});
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0x55D9B45A);
        c.setBackground(bg);
        c.addView(new IconView(this, icon), new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView l = Noor.text(this, ar + " · " + en, 11, Noor.IVORY, Noor.amiri(this));
        l.setSingleLine(true);
        c.addView(l);
        c.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.14f : 1f).scaleY(has ? 1.14f : 1f).rotationX(has ? -6f : 0f).translationZ(has ? dp(14) : 0).setDuration(180).start();
            bg.setStroke(has ? dp(3) : dp(1), has ? Noor.GOLD : 0x55D9B45A);
        });
        c.setOnClickListener(v -> safeStart(go));
        return c;
    }

    private void safeStart(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) { scroll.smoothScrollTo(0, 0); return true; }
        return super.onKeyDown(code, e);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private LinearLayout.LayoutParams top(int m) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(m);
        return l;
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, dp(48));
        l.setMargins(dp(8), 0, dp(8), 0);
        return l;
    }
}
