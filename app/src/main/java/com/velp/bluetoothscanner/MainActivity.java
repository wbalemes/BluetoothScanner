package com.velp.bluetoothscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private TextView logTextView;
    private Set<String> dispositivosEncontrados = new HashSet<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                dispositivosEncontrados.clear();
                String hora = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                logTextView.setText("🔍 [" + hora + "] Iniciando busca. \n Nova busca em 60 segundos...");

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null && !dispositivosEncontrados.contains(device.getAddress())) {
                    dispositivosEncontrados.add(device.getAddress());
                    String name = device.getName();
                    String address = device.getAddress();
                    logTextView.append("\n\n📱 " + name + "\n [" + address + "]");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        Button btnStart = findViewById(R.id.btnStartService);
        Button btnStop = findViewById(R.id.btnStopService);

        btnStart.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, BluetoothScanService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent);
                } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Serviço iniciado!", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, BluetoothScanService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Serviço parado!", Toast.LENGTH_SHORT).show();
        });


        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                insetsController.setAppearanceLightStatusBars(false); // ícones brancos
             } else {
            insetsController.setAppearanceLightStatusBars(true); // ícones escuros
        }

        logTextView = findViewById(R.id.logTextView);
            //nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            logTextView.setTextColor(Color.WHITE);
             } else {
            logTextView.setTextColor(Color.BLACK);
        }

        if (checkPermissions()) {
            startForegroundService(new Intent(this, BluetoothScanService.class));
            } else {
            requestPermissions();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.POST_NOTIFICATIONS
                }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startForegroundService(new Intent(this, BluetoothScanService.class));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
