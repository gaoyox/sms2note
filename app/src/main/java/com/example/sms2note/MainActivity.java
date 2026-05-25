package com.example.sms2note;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SMS_PERMISSION = 100;
    private Switch switchEnable;
    private SmsReceiver smsReceiver;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchEnable = findViewById(R.id.switch_enable);
        sharedPreferences = getSharedPreferences("Sms2Note", MODE_PRIVATE);
        boolean isEnabled = sharedPreferences.getBoolean("enabled", false);
        switchEnable.setChecked(isEnabled);

        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkSmsPermission()) {
                    enableSmsListener();
                } else {
                    requestSmsPermission();
                }
            } else {
                disableSmsListener();
            }
        });

        if (isEnabled && checkSmsPermission()) {
            registerReceiver();
        }
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                REQUEST_CODE_SMS_PERMISSION);
    }

    private void enableSmsListener() {
        registerReceiver();
        sharedPreferences.edit().putBoolean("enabled", true).apply();
        Toast.makeText(this, "已启用短信转小米笔记", Toast.LENGTH_SHORT).show();
    }

    private void disableSmsListener() {
        unregisterReceiver();
        sharedPreferences.edit().putBoolean("enabled", false).apply();
        Toast.makeText(this, "已停止短信转小米笔记", Toast.LENGTH_SHORT).show();
    }

    private void registerReceiver() {
        if (smsReceiver == null) {
            smsReceiver = new SmsReceiver();
        }
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(1000);
        try {
            registerReceiver(smsReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterReceiver() {
        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            smsReceiver = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                switchEnable.setChecked(true);
                enableSmsListener();
            } else {
                switchEnable.setChecked(false);
                Toast.makeText(this, "需要短信权限才能启用功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }
}