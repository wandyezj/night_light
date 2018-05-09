
//
// Color and shade constants
//

enum color
{
  OFF = 0,
  RED,
  ORANGE,
  YELLOW,
  CHARTREUSE,
  GREEN,
  AQUAMARINE,
  CYAN,
  AZURE,
  BLUE,
  VIOLET,
  MAGENTA,
  ROSE,
  WHITE,
  COLOR_COUNT // not an actual color just used to keep count
};

String color_name[color::COLOR_COUNT] =
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

// array of colors that can be output
byte color_values[color::COLOR_COUNT][3] =
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

// change brightness
// goes from brightest to dimmest
enum shade
{
  BRIGHT = 0,
  ONE,
  TWO,
  THREE,
  FOUR,
  FIVE,
  SIX,
  SEVEN,
  DARK, // 8, bitshifts to 0
  SHADE_COUNT // not an actual shade just used to keep count
};

String shade_name[shade::SHADE_COUNT] =
{
  "bright",
  "one",
  "two",
  "three",
  "four",
  "five",
  "six",
  "seven",
  "dark"
};


// Output Controls - Critical for teh entire program
// what should be done according to the sets of controls
bool use_hardware_override = true;

shade control_shade = shade::DARK; // shade always based of light level sensor

bool control_hardware_blink = false; // hardware is not allowed to blink
color control_hardware_color = color::OFF;

bool control_bluetooth_blink = false;
color control_bluetooth_color = color::OFF;


bool output_blink = false;
color output_color = color::OFF;
shade output_shade = shade::DARK;

//

#include "ble_config.h"

/*
 * Simple Bluetooth Demo
 * This code shows that the user can send simple digital write data from the
 * Android app to the Duo board.
 * Created by Liang He, April 27th, 2018
 * 
 * The Library is created based on Bjorn's code for RedBear BLE communication: 
 * https://github.com/bjo3rn/idd-examples/tree/master/redbearduo/examples/ble_led
 * 
 * Our code is created based on the provided example code (Simple Controls) by the RedBear Team:
 * https://github.com/RedBearLab/Android
 */

#if defined(ARDUINO) 
SYSTEM_MODE(SEMI_AUTOMATIC); 
#endif

#define RECEIVE_MAX_LEN    3
#define BLE_SHORT_NAME_LEN 0x08 // must be in the range of [0x01, 0x09]
#define BLE_SHORT_NAME 'A','A','A','h','o','m','e'  // define each char but the number of char should be BLE_SHORT_NAME_LEN-1

// UUID is used to find the device by other BLE-abled devices
static uint8_t service1_uuid[16]    = { 0x71,0x3d,0x00,0x00,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };
static uint8_t service1_tx_uuid[16] = { 0x71,0x3d,0x00,0x03,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };

// Define the configuration data
static uint8_t adv_data[] = {
  0x02,
  BLE_GAP_AD_TYPE_FLAGS,
  BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE, 
  
  BLE_SHORT_NAME_LEN,
  BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME,
  BLE_SHORT_NAME, 
  
  0x11,
  BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE,
  0x1e,0x94,0x8d,0xf1,0x48,0x31,0x94,0xba,0x75,0x4c,0x3e,0x50,0x00,0x00,0x3d,0x71 
};

// Define the receive and send handlers
static uint16_t receive_handle = 0x0000; // recieve

static uint8_t receive_data[RECEIVE_MAX_LEN] = { 0x01 };

/**
 * @brief Callback for writing event.
 *
 * @param[in]  value_handle  
 * @param[in]  *buffer       The buffer pointer of writting data.
 * @param[in]  size          The length of writting data.   
 *
 * @retval 
 */
int bleWriteCallback(uint16_t value_handle, uint8_t *buffer, uint16_t size) {
  Serial.print("Write value handler: ");
  Serial.println(value_handle, HEX);

  if (receive_handle == value_handle) {
    memcpy(receive_data, buffer, RECEIVE_MAX_LEN);
    Serial.print("Write value: ");
    for (uint8_t index = 0; index < RECEIVE_MAX_LEN; index++) {
      Serial.print(receive_data[index], HEX);
      Serial.print(" ");
    }
    Serial.println(" ");
    
    /* Process the data
     * TODO: Receive the data sent from other BLE-abled devices (e.g., Android app)
     * and process the data for different purposes (digital write, digital read, analog read, PWM write)
     */
    if (receive_data[0] == 0x01) { // Command is to control digital out pin
      if (receive_data[1] == 0x01)
        Serial.println("Ble High");
        //digitalWrite(DIGITAL_OUT_PIN, HIGH);
      else 
      {
        Serial.println("Ble Low");
        //digitalWrite(DIGITAL_OUT_PIN, LOW);
      }
        
    }
    else if (receive_data[0] == 0x02)
    {
      if (receive_data[1] == 0x01)
      {
        Serial.println("Ble Blink On");
        control_bluetooth_blink = true;
        use_hardware_override = false;
      }
      else
      {
        Serial.println("Ble Blink Off");
        control_bluetooth_blink = false;
        use_hardware_override = false;
      }
    }
    else if (receive_data[0] == 0x03)
    {
      int color_index = (int)receive_data[1];
      Serial.print("Ble Color: ");
      Serial.println(color_index);
      control_bluetooth_color = (color) color_index;
      use_hardware_override = false;
    }
    else if (receive_data[0] == 0x04)
    { 
      // Command is to initialize all.
      Serial.println("Ble Initialize All");
      //digitalWrite(DIGITAL_OUT_PIN, LOW);
    }
  }
  return 0;
}

void BleSetup()
{
  //Serial.begin(115200);
  delay(5000);
  
  Serial.println("Simple Digital Out Demo.");

  // Initialize ble_stack.
  ble.init();
  configureBLE(); //lots of standard initialization hidden in here - see ble_config.cpp
  // Set BLE advertising data
  ble.setAdvertisementData(sizeof(adv_data), adv_data);

  // Register BLE callback functions
  ble.onDataWriteCallback(bleWriteCallback);

  // Add user defined service and characteristics
  ble.addService(service1_uuid);
  receive_handle = ble.addCharacteristicDynamic(service1_tx_uuid, ATT_PROPERTY_NOTIFY|ATT_PROPERTY_WRITE|ATT_PROPERTY_WRITE_WITHOUT_RESPONSE, receive_data, RECEIVE_MAX_LEN);
  
  // BLE peripheral starts advertising now.
  ble.startAdvertising();
  Serial.println("BLE start advertising.");
}

/*
void setup() {
  Serial.begin(115200);
  //delay(5000);

  BleSetup();

  pinMode(DIGITAL_OUT_PIN, OUTPUT);
}
*/

// Rest of the code


/*
Notes:
  * Sensor readings can be slightly different each time, and there can be blips of random high or low values.
  To make sure that there is a 'constant' value, make sure that a number of readings in a row have the same value.

  * The slider input voltage should be stepped down with a couple (3) 10K ohm resistors to put the voltage in readable range.
  
  * The photo resistor works well with a 2.2k ohm resistor 
*/

//
// Pins
//

const int pin_blink = D0;

const int pin_red = D1;
const int pin_green = D2;
const int pin_blue = D3;

const int pin_sensor_slider_in = A0;
const int pin_sensor_slider_out = A1;

const int pin_sensor_light_in = A4;
const int pin_sensor_light_out = A5;


void ColorShade(byte &r, byte &g, byte &b, color c, shade s)
{
  r = color_values[c][0];
  g = color_values[c][1];
  b = color_values[c][2];

  // bitshift to adjust brightness
  r = r >> s;
  g = g >> s;
  b = b >> s;
}

//
// Board LED
//
void SetRGB(byte r, byte g, byte b)
{
  RGB.color(r, g, b);
}

void SetBoardLedColor(color c, shade s)
{
  //Serial.println(c);
  byte r, g, b;
  ColorShade(r, g, b, c, s);

  SetRGB(r, g, b);
}

void SetBoardLedColor(color c)
{
  SetBoardLedColor(c, shade::BRIGHT);
}

//
// Output LED
//

void SetLed(byte r, byte g, byte b)
{
  analogWrite(pin_red, r);
  analogWrite(pin_green, g);
  analogWrite(pin_blue, b);
}

void SetLedColor(color c, shade s)
{
  byte r, g, b;
  ColorShade(r, g, b, c, s);

  // using common anode
  r = 255 - r;
  g = 255 - g;
  b = 255 - b;
  
  SetLed(r, g, b);
}

void SetLedColor(color c)
{
  SetLedColor(c, shade::BRIGHT);
}


//
// DEBUG sensor voltage readings
//

void DebugSensorReadings(String name, const int v_in, const int v_out, const unsigned int value_count)
{
  int reading = SensorReading(v_in, v_out);
  unsigned int percent = SensorPercent(v_in, v_out);
  int value = SensorMapPercentToValue(percent, value_count);
  
  Serial.print(name);
  Serial.print(" ");
  Serial.print(v_in);
  Serial.print(" ");
  Serial.print(v_out);
  Serial.print(" ");
  Serial.print(reading);
  Serial.print(" ");
  Serial.print(percent);
  Serial.print(" ");
  Serial.print(value);
  Serial.print(" ");
  //Serial.print(color_name[(int)mapped_color]);
  Serial.println("");
}

//
// Resistor Sensors
//

int SensorReading(int v_in, int v_out)
{
  return abs(v_in - v_out);
}

unsigned int SensorPercent(int v_in, int v_out)
{
  int reading = SensorReading(v_in, v_out);
  int percent = floor(((float)reading / (float)v_in) * 100);

  return percent;
}

// maps percent to a discrete range of [0, N-1]
int SensorMapPercentToValue(unsigned int percent, unsigned int count)
{
  float value_percent = 100.0 / (float)(count - 1);

  int mapped_value = ceil(percent / value_percent);

  return mapped_value;
}

int SensorValue(const int pin_in, const int pin_out, const unsigned int value_count)
{
  int v_in = analogRead(pin_in);
  int v_out = analogRead(pin_out);

  unsigned int percent = SensorPercent(v_in, v_out);
  int value = SensorMapPercentToValue(percent, value_count);

  return value;
}


//
// Specific Sensors
//

color GetSensorSliderColor()
{
  return (color)SensorValue(pin_sensor_slider_in, pin_sensor_slider_out, color::COLOR_COUNT);
}

shade GetSensorLightShade()
{
  int value = SensorValue(pin_sensor_light_in, pin_sensor_light_out, shade::SHADE_COUNT);

  // invert value based on light level, per specifications.
  //value = (shade::SHADE_COUNT - 1) - value;
  return (shade)value;
}

//
// Helper functions
//

// function that handles smoothing from sensor readings, this prevents prevents odd blips that can create inconsistent output.
int CumulativeRead(const int current_value, const int new_read, const int last_read, int &last_read_count_in_a_row, const int required_count_in_a_row_to_switch)
{
    if (new_read == last_read)
    {
      last_read_count_in_a_row++;
    }
    else
    {
      last_read_count_in_a_row = 0;
    }

    if (last_read_count_in_a_row >= required_count_in_a_row_to_switch)
    {
      // enough evidence to switch
      return last_read;
    }
    
    // not enough constant reads to switch
    return current_value;
}

// check if the period has passed since the last check time
// if true, also update the last check time
bool SufficientTimePassed(unsigned long &last_check_time_milliseconds, const unsigned long period_milliseconds)
{
  unsigned long time_now = millis();

  if (time_now < last_check_time_milliseconds + period_milliseconds)
  {
    // need to wait more.
    return false;
  }

  // sufficient time passed, update the last time this should be checked
  last_check_time_milliseconds = time_now;
  return true;
}



color current_color = color::OFF;
color last_sensed_color = color::OFF;
int last_sensed_color_count_in_row = 0;
const int sensed_color_count_in_row_to_switch = 3;

color GetLoopSensorSliderColor()
{
  color new_color = GetSensorSliderColor();

  //Serial.println((int)selected_color);
  
  // make sure that there are consecutive readings to switch
  // really should do as average of last coupld consecutive readings (must have 3 in a row to switch, should stop random blips)

  current_color = (color)CumulativeRead((const int)current_color, (const int)new_color, (const int)last_sensed_color, last_sensed_color_count_in_row, sensed_color_count_in_row_to_switch);
  last_sensed_color = new_color;

  return current_color;
}

shade current_shade = shade::DARK;
shade last_sensed_shade = shade::DARK;
int last_sensed_shade_count_in_row = 0;
const int sensed_shade_count_in_row_to_switch = 3;

shade GetLoopSensorLightShade()
{
  shade new_shade = GetSensorLightShade();

  current_shade = (shade)CumulativeRead((const int)current_shade, (const int)new_shade, (const int)last_sensed_shade, last_sensed_shade_count_in_row, sensed_shade_count_in_row_to_switch);
  last_sensed_shade = new_shade;
  
  return current_shade;
}




//
// Take hardware sensor readings
//
unsigned long led_controller_last_check_time_milliseconds = 0;
const unsigned long led_controller_check_period_milliseconds = 20;

void LoopHardwareControl()
{
  if (SufficientTimePassed(led_controller_last_check_time_milliseconds, led_controller_check_period_milliseconds))
  {
    color selected_color = GetLoopSensorSliderColor();
    shade selected_shade = GetLoopSensorLightShade();
  
  // manual touching of controls overrides all
  use_hardware_override = use_hardware_override || control_hardware_color != selected_color;
  
  control_hardware_color = selected_color;
  control_shade = selected_shade;
  
  // Debugging
  //Serial.println((int)selected_color);
  /*
    Serial.print(color_name[(int)selected_color]);
    Serial.print(" ");
    Serial.print(shade_name[(int)selected_shade]);
    Serial.println(" ");
    */
  }
}


void LoopBluetoothControl()
{
  // TODO add bluetooth commands
  
}



void LoopConfigureLed()
{
  output_blink = control_bluetooth_blink;
  output_color = control_bluetooth_color;
  output_shade = control_shade;
  
  if(use_hardware_override)
  {
    output_blink = control_hardware_blink;
    output_color = control_hardware_color;
    output_shade = control_shade;
  }
}

//
// The LED output controler
//

bool in_blink = false;
unsigned long led_controller_last_blink_check_time_milliseconds = 0;
const unsigned long led_controller_blink_period_milliseconds = 1000;

void LoopLedController()
{
  color selected_color = output_color;
  shade selected_shade = output_shade;
  
  // flip between on and off
  if (SufficientTimePassed(led_controller_last_blink_check_time_milliseconds, led_controller_blink_period_milliseconds))
  {
    in_blink = !in_blink;
  }
  
  if (in_blink && output_blink)
  {
    selected_color = color::OFF;
  }
  
    SetLedColor(selected_color, selected_shade);
}





//
// Debug
//

// Debug Commands
bool do_blink = false;
bool do_board_color_cycle = false;
bool do_debug_rgb = false;


String input_command = ""; // a String to hold incoming data
void HandleCommand(String command)
{
  Serial.println("\nCommand: '" + command + "'");
  if (command.equalsIgnoreCase("help"))
  {
    Serial.println("Commands:");
    Serial.println("\thelp - show commands");
    Serial.println("\tblink on - turn blinking on");
    Serial.println("\tblink off - turn blinking off");
    Serial.println("\tboard cycle on - turn board light color cycle on");
    Serial.println("\tboard cycle off - turn board light color cycle off");
    Serial.println("\tdebug rgb on - cycle through red green and blue");
    Serial.println("\tdebug rgb off - stop cycle through red green and blue");
  }
  else if (command.equalsIgnoreCase("blink on"))
  {
    Serial.println("turning blink on");
    do_blink = true;
  }
  else if (command.equalsIgnoreCase("blink off"))
  {
    Serial.println("turning blink off");
    do_blink = false;
  }
  else if (command.equalsIgnoreCase("board cycle on"))
  {
    do_board_color_cycle = true;
  }
  else if (command.equalsIgnoreCase("board cycle off"))
  {
    do_board_color_cycle = false;
  }
  else if (command.equalsIgnoreCase("debug rgb on"))
  {
    do_debug_rgb = true;
  }
  else if (command.equalsIgnoreCase("debug rgb off"))
  {
    do_debug_rgb = false;
  }
  else
  {
    Serial.println("unrecognized command");
  }
}


/*
  SerialEvent occurs whenever a new data comes in the hardware serial RX. This
  routine is run between each time loop() runs, so using delay inside loop can
  delay response. Multiple bytes of data may be available.
*/
void serialEvent()
{
  if (!Serial.available())
  {
    return;
  }

  // Note this relies on a line ending

  // commands in the format
  // command {variables}
  while (Serial.available())
  {
    // get the new byte:
    char in_char = (char)Serial.read();
    // add it to the inputString:

    if (in_char == '\n')
    {
      // command complete
      HandleCommand(input_command);
      input_command = "";
    }
    else
    {
      input_command += in_char;
    }
  }
}


//
// Main Loops
//

void setup()
{
  Serial.begin(9600);

  BleSetup();

  pinMode(pin_blink, OUTPUT);

  pinMode(pin_red, OUTPUT);
  pinMode(pin_green, OUTPUT);
  pinMode(pin_blue, OUTPUT);

  RGB.control(true);
}

void loop()
{
  LoopHardwareControl();
  LoopBluetoothControl();
  LoopConfigureLed();
  LoopLedController();


/*
  for (byte c = 0; c < color::COLOR_COUNT; c++)
  {
    for (byte s = 0; s < shade::SHADE_COUNT; s++)
    {
      SetBoardLedColor((color)c, (shade)s);
      SetLedColor((color)c, (shade)s);
      delay(500);
    }
  }
  SetBoardLedColor(color::OFF);
  SetLedColor(color::OFF);
  //*/  

  if (do_debug_rgb)
  {
    SetBoardLedColor(color::RED);
    SetLedColor(color::RED);
    delay(2000);
  
    SetBoardLedColor(color::GREEN);
    SetLedColor(color::GREEN);
    delay(2000);
      
    SetBoardLedColor(color::BLUE);
    SetLedColor(color::BLUE);
    delay(2000);
  }


  if (do_board_color_cycle)
  {
    for (byte i = 0; i < color::COLOR_COUNT; i++)
    {
        SetBoardLedColor((color)i);
        delay(500);
    }
    SetBoardLedColor(color::OFF);
  }


  // Controls circuit
  if (do_blink)
  {
    digitalWrite(pin_blink, HIGH);
    delay(1000);
    digitalWrite(pin_blink, LOW);
    delay(1000);
  }
}



