package app.moire.anniversary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends BridgeActivity {
    private static final int BACKUP_REQUEST = 2302;
    private String pendingBackup = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getBridge().getWebView().addJavascriptInterface(
            new MoireWidget.Bridge(this), "MoireNative"
        );
        getBridge().getWebView().addJavascriptInterface(
            new BackupBridge(), "MoireBackup"
        );
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2301
            );
        }
    }

    public class BackupBridge {
        @JavascriptInterface
        public void save(String data, String filename) {
            runOnUiThread(() -> {
                pendingBackup = data;
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, filename);
                    startActivityForResult(intent, BACKUP_REQUEST);
                } catch (Exception e) {
                    pendingBackup = null;
                    backupResult(false, "파일 저장 창을 열지 못했습니다.");
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != BACKUP_REQUEST) return;
        if (resultCode != Activity.RESULT_OK || data == null ||
            data.getData() == null) {
            pendingBackup = null;
            backupResult(false, "백업 저장이 취소되었습니다.");
            return;
        }
        try (OutputStream out =
                 getContentResolver().openOutputStream(data.getData())) {
            if (out == null || pendingBackup == null)
                throw new Exception("No output stream or backup data");
            out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));
            out.flush();
            backupResult(true, "백업 파일이 저장되었습니다.");
        } catch (Exception e) {
            backupResult(false, "백업 저장에 실패했습니다.");
        } finally {
            pendingBackup = null;
        }
    }

    private void backupResult(boolean success, String message) {
        String script = "window.moireBackupResult && window.moireBackupResult(" +
            success + "," + org.json.JSONObject.quote(message) + ")";
        getBridge().getWebView().evaluateJavascript(script, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        MoireWidget.refresh(this);
    }
}
