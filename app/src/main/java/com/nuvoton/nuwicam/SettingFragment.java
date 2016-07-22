package com.nuvoton.nuwicam;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.preference.Preference;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.longevitysoft.android.xml.plist.domain.PListObject;
import com.longevitysoft.android.xml.plist.domain.sString;
import com.nuvoton.socketmanager.ReadConfigure;
import com.nuvoton.socketmanager.SocketInterface;
import com.nuvoton.socketmanager.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, SocketInterface{
    private ReadConfigure configure;
    private SocketManager socketManager;
    private String key; 
    private static String platform, cameraSerial, preferenceName;
    private String TAG = "SettingFragment";
    private ArrayList<Preference> settingArrayList;
    OnHideBottomBarListener onHideBottomBarListener;

    public static SettingFragment newInstance(Bundle bundle){
        platform = bundle.getString("Platform");
        cameraSerial = bundle.getString("CameraSerial");
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    public interface OnHideBottomBarListener{
        public void onHideBottomBar(boolean isHide);
        public void onEnableClick(boolean isEnable);
        public void restartStream();
    }

    public SettingFragment(){
        Log.d(TAG, "SettingFragment: " + platform);
    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        preferenceName = "Setup Camera " + String.valueOf(cameraSerial);

//        getActivity().getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        getPreferenceManager().setSharedPreferencesName(preferenceName);
//        getPreferenceManager().getSharedPreferences();
        Log.d(TAG, "onCreate: " + preferenceName + " pref name: " + getPreferenceManager().getSharedPreferencesName());
        // Inflate the layout for this fragment

        if (platform.equals("NuWicam")) {
            addPreferencesFromResource(R.xml.settings_nuwicam);
        }
        if (!isAdded()) return;
        configure = ReadConfigure.getInstance(getActivity().getApplicationContext());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged: " + key);
        determineSettings(key, sharedPreferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        if (!isAdded()) return;
        getActivity().getApplicationContext().getSharedPreferences(preferenceName, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
        updateSetting();
        if (onHideBottomBarListener != null){
            onHideBottomBarListener.onEnableClick(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        if (!isAdded()) return;
        getActivity().getApplicationContext().getSharedPreferences(preferenceName, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
        if (onHideBottomBarListener != null){
            onHideBottomBarListener.onEnableClick(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isAdded()) return;
        try{
            onHideBottomBarListener = (OnHideBottomBarListener) getActivity();
        }catch (ClassCastException e){
            throw new ClassCastException(getActivity().toString() + " must implement onHideBottomBarListener");
        }
    }

    private void determineSettings(String key, SharedPreferences sharedPreference){
        socketManager = new SocketManager();
        socketManager.setSocketInterface(this);
        String command = new String( getDeviceURL() );
        sString baseCommand;
        String value, commandType = "";
        switch (key){
            case "Resolution":
                ArrayList<Map> videoCommandSet = configure.videoCommandSet;
                Map<String, PListObject> targetCommand = videoCommandSet.get(1);
                baseCommand = (sString) targetCommand.get("Base Command");
                value = sharedPreference.getString(key, "0");
                if (value.equals("0")){ // QVGA
                    command = command + baseCommand.getValue() + "&VINWIDTH=320&JPEGENCWIDTH=320&VINHEIGHT=240&JPEGENCHEIGHT=240";
                }else if (value.equals("1")){ // VGA
                    command = command + baseCommand.getValue() + "&VINWIDTH=640&JPEGENCWIDTH=640&VINHEIGHT=480&JPEGENCHEIGHT=480";
                }else{
                    command = command + baseCommand.getValue() + "&VINWIDTH=640&JPEGENCWIDTH=640&VINHEIGHT=360&JPEGENCHEIGHT=360";
                }
                commandType = SocketManager.CMDSET_UPDATE_VIDEO;
                Log.d(TAG, "determineSettings: Resolution");
                break;
            case "Bit Rate":
                videoCommandSet = configure.videoCommandSet;
                targetCommand = videoCommandSet.get(1);
                baseCommand = (sString) targetCommand.get("Base Command");
                value = sharedPreference.getString(key, "1024");
                command = command + baseCommand.getValue() + "&BITRATE=" + value;
                commandType = SocketManager.CMDSET_UPDATE_VIDEO;
                Log.d(TAG, "determineSettings: Bit Rate");
                break;
            case "SSID":
                String option = sharedPreference.getString(key, "NuWicam");
                videoCommandSet = configure.configCommandSet;
                targetCommand = videoCommandSet.get(1);
                baseCommand = (sString) targetCommand.get("Base Command");
                command = command + baseCommand.getValue() + "&AP_SSID=" + option;
                commandType = SocketManager.CMDSET_UPDATE_WIFI;
                Log.d(TAG, "determineSettings: SSID");
                break;
            case "Password":
                option = sharedPreference.getString(key, "12345678");
                videoCommandSet = configure.configCommandSet;
                targetCommand = videoCommandSet.get(1);
                baseCommand = (sString) targetCommand.get("Base Command");
                command = command + baseCommand.getValue() + "&AP_AUTH_KEY=" + option;
                commandType = SocketManager.CMDSET_UPDATE_WIFI;
                ListPreference listPreference = (ListPreference) getPreferenceManager().findPreference("Show Password");
                listPreference.setValue("1");
                Log.d(TAG, "determineSettings: password");
                break;
            case "Restart Stream":
                option = sharedPreference.getString(key, "1");
                if (option.equals("0")){
                    videoCommandSet = configure.systemCommandSet;
                    targetCommand = videoCommandSet.get(2);
                    baseCommand = (sString) targetCommand.get("Base Command");
                    command = command + baseCommand.getValue();
                }else {
                    return;
                }
                commandType = SocketManager.CMDSET_RESTART_STREAM;
                if (socketManager != null){
                    socketManager.setSocketInterface(this);
                    socketManager.executeSendGetTask(command, commandType);
                }
                sharedPreference.edit().apply();

                option = "1";
                sharedPreference.edit().putString(key, option);
                listPreference = (ListPreference) getPreferenceManager().findPreference(key);
                listPreference.setValue(option);
                onHideBottomBarListener.restartStream();
                Log.d(TAG, "determineSettings: stream " + command);
                return;
            case "Restart Wi-Fi":
                option = sharedPreference.getString(key, "1");
                if (option.equals("0")){
                    videoCommandSet = configure.systemCommandSet;
                    targetCommand = videoCommandSet.get(1);
                    baseCommand = (sString) targetCommand.get("Base Command");
                    command = command + baseCommand.getValue();
                }else {
                    return;
                }
                commandType = SocketManager.CMDSET_RESTART_WIFI;
                if (socketManager != null){
                    socketManager.setSocketInterface(this);
                    socketManager.executeSendGetTask(command, commandType);
                }
                sharedPreference.edit().apply();
                option = "1";
                sharedPreference.edit().putString(key, option);
                listPreference = (ListPreference) getPreferenceManager().findPreference(key);
                listPreference.setValue(option);
                onHideBottomBarListener.restartStream();
                Log.d(TAG, "determineSettings: " + command);
                return;
            case "Show Password":
                option = sharedPreference.getString(key, "1");
                if (option.equals("0")){
                    EditTextPreference pref = (EditTextPreference) getPreferenceManager().findPreference("Password");
                    pref.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_CLASS_TEXT);
                }else {
                    EditTextPreference pref = (EditTextPreference) getPreferenceManager().findPreference("Password");
                    pref.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
                }
                listPreference = (ListPreference) getPreferenceManager().findPreference(key);
                listPreference.setValue(option);
                return;
        }
        String commandTemp = new String(getDeviceURL());
        if (command.trim().equals(commandTemp)){
            return;
        }
        if (socketManager != null){
            socketManager.setSocketInterface(this);
            socketManager.executeSendGetTask(command, commandType);
        }
        sharedPreference.edit().apply();
    }

    private String getDeviceURL(){
        String cameraName = "Setup Camera " + cameraSerial;
        if (!isAdded()) return "";
        SharedPreferences preference = getActivity().getApplicationContext().getSharedPreferences(cameraName, Context.MODE_PRIVATE);
        String urlString = preference.getString("URL", "DEFAULT");
        String [] ipCut = urlString.split("/");
        String ip = ipCut[2];
        String url = "http://" + ip + ":80/cgi-bin/";
        return url;
    }

    @Override
    public void showToastMessage(String message, int duration) {
        Log.d(TAG, "showToastMessage: ");
        if (!isAdded()) return;
        Toast.makeText(getActivity().getApplicationContext(), message, duration).show();
    }

    @Override
    public void updateFileList(ArrayList<FileContent> fileList) {
        Log.d(TAG, "updateFileList: ");
    }

    @Override
    public void deviceIsAlive() {
        Log.d(TAG, "deviceIsAlive: ");
    }

    @Override
    public void updateSettingContent(String category, String value) {
        String cameraName = "Setup Camera " + cameraSerial;
        if (!isAdded()) return;
        SharedPreferences preference = getActivity().getApplicationContext().getSharedPreferences(cameraName, Context.MODE_PRIVATE);
        preference.edit().putString(category, value);
        preference.edit().commit();
        if (category.equals("List Wi-Fi Setting")){
            EditTextPreference pref = (EditTextPreference)getPreferenceManager().findPreference(category);
            if (value.equals("1"))
                pref.setSummary("Recorder is recording");
            else
                pref.setSummary("Recorder is stopped");
        }else if(category.equals("List Video Setting")){
            Preference pref = (Preference)getPreferenceManager().findPreference(category);
            if (value.equals("1"))
                pref.setSummary("Storage available on device.");
            else
                pref.setSummary("No storage available on device.");
        }else {
            ListPreference pref = (ListPreference) getPreferenceManager().findPreference(category);
            pref.setValue(value);
        }
    }

    @Override
    public void updateSettingContent(String category, JSONObject map) {
        String cameraName = "Setup Camera " + cameraSerial;
        if (!isAdded()) return;
        SharedPreferences preference = getActivity().getApplicationContext().getSharedPreferences(cameraName, Context.MODE_PRIVATE);
        if (category.equals("List Wi-Fi Setting")){
            EditTextPreference pref = (EditTextPreference)getPreferenceManager().findPreference("SSID");
            String ssid = null;
            try {
                ssid = map.getString("AP_SSID");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String password = null;
            try {
                password = map.getString("AP_AUTH_KEY");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pref.setText(ssid);
            pref = (EditTextPreference)getPreferenceManager().findPreference("Password");
            pref.setText(password);
            preference.edit().putString("SSID", ssid);
            preference.edit().putString("Password", password);
            preference.edit().apply();
        }else if(category.equals("List Video Setting")){
            String resolution = null, temp = null;
            try {
                resolution = map.getString("VINHEIGHT");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String BitRate = null;
            try {
                BitRate = map.getString("BITRATE");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            int bitRateValue = Integer.valueOf(BitRate);
            if (resolution.compareTo("240") == 0){
                temp = "0"; // QVGA
            }else if (resolution.compareTo("480") == 0){
                temp = "1"; // VGA
            }else {
                temp = "2"; // 360p
            }
            BitRate = String.valueOf(bitRateValue);
            preference.edit().putString("Resolution", temp);
            preference.edit().putString("Bit Rate", BitRate);
            preference.edit().apply();

            ListPreference pref = (ListPreference)getPreferenceManager().findPreference("Resolution");
            pref.setValue(temp);

            pref = (ListPreference)getPreferenceManager().findPreference("Bit Rate");
            pref.setValue(BitRate);
        }else {
        }
    }

    private void updateSetting(){
        socketManager = new SocketManager();
        socketManager.setSocketInterface(this);
        sString baseCommand;
        String commandType = "";
        ArrayList<String> commandList = new ArrayList<>();
        String command = getDeviceURL();

// list video parameter
        ArrayList<Map> videoCommandSet = configure.videoCommandSet;
        Map<String, PListObject> targetCommand = videoCommandSet.get(0);
        baseCommand = (sString) targetCommand.get("Base Command");
        command = command + baseCommand.getValue();
        commandList.add(command);

// list wifi parameter
        command = getDeviceURL();
        ArrayList<Map> configCommandSet = configure.configCommandSet;
        targetCommand = configCommandSet.get(0);
        baseCommand = (sString) targetCommand.get("Base Command");
        command = command + baseCommand.getValue();
        commandList.add(command);


        commandType = SocketManager.CMDGET_ALL;
        socketManager.setCommandList(commandList);
        socketManager.executeSendGetTaskList(commandList, commandType);
    }

}
