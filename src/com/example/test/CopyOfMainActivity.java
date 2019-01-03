package com.example.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CopyOfMainActivity extends Activity {

    static final String SPP_UUID = "00000000-0000-1000-8000-00805F9B34FB";
    private String targetDeviceAddr = "FE:F7:46:05:51:62";
    private int REQUEST_ENABLE_BT =1;
    private Button btnSearch;
    private TextView tvStatus;
    private TextView tvReceive;
    private EditText etSend;
    private Button btnSend;
    /*
     * private ListView lvBTDevices; private ArrayAdapter<String> adtDevices;
     */
    private List<String> lstDevices = new ArrayList<String>();
    private BluetoothAdapter btAdapt;
    private static BluetoothSocket btSocket;

    private BluetoothDevice remoteDevice;

    private OutputStream outStream;
    private InputStream inStream;
    
    private ReadRunnable readRunnable;
    
    private volatile boolean mReadable = true;
    
    BlueManager blueManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new ClickEvent());

        tvStatus = (TextView) findViewById(R.id.tv_status);

        tvReceive = (TextView) findViewById(R.id.tvReceive);
        etSend = (EditText) findViewById(R.id.etSend);
        btnSend = (Button) findViewById(R.id.btSend);

        /*
         * lvBTDevices = (ListView) findViewById(R.id.lvDevices); adtDevices =
         * new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
         * lstDevices); lvBTDevices.setAdapter(adtDevices);
         * lvBTDevices.setOnItemClickListener(new ItemClickEvent());
         */
        
        blueManager = new BlueManager();

        //btAdapt = BluetoothAdapter.getDefaultAdapter();

        if (!blueManager.getBtAdapter().isEnabled()) {
            // 提示开启蓝牙
            
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            /*new AlertDialog.Builder(this)
                    .setTitle("蓝牙开关未打开")
                    .setMessage("是否打开蓝牙")
                    .setPositiveButton("打开",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // TODO Auto-generated method stub
                                    boolean isTurnOnSuccess;
                                    isTurnOnSuccess = btAdapt.enable();
                                    if (!isTurnOnSuccess) {
                                        Toast.makeText(MainActivity.this,
                                                "打开失败", Toast.LENGTH_LONG)
                                                .show();
                                        finish();
                                    }
                                }
                            })
                    .setNegativeButton("取消",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // TODO Auto-generated method stub
                                    finish();
                                }
                            }).show();*/
        }

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(searchDevices, intent);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
               //已启用，进行下一步初始化工作
         }else if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED){
               //未启用，退出应用
               Toast.makeText(CopyOfMainActivity.this,"请启用蓝牙",Toast.LENGTH_SHORT).show();  
               finish();  
         }
    }


    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            StringBuilder keyName = new StringBuilder();
            for (int i = 0; i < lstName.length; i++) {
                keyName.append(lstName[i].toString());
            }
            // Toast.makeText(MainActivity.this, keyName, Toast.LENGTH_LONG)
            // .show();

            BluetoothDevice device = null;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    String str = "未配对|" + device.getName() + "|"
                            + device.getAddress();
                    if (lstDevices.indexOf(str) == -1) {
                        lstDevices.add(str);
                        // adtDevices.notifyDataSetChanged();
                    }

                    //
                    if (device.getName() == null) {
                        return;
                    }
                    if (device.getAddress().equals(targetDeviceAddr)) {
                        if (btAdapt.isDiscovering()) {
                            btAdapt.cancelDiscovery();
                        }
                        String[] items = { targetDeviceAddr };
                        final String address = device.getAddress();
                        new AlertDialog.Builder(context)
                                .setTitle("选择蓝牙设备")
                                .setSingleChoiceItems(items, 0,
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                // TODO Auto-generated method
                                                // stub
                                                new ConnectTask()
                                                        .execute(address);
                                                dialog.dismiss();
                                            }
                                        }).show();
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    tvStatus.setText("正在配对……");
                    break;

                case BluetoothDevice.BOND_BONDED:
                    tvStatus.setText("完成配对");
                    btnSend.setEnabled(true);
                    etSend.setVisibility(View.VISIBLE);
                    break;

                case BluetoothDevice.BOND_NONE:
                    tvStatus.setText("取消配对");
                    break;

                default:
                    break;
                }
            }
        }

    };

    class ConnectTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub
            remoteDevice = btAdapt.getRemoteDevice(params[0]);
            try {
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    remoteDevice.createBond();
                } else if (remoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connect();
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            // Toast.makeText(MainActivity.this, ""+btSocket,
            // Toast.LENGTH_LONG).show();
            // runOnUiThread(new Runnable() {
            // public void run() {
            // Toast.makeText(MainActivity.this, ""+btSocket,
            // Toast.LENGTH_LONG).show();
            // }
            // });
            return null;
        }

    }
    
    class SendTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub
            remoteDevice = btAdapt.getRemoteDevice(params[0]);
            try {
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    remoteDevice.createBond();
                } else if (remoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connect();
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            return null;
        }
        
    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == btnSearch) {
                if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(CopyOfMainActivity.this, "请先打开蓝牙",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (btAdapt.isDiscovering()) {
                    btAdapt.cancelDiscovery();
                }
                lstDevices.clear();
                Object[] lstDevice = btAdapt.getBondedDevices().toArray();
                for (int i = 0; i < lstDevice.length; i++) {
                    BluetoothDevice device = (BluetoothDevice) lstDevice[i];
                    String str = "已配对|" + device.getName() + "|"
                            + device.getAddress();
                    lstDevices.add(str); // 获取设备名称和mac地址
                    // adtDevices.notifyDataSetChanged();
                    if (device.getAddress().equals(targetDeviceAddr)) {
                        remoteDevice = device;
                        etSend.setVisibility(View.VISIBLE);
                        btnSend.setEnabled(true);
                        new ConnectTask().execute(targetDeviceAddr);
                        break;
                    }
                }
                setTitle("本机蓝牙地址：" + btAdapt.getAddress());
                btAdapt.startDiscovery();
            }
        }

    }

    class ItemClickEvent implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            // TODO Auto-generated method stub
            if (btAdapt.isDiscovering()) {
                btAdapt.cancelDiscovery();
            }

            String str = lstDevices.get(position);
            String[] values = str.split("\\|");
            String address = values[2];

            BluetoothDevice remoteDevice = btAdapt.getRemoteDevice(address);
            try {
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    remoteDevice.createBond();
                } else if (remoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connect();
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

    }

    private void connect() {
        UUID uuid = UUID.fromString(SPP_UUID);

        try {
            btSocket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
            
            outStream.write("I am client".getBytes());
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            try {
                btSocket.close();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        if (btAdapt.isDiscovering()) {
            btAdapt.cancelDiscovery();
        }
        this.unregisterReceiver(searchDevices);
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void onClick(View v) {
        if (v == btnSend) {

            String str = etSend.getText().toString();
            if (str == null) {
                Toast.makeText(CopyOfMainActivity.this, "send message can't be null",
                        0).show();
                return;
            }

            sendMessage(targetDeviceAddr, etSend.getText().toString());

        }
    }

    private void sendMessage(String targetDeviceAddr2, String message) {
        // TODO Auto-generated method stub
        if (remoteDevice == null) {
            remoteDevice = btAdapt.getRemoteDevice(targetDeviceAddr2);
        }

        if (remoteDevice == null) {
            return;
        }

        if (btSocket == null) {
            UUID uuid = UUID.fromString(SPP_UUID);

            try {
                btSocket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
                /*btSocket.connect();
                outStream = btSocket.getOutputStream();
                inStream = btSocket.getInputStream();

                if (!btSocket.isConnected()) {
                    resetSocket();
                }
                */
                if (btSocket != null) {
                    btSocket.connect();
                    outStream = btSocket.getOutputStream();
                    outStream.write(message.getBytes());
                    outStream.flush();
                    outStream.close();
                    inStream = btSocket.getInputStream();
                    byte[] b = new byte[1024];
                    inStream.read(b);
                    Toast.makeText(this, "this received data:"+new String(b), Toast.LENGTH_LONG).show();
                    inStream.close();
                    btSocket.close();
                }

                /*if (outStream != null) {
                    try {
                        outStream.write((message.getBytes("utf-8")));
                        Log.e("MainActivity",
                                "onItemClick: " + btAdapt.getName() + ":"
                                        + message);
                    } catch (Exception e) {
                        resetSocket();
                        sendMessage(targetDeviceAddr2, message);
                    }
                }*/
            } catch (IOException e) {
                // TODO Auto-generated catch block
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

    private void resetSocket() {
        // TODO Auto-generated method stub
        UUID uuid = UUID.fromString(SPP_UUID);
        try {
            btSocket.close();
            btSocket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    class ReadRunnable implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            mReadable = true;
            InputStream stream = inStream;
            byte[] buffer = new byte[1024];
            //StringBuilder strBuilder = new StringBuilder();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            try {
                while (mReadable) {
                    int count = 0;
                    while (count == 0) {
                        count = stream.available();//输入流中的数据个数。
                    }
                    while((len=stream.read(buffer))!=-1) {
                        baos.write(buffer, 0, len);
                    }
                    final String str = baos.toString();
                    
                    baos.close();
                    stream.close();
                    
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvReceive.setText(str);
                        }
                    });
                }
                
            } catch (Exception e) {
                // TODO: handle exception
            }
            
        }
        
    }
}
