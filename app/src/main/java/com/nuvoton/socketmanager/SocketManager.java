package com.nuvoton.socketmanager;

import android.os.AsyncTask;
import android.util.Log;

import com.nuvoton.nuwicam.FileContent;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;

/**
 * Created by timsheu on 6/3/16.
 */
public class SocketManager {
    private ArrayList<String> commandList;
    private SocketInterface socketInterface = null;
    private URL url;
    private static final String TAG = "SocketManager";
    public static final String CMDSET_RESTART_STREAM="0";
    public static final String CMDSET_RESTART_WIFI="1";
    public static final String CMDSET_LIST_WIFI="2";
    public static final String CMDSET_UPDATE_WIFI="3";
    public static final String CMDSET_LIST_VIDEO="4";
    public static final String CMDSET_UPDATE_VIDEO="5";
    public static final String CMDGET_ALL="6";
    public static final String CMDGET_ALIVE="7";



    Timer timer = new Timer();

    private InputStream OpenHttpConnection(String urlString) throws IOException
    {
        InputStream in =null;
        int response = -1;

        url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if(!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if(response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        }
        catch (Exception ex)
        {
            //Log.d("Networking", ex.getLocalizedMessage());
            ex.printStackTrace();
            throw new IOException("Error connceting");
        }
        return  in;
    }
    private String SendGet(String url)
    {

        try {
            InputStream In = OpenHttpConnection(url);
            if (In == null){
                return null;
            }
            InputStreamReader isr = new InputStreamReader(In);
            int count = 0;
            int charRead;
            String result = "";
            //while (count == 0) {
            //    count = In.available();
            // }
            char[] buf = new char[64];
            //In.read(b);
            try{
                while((charRead=isr.read(buf))>0){
                    String readString = String.copyValueOf(buf,0,charRead);
                    result+=readString;
                    buf = new char[64];
                }
            }catch (IOException e){
                Log.d(TAG,e.getLocalizedMessage());
            }
            Log.d(TAG, "Response Content from server: " +result);

            In.close();
            return   result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private class SendGetTask extends AsyncTask<String,Void,String> {
        String httpcmd="";
        @Override
        protected String doInBackground(String... params) {
            String result= SendGet(params[0]);
            httpcmd = params[1];
            return result;
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject;
                if(httpcmd.equals(CMDGET_ALL)){
                    jsonObject = new JSONObject(result);
                    switch (commandList.size()){
                        case 0:
                            return;
                        case 1:
                            socketInterface.updateSettingContent("List Wi-Fi Setting", jsonObject);
                            break;
                        case 2:
                            socketInterface.updateSettingContent("List Video Setting", jsonObject);
                            break;

                    }
                    commandList.remove(0);
                    if (commandList.size() >0){
                        executeSendGetTaskList(commandList, CMDGET_ALL);
                    }
                    Log.d(TAG,"get all");
                }else if(httpcmd.equals(CMDSET_LIST_VIDEO)){
                    jsonObject = new JSONObject(result);
                    Log.d(TAG,"set recorder");
                    socketInterface.updateSettingContent("", jsonObject);
                }else if(httpcmd.equals(CMDSET_LIST_WIFI)){
                    jsonObject = new JSONObject(result);
                    Log.d(TAG,"set recorder");
                    if (result != null){
                        socketInterface.updateSettingContent("List Wi-Fi Setting", jsonObject);
                    }
                }else if(httpcmd.equals(CMDSET_UPDATE_VIDEO)){
                    jsonObject = new JSONObject(result);
                    Log.d(TAG,"set recorder");
                    if (result != null){
                        socketInterface.updateSettingContent("List Video Setting", jsonObject);
                    }
                }else if(httpcmd.equals(CMDSET_UPDATE_WIFI)){
                    jsonObject = new JSONObject(result);
                    Log.d(TAG,"set recorder");
                    if (result != null){
                        socketInterface.updateSettingContent("Recorder Status", jsonObject);
                    }
                }else if(httpcmd.equals(CMDGET_ALIVE)){
                    jsonObject = new JSONObject(result);
                    String resolution = (String) jsonObject.get("BITRATE");
                    if (resolution != null){
                        Log.d(TAG,"device is alive");
                        socketInterface.deviceIsAlive();
                    }else {
                        Log.d(TAG,"device is not alive:");
                    }
                }else if(httpcmd.equals(CMDSET_RESTART_STREAM)){
                    if (result != null){
                        Log.d(TAG,"Stream restarted");
                    }else {
                        Log.d(TAG,"Stream restart fail");
                    }
                }else if(httpcmd.equals(CMDSET_RESTART_WIFI)){
                    if (result != null){
                        Log.d(TAG,"Wi-Fi restarted");
                    }else {
                        Log.d(TAG,"Wi-Fi restart fail");
                    }
                }

                // }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void setSocketInterface(SocketInterface socketInterface){
        this.socketInterface = socketInterface;
    }

    public void executeSendGetTask(String command, String commandType){
        new SendGetTask().execute(command, commandType);
    }

    public void executeSendGetTaskList(ArrayList<String> list, String commandType){
        new SendGetTask().execute(list.get(0), commandType);
    }

    public void setCommandList(ArrayList<String> list){
        Log.d(TAG, "setCommandList: " + list.toString());
        commandList = list;
    }
}
