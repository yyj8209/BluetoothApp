package wayful.com.bluetoothapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
    static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    Button btnSearch, btnDis, btnExit;

    ToggleButton tbtnSwitch;

    ListView lvBTDevices;

    ArrayAdapter<String> adtDevices;

    List<String> lstDevices = new ArrayList<String>();

    BluetoothAdapter btAdapt;

    public static BluetoothSocket btSocket;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Button 设置
        btnSearch = (Button) this.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new ClickEvent());
        btnExit = (Button) this.findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new ClickEvent());
        btnDis = (Button) this.findViewById(R.id.btnDis);
        btnDis.setOnClickListener(new ClickEvent());

        // ToogleButton设置
        tbtnSwitch = (ToggleButton) this.findViewById(R.id.tbtnSwitch);
        tbtnSwitch.setOnClickListener(new ClickEvent());

        // ListView及其数据源 适配器
        lvBTDevices = (ListView) this.findViewById(R.id.lvDevices);
        adtDevices = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, lstDevices);
        lvBTDevices.setAdapter(adtDevices);
        lvBTDevices.setOnItemClickListener(new ItemClickEvent());

        btAdapt = BluetoothAdapter.getDefaultAdapter();// 初始化本机蓝牙功能

        if (btAdapt.getState() == BluetoothAdapter.STATE_ON)// 读取蓝牙状态并显示
            tbtnSwitch.setChecked(false);
        else if (btAdapt.getState() == BluetoothAdapter.STATE_OFF)
            tbtnSwitch.setChecked(true);

        // 注冊Receiver来获取蓝牙设备相关的结果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(searchDevices, intent);
    }

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            // 显示全部收到的消息及其细节
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e(keyName, String.valueOf(b.get(keyName)));
            }
            // 搜索设备时，取得设备的MAC地址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "|" + device.getAddress();
                if (lstDevices.indexOf(str) == -1)// 防止反复加入
                    lstDevices.add(str); // 获取设备名称和mac地址
                adtDevices.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(searchDevices);
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    class ItemClickEvent implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            btAdapt.cancelDiscovery();
            String str = lstDevices.get(arg2);
            String[] values = str.split("\\|");
            String address = values[1];
            Log.e("address", values[1]);
            UUID uuid = UUID.fromString(SPP_UUID);
            BluetoothDevice btDev = btAdapt.getRemoteDevice(address);
            try {
                btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();

            } catch (IOException e) {
                // TODO Auto-generated catch block

            }

        }

    }

    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == btnSearch)// 搜索蓝牙设备，在BroadcastReceiver显示结果
            {
                if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 假设蓝牙还没开启
                    Toast.makeText(MainActivity.this, "请先打开蓝牙", Toast.LENGTH_LONG).show();
                    return;
                }
                setTitle("本机蓝牙地址：" + btAdapt.getAddress());
                lstDevices.clear();
                btAdapt.startDiscovery();
            } else if (v == tbtnSwitch) {// 本机蓝牙启动/关闭
                if (tbtnSwitch.isChecked() == false)
                    btAdapt.enable();
                else if (tbtnSwitch.isChecked() == true)
                    btAdapt.disable();
            } else if (v == btnDis)// 本机能够被搜索
            {
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                startActivity(discoverableIntent);
            } else if (v == btnExit) {
                try {
                    if (btSocket != null)
                        btSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MainActivity.this.finish();
            }
        }

    }
}