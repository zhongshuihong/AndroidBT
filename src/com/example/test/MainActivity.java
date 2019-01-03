package com.example.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

/*    static final String SPP_UUID = "00000000-0000-1000-8000-00805F9B34FB";
    private String targetDeviceAddr = "FE:F7:46:05:51:62";*/
    private int REQUEST_ENABLE_BT = 1;
    private Button btnSearch;
    private TextView tvStatus;
    private TextView tvReceive;
    private EditText etSend;
    private Button btnSend;
    /*
     * private ListView lvBTDevices; private ArrayAdapter<String> adtDevices;
     */
    // private List<String> lstDevices = new ArrayList<String>();
    // private BluetoothAdapter btAdapt;

    // private ReadRunnable readRunnable;

    // private volatile boolean mReadable = true;

    BlueManager mblueManager;
    
    public static Handler handle;
    public static Handler getHandler() {
        return  handle;
    }
    

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
        
        handle = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 1) {
                    tvReceive.setText(msg.obj.toString());
                }
            }
        };

        /*
         * lvBTDevices = (ListView) findViewById(R.id.lvDevices); adtDevices =
         * new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
         * lstDevices); lvBTDevices.setAdapter(adtDevices);
         * lvBTDevices.setOnItemClickListener(new ItemClickEvent());
         */

        mblueManager = new BlueManager();

        // btAdapt = BluetoothAdapter.getDefaultAdapter();

        if (!mblueManager.isBlueEnable()) {
            // 提示开启蓝牙

            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        }

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(searchDevices, intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            // 已启用，进行下一步初始化工作
        } else if (requestCode == REQUEST_ENABLE_BT
                && resultCode == RESULT_CANCELED) {
            // 未启用，退出应用
            Toast.makeText(MainActivity.this, "请启用蓝牙", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();

            // BluetoothDevice device = null;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String strDevice = mblueManager.searchDevice(intent);
                if (strDevice != null) {
                    // choose to connect device
                    String[] items = { strDevice };
                    new AlertDialog.Builder(context)
                            .setTitle("选择蓝牙设备")
                            .setSingleChoiceItems(items, 0,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            // TODO Auto-generated method stub
                                            mblueManager.connectToDevice();
                                            dialog.dismiss();
                                        }
                                    }).show();
                } else {
                    // not found
/*                    new AlertDialog.Builder(context).setTitle("查找结束")
                            .setMessage("蓝牙设备未找到，请确认设备开启状态").show();*/
                }
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                abortBroadcast();
                mblueManager.pairing(intent);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.ERROR);
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);

                new AlertDialog.Builder(context)
                                .setTitle("需要pin码")
                                .setMessage("type="+type+"|pairingKey"+pairingKey)
                                .show();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                switch (mblueManager.getBondState(intent)) {
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

    class SendTask extends AsyncTask<String, String, Void> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(String... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            tvReceive.setText(values[0]);
        }

        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub
            String str = mblueManager.sendMessage(params[0]);
            if (str != null) {
                publishProgress(str);
            }
            // remoteDevice = btAdapt.getRemoteDevice(params[0]);
            /*
             * try { if (remoteDevice.getBondState() ==
             * BluetoothDevice.BOND_NONE) { remoteDevice.createBond(); } else if
             * (remoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
             * connect(); } } catch (Exception e) { // TODO: handle exception
             * e.printStackTrace(); }
             */
            return null;
        }

    }
    
    class ReceiveTask extends AsyncTask<Void, String, Void> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(String... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }

    class ConnectTask extends AsyncTask<Void, Boolean, Void> {

        /*
         * (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(Boolean... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            if (values[0]) {
                etSend.setVisibility(View.VISIBLE);
                btnSend.setEnabled(true);
            } else {
                new AlertDialog.Builder(MainActivity.this).setTitle("未连接")
                        .setMessage("未连接").show();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            if (mblueManager.searchAndConnectToDevice()) {
                publishProgress(true);
            } else {
                publishProgress(false);
            }
            // mblueManager.connectToDevice();
            return null;
        }

    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == btnSearch) {
                if (mblueManager.getBtAdapter().getState() == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(MainActivity.this, "请先打开蓝牙",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                new ConnectTask().execute();
                // mblueManager.searchAndConnectToDevice();

            }
        }

    }

    /*
     * private void connect() { UUID uuid = UUID.fromString(SPP_UUID);
     * 
     * try { btSocket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
     * btSocket.connect(); outStream = btSocket.getOutputStream(); inStream =
     * btSocket.getInputStream();
     * 
     * outStream.write("I am client".getBytes()); outStream.flush();
     * outStream.close(); } catch (IOException e) { // TODO Auto-generated catch
     * block try { btSocket.close(); } catch (IOException e1) { // TODO
     * Auto-generated catch block e1.printStackTrace(); } e.printStackTrace(); }
     * 
     * }
     */

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        mblueManager.finish();
        this.unregisterReceiver(searchDevices);

        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void onSendClick(View v) {
        if (v == btnSend) {

            String str = etSend.getText().toString();
            if (str == null) {
                Toast.makeText(MainActivity.this, "send message can't be null",
                        0).show();
                return;
            }
            new SendTask().execute(etSend.getText().toString());
            new ReceiveTask().execute();
            // mblueManager.sendMessage(targetDeviceAddr,
            // etSend.getText().toString());

        }
    }

    /*
     * private void resetSocket() { // TODO Auto-generated method stub UUID uuid
     * = UUID.fromString(SPP_UUID); try { btSocket.close(); btSocket =
     * remoteDevice.createRfcommSocketToServiceRecord(uuid); btSocket.connect();
     * outStream = btSocket.getOutputStream(); } catch (IOException e) {
     * e.printStackTrace(); } }
     */

    /*
     * class ReadRunnable implements Runnable {
     * 
     * @Override public void run() { // TODO Auto-generated method stub
     * mReadable = true; InputStream stream = inStream; byte[] buffer = new
     * byte[1024]; //StringBuilder strBuilder = new StringBuilder();
     * ByteArrayOutputStream baos = new ByteArrayOutputStream(); int len = -1;
     * try { while (mReadable) { int count = 0; while (count == 0) { count =
     * stream.available();//输入流中的数据个数。 } while((len=stream.read(buffer))!=-1) {
     * baos.write(buffer, 0, len); } final String str = baos.toString();
     * 
     * baos.close(); stream.close();
     * 
     * runOnUiThread(new Runnable() { public void run() {
     * tvReceive.setText(str); } }); }
     * 
     * } catch (Exception e) { // TODO: handle exception }
     * 
     * }
     * 
     * }
     */
}
