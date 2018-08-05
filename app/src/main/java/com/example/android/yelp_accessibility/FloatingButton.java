package com.example.android.yelp_accessibility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

/**
 * Essentially just copy pasted from https://github.com/recruit-lifestyle/FloatingView
 */
public class FloatingButton extends Service implements FloatingViewListener{

    private FloatingViewManager fvm;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if(fvm != null) {
            return START_STICKY;
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        final LayoutInflater inflater = LayoutInflater.from(this);
        final ImageView iconView = (ImageView) inflater.inflate(R.layout.widget_floatbutton, null, false);
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YelpAccessibilityService.buttonClick();
            }
        });

        fvm = new FloatingViewManager(this, this);
        fvm.setFixedTrashIconImage(R.drawable.ic_trash_fixed);                                  // trash_fixed and trash_action don't seem to be the right size.
        fvm.setActionTrashIconImage(R.drawable.ic_trash_action);
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        options.overMargin = (int) (16 * metrics.density);
        fvm.addViewToWindow(iconView, options);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (fvm != null) {
            fvm.removeAllViewToWindow();
            fvm = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {

    }
}