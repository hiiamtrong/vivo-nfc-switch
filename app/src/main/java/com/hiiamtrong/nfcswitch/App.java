package com.hiiamtrong.nfcswitch;

import android.app.Application;
import android.app.LocaleManager;
import android.content.SharedPreferences;
import android.os.LocaleList;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // First run: default to Vietnamese instead of the phone language; the user can change it in Settings.
        SharedPreferences p = getSharedPreferences("app", MODE_PRIVATE);
        if (!p.getBoolean("lang_init", false)) {
            p.edit().putBoolean("lang_init", true).apply();
            LocaleManager lm = getSystemService(LocaleManager.class);
            if (lm.getApplicationLocales().isEmpty()) lm.setApplicationLocales(LocaleList.forLanguageTags("vi"));
        }
    }
}
