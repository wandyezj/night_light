/* BLE demo: Use a single button to send data to the Duo board from the Android app to control the
 * LED on and off on the board through BLE.
 *
 * The app is built based on the example code provided by the RedBear Team:
 * https://github.com/RedBearLab/Android
 */
package com.example.lianghe.android_ble_basic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.lianghe.android_ble_basic.BLE.RBLGattAttributes;
import com.example.lianghe.android_ble_basic.BLE.RBLService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Switch.OnClickListener, SensorEventListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    // Define the device name and the length of the name
    // Note the device name and the length should be consistent with the ones defined in the Duo sketch
    private String mTargetDeviceName = "AAAhome";
    private int mNameLen = 0x08;

    private final static String TAG = MainActivity.class.getSimpleName();

    // Declare all variables associated with the UI components
    private Button mConnectBtn = null;
    private TextView mDeviceName = null;
    private TextView mRssiValue = null;
    private TextView mUUID = null;
    private String mBluetoothDeviceName = "";
    private String mBluetoothDeviceUUID = "";


    // Declare all Bluetooth stuff
    private BluetoothGattCharacteristic mCharacteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean mConnState = false;
    private boolean mScanFlag = false;

    private byte[] mData = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;   // millis

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // Process service connection. Created by the RedBear Team
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void setButtonDisable() {
        flag = false;
        enableBluetoothControl(flag);
        mConnState = false;
        mConnectBtn.setText("Connect");
        mRssiValue.setText("");
        mDeviceName.setText("");
        mUUID.setText("");
    }

    private void setButtonEnable() {
        flag = true;
        enableBluetoothControl(flag);
        mConnState = true;
        mConnectBtn.setText("Disconnect");
    }

    // Process the Gatt and get data if there is data coming from Duo board. Created by the RedBear Team
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    // Display the received RSSI on the interface
    private void displayData(String data) {
        if (data != null) {
            mRssiValue.setText(data);
            mDeviceName.setText(mBluetoothDeviceName);
            mUUID.setText(mBluetoothDeviceUUID);
        }
    }


    // Get Gatt service information for setting up the communication
    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        mCharacteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
    }

    // Start a thread to read RSSI from the board
    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    // Scan all available BLE-enabled devices
    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    // Callback function to search for the target Duo board which has matched UUID
    // If the Duo board cannot be found, debug if the received UUID matches the predefined UUID on the board
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					byte[] serviceUuidBytes = new byte[16];
					String serviceUuid = "";
                    for (int i = (21+mNameLen), j = 0; i >= (6+mNameLen); i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    /*
                     * This is where you can test if the received UUID matches the defined UUID in the Arduino
                     * Sketch and uploaded to the Duo board: 0x713d0000503e4c75ba943148f18d941e.
                     */
					serviceUuid = bytesToHex(serviceUuidBytes);
					if (stringToUuidString(serviceUuid).equals(
							RBLGattAttributes.BLE_SHIELD_SERVICE
									.toUpperCase(Locale.ENGLISH)) && device.getName().equals(mTargetDeviceName)) {
						mDevice = device;
						mBluetoothDeviceName = mDevice.getName();
						mBluetoothDeviceUUID = serviceUuid;
					}
				}
			});
        }
    };

    // Convert an array of bytes into Hex format string
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Convert a string to a UUID format
    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Associate all UI components with variables
        mConnectBtn = (Button) findViewById(R.id.connectBtn);
        mDeviceName = (TextView) findViewById(R.id.deviceName);
        mRssiValue = (TextView) findViewById(R.id.rssiValue);
        mUUID = (TextView) findViewById(R.id.uuidValue);

        // Connection button click event
        mConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i("Connect Button", "Connect Button Clicked");
                if (mScanFlag == false) {
                    // Scan all available devices through BLE
                    scanLeDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                mScanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast
                                                .makeText(
                                                        MainActivity.this,
                                                        "Couldn't search Ble Shiled device!",
                                                        Toast.LENGTH_SHORT);
                                        toast.setGravity(0, 0, Gravity.CENTER);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(mConnState);
                if (mConnState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });

        // Bluetooth setup. Created by the RedBear team.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(MainActivity.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        onCreateNightLight(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if BLE is enabled on the device. Created by the RedBear team.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    // Create a list of intent filters for Gatt updates. Created by the RedBear team.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    // Main color controls
    private SeekBar seekbar_color_selector;
    private TextView textview_selected_color;
    private TextView textview_status;

    // Gyroscope control (technically uses the gravity sensor)
    private Switch switch_color_gyroscope_on;
    private SensorManager sensor_manager;
    private Sensor sensor_gravity;

    // Double Tap blink
    private View view_blink_control;
    private GestureDetector gesture_detector;
    Animation blink_animation;

    String color_name[] =
            {
                    "off",
                    "red",
                    "orange",
                    "yellow",
                    "chartreuse",
                    "green",
                    "aquamarine",
                    "cyan",
                    "azure",
                    "blue",
                    "violet",
                    "magenta",
                    "rose",
                    "white"
            };

    int color_values[][] =
            {
                    {0, 0, 0}, // off
                    {255, 0,  0}, // red
                    {255, 127, 0}, // orange
                    {255, 255, 0}, // yellow
                    {127, 255, 0}, // chartreuse
                    {0, 255, 0}, // green
                    {0, 255, 127}, // aquamarine
                    {0, 255, 255}, // cyan
                    {0, 127, 255}, // azure
                    {0, 0, 255}, // blue
                    {127, 0, 255}, // violet
                    {255, 0, 255}, // magenta
                    {255, 0, 127}, // rose
                    {255, 255, 255} // white
            };


    private void onCreateNightLight(Bundle savedInstanceState) {

        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        seekbar_color_selector = (SeekBar) findViewById(R.id.seekbar_color_selector);
        seekbar_color_selector.setOnSeekBarChangeListener(this);

        textview_selected_color = (TextView)findViewById(R.id.textview_selected_color);
        textview_status = (TextView)findViewById(R.id.textview_status);

        switch_color_gyroscope_on = (Switch) findViewById(R.id.switch_color_gyroscope_on);
        switch_color_gyroscope_on.setOnClickListener(this);

        view_blink_control = findViewById(R.id.view_blink_control);

        // See https://developer.android.com/guide/topics/sensors/sensors_motion.html
        sensor_manager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensor_gravity = sensor_manager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensor_manager.registerListener(this, sensor_gravity, SensorManager.SENSOR_DELAY_NORMAL);

        gesture_detector = new GestureDetector(MainActivity.this, MainActivity.this);
        gesture_detector.setOnDoubleTapListener(this);

        blink_animation = new AlphaAnimation(0.0f, 1.0f);
        blink_animation.setDuration(1000); //You can manage the blinking time with this parameter
        blink_animation.setStartOffset(0);
        blink_animation.setRepeatMode(Animation.REVERSE);
        blink_animation.setRepeatCount(Animation.INFINITE);
    }

    private boolean bluetooth_enabled = false;
    private void enableBluetoothControl(boolean flag){
        bluetooth_enabled = flag;
    }


    boolean is_blinking = false;


    private void toggleBlink() {
        if (is_light_on) {

            is_blinking = !is_blinking;

            if (is_blinking) {
                setBlinkOn();
            } else {
                setBlinkOff();
            }

        } else {
            is_blinking = false;
        }

    }

    private void setBlinkOn(){
        view_blink_control.setBackgroundColor(current_color);
        view_blink_control.startAnimation(blink_animation);
        is_blinking = true;
        setBluetoothBlinkOn();
    }

    private void setBlinkOff() {
        blink_animation.cancel();
        view_blink_control.setBackgroundColor(Color.BLACK);
        is_blinking = false;
        setBluetoothBlinkOff();
    }

    private void sendBluetoothMessage(byte[] message) {
        if (bluetooth_enabled) {
            mCharacteristicTx.setValue(message);
            mBluetoothLeService.writeCharacteristic(mCharacteristicTx);
        }
    }

    // Functions that will communicate over bluetooth to adjust the color
    private void setBluetoothBlinkOn() {
        // TODO: Communicate across bluetooth to the light to set the blink
        byte message[] = new byte[] { (byte) 0x02, (byte) 0x01, (byte) 0x00 };
        sendBluetoothMessage(message);
    }

    private void setBluetoothBlinkOff() {
        // TODO: Communicate across bluetooth to the light to set the blink
        byte message[] = new byte[] { (byte) 0x02, (byte) 0x00, (byte) 0x00 };
        sendBluetoothMessage(message);
    }

    private void setBluetoothColor(int r, int g, int b) {
        // TODO: Communicate across bluetooth to the light to set the color
        byte message[] = new byte[] { (byte) 0x03, (byte) 0x00, (byte) 0x00 };

        byte color_index = (byte)GetClosestColor(r, g, b);
        message[1] = color_index;

        sendBluetoothMessage(message);
    }


    boolean is_light_on = false;

    // Called from progress bar
    private void setSelectedColor(int progress_value){
        is_light_on = progress_value > 0;
        if (!is_light_on) {
            setBlinkOff();
        }

        int[] color_value = color_values[progress_value];

        int r = color_value[0];
        int g = color_value[1];
        int b = color_value[2];

        setSelectedColor(r, g, b);
    }

    // called from gyroscope and progress bar function
    private void setSelectedColor(int r, int g, int b) {
        setSelectedColorTextUi(r,g,b);

        setBluetoothColor(r,g,b);
    }

    // current selected color
    int current_color = Color.BLACK;

    private void setSelectedColorTextUi(int r, int g, int b) {
        int progress_value = GetClosestColor(r, g, b);
        textview_status.setText(String.format("Status: %s", progress_value != 0 || gyroscope_on ? "On" : "Off"));

        seekbar_color_selector.setProgress(progress_value);

        current_color = Color.rgb(r, g, b);
        textview_selected_color.setText(color_name[progress_value]);
        textview_selected_color.setTextColor(current_color);

        view_blink_control.setBackgroundColor(is_light_on && is_blinking ? current_color : Color.BLACK);
    }


    private int GetClosestColor(int r, int g, int b) {

        int best_index = 0;
        int best_value = 255 + 255 + 255;
        for(int index = 0; index < color_values.length; index++){
            int[] color_value = color_values[index];
            int c_r = color_value[0];
            int c_g = color_value[1];
            int c_b = color_value[2];

            int value = Math.abs(c_r - r) + Math.abs(c_g - g) + Math.abs(c_b - b);

            if (value < best_value) {
                best_value = value;
                best_index = index;
            }
        }

        return best_index;
    }


    //
    // Switch
    //

    boolean gyroscope_on = false;
    @Override
    public void onClick(View view) {

        gyroscope_on = switch_color_gyroscope_on.isChecked();

        // disable seekbar when gyroscope is enabled
        seekbar_color_selector.setClickable(!gyroscope_on);
        seekbar_color_selector.setEnabled(!gyroscope_on);

        switch_color_gyroscope_on.setText(String.format("Gyroscope Select: %s", gyroscope_on ? "On": "Off"));
    }


    //
    // Seekbar color selector
    //

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress_value, boolean from_user) {
        setSelectedColor(progress_value);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    //
    // Gyroscope
    //
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float x, y, z;

        if (gyroscope_on) {
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:

                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];

                    Log.i("Gyroscope", String.format("[%f] [%f] [%f]", x, y, z));

                    break;

                case Sensor.TYPE_GRAVITY:

                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];

                    float total = x + y + z;
                    float abs_total = Math.abs(x) + Math.abs(y) + Math.abs(z);

                    float p_x, p_y, p_z;
                    p_x = (Math.abs(x) / abs_total);
                    p_y = (Math.abs(y) / abs_total);
                    p_z = (Math.abs(z) / abs_total);

                    // map percent to color
                    int r, g, b;
                    r = Math.round(255 * p_x);
                    g = Math.round(255 * p_y);
                    b = Math.round(255 * p_z);

                    setSelectedColor(r, g, b);

                    Log.i("Gravity", String.format("[%f] [%f] [%f] Total: [%f] Abs Total: [%f] Percents: [%f] [%f] [%f] Color: [%d] [%d] [%d]", x, y, z, total, abs_total, p_x, p_y, p_z, r, g, b));

                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    //
    // Tap listeners - handle touch events
    //

    // http://codetheory.in/android-gesturedetector/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gesture_detector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        Log.i("Touch", "Single Tap");
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        Log.i("Touch", "Double Tap");
        toggleBlink();
        return false;
    }

    // Other unused Events
    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        Log.i("Touch", "Double Tap Event");
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

}