package app.noor.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/** A prayer time arrived: show the card if the TV is on (never wakes a TV in standby), then plan the next one. */
public class PrayerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        String mode = Noor.prefs(c).getString("adhan_mode", "ask");
        long at = i.getLongExtra("at", 0);
        boolean fresh = Math.abs(System.currentTimeMillis() - at) < 10 * 60_000;
        if (!"off".equals(mode) && pm.isInteractive() && fresh) {
            c.startActivity(new Intent(c, AdhanActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("idx", i.getIntExtra("idx", 0)).putExtra("at", at));
        }
        final PendingResult pr = goAsync();
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            Prayer.refresh(app, false);
            Prayer.schedule(app);
            pr.finish();
        }).start();
    }
}
