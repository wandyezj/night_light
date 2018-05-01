package megacorp.nightlight;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{

    private SeekBar seekbar_color_selector;
    private TextView textview_selected_color;
    private TextView textview_status;

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

    }

    int progress_value = 0;
    private void setSelectedColorText(){
        textview_selected_color.setText(color_name[progress_value]);

        int[] color_value = color_values[progress_value];

        int r = color_value[0];
        int g = color_value[1];
        int b = color_value[2];

        textview_selected_color.setTextColor(Color.rgb(r, g, b));

        if (progress_value == 0) {
            textview_status.setText("Status: Off");
        } else {
            textview_status.setText("Status: On");
        }

    }


    @Override
    public void onProgressChanged(SeekBar seekbar, int progress_value, boolean from_user) {

        this.progress_value = progress_value;
        setSelectedColorText();
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
}
