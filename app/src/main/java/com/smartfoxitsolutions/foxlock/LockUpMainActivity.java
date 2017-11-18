package com.smartfoxitsolutions.foxlock;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.smartfoxitsolutions.foxlock.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.foxlock.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.foxlock.loyaltybonus.LoyaltyBonusMain;
import com.smartfoxitsolutions.foxlock.mediavault.MediaMoveActivity;
import com.smartfoxitsolutions.foxlock.mediavault.MediaVaultAlbumActivity;
import com.smartfoxitsolutions.foxlock.mediavault.services.MediaMoveService;
import com.smartfoxitsolutions.foxlock.mediavault.services.ShareMoveService;
import com.smartfoxitsolutions.foxlock.services.AppLockingService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by RAAJA on 16-09-2016.
 */
public class LockUpMainActivity extends AppCompatActivity {

    public static final int USAGE_ACCESS_PERMISSION_REQUEST=3;
    public static final int OVERLAY_PERMISSION_REQUEST = 5;
    private static final String USAGE_ACCESS_DIALOG_TAG = "usageAccessPermissionDialog";
    private static final String OVERLAY_ACCESS_DIALOG_TAG = "overlay_permission_dialog";

    private static final String SHOULD_SHOW_APP_LOCK_INFO_KEY = "shouldShowAppLockInfo";

    private AppCompatImageButton vaultActivityButton
                            , loyaltyBonusButton,settingsButton;

    private DialogFragment overlayPermissionDialog,usageDialog;
    private boolean shouldTrackUserPresence, shouldCloseAffinity, shouldStartAppLock,
            isAppLockFirstLoad, hasPermissionReturned,stopTrackAfterPermission;
    private AppCompatImageButton lockButton;
    private ScreenOffReceiver screenOffReceiver;
    private Button faqButton;
    private TextView appsCount, appsCountInfo;
    public static int lockedRecommendApps,lockedApps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_up_main_activity);
        shouldTrackUserPresence = true;
        vaultActivityButton = (AppCompatImageButton) findViewById(R.id.lockup_main_activity_vault_image);
        settingsButton= (AppCompatImageButton) findViewById(R.id.lockup_main_activity_settings_image);
        lockButton = (AppCompatImageButton) findViewById(R.id.lockup_main_activity_app_lock_button);
        faqButton = (Button) findViewById(R.id.lockup_main_activity_faq_button);
        loyaltyBonusButton = (AppCompatImageButton) findViewById(R.id.lockup_main_activity_loyalty_button);
        appsCount = (TextView) findViewById(R.id.lockup_main_activity_app_count);
        appsCountInfo = (TextView) findViewById(R.id.lockup_main_activity_app_count_info);
        setImageButtonListeners();
        lockedRecommendApps = 0;
        lockedApps = 0;
        setAppLockInfo();
    }


    void setImageButtonListeners(){
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    checkAndSetUsagePermissions();
                }else{
                    startActivity(new Intent(getBaseContext(),AppLockActivity.class));
                    shouldTrackUserPresence = false;
                }
            }
        });

        vaultActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!MediaMoveService.SERVICE_STARTED && !ShareMoveService.SERVICE_STARTED) {
                    startActivity(new Intent(getBaseContext(), MediaVaultAlbumActivity.class));
                    shouldTrackUserPresence = false;
                }else{
                    startActivity(new Intent(getBaseContext(),MediaMoveActivity.class));
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),LockUpSettingsActivity.class));
                shouldTrackUserPresence = false;
            }
        });

        loyaltyBonusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getBaseContext(), LoyaltyBonusMain.class));
                shouldTrackUserPresence = false;

                //Toast.makeText(LockUpMainActivity.this,"Coming Soon",Toast.LENGTH_SHORT).show();
            }
        });
        faqButton.setText(getString(R.string.main_screen_activity_faq_button_text));
        faqButton.setPaintFlags(faqButton.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
        faqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),FaqActivity.class));
                shouldTrackUserPresence = false;
            }
        });
        faqButton.setVisibility(View.INVISIBLE);

        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/arquitectabook.ttf");
        appsCount.setTypeface(typeface);
        appsCountInfo.setTypeface(typeface);

    }

    @TargetApi(21)
    void checkAndSetUsagePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
            if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED) {
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                        checkAndSetOverlayPermission();
                }else{
                    if(!hasPermissionReturned) {
                        shouldTrackUserPresence = false;
                        startActivity(new Intent(getBaseContext(), AppLockActivity.class));
                    }else{
                        shouldTrackUserPresence = false;
                        stopTrackAfterPermission = true;
                        startActivity(new Intent(getBaseContext(), AppLockActivity.class));
                    }
                }
                Log.d(AppLockingService.TAG,String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS
                        , Process.myUid(), getPackageName())
                        == AppOpsManager.MODE_ALLOWED));
            } else {
                startUsagePermissionDialog();
                Log.d(AppLockingService.TAG,"No Usage");
            }
        }
    }

    @TargetApi(23)
    void checkAndSetOverlayPermission(){
        if(Settings.canDrawOverlays(this)){
            if(!hasPermissionReturned) {
                shouldTrackUserPresence = false;
                startActivity(new Intent(getBaseContext(), AppLockActivity.class));
            }else{
                shouldTrackUserPresence = false;
                stopTrackAfterPermission = true;
                startActivity(new Intent(getBaseContext(), AppLockActivity.class));
            }
        }else{
            startOverlayPermissionDialog();
        }
    }

    void startUsagePermissionDialog(){
        Bundle usageBundle = new Bundle();
        usageBundle.putString("grandUsageStartType","appLockStart");
        usageDialog = new GrantUsageAccessDialog();
        usageDialog.setArguments(usageBundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(USAGE_ACCESS_DIALOG_TAG);
        usageDialog.show(fragmentTransaction,USAGE_ACCESS_DIALOG_TAG);
    }

    void startOverlayPermissionDialog(){
        Bundle overlayBundle = new Bundle();
        overlayBundle.putString("overlayStartType","appLockStart");
        overlayPermissionDialog = new OverlayPermissionDialog();
        overlayPermissionDialog.setArguments(overlayBundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(OVERLAY_ACCESS_DIALOG_TAG);
        overlayPermissionDialog.show(fragmentTransaction,OVERLAY_ACCESS_DIALOG_TAG);
    }

    @TargetApi(21)
    public void startUsageAccessSettingActivity(){
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),USAGE_ACCESS_PERMISSION_REQUEST);
        shouldTrackUserPresence = false;
    }

    @TargetApi(23)
    public void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),OVERLAY_PERMISSION_REQUEST);
        shouldTrackUserPresence = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == USAGE_ACCESS_PERMISSION_REQUEST){
            checkAndSetUsagePermissions();
            hasPermissionReturned =true;
        }else
        if(requestCode == OVERLAY_PERMISSION_REQUEST){
            checkAndSetOverlayPermission();
            hasPermissionReturned = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(!stopTrackAfterPermission) {
            shouldTrackUserPresence = true;
        }else{
            stopTrackAfterPermission = false;
            hasPermissionReturned = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        screenOffReceiver = new ScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver,filter);
        SharedPreferences prefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLock =prefs.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = prefs.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,true);
    }

    private void setAppLockInfo(){
        SharedPreferences appPrefs = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME
                , MODE_PRIVATE);
        AppLockModel appModel = AppLockModel.getInstance();
        appModel.setUpModel(appPrefs);
        String installerPackage = getAppInstallerPackage();
        LinkedHashMap<String,HashMap<String,Boolean>> recommendedMap = appModel.getRecommendedAppsMap();
        for(Map.Entry<String,HashMap<String,Boolean>> entry:recommendedMap.entrySet()){
            if(!entry.getKey().equals(installerPackage)){
                ArrayList<Boolean> tempList = new ArrayList<>(entry.getValue().values());
                if(tempList.get(0)){
                    lockedRecommendApps++;
                }
            }
        }
        TreeMap<String,String> checkedAppMap = appModel.getCheckedAppsMap();
        lockedApps = checkedAppMap.size();
        appsCount.setText(String.valueOf(lockedApps+lockedRecommendApps));
        if(lockedApps == 1) {
            appsCountInfo.setText(R.string.main_screen_apps_locked_info_single);
        }else{
            appsCountInfo.setText(R.string.main_screen_apps_locked_info);
        }
    }

    private String getAppInstallerPackage(){
        Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        installIntent.addCategory(Intent.CATEGORY_DEFAULT);
        installIntent.setDataAndType(Uri.parse("file:///"),"application/vnd.android.package-archive");
        List<ResolveInfo> installerPackages = getPackageManager().queryIntentActivities(installIntent, PackageManager.GET_META_DATA);

        if(installerPackages!=null && !installerPackages.isEmpty()){
            ResolveInfo installerInfo = installerPackages.get(0);
            return installerInfo.activityInfo.packageName;
        }else{
            return "none";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int allLockedApps = lockedApps+lockedRecommendApps;
        appsCount.setText(String.valueOf(allLockedApps));
        if(allLockedApps == 1) {
            appsCountInfo.setText(R.string.main_screen_apps_locked_info_single);
        }else{
            appsCountInfo.setText(R.string.main_screen_apps_locked_info);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity = true;
        }else{
            shouldCloseAffinity = false;
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startAppLock();
        finishAffinity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            startAppLock();
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
            unregisterReceiver(screenOffReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(shouldTrackUserPresence){
            unregisterReceiver(screenOffReceiver);
        }
    }

    void startAppLock(){
        if(shouldStartAppLock && !isAppLockFirstLoad){
            if(!AppLockingService.isAppLockRunning) {
                startService(new Intent(getBaseContext(), AppLockingService.class));
            }
        }
    }

    static class ScreenOffReceiver extends BroadcastReceiver{

        WeakReference<LockUpMainActivity> activity;
        ScreenOffReceiver(WeakReference<LockUpMainActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}
