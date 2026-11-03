package app.noor.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Prayer times and the Hijri date from Aladhan (no account, no key), cached for today and tomorrow. */
final class Prayer {
    static final String[] KEYS = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    static final String[] AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    static boolean configured(Context c) {
        return true; // defaults: Noor.DEF_LAT/DEF_LON/DEF_METHOD
    }

    private static String dayKey(int offset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offset);
        return new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.getTime());
    }

    /** Network: fetches today and tomorrow if missing. Call off the main thread. */
    static void refresh(Context c, boolean force) {
        if (!configured(c)) return;
        SharedPreferences p = Noor.prefs(c);
        for (int d = 0; d <= 1; d++) {
            String k = dayKey(d);
            if (!force && p.contains("t_" + k)) continue;
            try {
                String url = "https://api.aladhan.com/v1/timings/" + k + "?latitude=" + p.getFloat("lat", Noor.DEF_LAT)
                        + "&longitude=" + p.getFloat("lon", Noor.DEF_LON) + "&method=" + p.getInt("method", Noor.DEF_METHOD);
                JSONObject data = new JSONObject(get(url)).getJSONObject("data");
                p.edit().putString("t_" + k, data.toString()).apply();
            } catch (Exception ignored) {
            }
        }
        // drop old days
        SharedPreferences.Editor e = p.edit();
        for (String key : p.getAll().keySet())
            if (key.startsWith("t_") && !key.equals("t_" + dayKey(0)) && !key.equals("t_" + dayKey(1))) e.remove(key);
        e.apply();
    }

    static String get(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(10000);
        h.setReadTimeout(15000);
        h.setRequestProperty("User-Agent", "Noor-TV");
        try (InputStream in = h.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    static JSONObject day(Context c, int offset) {
        try {
            String s = Noor.prefs(c).getString("t_" + dayKey(offset), null);
            return s == null ? null : new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** "HH:mm" for each of KEYS, or null when not loaded. */
    static String[] times(Context c, int offset) {
        JSONObject d = day(c, offset);
        if (d == null) return null;
        JSONObject t = d.optJSONObject("timings");
        if (t == null) return null;
        String[] out = new String[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            String v = t.optString(KEYS[i], "--:--");
            out[i] = v.length() >= 5 ? v.substring(0, 5) : v;
        }
        return out;
    }

    /** e.g. "الجمعة ١٣ جمادى الأولى ١٤٤٨ هـ" plus month number and day for occasions. */
    static String hijriText(Context c) {
        JSONObject d = day(c, 0);
        if (d == null) return "";
        JSONObject h = d.optJSONObject("date") == null ? null : d.optJSONObject("date").optJSONObject("hijri");
        if (h == null) return "";
        String wd = h.optJSONObject("weekday") == null ? "" : h.optJSONObject("weekday").optString("ar");
        String m = h.optJSONObject("month") == null ? "" : h.optJSONObject("month").optString("ar");
        return (wd + " " + Noor.arDigits(String.valueOf(Integer.parseInt(h.optString("day", "0")))) + " " + m + " "
                + Noor.arDigits(h.optString("year")) + " هـ").trim();
    }

    static int[] hijriMonthDay(Context c) {
        JSONObject d = day(c, 0);
        try {
            JSONObject h = d.getJSONObject("date").getJSONObject("hijri");
            return new int[]{h.getJSONObject("month").getInt("number"), Integer.parseInt(h.getString("day"))};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    /** Next of the five prayers: {index into KEYS, millis}. Sunrise is skipped. Null when no data. */
    static long[] next(Context c) {
        long now = System.currentTimeMillis();
        for (int off = 0; off <= 1; off++) {
            String[] t = times(c, off);
            if (t == null) continue;
            for (int i = 0; i < t.length; i++) {
                if (i == 1) continue;
                long ms = at(t[i], off);
                if (ms > now) return new long[]{i, ms};
            }
        }
        return null;
    }

    static long at(String hhmm, int offset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offset);
        try {
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hhmm.substring(0, 2)));
            cal.set(Calendar.MINUTE, Integer.parseInt(hhmm.substring(3, 5)));
        } catch (Exception e) {
            return 0;
        }
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** "بعد ١ س ٢٠ د" */
    static String countdown(long ms) {
        long min = Math.max(0, (ms - System.currentTimeMillis()) / 60000);
        String s = min >= 60 ? (min / 60) + " س " + (min % 60) + " د" : min + " د";
        return "بعد " + Noor.arDigits(s);
    }

    /** One alarm for the next prayer; PrayerReceiver sets the following one. */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, PrayerReceiver.class);
        long[] n = next(c);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        if (n == null || "off".equals(Noor.prefs(c).getString("adhan_mode", "ask"))) {
            am.cancel(PendingIntent.getBroadcast(c, 1, i, flags));
            return;
        }
        i.putExtra("idx", (int) n[0]);
        i.putExtra("at", n[1]);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, i, flags);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n[1], pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n[1], pi);
    }

    static void refreshAndSchedule(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(() -> { refresh(app, false); schedule(app); }).start();
    }
}
