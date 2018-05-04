package megacorp.nightlight;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Switch;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Switch.OnClickListener, SensorEventListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

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
    private GestureDetector  gesture_detector;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    // Functions that will communicate over bluetooth to adjust the color
    private void setBluetoothBlinkOn() {
        // TODO: Communicate across bluetooth to the light to set the blink
    }

    private void setBluetoothBlinkOff() {
        // TODO: Communicate across bluetooth to the light to set the blink
    }

    private void setBluetoothColor(int r, int g, int b) {
        // TODO: Communicate across bluetooth to the light to set the color
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
