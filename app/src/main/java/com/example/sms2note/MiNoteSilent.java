package com.example.sms2note;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * 小米笔记 静默写入（不弹App、有标题、云同步、i.mi.com可见）
 * 适配：MIUI14/15、澎湃OS、红米K80
 */
public class MiNoteSilent {
    private static final String TAG = "MiNoteSilent";
    private static final Uri NOTE_URI = Uri.parse("content://com.miui.notes/notes");

    public static boolean createNote(Context ctx, String title, String content) {
        try {
            ContentResolver cr = ctx.getContentResolver();
            ContentValues cv = new ContentValues();

            cv.put("title", title);
            cv.put("content", content);
            cv.put("type", 0);
            cv.put("status", 0);
            cv.put("created_time", System.currentTimeMillis());
            cv.put("modified_time", System.currentTimeMillis());

            Uri result = cr.insert(NOTE_URI, cv);
            if (result != null) {
                Log.d(TAG, "✅ 静默笔记成功：" + title);
                return true;
            } else {
                Log.e(TAG, "❌ 写入失败：Uri为空");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 异常：" + e.getMessage());
            return false;
        }
    }
}