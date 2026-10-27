package de.trainingsbuch.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Notifs {
    static final String CH_ALERT = "pausenwechsel";
    static final String CH_RUN = "timer_laeuft";
    static final int ID_RUN = 1, ID_ALERT = 2;

    static void ensure(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel a = new NotificationChannel(CH_ALERT, "Pausenwechsel (dringend)", NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("Meldung bei jedem Wechsel zwischen Übung und Pause");
        a.enableVibration(true);
        a.setVibrationPattern(new long[]{0, 300, 150, 300});
        a.setSound(null, null);
        a.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        a.setShowBadge(false);
        nm.createNotificationChannel(a);
        NotificationChannel r = new NotificationChannel(CH_RUN, "Laufender Timer", NotificationManager.IMPORTANCE_LOW);
        r.setDescription("Zeigt die Restzeit, solange ein Timer läuft");
        r.setShowBadge(false);
        nm.createNotificationChannel(r);
    }

    static int icon(Context c) {
        int id = c.getResources().getIdentifier("ic_stat", "drawable", c.getPackageName());
        return id != 0 ? id : android.R.drawable.ic_lock_idle_alarm;
    }

    static PendingIntent open(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void alert(Context c, String title, String body) {
        try {
            ensure(c);
            Notification n = new Notification.Builder(c, CH_ALERT)
                    .setSmallIcon(icon(c))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(open(c))
                    .setTimeoutAfter(90000)
                    .setColor(0xFF2457D6)
                    .build();
            c.getSystemService(NotificationManager.class).notify(ID_ALERT, n);
        } catch (Throwable e) { }
    }

    static Notification ongoing(Context c, String label, long end) {
        return new Notification.Builder(c, CH_RUN)
                .setSmallIcon(icon(c))
                .setContentTitle(label)
                .setContentText("Trainingsbuch-Timer läuft")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open(c))
                .setColor(0xFF2457D6)
                .setShowWhen(true)
                .setWhen(end)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .build();
    }
}
