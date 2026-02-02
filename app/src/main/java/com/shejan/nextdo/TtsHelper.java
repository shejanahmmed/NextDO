package com.shejan.nextdo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

public class TtsHelper {
    private static final String TAG = "TtsHelper";
    private static final String PREFS_NAME = "TtsPreferences";
    private static final String KEY_PROMPT_DISMISSED = "tts_prompt_dismissed";
    private static final String GOOGLE_TTS_PACKAGE = "com.google.android.tts";

    /**
     * Check if a male English voice is available on the device
     */
    public static void checkMaleVoiceAvailability(Context context, VoiceCheckCallback callback) {
        final TextToSpeech[] ttsWrapper = new TextToSpeech[1];
        ttsWrapper[0] = new TextToSpeech(context, status -> {
            boolean hasMaleVoice = false;

            if (status == TextToSpeech.SUCCESS) {
                TextToSpeech tts = ttsWrapper[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        java.util.Set<android.speech.tts.Voice> voices = tts.getVoices();
                        if (voices != null) {
                            for (android.speech.tts.Voice voice : voices) {
                                if (voice.getLocale().getLanguage().equals("en")) {
                                    String voiceName = voice.getName().toLowerCase();
                                    if (voiceName.contains("male") && !voiceName.contains("female")) {
                                        hasMaleVoice = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking voices: " + e.getMessage());
                    }
                }
            }

            final boolean result = hasMaleVoice;
            ttsWrapper[0].shutdown();
            callback.onResult(result);
        });
    }

    /**
     * Check if Google TTS is installed
     */
    public static boolean isGoogleTtsInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(GOOGLE_TTS_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if user has dismissed the TTS installation prompt
     */
    public static boolean isPromptDismissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PROMPT_DISMISSED, false);
    }

    /**
     * Mark the TTS installation prompt as dismissed
     */
    public static void setPromptDismissed(Context context, boolean dismissed) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PROMPT_DISMISSED, dismissed).apply();
    }

    /**
     * Show dialog prompting user to install Google TTS
     */
    public static void promptInstallTts(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Install Voice for Reminders?")
                .setMessage(
                        "For the best voice reminder experience, we recommend installing Google Text-to-Speech. This adds high-quality male and female voices to your device.")
                .setPositiveButton("Install", (dialog, which) -> {
                    openGoogleTtsInPlayStore(context);
                    setPromptDismissed(context, true);
                })
                .setNegativeButton("Maybe Later", (dialog, which) -> {
                    setPromptDismissed(context, true);
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Open Google TTS in Play Store
     */
    private static void openGoogleTtsInPlayStore(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + GOOGLE_TTS_PACKAGE));
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to browser if Play Store not available
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + GOOGLE_TTS_PACKAGE));
                context.startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Could not open Play Store: " + ex.getMessage());
            }
        }
    }

    /**
     * Callback interface for voice availability check
     */
    public interface VoiceCheckCallback {
        void onResult(boolean hasMaleVoice);
    }
}
