package megacorp.nightlight;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Switch;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Switch.OnClickListener, SensorEventListener {

    private SeekBar seekbar_color_selector;
    private TextView textview_selected_color;
    private TextView textview_status;

    private Switch switch_color_gyroscope_on;

    private SensorManager sensor_manager;
    private Sensor sensor_gyro;
    private Sensor sensor_gravity;


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

        // See https://developer.android.com/guide/topics/sensors/sensors_motion.html
        sensor_manager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensor_gyro= sensor_manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensor_gravity= sensor_manager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensor_manager.registerListener(this, sensor_gravity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void setSelectedColorText(int progress_value){
        textview_selected_color.setText(color_name[progress_value]);

        int[] color_value = color_values[progress_value];

        int r = color_value[0];
        int g = color_value[1];
        int b = color_value[2];

        if (progress_value == 0) {
            textview_status.setText("Status: Off");
        } else {
            textview_status.setText("Status: On");
        }

        setSelectedColorText(r, g, b);
    }

    private void setSelectedColorText(int r, int g, int b) {

        if (gyroscope_on) {
            textview_status.setText("Status: On");
            int color_index = GetClosestColor(r, g, b);
            seekbar_color_selector.setProgress(color_index);
            textview_selected_color.setText(color_name[color_index]);
        }

        textview_selected_color.setTextColor(Color.rgb(r, g, b));

    }

    private int GetClosestColor(int r, int g, int b) {

        int best_index = 0;
        int best_value = 255 + 255 + 255;
        for(int index = 1; index < color_values.length; index++){
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
        switch_color_gyroscope_on.setText(String.format("Gyroscope Select: %s", gyroscope_on ? "On": "Off"));
    }


    //
    // Seekbar color selector
    //

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress_value, boolean from_user) {

        setSelectedColorText(progress_value);
        //progress = progresValue;
        //textview_selected_color.setText(Integer.toString(progress_value));

        //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        //textview_selected_color.setText(color_name[progress_value]);
        //textView.setText("Covered: " + progress + "/" + seekBar.getMax());
        //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
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

                    setSelectedColorText(r, g, b);
                    //textview_selected_color.setTextColor(Color.rgb(r, g, b));

                    Log.i("Gravity", String.format("[%f] [%f] [%f] Total: [%f] Abs Total: [%f] Percents: [%f] [%f] [%f] Color: [%d] [%d] [%d]", x, y, z, total, abs_total, p_x, p_y, p_z, r, g, b));

                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
