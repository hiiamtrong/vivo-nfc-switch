package com.hiiamtrong.nfcswitch;

import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
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
        boolean off = !Wallet.ready();
        render(getString(off && Wallet.canAutoStart(this) ? R.string.toast_starting_shizuku : R.string.switching), Tile.STATE_UNAVAILABLE);
        new Thread(() -> {
            String pkg = null;
            String error = null;
            try {
                if (off && !Wallet.startShizuku(this)) throw new IllegalStateException(getString(R.string.toast_need_shizuku));
                pkg = Wallet.toggle(this);
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            String p = pkg;
            String err = error;
            main.post(() -> Wallet.feedback(this, p, err)); // the control center shows no animation for a tile tap
            refresh();
        }).start();
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
