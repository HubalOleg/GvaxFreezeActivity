package com.oleg.hubal.freezeactivityplugin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hubal on 4/4/2018.
 */


public class FreezeUnityPlayerActivity extends UnityPlayerActivity {

    private static final String TAG = "FreezeUnityPlayer";

    private boolean mIsPaused;
    private boolean mIsFirstReceive;
    private Handler mHandler = new Handler();
    private boolean mHdmiState;

    private void checkUnityPlayerResume() {
        mHandler.postDelayed(mRestartAppRunnable, 3000);
    }

    private Runnable mRestartAppRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences sharedPreferences = getSharedPreferences("Gvax", MODE_PRIVATE);
            boolean isPaused = sharedPreferences.getBoolean("KEY_PAUSED", false);

            if (isPaused) {
                exitApplication();
            }
        }
    };

    private Runnable mRecieveHdmiRunnable = new Runnable() {
        @Override
        public void run() {
            if (mHdmiState) {
                if (mIsPaused) {
                    onResume();
                }
            } else {
                if (!mIsPaused) {
                    onPause();
                }
            }
        }
    };

    private BroadcastReceiver mHdmiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsFirstReceive) {
                mIsFirstReceive = false;
                return;
            }

            if (!isScheduleEmpty()) return;

            mHdmiState = intent.getBooleanExtra("state", false);
            mHandler.postDelayed(mRecieveHdmiRunnable, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsPaused = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mIsFirstReceive = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.HDMI_PLUGGED");
        registerReceiver(mHdmiReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsPaused) {
            mIsPaused = false;
            checkUnityPlayerResume();
        }
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        mIsPaused = true;

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mHdmiReceiver);
    }

    private boolean isScheduleEmpty() {
        SharedPreferences sharedPreferences = getSharedPreferences("Gvax", MODE_PRIVATE);
        return sharedPreferences.getBoolean("KEY_SCHEDULE_EMPTY", true);
    }

    private void exitApplication() {
        if (!isApplicationLauncher()) {
            restartApplication();
        }
        System.exit(0);
    }

    private void restartApplication() {
        Intent mStartActivity = new Intent(getApplicationContext(), FreezeUnityPlayerActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
    }

    boolean isApplicationLauncher() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<>();
        filters.add(filter);

        final String myPackageName = getPackageName();
        List<ComponentName> activities = new ArrayList<>();
        final PackageManager packageManager = getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}