package app.noor.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/** Shared helpers: settings, bundled data, fonts, colours, buttons. */
final class Noor {
    static final int GOLD = 0xFFD9B45A;
    static final int GOLD_SOFT = 0x99D9B45A;
    static final int IVORY = 0xFFF6EBD2;
    static final int EMERALD = 0xFF062A23;
    static final int NIGHT = 0xFF03131A;
    static final int MUTED = 0xFFB9C7BF;

    static final String DEF_KAHF = "https://server12.mp3quran.net/maher/018.mp3";
    // Moroccan adhan: Sheikh Omar Al-Kazabri (assabile.com)
    static final String DEF_ADHAN = "https://media.assabile.com/assabile/adhan_3435370/f77bff859f61.mp3";
    // Default location: Mecca; Aladhan method 4 = Umm al-Qura. Set your own with the ConfigProvider (see README).
    static final float DEF_LAT = 21.4225f, DEF_LON = 39.8262f;
    static final int DEF_METHOD = 4;

    private static JSONArray verses, names, quranData, hadith;
    private static int ayatTotal;
    private static Typeface quran, amiri, amiriBold;
    private static final Random RND = new Random();

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("noor", Context.MODE_PRIVATE);
    }

    static String asset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "[]";
        }
    }

    static synchronized JSONArray verses(Context c) {
        if (verses == null) try { verses = new JSONArray(asset(c, "verses.json")); } catch (Exception e) { verses = new JSONArray(); }
        return verses;
    }

    static synchronized JSONArray names(Context c) {
        if (names == null) try { names = new JSONArray(asset(c, "names.json")); } catch (Exception e) { names = new JSONArray(); }
        return names;
    }

    static synchronized JSONArray quranData(Context c) {
        if (quranData == null) {
            try { quranData = new JSONArray(asset(c, "quran.json")); } catch (Exception e) { quranData = new JSONArray(); }
            ayatTotal = 0;
            for (int i = 0; i < quranData.length(); i++) ayatTotal += quranData.optJSONArray(i).optJSONArray(1).length();
        }
        return quranData;
    }

    /** A random ayah from the whole Quran (short enough for the screen); on Fridays often from Al-Kahf. */
    static JSONObject randomAyah(Context c) {
        JSONArray q = quranData(c);
        if (q.length() == 0) return pickVerse(c);
        for (int tries = 0; tries < 12; tries++) {
            int s, a;
            if (isFriday() && RND.nextInt(3) == 0) {
                s = 17;
                a = RND.nextInt(q.optJSONArray(s).optJSONArray(1).length());
            } else {
                int g = RND.nextInt(ayatTotal);
                s = 0;
                while (g >= q.optJSONArray(s).optJSONArray(1).length()) { g -= q.optJSONArray(s).optJSONArray(1).length(); s++; }
                a = g;
            }
            String t = q.optJSONArray(s).optJSONArray(1).optString(a);
            if (t.length() > 330 && tries < 11) continue;
            try {
                return new JSONObject().put("ref", (s + 1) + ":" + (a + 1)).put("surah", q.optJSONArray(s).optString(0))
                        .put("text", t + " ﴿" + arDigits(String.valueOf(a + 1)) + "﴾");
            } catch (Exception e) {
                break;
            }
        }
        return pickVerse(c);
    }

    static synchronized JSONArray hadith(Context c) {
        if (hadith == null) try { hadith = new JSONArray(asset(c, "hadith.json")); } catch (Exception e) { hadith = new JSONArray(); }
        return hadith;
    }

    /** {text, source} of a random hadith from Sahih al-Bukhari / Sahih Muslim, or null. */
    static String[] randomHadith(Context c) {
        JSONArray h = hadith(c);
        if (h.length() == 0) return null;
        JSONArray x = h.optJSONArray(RND.nextInt(h.length()));
        String book = "bukhari".equals(x.optString(1)) ? "صحيح البخاري" : "صحيح مسلم";
        return new String[]{"قال رسول الله ﷺ: «" + x.optString(0) + "»", book + " · " + arDigits(String.valueOf(x.optInt(2)))};
    }

    /** A verse different from the last one shown; on Fridays the Jumu'ah verses come up more often. */
    static JSONObject pickVerse(Context c) {
        JSONArray v = verses(c);
        if (v.length() == 0) return new JSONObject();
        int last = prefs(c).getInt("last_verse", -1);
        int i;
        if (isFriday() && RND.nextInt(3) == 0) {
            int[] fr = new int[v.length()];
            int k = 0;
            for (int j = 0; j < v.length(); j++) if (v.optJSONObject(j).optBoolean("friday")) fr[k++] = j;
            i = k > 0 ? fr[RND.nextInt(k)] : RND.nextInt(v.length());
        } else {
            do { i = RND.nextInt(v.length()); } while (i == last && v.length() > 1);
        }
        prefs(c).edit().putInt("last_verse", i).apply();
        return v.optJSONObject(i);
    }

    /** Verse of the day: the same one all day long. */
    static JSONObject verseOfDay(Context c) {
        JSONArray v = verses(c);
        if (v.length() == 0) return new JSONObject();
        return v.optJSONObject(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) * 7 % v.length());
    }

    static JSONObject pickName(Context c) {
        JSONArray n = names(c);
        return n.length() == 0 ? new JSONObject() : n.optJSONObject(RND.nextInt(n.length()));
    }

    /** "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ" taken from Al-Fatiha 1:1 in the bundled Uthmani text. */
    static String basmala(Context c) {
        JSONArray v = verses(c);
        for (int i = 0; i < v.length(); i++) {
            JSONObject o = v.optJSONObject(i);
            if (o.optString("ref").startsWith("1:")) {
                String t = o.optString("text");
                int end = t.indexOf(" ﴿");
                return end > 0 ? t.substring(0, end) : t;
            }
        }
        return "";
    }

    static Typeface quran(Context c) {
        if (quran == null) quran = Typeface.createFromAsset(c.getAssets(), "AmiriQuran-Regular.ttf");
        return quran;
    }

    static Typeface amiri(Context c) {
        if (amiri == null) amiri = Typeface.createFromAsset(c.getAssets(), "Amiri-Regular.ttf");
        return amiri;
    }

    static Typeface amiriBold(Context c) {
        if (amiriBold == null) amiriBold = Typeface.createFromAsset(c.getAssets(), "Amiri-Bold.ttf");
        return amiriBold;
    }

    static boolean isFriday() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static String arDigits(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        return b.toString();
    }

    /** "سورة البقرة · ٢٥٥" from a verse object. */
    static String reference(JSONObject v) {
        String ref = v.optString("ref");
        String ayat = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : "";
        return v.optString("surah") + "  ·  " + arDigits(ayat.replace("-", "–"));
    }

    static TextView text(Context c, String s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    static Button button(Context c, String label, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setTypeface(amiriBold(c));
        b.setPadding(48, 8, 48, 8);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        GradientDrawable on = new GradientDrawable();
        on.setCornerRadius(60);
        on.setColor(GOLD);
        GradientDrawable off = new GradientDrawable();
        off.setCornerRadius(60);
        off.setColor(0x33062A23);
        off.setStroke(3, GOLD_SOFT);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, on);
        bg.addState(new int[]{android.R.attr.state_pressed}, on);
        bg.addState(new int[]{}, off);
        b.setBackground(bg);
        b.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{Color.parseColor("#0A1F1A"), IVORY}));
        b.setOnClickListener(click);
        return b;
    }

    /** Islamic occasions from the Hijri date (month 1-12, day). Empty when none. */
    static String occasion(int month, int day, boolean friday) {
        if (month == 9) return day >= 21 ? "العشر الأواخر من رمضان · تحرّوا ليلة القدر" : "رَمَضَانُ مُبَارَك";
        if (month == 10 && day <= 3) return "عِيدٌ مُبَارَك · عيد الفطر";
        if (month == 12 && day == 9) return "يوم عرفة";
        if (month == 12 && day >= 10 && day <= 13) return "عِيدٌ مُبَارَك · عيد الأضحى";
        if (month == 12 && day < 9) return "العشر الأوائل من ذي الحجة";
        if (month == 1 && day == 1) return "رأس السنة الهجرية";
        if (month == 1 && (day == 9 || day == 10)) return "يوم عاشوراء";
        if (friday) return "جُمُعَةٌ مُبَارَكَة";
        if (day >= 13 && day <= 15) return "الأيام البيض";
        return "";
    }
}
