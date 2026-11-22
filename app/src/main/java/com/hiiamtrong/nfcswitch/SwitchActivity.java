package com.hiiamtrong.nfcswitch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
        String action = String.valueOf(getIntent().getAction());
        boolean off = !Wallet.ready();
        if (off && Wallet.canAutoStart(this)) Toast.makeText(this, R.string.toast_starting_shizuku, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            if (off && !Wallet.startShizuku(this)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_need_shizuku, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
                return;
            }
            if (action.equals(ACTION_OPEN)) {
                openCurrent();
                return;
            }
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
                Wallet.feedback(this, p, err);
                finish();
            });
        }).start();
    }

    /** Opens the app of the wallet that is currently the default. */
    private void openCurrent() {
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
    }
}
