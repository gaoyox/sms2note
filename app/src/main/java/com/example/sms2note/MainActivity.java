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
    private static final int REQUEST_CODE_ALL_PERMISSIONS = 101;
    
    // 需要的权限列表
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };
    
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
                if (checkAllPermissions()) {
                    enableSmsListener();
                } else {
                    requestAllPermissions();
                }
            } else {
                disableSmsListener();
            }
        });

        if (isEnabled && checkAllPermissions()) {
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

    /**
     * 检查所有必需权限
     */
    private boolean checkAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                addLog("权限未获取: " + permission);
                return false;
            }
        }
        return true;
    }

    /**
     * 请求所有必需权限
     */
    private void requestAllPermissions() {
        addLog("请求必需权限");
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_ALL_PERMISSIONS);
    }

    /**
     * 检查单个短信权限（保持兼容性）
     */
    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableSmsListener() {
        registerReceiver();
        sharedPreferences.edit().putBoolean("enabled", true).apply();
        addLog("已启用短信转小米笔记");
        writeToMiNotes("短信转小米笔记", "短信转小米笔记已启用\n\n日志内容:\n" + tvLog.getText().toString());
        Toast.makeText(this, "已启用短信转小米笔记", Toast.LENGTH_SHORT).show();
    }

    /**
     * 写入小米笔记（优先静默广播，失败则回退到分享方式）
     */
    private void writeToMiNotes(String title, String content) {
        // 直接尝试静默广播方式（新版MIUI/澎湃OS）
        boolean broadcastSuccess = trySilentBroadcast(title, content);
        if (broadcastSuccess) {
            addLog("✅ 已发送静默广播到小米笔记");
            return;
        }
        
        // 回退到分享方式
        addLog("⚠️ 静默方式失败，使用分享方式");
        tryShareWrite(title, content);
    }

    /**
     * 尝试静默广播写入（新版MIUI/澎湃OS）
     */
    private boolean trySilentBroadcast(String title, String content) {
        try {
            Intent intent = new Intent("com.miui.notes.action.CREATE_NOTE");
            intent.setPackage("com.miui.notes");
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            intent.putExtra("silent", true);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            
            sendBroadcast(intent);
            return true;
        } catch (Exception e) {
            addLog("❌ 静默广播失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用分享方式写入小米笔记（回退方案）
     */
    private void tryShareWrite(String title, String content) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setPackage("com.miui.notes");
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, title);
            intent.putExtra(Intent.EXTRA_TEXT, content);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            addLog("✅ 已打开小米笔记分享界面");
        } catch (Exception e) {
            e.printStackTrace();
            addLog("❌ 分享方式也失败: " + e.getMessage());
        }
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
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (version == null || version.isEmpty()) {
                return "1.0.20"; // 硬编码默认版本号
            }
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "1.0.20";
        }
    }
}