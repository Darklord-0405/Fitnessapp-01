package de.trainingsbuch.app;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;

public class Bridge {
    private final MainActivity a;
    private Beeper test;

    Bridge(MainActivity a) { this.a = a; }

    @JavascriptInterface
    public void schedule(String json) {
        try {
            JSONObject o = new JSONObject(json);
            JSONArray ph = o.optJSONArray("phases");
            Intent i = new Intent(a, TimerService.class);
            if (ph == null || ph.length() == 0) {
                if (TimerService.running) {
                    i.setAction(TimerService.ACTION_STOP);
                    a.startService(i);
                }
                return;
            }
            i.setAction(TimerService.ACTION_SCHEDULE);
            i.putExtra("json", json);
            a.startForegroundService(i);
        } catch (Throwable e) {
            // Timer läuft dann nur in der App weiter
        }
    }

    @JavascriptInterface
    public void testSound(final String json) {
        a.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject s = new JSONObject(json);
                    if (test == null) test = new Beeper(a, new Handler(Looper.getMainLooper()));
                    test.set(s.optBoolean("alarm", true), s.optInt("vol", 100), s.optBoolean("duck", true));
                    final boolean cd = s.optBoolean("countdown", true);
                    Handler h = new Handler(Looper.getMainLooper());
                    test.duckFor(4500);
                    if (cd) {
                        h.postDelayed(new Runnable() { public void run() { test.soft(); } }, 500);
                        h.postDelayed(new Runnable() { public void run() { test.soft(); } }, 1500);
                    }
                    h.postDelayed(new Runnable() { public void run() { test.loud(); } }, cd ? 2500 : 500);
                } catch (Throwable e) { }
            }
        });
    }

    @JavascriptInterface
    public boolean notificationsAllowed() {
        try {
            return a.getSystemService(NotificationManager.class).areNotificationsEnabled();
        } catch (Throwable e) { return false; }
    }

    @JavascriptInterface
    public void requestNotifications() {
        a.runOnUiThread(new Runnable() { public void run() { a.askNotif(); } });
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
        } catch (Throwable e) { }
    }

    @JavascriptInterface
    public void testNotification() {
        Notifs.alert(a, "Pause vorbei", "Weiter mit: Kniebeugen mit Band (Satz 2). So sieht die Meldung beim Wechsel aus.");
    }
}
