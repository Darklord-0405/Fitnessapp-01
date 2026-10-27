package de.trainingsbuch.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

/** Erzeugt die Pieptöne selbst und senkt währenddessen andere Audio-Apps ab (Ducking). */
public class Beeper {
    private final AudioManager am;
    private final Handler h;
    private boolean alarm = true, duck = true;
    private int vol = 100;
    private AudioFocusRequest focus;
    private boolean ducked = false;
    private long duckUntil = 0;

    Beeper(Context c, Handler h) {
        this.am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        this.h = h;
    }

    void set(boolean alarm, int vol, boolean duck) {
        this.alarm = alarm;
        this.vol = Math.max(10, Math.min(100, vol));
        this.duck = duck;
    }

    private AudioAttributes attrs() {
        return new AudioAttributes.Builder()
                .setUsage(alarm ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    private final Runnable releaser = new Runnable() {
        @Override public void run() {
            if (ducked && System.currentTimeMillis() >= duckUntil - 30) {
                try { am.abandonAudioFocusRequest(focus); } catch (Throwable e) { }
                ducked = false;
            }
        }
    };

    void duckFor(long ms) {
        if (!duck) return;
        long until = System.currentTimeMillis() + ms;
        if (until > duckUntil) duckUntil = until;
        if (!ducked) {
            try {
                focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attrs()).build();
                ducked = am.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            } catch (Throwable e) { ducked = false; }
        }
        h.postDelayed(releaser, ms + 60);
    }

    private void play(double freq, int ms, int count, int gapMs, double level) {
        try {
            final int sr = 44100;
            int toneN = sr * ms / 1000, gapN = sr * gapMs / 1000;
            int total = count * toneN + (count - 1) * gapN;
            short[] buf = new short[total];
            double amp = 32000.0 * level * vol / 100.0;
            int fade = sr * 6 / 1000;
            for (int k = 0; k < count; k++) {
                int off = k * (toneN + gapN);
                for (int n = 0; n < toneN; n++) {
                    double env = 1.0;
                    if (n < fade) env = n / (double) fade;
                    else if (n > toneN - fade) env = (toneN - n) / (double) fade;
                    double t = n / (double) sr;
                    double s = 0.8 * Math.sin(2 * Math.PI * freq * t) + 0.2 * Math.sin(4 * Math.PI * freq * t);
                    buf[off + n] = (short) Math.max(-32767, Math.min(32767, amp * env * s));
                }
            }
            final AudioTrack tr = new AudioTrack.Builder()
                    .setAudioAttributes(attrs())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sr)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(total * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            tr.write(buf, 0, total);
            tr.play();
            h.postDelayed(new Runnable() {
                @Override public void run() {
                    try { tr.stop(); } catch (Throwable e) { }
                    tr.release();
                }
            }, total * 1000L / sr + 400);
        } catch (Throwable e) { }
    }

    /** Countdown-Piep: kurz und etwas leiser. */
    void soft() { duckFor(1600); play(880, 120, 1, 0, 0.55); }

    /** Wechsel-Ton: drei kräftige, höhere Pieps. */
    void loud() { duckFor(2400); play(1175, 230, 3, 110, 1.0); }
}
