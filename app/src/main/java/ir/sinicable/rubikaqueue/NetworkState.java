package ir.sinicable.rubikaqueue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Lightweight network gate so recipient retry counters are not consumed while offline. */
public final class NetworkState {
    private NetworkState() {}

    public static boolean online(Context context) {
        try {
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return true; // Fail open; actual socket errors remain retryable.
            Network network=cm.getActiveNetwork();
            if(network==null)return false;
            NetworkCapabilities caps=cm.getNetworkCapabilities(network);
            if(caps==null)return false;
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (RuntimeException unavailable) {
            return true;
        }
    }
}
