# Trainingsbuch

Android-App zum Abhaken deines Trainings: Krafttraining zuhause mit Widerstandsbändern, Laufen und eine tägliche Morgenroutine. Dazu kommen Timer und ein KI-Coach.

**Aktuelle Version: 2.0**

## Funktionen

- **Wochenplan zum Abhaken:** jede Übung und jeder Satz einzeln, dazu eine „Heute“-Karte mit Fortschrittsring
- **Timer:** automatische Satzpause (60/90/120 s), Übungstimer für zeitbasierte Übungen und eine geführte Morgenroutine
- **Lauter Wechsel-Ton:** über die Wecker-Lautstärke. Läuft Musik, wird sie dabei kurz leiser gestellt
- **Countdown:** in den letzten 5 Sekunden ein leiserer Piep pro Sekunde
- **Dringende Benachrichtigungen** bei jedem Wechsel zwischen Übung und Pause, auch bei gesperrtem Handy
- **Eigene Trainingspläne:** anlegen, bearbeiten, kopieren und zwischen Plänen wechseln
- **KI-Coach** über Ollama, entweder mit der Ollama-Cloud und gemma4 oder mit dem eigenen Mac im WLAN

## Installieren

1. Die neueste `Trainingsbuch.apk` unter **Releases** herunterladen.
2. Auf dem Handy öffnen und die Installation aus dieser Quelle erlauben.
3. Warnt Play Protect, auf „Trotzdem installieren“ tippen.

Voraussetzung ist Android 8.0 oder neuer.

## Selbst bauen

Bei jedem Push auf `main` baut GitHub Actions die APK automatisch. Du findest sie unter **Actions → APK bauen → Artifacts**.

Lokal mit Android SDK und Gradle 8.7:

```bash
gradle assembleRelease
```

## Aufbau

| Pfad | Inhalt |
|---|---|
| `app/src/main/assets/index.html` | Komplette Oberfläche (HTML/CSS/JS) |
| `app/src/main/java/de/trainingsbuch/app/` | Nativer Teil: WebView, Timer-Dienst, Töne mit Ducking, Benachrichtigungen |
| `app/src/main/res/` | App-Icon, Farben, Theme |
| `web/trainingsbuch.html` | Web-Version 1.0 (Claude-Artifact) |

---

© 2026 Benedikt Diel · Created by Claude · Version 2.0
