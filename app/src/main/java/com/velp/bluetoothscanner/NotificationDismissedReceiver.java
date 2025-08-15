package com.velp.bluetoothscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationDismissedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            Intent restartIntent = new Intent(context, BluetoothScanService.class);
            context.startForegroundService(restartIntent);
        }
    }
}
