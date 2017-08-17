package com.smartfoxitsolutions.foxlock.loyaltybonus.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.smartfoxitsolutions.foxlock.loyaltybonus.LoyaltyBonusModel;
import com.smartfoxitsolutions.foxlock.loyaltybonus.services.UserReportIntentService;

/**
 * Created by RAAJA on 06-02-2017.
 */

public class UserReportBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("LockupUserReport","Alarm fired --------");
        SharedPreferences preferences = context.getSharedPreferences(LoyaltyBonusModel.LOYALTY_BONUS_PREFERENCE_NAME,Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(LoyaltyBonusModel.LOGIN_USER_LOGGED_IN_KEY,false);
        if(isLoggedIn) {
            startWakefulService(context, new Intent(context, UserReportIntentService.class));
        }
    }
}
