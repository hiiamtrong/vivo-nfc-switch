package com.hiiamtrong.nfcswitch;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: one tap switches the NFC wallet. Without Shizuku it opens the system wallet picker. */
public class SwitchTile extends TileService {

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
            try {
                Wallet.toggle(this);
            } catch (Exception ignored) { // refresh() below shows the real state, even when the switch failed
            }
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
                render(Wallet.name(this, Wallet.holder()), Tile.STATE_ACTIVE);
            } catch (Exception e) {
                render(getString(R.string.tile_error), Tile.STATE_INACTIVE);
            }
        }).start();
    }

    private void render(String subtitle, int state) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(getString(R.string.tile_label));
        tile.setSubtitle(subtitle);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_swap));
        tile.setState(state);
        tile.updateTile();
    }
}
