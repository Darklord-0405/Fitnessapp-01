package de.trainingsbuch.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import org.json.JSONArray;
import org.json.JSONObject;

/** Läuft im Vordergrund, solange ein Timer aktiv ist: Countdown, Wechsel-Ton und dringende Meldung – auch bei gesperrtem Handy. */
public class TimerService extends Service {
    static final String ACTION_SCHEDULE = "de.trainingsbuch.app.SCHEDULE";
    static final String ACTION_STOP = "de.trainingsbuch.app.STOP";
    static volatile boolean running = false;

    private Handler sched;
    private Beeper beeper;
    private PowerManager.WakeLock wl;
    private boolean countdown = true, notify = true;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        Handler main = new Handler(Looper.getMainLooper());
        sched = new Handler(Looper.getMainLooper());
        beeper = new Beeper(this, main);
        Notifs.ensure(this);
    }

    private void goForeground(String label, long end) {
        Notification n = Notifs.ongoing(this, label, end);
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(Notifs.ID_RUN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(Notifs.ID_RUN, n);
        } catch (Throwable e) { }
        running = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (!ACTION_SCHEDULE.equals(action)) {
            stopAll();
            return START_NOT_STICKY;
        }
        goForeground("Timer läuft", System.currentTimeMillis());
        try {
            JSONObject o = new JSONObject(intent.getStringExtra("json"));
            JSONObject s = o.optJSONObject("settings");
            if (s == null) s = new JSONObject();
            beeper.set(s.optBoolean("alarm", true), s.optInt("vol", 100), s.optBoolean("duck", true));
            countdown = s.optBoolean("countdown", true);
            notify = s.optBoolean("notify", true);
            schedule(o.optJSONArray("phases"));
        } catch (Throwable e) {
            stopAll();
        }
        return START_NOT_STICKY;
    }

    private void at(long when, Runnable r) {
        long d = when - System.currentTimeMillis();
        sched.postDelayed(r, Math.max(0, d));
    }

    private void schedule(JSONArray ph) throws Exception {
        sched.removeCallbacksAndMessages(null);
        long now = System.currentTimeMillis();
        long last = 0;
        boolean first = true;
        int n = ph == null ? 0 : ph.length();
        for (int i = 0; i < n; i++) {
            JSONObject p = ph.getJSONObject(i);
            final long end = p.getLong("end");
            if (end < now - 300) continue;
            final String title = p.optString("title", "Zeit um");
            final String body = p.optString("body", "");
            final String nextLabel = i + 1 < n ? ph.getJSONObject(i + 1).optString("label", "") : null;
            final long nextEnd = i + 1 < n ? ph.getJSONObject(i + 1).optLong("end", 0) : 0;
            if (first) {
                ((NotificationManager) getSystemService(NotificationManager.class))
                        .notify(Notifs.ID_RUN, Notifs.ongoing(this, p.optString("label", "Timer läuft"), end));
                first = false;
            }
            at(end - (countdown ? 5400 : 450), new Runnable() { public void run() { beeper.duckFor(countdown ? 8000 : 3000); } });
            if (countdown) {
                for (int s = 5; s >= 1; s--) {
                    long t = end - s * 1000L;
                    if (t >= now - 100) at(t, new Runnable() { public void run() { beeper.soft(); } });
                }
            }
            at(end, new Runnable() {
                public void run() {
                    beeper.loud();
                    if (notify) Notifs.alert(TimerService.this, title, body);
                    if (nextLabel != null && nextEnd > 0) {
                        ((NotificationManager) getSystemService(NotificationManager.class))
                                .notify(Notifs.ID_RUN, Notifs.ongoing(TimerService.this, nextLabel, nextEnd));
                    }
                }
            });
            last = end;
        }
        if (last == 0) { stopAll(); return; }
        try {
            if (wl == null) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "trainingsbuch:timer");
                wl.setReferenceCounted(false);
            }
            wl.acquire(last - now + 15000);
        } catch (Throwable e) { }
        at(last + 3500, new Runnable() { public void run() { stopAll(); } });
    }

    private void stopAll() {
        sched.removeCallbacksAndMessages(null);
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Throwable e) { }
        running = false;
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (Throwable e) { }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        sched.removeCallbacksAndMessages(null);
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Throwable e) { }
        running = false;
        super.onDestroy();
    }
}
