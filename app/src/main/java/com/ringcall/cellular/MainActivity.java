package com.ringcall.cellular;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 10;

    private Prefs prefs;
    private Switch switchEnable;
    private TextView txtStatus;
    private Button btnStart, btnEnd, btnPermDnd, btnPermOverlay, btnPermPhone;
    private LinearLayout listNumbers;
    private EditText editNumber;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        prefs = Prefs.get(this);

        switchEnable = findViewById(R.id.switchEnable);
        txtStatus = findViewById(R.id.txtStatus);
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnPermDnd = findViewById(R.id.btnPermDnd);
        btnPermOverlay = findViewById(R.id.btnPermOverlay);
        btnPermPhone = findViewById(R.id.btnPermPhone);
        listNumbers = findViewById(R.id.listNumbers);
        editNumber = findViewById(R.id.editNumber);

        // --- enable switch
        switchEnable.setChecked(prefs.isEnabled());
        switchEnable.setOnCheckedChangeListener((b, on) -> {
            prefs.setEnabled(on);
            if (on) {
                requestMissingPermissions();
                RingControlService.start(this);
            } else {
                RingControlService.stop(this);
            }
            RingerController.apply(this);
            refreshUi();
        });

        // --- schedule
        btnStart.setOnClickListener(v -> pickTime(true));
        btnEnd.setOnClickListener(v -> pickTime(false));

        RadioGroup groupMode = findViewById(R.id.groupMode);
        groupMode.check(Prefs.MODE_VIBRATE.equals(prefs.getMode()) ? R.id.radioVibrate : R.id.radioSilent);
        groupMode.setOnCheckedChangeListener((g, id) -> {
            prefs.setMode(id == R.id.radioVibrate ? Prefs.MODE_VIBRATE : Prefs.MODE_SILENT);
            RingerController.apply(this);
        });

        // --- allowed numbers
        findViewById(R.id.btnAdd).setOnClickListener(v -> addNumber());

        // --- red dot
        CheckBox checkDot = findViewById(R.id.checkDot);
        checkDot.setChecked(prefs.isDotEnabled());
        checkDot.setOnCheckedChangeListener((b, on) -> {
            prefs.setDotEnabled(on);
            if (on && !Settings.canDrawOverlays(this)) openOverlaySettings();
            RingControlService.refresh(this);
        });
        SeekBar seek = findViewById(R.id.seekOffset);
        seek.setProgress(prefs.getDotOffsetDp());
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                if (!fromUser) return;
                prefs.setDotOffsetDp(value);
                RingControlService.refresh(MainActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // --- language
        RadioGroup groupLang = findViewById(R.id.groupLang);
        groupLang.check("en".equals(prefs.getLanguage()) ? R.id.radioEn : R.id.radioHe);
        groupLang.setOnCheckedChangeListener((g, id) -> {
            String lang = id == R.id.radioEn ? "en" : "iw";
            if (lang.equals(prefs.getLanguage())) return;
            prefs.setLanguage(lang);
            if (prefs.isEnabled()) { // restart so the notification uses the new language
                RingControlService.stop(this);
                RingControlService.start(this);
            }
            recreate();
        });

        // --- permissions
        btnPermDnd.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        btnPermOverlay.setOnClickListener(v -> openOverlaySettings());
        btnPermPhone.setOnClickListener(v -> requestRuntimePermissions());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.isEnabled()) RingControlService.start(this);
        RingerController.apply(this);
        refreshUi();
    }

    private void refreshUi() {
        btnStart.setText(getString(R.string.start_time, RingerController.formatTime(prefs.getStartMinutes())));
        btnEnd.setText(getString(R.string.end_time, RingerController.formatTime(prefs.getEndMinutes())));

        if (!prefs.isEnabled()) txtStatus.setText(R.string.status_disabled);
        else if (RingerController.isInQuietHours(prefs)) txtStatus.setText(R.string.status_active);
        else txtStatus.setText(R.string.status_idle);

        setPermButton(btnPermDnd, R.string.perm_dnd, RingerController.hasDndAccess(this));
        setPermButton(btnPermOverlay, R.string.perm_overlay, Settings.canDrawOverlays(this));
        setPermButton(btnPermPhone, R.string.perm_phone, hasPhonePermissions());

        renderNumbers();
    }

    private void setPermButton(Button b, int label, boolean granted) {
        b.setText(granted ? getString(label) + "  " + getString(R.string.perm_ok) : getString(label));
        b.setEnabled(!granted);
    }

    private void pickTime(boolean start) {
        int cur = start ? prefs.getStartMinutes() : prefs.getEndMinutes();
        new TimePickerDialog(this, (view, h, m) -> {
            if (start) prefs.setStartMinutes(h * 60 + m);
            else prefs.setEndMinutes(h * 60 + m);
            RingerController.apply(this);
            RingControlService.refresh(this);
            refreshUi();
        }, cur / 60, cur % 60, DateFormat.is24HourFormat(this)).show();
    }

    // ------------------------------------------------------------------ allowed numbers

    private void addNumber() {
        String n = editNumber.getText().toString().trim();
        if (PhoneNumbers.digitsOnly(n).length() < 3) {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
            return;
        }
        Set<String> set = prefs.getWhitelist();
        set.add(n);
        prefs.setWhitelist(set);
        editNumber.setText("");
        if (!hasPhonePermissions()) requestRuntimePermissions();
        renderNumbers();
    }

    private void renderNumbers() {
        listNumbers.removeAllViews();
        Set<String> set = prefs.getWhitelist();
        if (set.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.whitelist_empty);
            empty.setPadding(0, 8, 0, 8);
            listNumbers.addView(empty);
            return;
        }
        for (String number : set) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tv = new TextView(this);
            tv.setText(number);
            tv.setTextSize(16);
            tv.setTextDirection(View.TEXT_DIRECTION_LTR);
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button rm = new Button(this);
            rm.setText(R.string.remove);
            rm.setOnClickListener(v -> {
                Set<String> s = prefs.getWhitelist();
                s.remove(number);
                prefs.setWhitelist(s);
                renderNumbers();
            });
            row.addView(rm);
            listNumbers.addView(row);
        }
    }

    // ------------------------------------------------------------------ permissions

    private boolean hasPhonePermissions() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRuntimePermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.READ_PHONE_STATE);
        perms.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(perms.toArray(new String[0]), REQ_PERMS);
    }

    private void requestMissingPermissions() {
        if (!RingerController.hasDndAccess(this)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } else if (prefs.isDotEnabled() && !Settings.canDrawOverlays(this)) {
            openOverlaySettings();
        } else if (!hasPhonePermissions()) {
            requestRuntimePermissions();
        }
    }

    private void openOverlaySettings() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshUi();
    }
}
