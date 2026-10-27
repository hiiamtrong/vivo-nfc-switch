package com.hiiamtrong.nfcswitch;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** Quick Settings tile: one tap switches the NFC wallet. Without Shizuku it opens the system wallet picker. */
public class SwitchTile extends TileService {
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        if (!Wallet.ready()) {
            Intent i = new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE));
            return;
        }
        render(getString(R.string.switching), Tile.STATE_UNAVAILABLE);
        new Thread(() -> {
            String pkg = null;
            String error = null;
            try {
                pkg = Wallet.toggle(this);
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            String p = pkg;
            String err = error;
            main.post(() -> feedback(p, err));
            refresh();
        }).start();
    }

    /** The control center shows no animation for a tile tap, so confirm the result with a toast and a haptic tick. */
    private void feedback(String pkg, String error) {
        getSystemService(Vibrator.class).vibrate(VibrationEffect.createPredefined(
                error == null ? VibrationEffect.EFFECT_CLICK : VibrationEffect.EFFECT_DOUBLE_CLICK));
        Toast.makeText(this, error == null
                ? getString(R.string.toast_switched_fmt, Wallet.name(this, pkg))
                : getString(R.string.toast_failed_fmt, error), Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        new Thread(() -> {
            if (!Wallet.ready()) {
                render(getString(R.string.tile_no_shizuku), Tile.STATE_INACTIVE);
                return;
            }
            try {
                render(getString(R.string.tile_current_fmt, Wallet.name(this, Wallet.holder())), Tile.STATE_ACTIVE);
            } catch (Exception e) {
                render(getString(R.string.tile_error), Tile.STATE_INACTIVE);
            }
        }).start();
    }

    /** The wallet name goes on the label because some control centers do not show a tile subtitle. */
    private void render(String label, int state) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(label);
        tile.setSubtitle(getString(R.string.tile_label));
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_swap));
        tile.setState(state);
        tile.updateTile();
    }
}
