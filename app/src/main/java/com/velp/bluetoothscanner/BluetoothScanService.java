package com.velp.bluetoothscanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BluetoothScanService extends Service {

    private static final String CHANNEL_ID = "BluetoothScanChannel";

    public static int SCAN_INTERVAL_MS = 60000; // scan padrão a cada 60 segundos
    private final Handler handler = new Handler();
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
            handler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());
        handler.post(scanRunnable);
    }

    // No onStartCommand, adicione a lógica para capturar o novo intervalo
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("NEW_INTERVAL")) {
            int novoIntervalo = intent.getIntExtra("NEW_INTERVAL",SCAN_INTERVAL_MS);
            this.SCAN_INTERVAL_MS = novoIntervalo;

            // Reinicia o loop do handler com o novo tempo
            handler.removeCallbacks(scanRunnable);
            handler.post(scanRunnable);
        }
        return START_STICKY;
    }
    private void startScan() {
        // 1. Reinicia o canal com segurança
        // recreateNotificationChannel();
        createNotificationChannel();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }
    private void recreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {

                // 1. NÃO CHAME stopForeground().
                // Apenas delete o canal. O serviço continuará em foreground "órfão" por um instante.
                if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                    manager.deleteNotificationChannel(CHANNEL_ID);
                }

                // 2. Recrie o canal imediatamente
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Canal de Scanner Bluetooth",
                        NotificationManager.IMPORTANCE_LOW
                );
                manager.createNotificationChannel(serviceChannel);

                // 3. Em vez de startForeground (que causa o erro), use o notify()
                // para atualizar a notificação existente vinculando-a ao novo canal.
                manager.notify(1, getNotification());
            }
        }
    }
    public Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /* Intent dismissIntent = new Intent(this, NotificationDismissedReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );*/

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Scanner Service")
                .setContentText("O serviço está buscando dispositivos a cada "+SCAN_INTERVAL_MS / 1000 +" segundos.")

                .setSmallIcon(R.drawable.btscanner)
                .setOngoing(true) // <- IMPEDIR REMOÇÃO DA NOTIFICAÇÃO em  android =< 13
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                //.setDeleteIntent(dismissPendingIntent) // <- Detecta quando o usuário descarta a notificação android >= 14
                .build();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                // Verifica se o canal já existe
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel serviceChannel = new NotificationChannel(
                            CHANNEL_ID,
                            "Canal de Scanner Bluetooth",
                            NotificationManager.IMPORTANCE_LOW // Sem som

                    );
                    manager.createNotificationChannel(serviceChannel);
                } else
                    manager.notify(1, getNotification());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        handler.removeCallbacks(scanRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
