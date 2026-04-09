package hcmute.edu.vn.documentfileeditor.App;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import hcmute.edu.vn.documentfileeditor.Activity.CrashReportActivity;
import hcmute.edu.vn.documentfileeditor.Util.ThemeManager;

public class DocumentEditorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("DocumentEditorCrash", "Uncaught exception on thread " + thread.getName(), throwable);

            Intent intent = new Intent(this, CrashReportActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, buildErrorMessage(throwable));

            try {
                startActivity(intent);
                Thread.sleep(300);
            } catch (Exception startError) {
                Log.e("DocumentEditorCrash", "Failed to launch crash screen", startError);
            }

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });
    }

    private String buildErrorMessage(Throwable throwable) {
        String message = throwable.getClass().getSimpleName();
        if (throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
            message += ": " + throwable.getMessage();
        }
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            message += "\nAt: " + stackTrace[0].toString();
        }
        return message;
    }
}
