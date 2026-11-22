package com.hiiamtrong.nfcswitch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

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
    static final String ACCESS_CARD = "com.vivo.seclient";
    static final String GOOGLE = "com.google.android.gms";
    private static final String GOOGLE_WALLET_APP = "com.google.android.apps.walletnfcrel";
    private static final String ROLE = "android.app.role.WALLET";
    private static final String SHIZUKU = "moe.shizuku.privileged.api";
    private static final long ATTACH_WAIT_MS = 3_000;
    private static final long START_TIMEOUT_MS = 20_000; // wireless debugging start takes several seconds
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
        return prefs(c).getString("slot" + i, i == 0 ? vivoWallet(c) : GOOGLE);
    }

    /** vivo's own wallet app: vivo Wallet on OriginOS (China), Access Card on Funtouch OS (Global). */
    private static String vivoWallet(Context c) {
        return (installed(c, VIVO) || !installed(c, ACCESS_CARD)) ? VIVO : ACCESS_CARD;
    }

    private static boolean installed(Context c, String pkg) {
        try {
            c.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static void setSlot(Context c, int i, String pkg) {
        prefs(c).edit().putString("slot" + i, pkg).apply();
    }

    /** The vivo wallet apps that are installed (cards live on the eSE, no HCE service) plus every app with an HCE service declaring the payment category. */
    static List<String> candidates(Context c) {
        PackageManager pm = c.getPackageManager();
        Set<String> pkgs = new LinkedHashSet<>();
        for (String vivo : new String[] {VIVO, ACCESS_CARD}) {
            if (installed(c, vivo)) pkgs.add(vivo);
        }
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

    /** Code of the Shizuku fork (View intents, Extras) that authorizes the START broadcast. */
    static String authToken(Context c) {
        return prefs(c).getString("auth", "");
    }

    static boolean canAutoStart(Context c) {
        return !authToken(c).isEmpty();
    }

    static void setAuthToken(Context c, String token) {
        prefs(c).edit().putString("auth", token.trim()).apply();
    }

    /** Asks the Shizuku fork to start itself. Only the thedjchi fork has this receiver, and it rejects a START without its code. */
    static void requestStart(Context c) {
        String token = authToken(c);
        if (token.isEmpty()) return;
        c.sendBroadcast(new Intent("moe.shizuku.privileged.api.START").setPackage(SHIZUKU).putExtra("auth", token));
    }

    /** Blocking. Starts Shizuku if needed and waits until the app can use it. */
    static boolean startShizuku(Context c) {
        boolean running = Shizuku.pingBinder();
        if (!running) {
            if (!canAutoStart(c)) return false;
            requestStart(c);
        }
        // A running Shizuku only needs a moment to attach this process, a stopped one needs a full start.
        try {
            return Wait.until(Wallet::ready, running ? ATTACH_WAIT_MS : START_TIMEOUT_MS, 500,
                    SystemClock::elapsedRealtime, Thread::sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static boolean ready() {
        if (!Shizuku.pingBinder()) return false;
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (IllegalStateException e) { // seen in logcat: "Not an attached client" while Shizuku is up but has not attached this process yet
            return false;
        }
    }

    /** UI thread only. Confirms a switch with a haptic tick and a toast, or shows the error. */
    static void feedback(Context c, String pkg, String error) {
        c.getSystemService(Vibrator.class).vibrate(VibrationEffect.createPredefined(
                error == null ? VibrationEffect.EFFECT_CLICK : VibrationEffect.EFFECT_DOUBLE_CLICK));
        Toast.makeText(c, error == null
                ? c.getString(R.string.toast_switched_fmt, name(c, pkg))
                : c.getString(R.string.toast_failed_fmt, error), Toast.LENGTH_SHORT).show();
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
