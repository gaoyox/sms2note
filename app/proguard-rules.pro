-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# 保护版本信息不被混淆
-keep class android.content.pm.PackageInfo {
    java.lang.String versionName;
    int versionCode;
}

# 保护MainActivity中的版本获取方法
-keepclassmembers class com.example.sms2note.MainActivity {
    private java.lang.String getVersionName();
}