package app.noor.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After a reboot or an update: start the wake watcher, show a verse once Home is up, plan the next prayer. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(i.getAction());
        c.startForegroundService(new Intent(c, WakeService.class).putExtra("boot", boot));
        Prayer.refreshAndSchedule(c);
    }
}
