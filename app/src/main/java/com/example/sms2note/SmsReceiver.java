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
    
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile(
        "(验证码|验证|code|Code|CODE|短信验证|校验码|动态码|OTP|token|Token|TOKEN)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
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
                            saveToMiNotes(context, sender, content, timestamp);
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

    private void saveToMiNotes(Context context, String sender, String content, long timestamp) {
        try {
            String title = "验证码 - " + (sender != null ? sender : "未知");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timeStr = sdf.format(new Date(timestamp));
            
            String noteContent = "时间：" + timeStr + "\n\n" + content;

            android.net.Uri uri = android.net.Uri.parse("content://com.miui.notes.provider/notes");
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("title", title);
            values.put("content", noteContent);
            values.put("folder_id", 0);
            values.put("created_time", System.currentTimeMillis());
            values.put("modified_time", System.currentTimeMillis());

            try {
                android.net.Uri result = context.getContentResolver().insert(uri, values);
                if (result != null) {
                    Log.d(TAG, "Verification code saved to MIUI Notes");
                } else {
                    Log.e(TAG, "Failed to save note");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving to MIUI Notes: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing verification code: " + e.getMessage());
        }
    }
}