package com.example.sms2note;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String ACTION_LOG = "com.example.sms2note.ACTION_LOG";
    public static final String EXTRA_LOG_MESSAGE = "log_message";

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile(
        "(验证码|验证|code|Code|CODE|短信验证|校验码|动态码|OTP|token|Token|TOKEN)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            log(context, "检测到新短信");
            
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String content = smsMessage.getMessageBody();
                        long timestamp = smsMessage.getTimestampMillis();
                        
                        if (isVerificationCode(content)) {
                            log(context, "检测到验证码短信，发送者: " + sender);
                            saveToMiNotes(context, sender, content, timestamp);
                        } else {
                            log(context, "非验证码短信，跳过");
                        }
                    }
                }
            }
        }
    }

    private boolean isVerificationCode(String content) {
        if (content == null) {
            return false;
        }
        return VERIFICATION_CODE_PATTERN.matcher(content).find();
    }

    /**
     * 写入小米笔记（使用静默ContentProvider方式）
     * 适配：MIUI14/15、澎湃OS
     */
    private void saveToMiNotes(Context context, String sender, String content, long timestamp) {
        try {
            String title = "验证码 - " + (sender != null ? sender : "未知");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timeStr = sdf.format(new Date(timestamp));
            
            String noteContent = "时间：" + timeStr + "\n\n" + content;

            // 使用静默ContentProvider写入
            boolean success = MiNoteSilent.createNote(context, title, noteContent);
            if (success) {
                log(context, "✅ 静默写入小米笔记成功: " + title);
                Log.d(TAG, "Note saved silently to MIUI Notes");
            } else {
                log(context, "❌ 静默写入小米笔记失败");
                Log.e(TAG, "Failed to save note to MIUI Notes");
            }
            
        } catch (Exception e) {
            log(context, "处理验证码异常: " + e.getMessage());
            Log.e(TAG, "Error processing verification code: " + e.getMessage());
        }
    }

    private void log(Context context, String message) {
        Intent logIntent = new Intent(ACTION_LOG);
        logIntent.putExtra(EXTRA_LOG_MESSAGE, message);
        context.sendBroadcast(logIntent);
    }
}