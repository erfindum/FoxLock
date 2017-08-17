package com.smartfoxitsolutions.foxlock.loyaltybonus;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.smartfoxitsolutions.foxlock.AppLockActivity;
import com.smartfoxitsolutions.foxlock.AppLockModel;
import com.smartfoxitsolutions.foxlock.LockUpSettingsActivity;
import com.smartfoxitsolutions.foxlock.R;
import com.smartfoxitsolutions.foxlock.dialogs.GrantUsageAccessDialog;
import com.smartfoxitsolutions.foxlock.dialogs.OverlayPermissionDialog;
import com.smartfoxitsolutions.foxlock.loyaltybonus.dialogs.OnRequestRedeemListener;
import com.smartfoxitsolutions.foxlock.loyaltybonus.receivers.UserReportBroadcastReceiver;
import com.smartfoxitsolutions.foxlock.services.AppLockingService;
import com.smartfoxitsolutions.foxlock.services.GetPaletteColorService;

import java.lang.ref.WeakReference;

import static com.smartfoxitsolutions.foxlock.AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY;
import static com.smartfoxitsolutions.foxlock.LockUpSettingsActivity.SWIPE_LOCK_PREFERENCE_KEY;

/**
 * Created by RAAJA on 29-01-2017.
 */

public class LoyaltyUserActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    boolean shouldTrackUserPresence, shouldCloseAffinity, stopTrackAfterPermission
            ,shouldStartAppLockOn,isAppLockFirstLoad, isUserLoggedIn, shouldSwipeLock;
    private LoyaltyUserScreenOffReceiver loyaltyUserScreenOffReceiver;
    private GrantUsageAccessDialog usageDialog;
    private OverlayPermissionDialog overlayPermissionDialog;
    private boolean hasPermissionReturned;
    private OnRequestRedeemListener onRequestRedeemListener;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loyalty_bonus_user_activity);
        toolbar = (Toolbar) findViewById(R.id.loyalty_bonus_user_activity_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentManager = getFragmentManager();
        shouldTrackUserPresence = true;
        try {
            MobileAds.initialize(this.getApplicationContext());
        }catch (Exception e){
            Log.d("LockUp","User Profile Ad Exception : " +e);
        }
    }

    void setOnRequestRedeemListener(OnRequestRedeemListener redeemListener){
        onRequestRedeemListener = redeemListener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(fragmentManager.findFragmentByTag("loyaltyProfileFragment")==null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            toolbar.setTitle(getString(R.string.loyalty_user_main_title));
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.loyalty_bonus_user_activity_container, new LoyaltyUserProfileFragment(), "loyaltyProfileFragment");
            transaction.addToBackStack("loyaltyProfileFragment");
            transaction.commit();
        }

        loyaltyUserScreenOffReceiver = new LoyaltyUserScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(loyaltyUserScreenOffReceiver,filter);

        SharedPreferences loyaltyPreferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,MODE_PRIVATE);
        isUserLoggedIn = loyaltyPreferences.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);

        preferences = getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME,MODE_PRIVATE);
        shouldStartAppLockOn = preferences.getBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = preferences.getBoolean(AppLockActivity.APP_LOCK_FIRST_START_PREFERENCE_KEY,true);
        if(shouldStartAppLockOn) {
            shouldSwipeLock = preferences.getBoolean(SWIPE_LOCK_PREFERENCE_KEY, false);
        }else{
            shouldSwipeLock = false;
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
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity=true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SubMenu subMenu = menu.addSubMenu(24,1,Menu.NONE,getString(R.string.loyalty_user_main_logout));
        subMenu.setIcon(R.drawable.selector_menu_dot);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getItemId()==1){
            Log.d("LoyaltyUserMain","LogoutRequested");
            SharedPreferences preferences = getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,
                    MODE_PRIVATE);
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
            edit.apply();
            shouldTrackUserPresence = false;
            isUserLoggedIn = false;
            startActivity(new Intent(this,LoyaltyBonusMain.class).putExtra("userLoggedOut",25));
            finish();
            return true;
        }
        return false;
    }

    void startRedeemFragment(String type){
        toolbar.setTitle(getString(R.string.loyalty_bonus_redeem_title));
        Bundle args = new Bundle();
        args.putString("redeemType",type);
        LoyaltyUserRedeemFragment redeemFragment = new LoyaltyUserRedeemFragment();
        redeemFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.loyalty_bonus_user_activity_container,redeemFragment,"loyaltyRedeemFragment");
        transaction.addToBackStack("loyaltyRedeemFragment");
        transaction.commit();
    }

    void startRedeemFinal(String type, int selection){
        toolbar.setTitle(getString(R.string.loyalty_redeem_final_redeem_title));
        Bundle args = new Bundle();
        args.putString("redeemFinalType",type);
        args.putInt("redeemFinalSelection",selection);
        LoyaltyUserRedeemFinalFragment redeemFinalFragment = new LoyaltyUserRedeemFinalFragment();
        redeemFinalFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.loyalty_bonus_user_activity_container,redeemFinalFragment,"loyaltyRedeemFinalFragment");
        transaction.addToBackStack("loyaltyRedeemFinalFragment");
        transaction.commit();
    }

    boolean isSwipeLockOn(){
        return shouldSwipeLock;
    }

    boolean activateSwipeLock(){
        if(isAppLockFirstLoad){
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                checkAndSetUsagePermissions();
            }else{
                return initializeAppLock();
            }
            return false;
        }
        if(!shouldStartAppLockOn){
            Toast.makeText(LoyaltyUserActivity.this,"Switch on AppLock first",Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        SharedPreferences.Editor edit = preferences.edit();
        if(shouldSwipeLock){
            edit.putBoolean(SWIPE_LOCK_PREFERENCE_KEY,false);
            shouldSwipeLock = false;
            edit.apply();
            startAppLockService();
            return false;
        }else{
            edit.putBoolean(SWIPE_LOCK_PREFERENCE_KEY,true);
            shouldSwipeLock = true;
            edit.apply();
            startAppLockService();
            return true;
        }
    }

    @TargetApi(21)
    boolean checkAndSetUsagePermissions(){
        AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(APP_OPS_SERVICE);
        if (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
                == AppOpsManager.MODE_ALLOWED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkAndSetOverlayPermission();
            } else {
                if (!hasPermissionReturned) {
                    shouldTrackUserPresence = false;
                    startAppLockService();
                    return initializeAppLock();
                } else {
                    shouldTrackUserPresence = false;
                    stopTrackAfterPermission = true;
                    startAppLockService();
                    return initializeAppLock();
                }
            }
            Log.d(AppLockingService.TAG, String.valueOf(opsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS
                    , Process.myUid(), getPackageName())
                    == AppOpsManager.MODE_ALLOWED));
        } else {
            startUsagePermissionDialog();
        }
        return false;
    }

    void startAppLockService(){
        startService(new Intent(getBaseContext(),AppLockingService.class));
    }

    @TargetApi(23)
    boolean checkAndSetOverlayPermission(){
        if(Settings.canDrawOverlays(this)){
            if(!hasPermissionReturned) {
                shouldTrackUserPresence = false;
                startAppLockService();
                return initializeAppLock();
            }else{
                shouldTrackUserPresence = false;
                stopTrackAfterPermission = true;
                startAppLockService();
                return initializeAppLock();
            }
        }else{
            startOverlayPermissionDialog();
        }
        return false;
    }

    boolean initializeAppLock(){
        shouldStartAppLockOn = true;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(APP_LOCK_FIRST_START_PREFERENCE_KEY,false);
        isAppLockFirstLoad = false;
        editor.putBoolean(LockUpSettingsActivity.APP_LOCKING_SERVICE_START_PREFERENCE_KEY,true);
        editor.putBoolean(SWIPE_LOCK_PREFERENCE_KEY,true);
        editor.apply();
        shouldSwipeLock = true;
        startService(new Intent(LoyaltyUserActivity.this, GetPaletteColorService.class));
        return true;
    }

    void startUsagePermissionDialog(){
        Bundle usageBundle = new Bundle();
        usageBundle.putString("grandUsageStartType","loyaltyBonusStart");
        usageDialog = new GrantUsageAccessDialog();
        usageDialog.setArguments(usageBundle);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("loyaltyUsagePermission");
        usageDialog.show(fragmentTransaction,"loyaltyUsagePermission");
    }

    void startOverlayPermissionDialog(){
        Bundle overlayBundle = new Bundle();
        overlayBundle.putString("overlayStartType","loyaltyBonusStart");
        overlayPermissionDialog = new OverlayPermissionDialog();
        overlayPermissionDialog.setArguments(overlayBundle);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("loyaltyOverlayPermission");
        overlayPermissionDialog.show(fragmentTransaction,"loyaltyOverlayPermission");
    }

    @TargetApi(21)
    public void startUsageAccessSettingActivity(){
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),43);
        shouldTrackUserPresence = false;
    }

    @TargetApi(23)
    public void requestOverlayPermission(){
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),44);
        shouldTrackUserPresence = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 43){
            checkAndSetUsagePermissions();
            hasPermissionReturned = true;
        }else
        if(requestCode == 44){
            checkAndSetOverlayPermission();
            hasPermissionReturned = true;
        }
    }

    public void requestRedeem(){
        if(onRequestRedeemListener !=null){
            onRequestRedeemListener.requestRedeem();
        }
    }

    public void requestCancelled(){
        if(onRequestRedeemListener!=null){
            onRequestRedeemListener.requestCancelled();
        }
    }

    public void redeemBonusSuccess(){
        if(fragmentManager.findFragmentByTag("loyaltyProfileFragment")!=null) {
            fragmentManager.popBackStack("loyaltyProfileFragment",FragmentManager.POP_BACK_STACK_INCLUSIVE);
            toolbar.setTitle(getString(R.string.loyalty_user_main_title));
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.loyalty_bonus_user_activity_container, new LoyaltyUserProfileFragment(), "loyaltyProfileFragment");
            transaction.addToBackStack("loyaltyProfileFragment");
            transaction.commit();
        }
    }

    private void stopReportAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent reportPendingIntent = PendingIntent.getBroadcast(
                this,23,new Intent(this,UserReportBroadcastReceiver.class),0
        );
        alarmManager.cancel(reportPendingIntent);
        Log.d("LockupUserReport","Report Alarm Stopped --------");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(fragmentManager.getBackStackEntryCount()==0){
            finish();
            return;
        }

        FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount()-1);
        if(entry.getName().equals("loyaltyProfileFragment")){
            toolbar.setTitle(getString(R.string.loyalty_user_main_title));
        }
        if(entry.getName().equals("loyaltyRedeemFragment")){
            toolbar.setTitle(getString(R.string.loyalty_bonus_redeem_title));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldStartAppLockOn && !isAppLockFirstLoad){
            if(!isUserLoggedIn) {
                stopReportAlarm();
            }
            startService(new Intent(getBaseContext(), AppLockingService.class));
        }

        if(shouldCloseAffinity){
            finishAffinity();
        }
        if(!shouldTrackUserPresence){
           unregisterReceiver(loyaltyUserScreenOffReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(shouldTrackUserPresence){
            unregisterReceiver(loyaltyUserScreenOffReceiver);
        }
    }

    static class LoyaltyUserScreenOffReceiver extends BroadcastReceiver {

        WeakReference<LoyaltyUserActivity> activity;
        LoyaltyUserScreenOffReceiver(WeakReference<LoyaltyUserActivity> activity){
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
