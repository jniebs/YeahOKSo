# YeahOKSo

Single-screen Android app (Kotlin, Jetpack Compose). Male/Female toggle plus three buttons:
So (says "So" 95% of the time, "OK" 5%), Yeah / OK ("Yeah so" or "OK so", 50/50), and Yeah OK So ("Yeah OK so" or "OK yeah so", 50/50). Speaks with the phone's built-in text-to-speech.

Build: open in Android Studio, sync, run on a device.

Voices: uses an installed English voice that reports the chosen gender when available,
otherwise the default voice with a pitch shift (see PITCH_MALE / PITCH_FEMALE in MainActivity.kt).
