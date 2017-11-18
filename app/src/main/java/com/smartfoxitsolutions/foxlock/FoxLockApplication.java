package com.smartfoxitsolutions.foxlock;

import android.app.Application;
import android.content.Context;
//import com.startapp.android.publish.adsCommon.StartAppSDK;
//import com.squareup.leakcanary.LeakCanary;

/**
 * Created by RAAJA on 20-12-2016.
 */

public class FoxLockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    /* if(LeakCanary.isInAnalyzerProcess(this)){
            return;
        }
        LeakCanary.install(this);
        */
        //StartAppSDK.init(this.getApplicationContext(), "204970014", false);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
