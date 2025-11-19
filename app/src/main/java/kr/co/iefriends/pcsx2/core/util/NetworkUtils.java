package kr.co.iefriends.pcsx2.core.util;

import android.content.Context;

public class NetworkUtils {
    public static boolean hasInternetConnection(Context context) {
        if (context == null) {
            return false;
        }
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                android.net.Network nw = cm.getActiveNetwork();
                if (nw == null) return false;
                android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(nw);
                return nc != null && (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                        || nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                        || nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnected();
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    //private static boolean hasInternetConnection() {
        //return hasInternetConnection(this);
    //}
}
