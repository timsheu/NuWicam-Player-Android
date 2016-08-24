package com.nuvoton.nuwicam;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.firebase.iid.FirebaseInstanceId;
import com.nuvoton.socketmanager.ReadConfigure;

import android.app.FragmentManager;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;

public class LivePage extends AppCompatActivity implements LiveFragment.OnHideBottomBarListener, SettingFragment.OnHideBottomBarListener {
    // live view callbacks
    public void onHideBottomBar(boolean isHide){
        if (isHide){
            bottomNavigation.hideBottomNavigation(true);
        } else {
            bottomNavigation.restoreBottomNavigation(true);
        }
    }

    public void onEnableClick(boolean isEnable){
        bottomNavigation.setEnabled(isEnable);
        Log.d(TAG, "onEnableClick: " + String.valueOf(isEnable));
    }

    @Override
    public void restartStream() {
        Log.d(TAG, "restartStream: ");
        if (liveFragment != null){
            liveFragment.stopStreamFromSetting();
        }
    }

    private int index=0;
    private String platform = "NuWicam";
    private String cameraSerial = "5";
    private boolean clicked = false;
    private boolean isLandscape = false;
    private static final String TAG = "SkyEye", FRAGMENT_TAG = "CURRENT_FRAGMENT_INDEX";
    private AHBottomNavigation bottomNavigation;
    private ArrayList<AHBottomNavigationItem> bottomNavigationItems = new ArrayList<>();
    private FragmentManager fragmentManager = getFragmentManager();
    private LiveFragment liveFragment = null;
    private SettingFragment settingFragment = null;
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        View decorView = getWindow().getDecorView();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (index != 1){
                bottomNavigation.hideBottomNavigation(true);
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                isLandscape = true;
            }
        } else {
            bottomNavigation.restoreBottomNavigation(true);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            isLandscape = false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int orientation = getWindowManager().getDefaultDisplay().getRotation();
        View decorView = getWindow().getDecorView();
        setContentView(R.layout.activity_live_page);
        Log.d(TAG, "onCreate:" + platform + ", " + cameraSerial);
        initUI();
        ReadConfigure configure = ReadConfigure.getInstance(this, false);
        String cameraName = "Setup Camera " + cameraSerial;
        SharedPreferences preference = getApplicationContext().getSharedPreferences(cameraName, Context.MODE_PRIVATE);
        String version = preference.getString("Version", "0");
        if (version.compareTo("1.1.6") != 0){
            configure.initSharedPreference(Integer.valueOf(cameraSerial), true);
        }
        switch (orientation){
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (index != 1){
                    bottomNavigation.hideBottomNavigation(true);
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    isLandscape = true;
                }
                break;
        }
//        Log.d(TAG, "onCreate: before get token");
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null){
            Log.d(TAG, "onCreate: token" + token);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        outState.putInt(FRAGMENT_TAG, index);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState: " + String.valueOf(savedInstanceState.getInt(FRAGMENT_TAG)));
        changeFragment(savedInstanceState.getInt(FRAGMENT_TAG));
    }

    private void initUI(){
        final Bundle bundle = new Bundle();
        bundle.putString("Platform", platform);
        bundle.putString("CameraSerial", cameraSerial);
        if (liveFragment == null){
            liveFragment = LiveFragment.newInstance(bundle);
        }
        if (settingFragment == null){
            settingFragment = SettingFragment.newInstance(bundle);
        }
        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
        AHBottomNavigationItem liveItem = new AHBottomNavigationItem("Live", R.drawable.livetab);
        final AHBottomNavigationItem settingItem = new AHBottomNavigationItem("Setting", R.drawable.geartab);

        bottomNavigationItems.add(liveItem);
        bottomNavigationItems.add(settingItem);

        bottomNavigation.addItems(bottomNavigationItems);
        bottomNavigation.setAccentColor(Color.parseColor("#007DFF"));

        bottomNavigation.setNotification(0, 0);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected){
                if (index == position) return true;
                FragmentTransaction trans = fragmentManager.beginTransaction();
                if (liveFragment.isAdded()) trans.hide(liveFragment);
                if (settingFragment.isAdded()) trans.hide(settingFragment);

                if (position == 0){
                    if (!liveFragment.isAdded()){
                        trans.add(R.id.fragment_content, liveFragment);
                    }else{
                        trans.show(liveFragment);
                    }
                }else{
                    if (!settingFragment.isAdded()){
                        trans.add(R.id.fragment_content, settingFragment);
                    }else{
                        trans.show(settingFragment);
                    }
                }
                bottomNavigation.setNotification(0, position);
                index = position;
                trans.commit();
                return true;
            }
        });
        FragmentTransaction trans = fragmentManager.beginTransaction();
        if (!liveFragment.isAdded()){
            trans.add(R.id.fragment_content, liveFragment).commit();
        }else{
            trans.hide(settingFragment).show(liveFragment).commit();
        }
    }

    private void changeFragment(int savedIndex){
        Bundle bundle = new Bundle();
        bundle.putString("Platform", platform);
        bundle.putString("CameraSerial", cameraSerial);
        index = savedIndex;
        if (index == 0){
            FragmentTransaction trans = fragmentManager.beginTransaction();
            if (!liveFragment.isAdded()){
                trans.hide(settingFragment).add(R.id.fragment_content, liveFragment).commit();
            }else{
                trans.hide(settingFragment).show(liveFragment).commit();
            }
        }else{
            FragmentTransaction trans = fragmentManager.beginTransaction();
            if (!settingFragment.isAdded()){
                trans.hide(liveFragment).add(R.id.fragment_content, settingFragment).commit();
            }else{
                trans.hide(liveFragment).show(settingFragment).commit();
            }
        }
        bottomNavigation.setNotification(0, index);
        bottomNavigation.setCurrentItem(index);
    }

    public void showBottomBar(boolean option){
        if (option == false){
            bottomNavigation.hideBottomNavigation(true);
        }else {
            bottomNavigation.restoreBottomNavigation(true);
        }
    }

    private boolean exit = false;
    @Override
    public void onBackPressed() {
        if (exit){
            super.onBackPressed();
            return;
        }else {
            Toast.makeText(this, "Press Back again to Exit !", Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3000);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

}
