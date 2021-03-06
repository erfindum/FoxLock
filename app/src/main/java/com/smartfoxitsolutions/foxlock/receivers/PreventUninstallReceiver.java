package com.smartfoxitsolutions.foxlock.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smartfoxitsolutions.foxlock.AppLockModel;
import com.smartfoxitsolutions.foxlock.LockUpSettingsActivity;
import com.smartfoxitsolutions.foxlock.R;

/**
 * Created by RAAJA on 23-12-2016.
 */

public class PreventUninstallReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                .edit().putBoolean(LockUpSettingsActivity.DEVICE_ADMIN_PREFERENCE_KEY,true).apply();
        LockUpSettingsActivity.isPreventUninstallEnabled = true;
        Log.d("LockupDevice","DeviceAdminEnabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        context.getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,Context.MODE_PRIVATE)
                .edit().putBoolean(LockUpSettingsActivity.DEVICE_ADMIN_PREFERENCE_KEY,false).apply();
        LockUpSettingsActivity.isPreventUninstallEnabled = false;
        Log.d("LockupDevice","DeviceAdminDisabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        super.onDisableRequested(context, intent);
        return context.getResources().getString(R.string.appLock_activity_prevent_uninstall_info_text);
    }
}
