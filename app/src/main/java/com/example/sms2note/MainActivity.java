package com.example.sms2note;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SMS_PERMISSION = 100;
    private Switch switchEnable;
    private SmsReceiver smsReceiver;
    private SharedPreferences sharedPreferences;
    private TextView tvLog;
    private TextView tvVersion;
    private ScrollView scrollView;
    private BroadcastReceiver logReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchEnable = findViewById(R.id.switch_enable);
        tvLog = findViewById(R.id.tv_log);
        tvVersion = findViewById(R.id.tv_version);
        scrollView = findViewById(R.id.scroll_view);
        sharedPreferences = getSharedPreferences("Sms2Note", MODE_PRIVATE);
        boolean isEnabled = sharedPreferences.getBoolean("enabled", false);
        switchEnable.setChecked(isEnabled);

        tvVersion.setText("v" + getVersionName());
        addLog("程序启动 v" + getVersionName());

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
            addLog("已恢复监听状态");
        }

        registerLogReceiver();
    }

    private void registerLogReceiver() {
        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SmsReceiver.ACTION_LOG.equals(intent.getAction())) {
                    String message = intent.getStringExtra(SmsReceiver.EXTRA_LOG_MESSAGE);
                    if (message != null) {
                        addLog(message);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(SmsReceiver.ACTION_LOG);
        registerReceiver(logReceiver, filter);
    }

    private void addLog(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        String logEntry = "[" + timeStr + "] " + message;
        
        String currentLog = tvLog.getText().toString();
        if (!currentLog.isEmpty()) {
            currentLog = logEntry + "\n" + currentLog;
        } else {
            currentLog = logEntry;
        }
        
        tvLog.setText(currentLog);
        
        scrollView.post(() -> scrollView.scrollTo(0, 0));
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        addLog("请求短信权限");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                REQUEST_CODE_SMS_PERMISSION);
    }

    private void enableSmsListener() {
        registerReceiver();
        sharedPreferences.edit().putBoolean("enabled", true).apply();
        addLog("已启用短信转小米笔记");
        Toast.makeText(this, "已启用短信转小米笔记", Toast.LENGTH_SHORT).show();
    }

    private void disableSmsListener() {
        unregisterReceiver();
        sharedPreferences.edit().putBoolean("enabled", false).apply();
        addLog("已停止短信转小米笔记");
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
            addLog("短信接收器已注册");
        } catch (Exception e) {
            e.printStackTrace();
            addLog("短信接收器注册失败: " + e.getMessage());
        }
    }

    private void unregisterReceiver() {
        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
                addLog("短信接收器已注销");
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
                addLog("短信权限已获取");
                switchEnable.setChecked(true);
                enableSmsListener();
            } else {
                addLog("短信权限被拒绝");
                switchEnable.setChecked(false);
                Toast.makeText(this, "需要短信权限才能启用功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        if (logReceiver != null) {
            try {
                unregisterReceiver(logReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        addLog("程序退出");
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "1.0";
        }
    }
}