package com.smartfoxitsolutions.foxlock.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.nativeads.NativeAd;
import com.smartfoxitsolutions.foxlock.AppLockModel;
import com.smartfoxitsolutions.foxlock.DimensionConverter;
import com.smartfoxitsolutions.foxlock.LockUpSettingsActivity;
import com.smartfoxitsolutions.foxlock.R;
import com.smartfoxitsolutions.foxlock.services.AppLockingService;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by RAAJA on 06-10-2016.
 */

public class LockPatternView extends FrameLayout implements PatternLockView.OnPatternChangedListener {
    private OnPinLockUnlockListener patternLockListener;
    PatternLockView patternView;
    ImageView appIconView;
    private String selectedPatternNode, patternPassCode, salt;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled, shouldHidePatternLine, shouldShowBigIcon, shouldShowAd;
    Vibrator patternViewVibrator;
    ValueAnimator patternAnimator;
    private RelativeLayout patternViewParent, patternAdParent;
    private RectF patternRect;
    private View adView;


    public LockPatternView(Context context, OnPinLockUnlockListener patternLockListener) {
        super(context);
        setPinLockUnlockListener(patternLockListener);
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity, this, true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_parent_view);
        patternViewVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        patternAdParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_ad_parent);
        initializeLockView();
    }

    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener) {
        this.patternLockListener = pinLockListener;
    }

    public void setPackageName(String packageName) {
        patternLockListener.onPinLocked(packageName);
        setAppIcon(packageName);
    }

    public void setWindowBackground(Integer colorVibrant, int displayHeight) {
        if (colorVibrant == null) {
            colorVibrant = Color.parseColor("#2874F0");
        }
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {Color.parseColor("#263238"), colorVibrant};
        drawable.setColors(colors);
        //float radius = Math.round(displayHeight * .95);
        //drawable.setGradientRadius(radius);
        //drawable.setGradientCenter(0.5f, 1f);
        drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        patternViewParent.setBackground(drawable);
    }

    void initializeLockView() {
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_pattern_view);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = getContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME, Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(LockUpSettingsActivity.VIBRATOR_ENABLED_PREFERENCE_KEY, false);
        shouldHidePatternLine = prefs.getBoolean(LockUpSettingsActivity.HIDE_PATTERN_LINE_PREFERENCE_KEY, false);
        if (shouldHidePatternLine) {
            patternView.setLinePaintTransparency(0);
        }
        patternPassCode = prefs.getString(AppLockModel.USER_SET_LOCK_PASS_CODE, "noPin");
        salt = prefs.getString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY, "noColor");
        registerListeners();
        setPatternAnimator();
    }

    void setAppIcon(String packageName) {
        try {
            Log.e(AppLockingService.TAG, "App Icon CAlled " + packageName);
            Drawable appIcon = getContext().getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    void registerListeners() {
        patternView.setOnPatternChangedListener(this);

        patternRect = new RectF();
        patternViewParent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (patternRect.contains(event.getX(), event.getY())) {
                    event.setLocation(event.getX() - patternRect.left, event.getY() - patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    event.setLocation(event.getX() - patternRect.left, event.getY() - patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                return true;
            }
        });

        final ViewTreeObserver viewTreeObserver = patternViewParent.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    patternRect.set(patternView.getLeft(), patternView.getTop()
                            , patternView.getRight(), patternView.getBottom());
                    if (shouldShowBigIcon) {
                        addBigAppIcon();
                    }
                    if(shouldShowAd){
                        addAdView();
                    }
                    patternViewParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    void unRegisterListeners() {
        patternView.setOnPatternChangedListener(null);
        patternViewParent.setOnTouchListener(null);
        patternAnimator.removeAllUpdateListeners();
        appIconView.setImageDrawable(null);
    }

    void setPatternAnimator() {
        patternAnimator = ValueAnimator.ofFloat(0, 1);
        patternAnimator.setDuration(200).setRepeatCount(3);
        patternAnimator.setInterpolator(new OvershootInterpolator());
        patternAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternView.postPatternError();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                patternView.resetPatternView();
                resetPatternData();
            }
        });
    }

    public void addNativeAd(NativeAd nativeAd, final OnLockAdImpressedClicked onLockAdImpressedClicked) {
        if(nativeAd==null){
            shouldShowBigIcon = true;
            return;
        }
        adView = nativeAd.createAdView(getContext(), null);
        nativeAd.renderAdView(adView);
        nativeAd.prepare(adView);
        nativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
            @Override
            public void onImpression(View view) {
                    if(onLockAdImpressedClicked==null){
                        return;
                    }

                onLockAdImpressedClicked.onAdImpressed();
            }

            @Override
            public void onClick(View view) {
                if(onLockAdImpressedClicked==null){
                    return;
                }
                onLockAdImpressedClicked.onAdClicked();
            }
        });
        shouldShowAd = true;
    }

    private void addAdView(){
        patternAdParent.addView(adView);
        Log.d("AppLock"," Parent Width And Height " + patternAdParent.getMeasuredWidth());
    }


    private void addBigAppIcon() {
        int size = Math.round(DimensionConverter.convertDpToPixel(60, getContext()));
        int margin = ((patternViewParent.getHeight() - patternView.getHeight()) / 2) - (size / 2);
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) appIconView.getLayoutParams();
        parms.height = size;
        parms.width = size;
        parms.bottomMargin = margin;
        appIconView.setLayoutParams(parms);
    }

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
        this.selectedPatternNode = this.selectedPatternNode + String.valueOf(selectedPatternNode);
        // Log.d("AppLock","Selected Pattern "+ this.selectedPatternNode);
        patternNodeSelectedCount = patternNodeSelectedCount + 1;
        if (isVibratorEnabled) {
            patternViewVibrator.vibrate(30);
        }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if (patternCompleted && !selectedPatternNode.equals("")) {
            // Log.d("PatternLock Confirm",selectedPatternNode);
            if (patternPassCode.equals(validatePassword(selectedPatternNode))) {
                //    Log.d("AppLock",selectedPatternNode + " " + patternPassCode);
                patternView.resetPatternView();
                resetPatternData();
                postPatternCompleted();
            } else {
                patternAnimator.start();
            }
        }
    }

    private String validatePassword(String userPass) {
        StringBuilder builder = new StringBuilder();
        try {
            byte[] usePassByte = (userPass + salt).getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = digest.digest(usePassByte);
            for (int i = 0; i < messageDigest.length; ++i) {
                builder.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1, 3));
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    @Override
    public void onErrorStop() {
        if (patternAnimator.isStarted()) {
            patternAnimator.end();
            patternAnimator.cancel();
            patternView.resetPatternView();
            resetPatternData();
        }
    }

    void resetPatternData() {
        selectedPatternNode = "";
        patternNodeSelectedCount = 0;
    }

    private void postPatternCompleted() {
        if (patternLockListener != null) {
            patternLockListener.onPinUnlocked();
        }
    }

    void startHome() {
        getContext().startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP && (event.getKeyCode() != KeyEvent.KEYCODE_BACK)) {
            return super.dispatchKeyEvent(event);
        }
        startHome();
        return true;
    }

    public void removeView() {
        unRegisterListeners();
        patternView.closePatternView();
        if(adView!=null){
            patternAdParent.removeView(adView);
        }
        setPinLockUnlockListener(null);
    }
}
