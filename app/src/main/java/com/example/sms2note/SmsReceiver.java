package com.example.sms2note;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

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
                        
                        saveToMiNotes(context, sender, content, timestamp);
                    }
                }
            }
        }
    }

    private void saveToMiNotes(Context context, String sender, String content, long timestamp) {
        try {
            String title = "短信 - " + (sender != null ? sender : "未知");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timeStr = sdf.format(new Date(timestamp));
            
            String noteContent = "时间：" + timeStr + "\n\n" + content;

            Uri uri = Uri.parse("content://com.miui.notes.provider/notes");
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("title", title);
            values.put("content", noteContent);
            values.put("folder_id", 0);
            values.put("created_time", System.currentTimeMillis());
            values.put("modified_time", System.currentTimeMillis());

            try {
                Uri result = context.getContentResolver().insert(uri, values);
                if (result != null) {
                    Log.d(TAG, "Note saved successfully");
                } else {
                    Log.e(TAG, "Failed to save note");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving to MIUI Notes: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage());
        }
    }
}