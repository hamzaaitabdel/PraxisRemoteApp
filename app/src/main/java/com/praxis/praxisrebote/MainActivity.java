package com.praxis.praxisrebote;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class MainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private Button buttonConnect;
    private CircularSeekBar seekBar;
    private String selected_join="";
    private SensorManager sensorManager;

    public Button join_1;
    public Button join_2;
    public Button join_3;
    public class SelectJoinListner implements View.OnTouchListener{
        private String join;
        SelectJoinListner(String join){
            this.join=join;
        }
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            sendMessage(join,mGeomagnetic[0]);
            return false;
        }
    }
    @SuppressLint("ResourceAsColor")
    public void update_stat_bar(){
        TextView text=findViewById(R.id.ba_text);
        LinearLayout bg=findViewById(R.id.stat_bar);
        if(deviceName !=null){
            text.setText(deviceName+" is connected.");
            bg.setBackgroundColor(R.color.teal_700);
        }
        else{
            text.setText("no device is connected.");
            bg.setBackgroundColor(R.color.red);
        }
    }
    public void sendMessage(String join,float value){
        String cmdText="";
        cmdText = "<"+join+"_changed to:"+value+"> \n";
        connectedThread.write(cmdText);
        //Toast.makeText(this, selected_join+"is now selected and the data:"+value+" is sent", Toast.LENGTH_SHORT).show();
        Log.i("Data",cmdText);
    }
    public void select_join(View view){
        String cmdText="";
        int id =view.getId();
        switch (id){
            case R.id.join_1:
                selected_join="join_1";
                cmdText = "<"+selected_join+"_changed to:"+mGeomagnetic[0]+"> \n";
            case R.id.join_2:
                selected_join="join_2";
                cmdText = "<"+selected_join+"_changed to:"+mGeomagnetic[0]+"> \n";
            case R.id.join_3:
                selected_join="join_3";
                cmdText = "<"+selected_join+"_changed to:"+mGeomagnetic[0]+"> \n";
            connectedThread.write(cmdText);
        }
        Toast.makeText(this, selected_join+"is now selected and the data:"+mGeomagnetic[0]+" is sent", Toast.LENGTH_SHORT).show();
    }
    float[] mGeomagnetic = new float[3];
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonConnect=findViewById(R.id.connect);
        // UI Initialization
        join_1=findViewById(R.id.join_1);
        join_2=findViewById(R.id.join_1);
        join_3=findViewById(R.id.join_1);

        join_1.setOnTouchListener(new SelectJoinListner("join_1"));
        join_2.setOnTouchListener(new SelectJoinListner("join_2"));
        join_3.setOnTouchListener(new SelectJoinListner("join_3"));


        SensorEventListener mMagnetometerListener = new SensorEventListener(){
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

            public void onSensorChanged(SensorEvent event) {
                Log.i("data","onSensorChanged");
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                    mGeomagnetic = event.values.clone();
                }
            }
        };
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        //sensorManager.registerListener(mAccelerometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(mMagnetometerListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        update_stat_bar();
        Log.i("debug_1",deviceName+"");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status


            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(this,bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
//                switch (msg.what){
//                    case CONNECTING_STATUS:
//                        switch(msg.arg1){
//                            case 1:
//                                toolbar.setSubtitle("Connected to " + deviceName);
//                                progressBar.setVisibility(View.GONE);
//                                buttonConnect.setEnabled(true);
//                                buttonToggle.setEnabled(true);
//                                break;
//                            case -1:
//                                toolbar.setSubtitle("Device fails to connect");
//                                progressBar.setVisibility(View.GONE);
//                                buttonConnect.setEnabled(true);
//                                break;
//                        }
//                        break;
//
//                    case MESSAGE_READ:
//                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
//                        switch (arduinoMsg.toLowerCase()){
//                            case "led is turned on":
//                                imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
//                            case "led is turned off":
//                                imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
//                        }
//                        break;
//                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });


        /*buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState) {
                    case "turn on":
                        buttonToggle.setText("Turn Off");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn on>";
                        break;
                    case "turn off":
                        buttonToggle.setText("Turn On");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                connectedThread.write(cmdText);
            }
        });*/
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public class CreateConnectThread extends Thread {
        private Context context;
        public CreateConnectThread(Context context,BluetoothAdapter bluetoothAdapter, String address) {
            context=context;
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN}, 2);
                    return;
                }
            }
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN}, 2);
                    return;
                }
            }
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}