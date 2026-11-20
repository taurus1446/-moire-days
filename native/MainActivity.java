package app.moire.anniversary;
import android.os.Bundle;import android.Manifest;import android.content.pm.PackageManager;import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
 @Override public void onCreate(Bundle state){super.onCreate(state);getBridge().getWebView().addJavascriptInterface(new MoireWidget.Bridge(this),"MoireNative");if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2301);}
 @Override public void onResume(){super.onResume();MoireWidget.refresh(this);}
}
