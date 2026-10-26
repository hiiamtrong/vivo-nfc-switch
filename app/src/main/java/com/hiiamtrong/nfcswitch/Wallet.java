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
 * Đổi ví NFC bằng cách đổi holder của role WALLET, chạy lệnh `cmd role` với quyền shell qua Shizuku.
 * Hai ví đổi cho nhau (slot 0 và 1) do người dùng chọn, lưu trong SharedPreferences.
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

    // ---- cấu hình hai ví ----

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("wallet", Context.MODE_PRIVATE);
    }

    static String slot(Context c, int i) {
        return prefs(c).getString("slot" + i, i == 0 ? VIVO : GOOGLE);
    }

    static void setSlot(Context c, int i, String pkg) {
        prefs(c).edit().putString("slot" + i, pkg).apply();
    }

    /** vivo Wallet (thẻ nằm trên eSE, không có service HCE) và mọi app có service HCE khai báo category payment. */
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
            } catch (XmlPullParserException | IOException ignored) { // XML của app khác hỏng thì coi như không phải ví
            }
        }
        return false;
    }

    // ---- hiển thị ----

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
            return pm.getApplicationIcon(GOOGLE.equals(pkg) ? GOOGLE_WALLET_APP : pkg); // icon của gms là mảnh ghép, không phải Google Pay
        } catch (PackageManager.NameNotFoundException e) {
            return pm.getDefaultActivityIcon();
        }
    }

    /** Intent mở ví {@code pkg}: vivo mở thẳng màn quẹt thẻ (màn vivo mở khi bấm nguồn 2 lần), ví khác mở màn chính. Null nếu không mở được. */
    static Intent openIntent(Context c, String pkg) {
        if (VIVO.equals(pkg)) return new Intent("vivo.intent.action.NFC_BUS_SWINGSPLASH").setPackage(VIVO);
        return c.getPackageManager().getLaunchIntentForPackage(GOOGLE.equals(pkg) ? GOOGLE_WALLET_APP : pkg);
    }

    // ---- đổi ví (chặn luồng: gọi từ thread nền) ----

    static boolean ready() {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    /** Package đang giữ role WALLET, hoặc chuỗi rỗng nếu chưa có. */
    static String holder() throws Exception {
        return run("cmd", "role", "get-role-holders", ROLE);
    }

    /** Đặt thẳng ví của ô {@code slot} làm mặc định. Trả về package vừa đặt. */
    static String use(Context c, int slot) throws Exception {
        String pkg = slot(c, slot);
        run("cmd", "role", "add-role-holder", "--user", "0", ROLE, pkg);
        return pkg;
    }

    /** Sang ví còn lại của cặp; đang ở ví ngoài cặp thì sang ví thứ nhất. Trả về package vừa đặt. */
    static String toggle(Context c) throws Exception {
        return use(c, holder().equals(slot(c, 0)) ? 1 : 0);
    }

    // shortcut: Shizuku.newProcess là private từ API 13.1, gọi bằng reflection; nếu bản Shizuku sau gỡ thì chuyển sang UserService.
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
