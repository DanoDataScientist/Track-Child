package com.bmh.trackchild.services;

import java.util.HashSet;

import com.bmh.trackchild.ChildLocation;
import com.bmh.trackchild.R;
import com.bmh.trackchild.Tools.MediaAlert;
import com.bmh.trackchild.Tools.SharedPrefs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class BluetoothService extends Service {

    BluetoothAdapter mBluetoothAdapter;
    Thread thread;
    boolean threadFlag;
    HashSet<BluetoothDevice> devicesSet;
    MediaAlert mediaAlert;

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devicesSet = new HashSet<BluetoothDevice>();
        threadFlag = true;
        mediaAlert = new MediaAlert(getApplicationContext());
        discoverDevices();
    }


    public void displayAlertDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothService.this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View content = inflater.inflate(R.layout.confirmation_dialog, null);
        ((TextView) content.findViewById(R.id.txtTitle)).setText(getApplicationContext().getResources().getString(R.string.out_of_range));
        ((TextView) content.findViewById(R.id.btnYes)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thread.interrupted();
                mediaAlert.stopAlert();
                startActivity(new Intent(BluetoothService.this, ChildLocation.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            }
        });
        ((TextView) content.findViewById(R.id.btnNo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaAlert.stopAlert();

            }
        });
        builder.setView(content);
        builder.setCancelable(false);

        AlertDialog alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.getWindow().getAttributes().gravity = Gravity.CENTER;
        alert.show();


    }


    private void discoverDevices() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mRepeatedReceiver, filter);

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();

    }

    private void startPeriodicDiscovery() {
        //sleep 30 seconds and startDiscovery()
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (threadFlag) {
                        Thread.sleep(30 * 1000);
                        mBluetoothAdapter.startDiscovery();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
    }

    private final BroadcastReceiver mRepeatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            BluetoothDevice childDevice = mBluetoothAdapter.getRemoteDevice(new SharedPrefs(getApplicationContext()).getPreferences(R.string.Key_ChildDeviceAddressMac, ""));

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devicesSet.add(foundDevice);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (devicesSet.contains(childDevice)) {
                    Toast.makeText(getApplicationContext(), "Device in range", Toast.LENGTH_LONG).show();
                    //clear device set
                    devicesSet.clear();
                    startPeriodicDiscovery();

                } else {
                    // ////////alert that your child's device not found/////////////
                   mediaAlert.startAlert();
                    displayAlertDialog();
                    devicesSet.clear();
                }

            }
        }
    };


    @Override
    public void onDestroy() {
        threadFlag = false;
        unregisterReceiver(mRepeatedReceiver);
    }

    ;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
