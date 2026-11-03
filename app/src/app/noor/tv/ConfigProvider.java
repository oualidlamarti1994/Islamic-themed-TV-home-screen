package app.noor.tv;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.util.Base64;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Set up from the computer over ADB, like CleanMyTV Home:
 *   content call --uri content://app.noor.tv.config --method get
 *   content call --uri content://app.noor.tv.config --method set64 --arg <base64 JSON>
 *   content call --uri content://app.noor.tv.config --method refresh
 * Keys: city, lat, lon, method, wake_on, wake_seconds, friday_on, adhan_mode (ask|silent|off), kahf_url, adhan_url, reciter.
 * Only the shell user (ADB) and root may call it.
 */
public class ConfigProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        if (uid != 2000 && uid != 0 && uid != Process.myUid()) throw new SecurityException("only ADB may configure Noor");
        SharedPreferences p = Noor.prefs(getContext());
        try {
            if ("set64".equals(method)) {
                JSONObject o = new JSONObject(new String(Base64.decode(arg, Base64.DEFAULT), "UTF-8"));
                SharedPreferences.Editor e = p.edit();
                for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                    String k = it.next();
                    Object v = o.get(k);
                    if (k.equals("lat") || k.equals("lon")) e.putFloat(k, (float) o.getDouble(k));
                    else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
                    else if (v instanceof Integer) e.putInt(k, (Integer) v);
                    else e.putString(k, String.valueOf(v));
                }
                // location or method changed: cached times are wrong now
                if (o.has("lat") || o.has("method")) for (String k : p.getAll().keySet()) if (k.startsWith("t_")) e.remove(k);
                e.commit();
                Prayer.refreshAndSchedule(getContext());
                WakeService.start(getContext());
            } else if ("refresh".equals(method)) {
                Prayer.refresh(getContext(), true);
                Prayer.schedule(getContext());
            } else if ("show".equals(method)) {
                getContext().startActivity(VerseActivity.show(getContext()).putExtra("quiet", "quiet".equals(arg)));
            } else if (!"get".equals(method)) {
                throw new IllegalArgumentException("unknown method: " + method);
            }
            JSONObject out = new JSONObject();
            for (Map.Entry<String, ?> en : p.getAll().entrySet())
                if (!en.getKey().startsWith("t_")) out.put(en.getKey(), en.getValue());
            String[] t = Prayer.times(getContext(), 0);
            if (t != null) out.put("today", String.join(" ", t));
            out.put("hijri", Prayer.hijriText(getContext()));
            Bundle b = new Bundle();
            b.putString("config", out.toString());
            return b;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
