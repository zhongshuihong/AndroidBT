package com.example.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class BlueManager {

    static final String SPP_UUID = "00000000-0000-1000-8000-00805F9B34FB";
    public static final String strTargetDeviceAddr = "FE:F7:46:05:51:62";
    private String pin = "123456";
    
    private BluetoothAdapter mbluetoothAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice mbluetoothDevice;
    private OutputStream outStream;
    private InputStream inStream;
    
    //private ReadRunnable readRunnable;
    
    public BlueManager() {
        mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
    }
    
    public BluetoothAdapter getBtAdapter() {
        return mbluetoothAdapter;
    }
    
    public boolean isBlueEnable() {
        return isSupportBlue() && mbluetoothAdapter.isEnabled();
    }
    
    public boolean isSupportBlue() {
        return mbluetoothAdapter != null;
    }
    
    public void pairing(Intent intent) {
        mbluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                BluetoothDevice.ERROR);
        try {
            //ClsUtils.setPairingConfirmation(mbluetoothDevice.getClass(), mbluetoothDevice, true);
            Method setPairingConfirmation = mbluetoothDevice.getClass().getDeclaredMethod("setPairingConfirmation", boolean.class);
            setPairingConfirmation.invoke(mbluetoothDevice, true);
            boolean ret = ClsUtils.setPin(mbluetoothDevice.getClass(), mbluetoothDevice, pin);
            Method removeBondMethod = mbluetoothDevice.getClass().getDeclaredMethod("setPin", new Class[]{byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(mbluetoothDevice, new Object[]{pin.getBytes()});
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
    
    public boolean searchAndConnectToDevice() {
        if (mbluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
            //Toast.makeText(MainActivity.this, "请先打开蓝牙",
            //        Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (mbluetoothAdapter.isDiscovering()) {
            mbluetoothAdapter.cancelDiscovery();
        }
        Set<BluetoothDevice> pairedDevices = mbluetoothAdapter.getBondedDevices();
        //if there are paired devices
        if (pairedDevices.size() > 0) {
            //loop through paired devices and find the target
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(strTargetDeviceAddr)) {
                    try {
                        connectToDevice();
                        //new ConnectTask().execute(device);
                    } catch (Exception e) {
                        // TODO: handle exception
                        return false;
                    }
                    return true;
                }
            }
        }
        mbluetoothAdapter.startDiscovery();
        return true;
    }
    
    public void connectToDevice() {
        //new ConnectTask().execute(mbluetoothDevice);
        try {
            if (mbluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                mbluetoothDevice.createBond();
            } else if (mbluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                connect(mbluetoothDevice);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
    
    
    private void connect(BluetoothDevice device) {
        UUID uuid = UUID.fromString(SPP_UUID);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
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
    
    public String searchDevice(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        //if the device is not bounded and the device is the target
        if ((device.getBondState() == BluetoothDevice.BOND_NONE)
                && (device.getAddress().equals(strTargetDeviceAddr))) {
            //stop discovering
            if (mbluetoothAdapter.isDiscovering()) {
                mbluetoothAdapter.cancelDiscovery();
            }
            mbluetoothDevice = device;
            return device.getName()+"|"+device.getAddress();
        }
        return null;
    }
    
    public int getBondState(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device != null) {
            return device.getBondState();
        }
        return -1;
    }
    
    public String sendMessage(String message) {
        // TODO Auto-generated method stub
        if (mbluetoothDevice == null) {
            mbluetoothDevice = mbluetoothAdapter.getRemoteDevice(strTargetDeviceAddr);
        }

        if (mbluetoothDevice == null) {
            return null;
        }

        String str = null;
        UUID uuid = UUID.fromString(SPP_UUID);

        try {
            if (btSocket == null) {
                btSocket = mbluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();
            }
            /*btSocket.connect();
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();

            if (!btSocket.isConnected()) {
                resetSocket();
            }
            */
            if (btSocket != null) {
                //btSocket.connect();
                int i = 0;
                Handler handle = MainActivity.getHandler();
                //while (true) {
                    outStream = btSocket.getOutputStream();
                    inStream = btSocket.getInputStream();
                    outStream.write((message+i).getBytes());
                    outStream.flush();
                    //outStream.close();
                    i++;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    byte[] b = new byte[1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len2 = inStream.read(b);
                    baos.write(b, 0, len2);
                    Log.e("zsh sendMessage", baos.toString());
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.obj = baos.toString();
                    handle.sendMessage(msg);
                //}
                //outStream.flush();
                //outStream.close();
                /*try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                /*inStream = btSocket.getInputStream();
                Thread.sleep(500);
                int len = inStream.available();
                byte[] b = new byte[len];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len2 = inStream.read(b);
                baos.write(b, 0, len2);
                str = baos.toString();
                //Toast.makeText(this, "this received data:"+new String(b), Toast.LENGTH_LONG).show();
                inStream.close();
                btSocket.close();*/
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
        return str;
    
    }
    
    public void receiveMessage() {
        
    }
    
    public void finish() {
        if (mbluetoothAdapter.isDiscovering()) {
            mbluetoothAdapter.cancelDiscovery();
        }
        
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
