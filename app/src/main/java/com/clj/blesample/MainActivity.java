package com.clj.blesample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.blesample.adapter.DeviceAdapter;
import com.clj.blesample.aesUtil.AESUtil;
import com.clj.blesample.comm.ObserverManager;
import com.clj.blesample.operation.OperationActivity;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

//    private LinearLayout layout_setting;
//    private TextView txt_setting;
//    private EditText et_name, et_mac, et_uuid;
//    private Switch sw_auto;
    private Button btn_scan;
    private ImageView img_loading;

    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;


    UUID UUID36f6 = UUID.randomUUID();
    UUID UUIDService = UUID.randomUUID();
    UUID UUID36f5 = UUID.randomUUID();

    // password
    private byte[] password = new byte[]{0x01,0x02,0x03,0x04,0x05,0x06};
    private byte[] new_password = new byte[]{0x01,0x02,0x03,0x04,0x05,0x06};

    //init token
    private byte[] token = new byte[4];

    //openlock byte[]
    private byte[] openlock = new byte[]{0x05,0x01,0x06,0x30,0x30,0x30,0x30,0x30,
                                         0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};
    //获取token的byte[]
    byte[] access_token = new byte[]{0x06,0x01,0x01,0x01,0x5C,0x01,0x21,0x1F,
                                     0x29,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    //获取lock status的byte[]
    byte[] lock_status = new byte[]{0x05,0x0E,0x01,0x01,0x5C,0x01,0x21,0x1F,
                                    0x29,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    //开仓的byte[]
    byte[] open_store = new byte[]{0x10,0x01,0x06,0x30,0x30,0x30,0x30,0x30,
                                   0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    //获取电量的byte[]
    byte[] access_power = new byte[]{0x02,0x01,0x01,0x01,0x30,0x30,0x30,0x30,
                                     0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    //修改密码
    byte[] change_password_1 = new byte[]{0x05,0x03,0x06,0x01,0x30,0x30,0x30,0x30,
                                          0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};
    byte[] change_password_2 = new byte[]{0x05,0x04,0x06,0x01,0x30,0x30,0x30,0x30,
                                          0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    // 查询工作状态
    byte[] full_status = new byte[]{0x05,0x22,0x01,0x00,0x30,0x30,0x30,0x30,
                                    0x30,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化
        initView();

        BleManager.getInstance().init(getApplication());

        BleManager.getInstance()
                .enableLog(true)
                .setMaxConnectCount(7)
                .setOperateTimeout(5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BleManager.getInstance().cancelScan();
                }
                break;

        }
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));
        btn_scan.setOnClickListener(this);

        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(BleDevice bleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            @Override
            public void onDisConnect(BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice);
                }
            }

//            @Override
//            public void onDetail(BleDevice bleDevice) {
//                if (BleManager.getInstance().isConnected(bleDevice)) {
//                    Intent intent = new Intent(MainActivity.this, OperationActivity.class);
//                    intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
//                    startActivity(intent);
//                }
//            }

            @Override
            public void onOpenLock(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }

                openlock[3] = password[0];
                openlock[4] = password[1];
                openlock[5] = password[2];
                openlock[6] = password[3];
                openlock[7] = password[4];
                openlock[8] = password[5];
                openlock[9] = token[0];
                openlock[10] = token[1];
                openlock[11] = token[2];
                openlock[12] = token[3];
                byte[] encrypt = AESUtil.Encrypt(openlock, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "open lock");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "open lock");
                                System.out.println(exception.toString());
                            }
                        });


            }

            @Override
            public void onLockStatus(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }

                lock_status[4] = token[0];
                lock_status[5] = token[1];
                lock_status[6] = token[2];
                lock_status[7] = token[3];
                byte[] encrypt = AESUtil.Encrypt(lock_status, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "access status");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "access status");
                                System.out.println(exception.toString());
                            }
                        });
            }

            @Override
            public void onChanged(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }
                open_store[3] = password[0];
                open_store[4] = password[1];
                open_store[5] = password[2];
                open_store[6] = password[3];
                open_store[7] = password[4];
                open_store[8] = password[5];
                open_store[9] = token[0];
                open_store[10] = token[1];
                open_store[11] = token[2];
                open_store[12] = token[3];
                byte[] encrypt = AESUtil.Encrypt(open_store, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "open store");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "open store");
                                System.out.println(exception.toString());
                            }
                        });

            }

            @Override
            public void onPower(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }


                access_power[4] = token[0];
                access_power[5] = token[1];
                access_power[6] = token[2];
                access_power[7] = token[3];

                byte[] encrypt = AESUtil.Encrypt(access_power, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "access power");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "access power");
                                System.out.println(exception.toString());
                            }
                        });

            }

            @Override
            public void onFullStatus(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }

                full_status[4] = token[0];
                full_status[5] = token[1];
                full_status[6] = token[2];
                full_status[7] = token[3];

                byte[] encrypt = AESUtil.Encrypt(full_status, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "full status");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "full status");
                                System.out.println(exception.toString());
                            }
                        });


            }

            @Override
            public void onChangePassword(BleDevice bleDevice){
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    connect(bleDevice);
                }

                change_password_1[3] = password[0];
                change_password_1[4] = password[1];
                change_password_1[5] = password[2];
                change_password_1[6] = password[3];
                change_password_1[7] = password[4];
                change_password_1[8] = password[5];
                change_password_1[9] = token[0];
                change_password_1[10] = token[1];
                change_password_1[11] = token[2];
                change_password_1[12] = token[3];
                byte[] encrypt = AESUtil.Encrypt(change_password_1, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "change password 01");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "change password 01");
                                System.out.println(exception.toString());
                            }
                        });

                try{
                    Thread.sleep(200);
                }catch (Exception e){
                    e.printStackTrace();
                }

                change_password_2[3] = new_password[0];
                change_password_2[4] = new_password[1];
                change_password_2[5] = new_password[2];
                change_password_2[6] = new_password[3];
                change_password_2[7] = new_password[4];
                change_password_2[8] = new_password[5];
                change_password_2[9] = token[0];
                change_password_2[10] = token[1];
                change_password_2[11] = token[2];
                change_password_2[12] = token[3];
                byte[] encrypt_2 = AESUtil.Encrypt(change_password_2, AESUtil.PRIVATE_AES);
                BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt_2,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.e("write success", "change password 02");
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.e("write fail", "change password 02");
                                System.out.println(exception.toString());
                            }
                        });
            }
        });

        ListView listView_device = (ListView) findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }



    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
            }
        });
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }


            @Override
            public void onConnectSuccess(final BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.e("gatt callback","connect succcess");

                List<BluetoothGattService> list = gatt.getServices();
                Log.e("list",list.size()+"");


                for(BluetoothGattService service : list){
                    UUID uuid = service.getUuid();
                    if(!uuid.toString().contains("fee7")){
                        continue;
                    }else{
                        UUIDService = uuid;
                    }

                    List<BluetoothGattCharacteristic> BTGCList = service.getCharacteristics();
                    Log.e("service uuid",uuid.toString());
                    for(BluetoothGattCharacteristic BTGC : BTGCList){
                        UUID BTGCUuid = BTGC.getUuid();
                        Log.e("BTGCUuid",BTGCUuid.toString());

                        if(BTGCUuid.toString().contains("36f6")){
                            UUID36f6 = BTGCUuid;
                        }

                        if(BTGCUuid.toString().contains("36f5")){
                            UUID36f5 = BTGCUuid;
                        }
                    }

                    // set notify callback for indicate UUIDService and UUIDCharactor
                    BleManager.getInstance().notify(bleDevice, UUIDService.toString(), UUID36f6.toString(),
                            bleNotifyCallback);
                    try{
                        Thread.sleep(200);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    // wrirte charactor for access token
                    byte[] encrypt = AESUtil.Encrypt(access_token, AESUtil.PRIVATE_AES);
                    BleManager.getInstance().write(bleDevice, UUIDService.toString(), UUID36f5.toString(), encrypt,
                            new BleWriteCallback() {
                                @Override
                                public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                    Log.e("write success", "access token");
                                }

                                @Override
                                public void onWriteFailure(BleException exception) {
                                    Log.e("write fail", "access tokcen");
                                    System.out.println(exception.toString());
                                }
                            });

                }


                progressDialog.dismiss();
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
//
//                readRssi(bleDevice);
//                setMtu(bleDevice, 23);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();

                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }

    // 通知回调类
    private BleNotifyCallback bleNotifyCallback = new BleNotifyCallback(){
        @Override
        public  void onNotifySuccess(){
            Log.e("setting notify success","xxx");
        }
        @Override
        public  void onNotifyFailure(BleException exception){
            Log.e("setting notify failure","xxx");
        }
        @Override
        public  void onCharacteristicChanged(byte[] data){
            Log.e("notify on character","xxx");
            byte[] decrypt = AESUtil.Decrypt(data,AESUtil.PRIVATE_AES);


            // 获取令牌的回调
            if (decrypt[0] == 0x06 && decrypt[1] == 0x02) {
                token[0] = decrypt[3];
                token[1] = decrypt[4];
                token[2] = decrypt[5];
                token[3] = decrypt[6];
                Log.e("access token",token[0]+","+token[1]+","+token[2]+","+token[3]);
                Log.e("锁开关 : ",decrypt[12]+": 0 close , 1 open");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x02 && decrypt[2] == 01){
                Log.e("open lock  ",decrypt[3] + " -> 0 success , 1 failure");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x0D && decrypt[2] == 01){
                Log.e("close lock",decrypt[3] + "-> 0 success , 1 failure");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x08 && decrypt[2] == 01){
                Log.e("close lock",decrypt[3] + "-> 0 success , 1 failure");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x0F && decrypt[2] == 01){
                Log.e("lock status",decrypt[3] + "-> 0 open , 1 close");
                return;
            }

            if(decrypt[0] == 0x10 && decrypt[1] == 0x02 && decrypt[2] == 01){
                Log.e("开仓",decrypt[3] + "-> 0 开仓成功 , 1 开仓失败");
                return;
            }

            if(decrypt[0] == 0x02 && decrypt[1] == 0x02 && decrypt[2] == 01){
                Log.e("power",decrypt[3] + "");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x05 && decrypt[2] == 01){
                Log.e("change password",decrypt[3] + " -> 0 success , 1 failure");
                return;
            }

            if(decrypt[0] == 0x05 && decrypt[1] == 0x22 && decrypt[2] == 0x08){
                Log.e("change password",decrypt[3] + " -> 0 开 , 1 关");
                return;
            }

            String decryptStr = "";
            for(byte b : decrypt){
                decryptStr = decryptStr + b + ",";
            }
            Log.e("decrypt str",decryptStr);

        }

    };

    private void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            bluetoothAdapter.enable();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
//                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
//                setScanRule();
                startScan();
            }
        }
    }

}
