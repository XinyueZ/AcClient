package thereisnospon.acclient;

import android.app.Application;

import com.google.gson.Gson;
import com.tencent.bugly.crashreport.CrashReport;

import thereisnospon.acclient.utils.AcClientActivityStack;

/**
 * Created by yzr on 16/6/5.
 */
public final class AppApplication extends Application {
    public final static Gson gson = new Gson();
    public static AppApplication  context;
    public AcClientActivityStack activityStack = new AcClientActivityStack();

    @Override
    public void onCreate() {
        super.onCreate();
        context= this;
        CrashReport.initCrashReport(getApplicationContext(), getString(R.string.crash_report_id), false);
        registerActivityLifecycleCallbacks(activityStack);
    }

}
