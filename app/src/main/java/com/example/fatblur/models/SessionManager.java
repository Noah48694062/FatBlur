package com.example.fatblur.models;

import android.content.Context;

public class SessionManager {
    private static final String PREF_NAME = "SecretLoveSession";
    private static final String KEY_SESSION_ID = "current_session_id";

    public static void saveSessionId(Context context, String sessionId) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SESSION_ID, sessionId).apply();
    }

    public static String getSessionId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSION_ID, "");
    }
}
