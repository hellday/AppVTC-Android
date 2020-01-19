package com.example.terry.appvtc;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


public class NetworkChangeReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {

        try
        {
            // Location GPS Receiver
            if (intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {

                //Toast.makeText(context, getGPSProviderMode(context), Toast.LENGTH_SHORT).show();

            }else {
                if (isOnline(context)) {

                    Log.v("Internet Test", "ON");

                    Intent in = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
                    in.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true);
                    context.sendBroadcast(in);
                } else {
                    Log.v("Internet Test", "OFF");

                    Intent in = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
                    in.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                    context.sendBroadcast(in);
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = null;
            if (cm != null) {
                netInfo = cm.getActiveNetworkInfo();
            }
            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getGPSProviderMode(Context context){
        String locationMode ="";
        ContentResolver contentResolver = context.getContentResolver();
        // Find out what the settings say about which providers are enabled
        int mode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (mode != Settings.Secure.LOCATION_MODE_OFF) {
            if (mode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                locationMode = "High accuracy. Uses GPS, Wi-Fi, and mobile networks to determine location";
            } else if (mode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) {
                locationMode = "Device only. Uses GPS to determine location";
            } else if (mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
                locationMode = "Battery saving. Uses Wi-Fi and mobile networks to determine location";
            }
        }else {
            locationMode = "Location MODE OFF";
        }
        return locationMode;
    }
}
