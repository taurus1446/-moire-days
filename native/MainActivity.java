package app.moire.anniversary;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.BridgeActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

public class MainActivity extends BridgeActivity {
  private static final int MAX_BACKUP_BYTES=20*1024*1024;
  private String pendingBackup;
  private ActivityResultLauncher<String> savePicker;
  private ActivityResultLauncher<String[]> openPicker;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    savePicker=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),uri->{
      if(uri==null){sendJs("moireBackupSaved",false,"취소");pendingBackup=null;return;}
      String data=pendingBackup;pendingBackup=null;
      if(data==null){sendJs("moireBackupSaved",false,"저장할 데이터가 없습니다.");return;}
      try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){
        if(stream==null)throw new Exception("파일을 열 수 없습니다.");
        stream.write(data.getBytes(StandardCharsets.UTF_8));stream.flush();
        sendJs("moireBackupSaved",true,"선택한 위치");
      }catch(Exception e){Log.e("MOIRE","backup save",e);sendJs("moireBackupSaved",false,e.getMessage());}
    });
    openPicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
      if(uri==null){sendJs("moireBackupOpened",null,"취소");return;}
      try(InputStream stream=getContentResolver().openInputStream(uri)){
        if(stream==null)throw new Exception("파일을 열 수 없습니다.");
        ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int count,total=0;
        while((count=stream.read(chunk))!=-1){total+=count;if(total>MAX_BACKUP_BYTES)throw new Exception("20MB 이하의 백업 파일을 선택해 주세요.");buf.write(chunk,0,count);}
        sendJs("moireBackupOpened",buf.toString("UTF-8"),null);
      }catch(Exception e){Log.e("MOIRE","backup open",e);sendJs("moireBackupOpened",null,e.getMessage());}
    });
    getBridge().getWebView().addJavascriptInterface(new MoireWidget.Bridge(this),"MoireNative");
    getBridge().getWebView().addJavascriptInterface(new FileBridge(),"MoireFiles");
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2301);
    if(Build.VERSION.SDK_INT>=31){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);
      if(!a.canScheduleExactAlarms())try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}
    }
  }
  private static String jsonArg(Object value){
    if(value==null)return "null";
    if(value instanceof String)return JSONObject.quote((String)value);
    if(value instanceof Boolean || value instanceof Number)return value.toString();
    return JSONObject.quote(value.toString());
  }
  private void sendJs(String callback,Object first,Object second){
    String script="window."+callback+"&&window."+callback+"("+jsonArg(first)+","+jsonArg(second)+")";
    runOnUiThread(()->{if(getBridge()!=null&&getBridge().getWebView()!=null)getBridge().getWebView().evaluateJavascript(script,null);});
  }
  public class FileBridge {
    @JavascriptInterface public void saveBackup(String json,String filename){
      runOnUiThread(()->{
        if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BACKUP_BYTES){sendJs("moireBackupSaved",false,"백업 크기는 20MB 이하여야 합니다.");return;}
        pendingBackup=json;
        String safe=filename==null?"MOIRE-backup.json":filename.replaceAll("[^a-zA-Z0-9._-]","_");
        savePicker.launch(safe.endsWith(".json")?safe:safe+".json");
      });
    }
    @JavascriptInterface public void openBackup(){runOnUiThread(()->openPicker.launch(new String[]{"application/json","text/plain","application/octet-stream"}));}
  }
  @Override public void onResume(){super.onResume();MoireWidget.refresh(this);MoireAlarm.scheduleAll(this);}
  public void chooseSound(){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);startActivityForResult(i,2402);}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==2402&&result==RESULT_OK&&data!=null){Uri uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);if(uri!=null)getSharedPreferences(MoireAlarm.PREF,0).edit().putString("sound_uri",uri.toString()).apply();}}
}
