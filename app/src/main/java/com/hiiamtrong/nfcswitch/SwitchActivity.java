package com.hiiamtrong.nfcswitch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

/** No-UI target of the shortcuts: switch the wallet, give a haptic tick, report with a toast, then close. */
public class SwitchActivity extends Activity {
    static final String ACTION_SLOT_PREFIX = "com.hiiamtrong.nfcswitch.SLOT";
    private static final String ACTION_OPEN = "com.hiiamtrong.nfcswitch.OPEN";

    static Intent slotIntent(android.content.Context c, int slot) {
        return new Intent(ACTION_SLOT_PREFIX + slot).setClass(c, SwitchActivity.class);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Wallet.ready()) {
            Toast.makeText(this, R.string.toast_need_shizuku, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        String action = String.valueOf(getIntent().getAction());
        if (action.equals(ACTION_OPEN)) {
            openCurrent();
            return;
        }
        new Thread(() -> {
            String pkg = null;
            String error = null;
            try {
                if (action.equals(ACTION_SLOT_PREFIX + 0)) pkg = Wallet.use(this, 0);
                else if (action.equals(ACTION_SLOT_PREFIX + 1)) pkg = Wallet.use(this, 1);
                else pkg = Wallet.toggle(this);
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            String p = pkg;
            String err = error;
            runOnUiThread(() -> {
                getSystemService(Vibrator.class).vibrate(VibrationEffect.createPredefined(
                        err == null ? VibrationEffect.EFFECT_CLICK : VibrationEffect.EFFECT_DOUBLE_CLICK));
                Toast.makeText(this, err == null
                        ? getString(R.string.toast_switched_fmt, Wallet.name(this, p))
                        : getString(R.string.toast_failed_fmt, err), Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    /** Opens the app of the wallet that is currently the default. */
    private void openCurrent() {
        new Thread(() -> {
            Intent open = null;
            String error = null;
            try {
                open = Wallet.openIntent(this, Wallet.holder());
                if (open == null) error = getString(R.string.none);
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            Intent i = open;
            String err = error;
            runOnUiThread(() -> {
                if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                else Toast.makeText(this, getString(R.string.toast_failed_fmt, err), Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
