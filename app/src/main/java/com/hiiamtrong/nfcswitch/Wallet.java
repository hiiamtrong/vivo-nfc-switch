package com.hiiamtrong.nfcswitch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rikka.shizuku.Shizuku;

/**
 * Switches the NFC wallet by changing the holder of the WALLET role, running `cmd role` with shell privileges through Shizuku.
 * The two wallets that swap (slot 0 and 1) are chosen by the user and stored in SharedPreferences.
 */
final class Wallet {
    static final String VIVO = "com.vivo.wallet";
    static final String GOOGLE = "com.google.android.gms";
    private static final String GOOGLE_WALLET_APP = "com.google.android.apps.walletnfcrel";
    private static final String ROLE = "android.app.role.WALLET";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String[] ACTIONS = {
            "android.nfc.cardemulation.action.HOST_APDU_SERVICE",
            "android.nfc.cardemulation.action.OFF_HOST_APDU_SERVICE"};
    private static final String[] META = {
            "android.nfc.cardemulation.host_apdu_service",
            "android.nfc.cardemulation.off_host_apdu_service"};

    private Wallet() {}

    // ---- the two-wallet configuration ----

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("wallet", Context.MODE_PRIVATE);
    }

    static String slot(Context c, int i) {
        return prefs(c).getString("slot" + i, i == 0 ? VIVO : GOOGLE);
    }

    static void setSlot(Context c, int i, String pkg) {
        prefs(c).edit().putString("slot" + i, pkg).apply();
    }

    /** vivo Wallet (cards live on the eSE, no HCE service) plus every app with an HCE service declaring the payment category. */
    static List<String> candidates(Context c) {
        PackageManager pm = c.getPackageManager();
        Set<String> pkgs = new LinkedHashSet<>();
        pkgs.add(VIVO);
        for (String action : ACTIONS) {
            for (ResolveInfo ri : pm.queryIntentServices(new Intent(action), PackageManager.GET_META_DATA)) {
                if (isPayment(pm, ri.serviceInfo)) pkgs.add(ri.serviceInfo.packageName);
            }
        }
        pkgs.add(slot(c, 0));
        pkgs.add(slot(c, 1));
        return new ArrayList<>(pkgs);
    }

    private static boolean isPayment(PackageManager pm, ServiceInfo si) {
        for (String meta : META) {
            try (XmlResourceParser p = si.loadXmlMetaData(pm, meta)) {
                if (p == null) continue;
                for (int e = p.getEventType(); e != XmlResourceParser.END_DOCUMENT; e = p.next()) {
                    if (e == XmlResourceParser.START_TAG && "aid-group".equals(p.getName())
                            && "payment".equals(p.getAttributeValue(ANDROID_NS, "category"))) {
                        return true;
                    }
                }
            } catch (XmlPullParserException | IOException ignored) { // malformed XML in another app means it is not a wallet
            }
        }
        return false;
    }

    // ---- display ----

    static String name(Context c, String pkg) {
        if (VIVO.equals(pkg)) return "vivo Wallet";
        if (GOOGLE.equals(pkg)) return "Google Pay";
        try {
            return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return String.valueOf(pkg);
        }
    }

    static Drawable icon(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        try {
            return pm.getApplicationIcon(GOOGLE.equals(pkg) ? GOOGLE_WALLET_APP : pkg); // the gms icon is a puzzle piece, not Google Pay
        } catch (PackageManager.NameNotFoundException e) {
            return pm.getDefaultActivityIcon();
        }
    }

    /** Intent that opens wallet {@code pkg}: vivo opens its swipe screen (the one vivo shows on a power double press), others open their main screen. Null if it cannot be opened. */
    static Intent openIntent(Context c, String pkg) {
        if (VIVO.equals(pkg)) return new Intent("vivo.intent.action.NFC_BUS_SWINGSPLASH").setPackage(VIVO);
        return c.getPackageManager().getLaunchIntentForPackage(GOOGLE.equals(pkg) ? GOOGLE_WALLET_APP : pkg);
    }

    // ---- switching (blocking: call from a background thread) ----

    static boolean ready() {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    /** Package currently holding the WALLET role, or an empty string if none. */
    static String holder() throws Exception {
        return run("cmd", "role", "get-role-holders", ROLE);
    }

    /** Makes the wallet of {@code slot} the default. Returns the package just set. */
    static String use(Context c, int slot) throws Exception {
        String pkg = slot(c, slot);
        run("cmd", "role", "add-role-holder", "--user", "0", ROLE, pkg);
        return pkg;
    }

    /** Moves to the other wallet of the pair; from a wallet outside the pair it goes to the first. Returns the package just set. */
    static String toggle(Context c) throws Exception {
        return use(c, holder().equals(slot(c, 0)) ? 1 : 0);
    }

    // shortcut: Shizuku.newProcess is private since API 13.1 and called by reflection, move to a UserService if a later Shizuku removes it.
    private static String run(String... cmd) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        Process p = (Process) m.invoke(null, cmd, null, null);
        String out = new String(p.getInputStream().readAllBytes()).trim();
        String err = new String(p.getErrorStream().readAllBytes()).trim();
        if (p.waitFor() != 0) throw new IOException(err.isEmpty() ? out : err);
        return out;
    }
}
