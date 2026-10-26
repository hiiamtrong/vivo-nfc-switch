package com.hiiamtrong.nfcswitch;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int ACCENT = 0xFF2DD4BF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xB3FFFFFF;
    private static final int PANEL = 0x14FFFFFF;
    private static final long ANIM_MS = 500;

    private LinearLayout content;
    private LinearLayout stage;
    private final Card[] cards = new Card[2];
    private FrameLayout swapButton;
    private ImageView swapIcon;
    private TextView status;
    private TextView hint;
    private LinearLayout banner;
    private TextView bannerText;
    private Button bannerButton;
    private final TextView[] slotValue = new TextView[2];
    private TextView languageValue;
    private TextView shortcutHint;

    private String current = "";
    private boolean busy;
    private ObjectAnimator spin;

    private final Shizuku.OnRequestPermissionResultListener onPermission = (code, result) -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderReceivedListener onBinder = () -> runOnUiThread(this::refresh);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0B1220, 0xFF0F2F33});
        scroll.setBackground(bg);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        // targetSdk 35 vẽ tràn dưới thanh hệ thống, nên đệm theo insets để nội dung không bị che.
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(pad, pad + bars.top, pad, pad + bars.bottom);
            return insets;
        });
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        buildHeader();
        buildBanner();
        buildStage();
        buildSettings();
        buildFooter();

        Shizuku.addRequestPermissionResultListener(onPermission);
        Shizuku.addBinderReceivedListener(onBinder);
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(onPermission);
        Shizuku.removeBinderReceivedListener(onBinder);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    // ---- dựng giao diện ----

    private void buildHeader() {
        content.addView(label(getString(R.string.app_name), 28, TEXT, true));
        content.addView(label(getString(R.string.header_sub), 14, TEXT_DIM, false), lp(0, 4, 0, 24));
    }

    private void buildBanner() {
        banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(16), dp(14), dp(16), dp(14));
        banner.setBackground(round(0x33F59E0B, 0, 16));
        bannerText = label("", 14, TEXT, false);
        bannerButton = new Button(this);
        bannerButton.setAllCaps(false);
        banner.addView(bannerText);
        banner.addView(bannerButton);
        banner.setVisibility(View.GONE);
        content.addView(banner, lp(0, 0, 0, 20));
    }

    private void buildStage() {
        stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.HORIZONTAL);
        stage.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < 2; i++) {
            cards[i] = new Card(this);
        }
        swapButton = new FrameLayout(this);
        swapButton.setBackground(round(ACCENT, 0, 28));
        swapButton.setElevation(dp(6));
        swapIcon = new ImageView(this);
        swapIcon.setImageResource(R.drawable.ic_swap);
        swapIcon.setColorFilter(0xFF06201E);
        swapButton.addView(swapIcon, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        swapButton.setOnClickListener(v -> swap());

        LinearLayout.LayoutParams card = new LinearLayout.LayoutParams(0, dp(168), 1f);
        stage.addView(cards[0], card);
        LinearLayout.LayoutParams mid = new LinearLayout.LayoutParams(dp(56), dp(56));
        mid.setMargins(dp(-14), 0, dp(-14), 0); // nút đè lên mép hai thẻ
        stage.addView(swapButton, mid);
        stage.addView(cards[1], card);
        content.addView(stage);

        status = label("", 22, TEXT, true);
        status.setGravity(Gravity.CENTER);
        content.addView(status, lp(0, 24, 0, 0));
        hint = label("", 13, TEXT_DIM, false);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint, lp(0, 4, 0, 28));
    }

    private void buildSettings() {
        content.addView(label(getString(R.string.settings_header), 16, ACCENT, true), lp(0, 0, 0, 8));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(round(PANEL, 0, 16));
        for (int i = 0; i < 2; i++) {
            final int slot = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            TextView title = label(getString(i == 0 ? R.string.slot_first : R.string.slot_second), 15, TEXT, false);
            slotValue[i] = label("", 15, TEXT_DIM, false);
            slotValue[i].setGravity(Gravity.END);
            row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(slotValue[i]);
            row.addView(label("  ›", 18, TEXT_DIM, false));
            row.setOnClickListener(v -> pickWallet(slot));
            panel.addView(row);
        }
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setGravity(Gravity.CENTER_VERTICAL);
        langRow.setPadding(dp(16), dp(16), dp(16), dp(16));
        langRow.setBackgroundResource(android.R.drawable.list_selector_background);
        languageValue = label("", 15, TEXT_DIM, false);
        languageValue.setGravity(Gravity.END);
        langRow.addView(label(getString(R.string.language), 15, TEXT, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        langRow.addView(languageValue);
        langRow.addView(label("  ›", 18, TEXT_DIM, false));
        langRow.setOnClickListener(v -> pickLanguage());
        panel.addView(langRow);
        content.addView(panel);
        shortcutHint = label("", 13, TEXT_DIM, false);
        content.addView(shortcutHint, lp(4, 12, 4, 0));
    }

    private void buildFooter() {
        Button sys = new Button(this);
        sys.setAllCaps(false);
        sys.setText(R.string.open_system);
        sys.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)));
        content.addView(sys, lp(0, 20, 0, 0));
    }

    // ---- trạng thái ----

    private void refresh() {
        bindCards();
        if (!Shizuku.pingBinder()) {
            showBanner(getString(R.string.banner_no_shizuku), getString(R.string.btn_open_shizuku), this::openShizuku);
            setIdle(getString(R.string.idle_no_shizuku));
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            showBanner(getString(R.string.banner_no_permission), getString(R.string.btn_grant), () -> Shizuku.requestPermission(1));
            setIdle(getString(R.string.idle_no_permission));
        } else {
            banner.setVisibility(View.GONE);
            new Thread(() -> {
                try {
                    String cur = Wallet.holder();
                    runOnUiThread(() -> showCurrent(cur, false));
                } catch (Exception e) {
                    runOnUiThread(() -> setIdle(getString(R.string.error_fmt, e.getMessage())));
                }
            }).start();
        }
    }

    private void bindCards() {
        for (int i = 0; i < 2; i++) {
            String pkg = Wallet.slot(this, i);
            cards[i].bind(pkg);
            slotValue[i].setText(Wallet.name(this, pkg));
        }
        languageValue.setText(languageName(currentLanguage()));
        publishShortcuts();
    }

    private void setIdle(String text) {
        current = "";
        cards[0].setActive(false, false);
        cards[1].setActive(false, false);
        swapButton.setAlpha(0.4f);
        status.setText(text);
        hint.setText("");
    }

    private void showCurrent(String cur, boolean animate) {
        current = cur;
        swapButton.setAlpha(1f);
        cards[0].setActive(cur.equals(Wallet.slot(this, 0)), animate);
        cards[1].setActive(cur.equals(Wallet.slot(this, 1)), animate);
        status.setText(getString(R.string.using_fmt, cur.isEmpty() ? getString(R.string.none) : Wallet.name(this, cur)));
        boolean inPair = cur.equals(Wallet.slot(this, 0)) || cur.equals(Wallet.slot(this, 1));
        hint.setText(inPair ? R.string.hint_pair : R.string.hint_outside);
    }

    private void showBanner(String text, String action, Runnable onClick) {
        bannerText.setText(text);
        bannerButton.setText(action);
        bannerButton.setOnClickListener(v -> onClick.run());
        banner.setVisibility(View.VISIBLE);
    }

    private void openShizuku() {
        Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if (i != null) startActivity(i);
    }

    // ---- đổi ví + animation ----

    private void swap() {
        if (busy || !Wallet.ready()) return;
        busy = true;
        swapButton.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        spin = ObjectAnimator.ofFloat(swapIcon, View.ROTATION, swapIcon.getRotation(), swapIcon.getRotation() + 360f);
        spin.setDuration(700);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.start();
        status.setText(R.string.switching);
        long started = System.currentTimeMillis();
        new Thread(() -> {
            String result = null;
            String error = null;
            try {
                result = Wallet.toggle(this);
            } catch (Exception e) {
                error = String.valueOf(e.getMessage());
            }
            long wait = Math.max(0, 600 - (System.currentTimeMillis() - started)); // đủ lâu để mắt kịp thấy vòng xoay
            String r = result;
            String err = error;
            swapButton.postDelayed(() -> finishSwap(r, err), wait);
        }).start();
    }

    private void finishSwap(String result, String error) {
        spin.cancel();
        float settle = (float) Math.ceil(swapIcon.getRotation() / 180f) * 180f;
        swapIcon.animate().rotation(settle).setDuration(350).setInterpolator(new DecelerateInterpolator()).start();
        if (error == null) {
            showCurrent(result, true);
            swapButton.animate().scaleX(1.25f).scaleY(1.25f).setDuration(160)
                    .withEndAction(() -> swapButton.animate().scaleX(1f).scaleY(1f).setDuration(240)
                            .setInterpolator(new OvershootInterpolator(3f)).start()).start();
        } else {
            status.setText(getString(R.string.error_fmt, error));
            ObjectAnimator.ofFloat(stage, View.TRANSLATION_X, 0, -dp(14), dp(14), -dp(9), dp(9), 0).setDuration(400).start();
        }
        busy = false;
    }

    // ---- chọn ví cho từng ô ----

    private void pickWallet(int slot) {
        int otherSlot = 1 - slot;
        List<String> options = new ArrayList<>();
        for (String pkg : Wallet.candidates(this)) {
            if (!pkg.equals(Wallet.slot(this, otherSlot))) options.add(pkg);
        }
        String[] names = new String[options.size()];
        int checked = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = Wallet.name(this, options.get(i));
            if (options.get(i).equals(Wallet.slot(this, slot))) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(slot == 0 ? R.string.pick_first : R.string.pick_second)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    Wallet.setSlot(this, slot, options.get(which));
                    d.dismiss();
                    bindCards();
                    if (!current.isEmpty()) showCurrent(current, true);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---- ngôn ngữ ----

    private static final String[] LANG_TAGS = {"", "vi", "en"};

    private String currentLanguage() {
        LocaleList l = getSystemService(LocaleManager.class).getApplicationLocales();
        return l.isEmpty() ? "" : l.get(0).getLanguage();
    }

    private String languageName(String tag) {
        return getString(tag.equals("vi") ? R.string.lang_vi : tag.equals("en") ? R.string.lang_en : R.string.lang_system);
    }

    private void pickLanguage() {
        String[] names = new String[LANG_TAGS.length];
        int checked = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = languageName(LANG_TAGS[i]);
            if (LANG_TAGS[i].equals(currentLanguage())) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    // Hệ thống tự tạo lại activity với ngôn ngữ mới.
                    getSystemService(LocaleManager.class).setApplicationLocales(
                            LANG_TAGS[which].isEmpty() ? LocaleList.getEmptyLocaleList() : LocaleList.forLanguageTags(LANG_TAGS[which]));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---- shortcut (Smart Button, nhấn giữ icon) ----

    /** Shortcut "Dùng <tên ví>" cho từng ô; shortcut "Đổi ví" tĩnh nằm trong res/xml/shortcuts.xml. */
    private void publishShortcuts() {
        List<ShortcutInfo> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String name = Wallet.name(this, Wallet.slot(this, i));
            list.add(new ShortcutInfo.Builder(this, "slot" + i)
                    .setShortLabel(getString(R.string.shortcut_use_fmt, name))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher))
                    .setIntent(SwitchActivity.slotIntent(this, i))
                    .build());
        }
        getSystemService(ShortcutManager.class).setDynamicShortcuts(list);
        shortcutHint.setText(getString(R.string.shortcut_hint, getString(R.string.shortcut_toggle),
                getString(R.string.shortcut_use_fmt, Wallet.name(this, Wallet.slot(this, 0)))));
    }

    // ---- view phụ ----

    /** Thẻ ví: icon + tên, lớp "glow" bật sáng khi ví đang được dùng. */
    private final class Card extends FrameLayout {
        private final View glow;
        private final ImageView icon;
        private final TextView name;

        Card(Context c) {
            super(c);
            setBackground(round(PANEL, 0, 28));
            glow = new View(c);
            glow.setBackground(round(0x332DD4BF, ACCENT, 28));
            glow.setAlpha(0f);
            addView(glow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setPadding(dp(12), dp(12), dp(12), dp(12));
            icon = new ImageView(c);
            col.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
            name = label("", 14, TEXT, true);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            col.addView(name, lp(0, 10, 0, 0));
            addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            setActive(false, false);
        }

        void bind(String pkg) {
            icon.setImageDrawable(Wallet.icon(getContext(), pkg));
            name.setText(Wallet.name(getContext(), pkg));
        }

        void setActive(boolean on, boolean animate) {
            float scale = on ? 1f : 0.88f;
            float alpha = on ? 1f : 0.5f;
            if (!animate) {
                setScaleX(scale);
                setScaleY(scale);
                setAlpha(alpha);
                glow.setAlpha(on ? 1f : 0f);
                return;
            }
            animate().scaleX(scale).scaleY(scale).alpha(alpha).setDuration(ANIM_MS)
                    .setInterpolator(on ? new OvershootInterpolator(2f) : new DecelerateInterpolator()).start();
            glow.animate().alpha(on ? 1f : 0f).setDuration(ANIM_MS).start();
        }
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(2), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
