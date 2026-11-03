package app.noor.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** Keeps one SCREEN_ON receiver alive (Android only delivers it to receivers registered at run time).
 *  No polling, no wake lock. On wake it shows VerseActivity once. */
public class WakeService extends Service {
    private final Handler h = new Handler(Looper.getMainLooper());
    private BroadcastReceiver rx;

    static void start(Context c) {
        c.startForegroundService(new Intent(c, WakeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("noor", "Noor", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "noor").setSmallIcon(R.mipmap.icon)
                .setContentTitle("Noor · نور").setContentText("Verse on wake").build();
        startForeground(1, n);
        rx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) show(1500);
                else h.removeCallbacksAndMessages(null);
            }
        };
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(rx, f);
    }

    private void show(long delay) {
        h.postDelayed(() -> {
            long now = System.currentTimeMillis();
            if (!Noor.prefs(this).getBoolean("wake_on", true)) return;
            if (now - Noor.prefs(this).getLong("last_wake", 0) < 30_000) return;
            Noor.prefs(this).edit().putLong("last_wake", now).apply();
            startActivity(VerseActivity.show(this));
        }, delay);
        Prayer.refreshAndSchedule(this);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && i.getBooleanExtra("boot", false)) show(9000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (rx != null) unregisterReceiver(rx);
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
