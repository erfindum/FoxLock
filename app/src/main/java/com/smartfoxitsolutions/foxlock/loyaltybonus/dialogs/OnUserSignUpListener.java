package com.smartfoxitsolutions.foxlock.loyaltybonus.dialogs;

/**
 * Created by RAAJA on 18-01-2017.
 */

public interface OnUserSignUpListener {
     void onUserDatePicked(int year,int monthOfYear, int dayOfMonth);
     void onErrorDialogCancel();
}
