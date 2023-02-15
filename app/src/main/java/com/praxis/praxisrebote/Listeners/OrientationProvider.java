package com.praxis.praxisrebote.Listeners;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import com.praxis.praxisrebote.MainActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OrientationProvider implements SensorEventListener {

    private static final int MIN_VALUES = 20;


    private static OrientationProvider provider;

    /**
     * Rotation Matrix
     */
    private final float[] MAG = new float[]{1f, 1f, 1f};
    private final float[] I = new float[16];
    private final float[] R = new float[16];
    private final float[] outR = new float[16];
    private final float[] LOC = new float[3];
    /**
     * Orientation
     */
    private float pitch;
    private float roll;
    private final int displayOrientation;
    private Sensor sensor;
    private SensorManager sensorManager;
    private OrientationListener listener;
    /**
     * indicates whether or not Accelerometer Sensor is supported
     */
    private Boolean supported;
    /**
     * indicates whether or not Accelerometer Sensor is running
     */
    private boolean running = false;
    private boolean calibrating = false;
    private float balance;
    private float tmp;
    private float oldPitch;
    private float oldRoll;
    private float oldBalance;
    private float minStep = 360;
    private float refValues = 0;

    public OrientationProvider() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.displayOrientation = MainActivity.getContext().getDisplay().getRotation();
        } else {
            this.displayOrientation = MainActivity.getContext().getWindowManager().getDefaultDisplay().getRotation();
        }
    }

    public static OrientationProvider getInstance() {
        if (provider == null) {
            provider = new OrientationProvider();
        }
        return provider;
    }

    /**
     * Returns true if the manager is listening to orientation changes
     */
    public boolean isListening() {
        return running;
    }

    /**
     * Unregisters listeners
     */
    public void stopListening() {
        running = false;
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
        }
    }

    private List<Integer> getRequiredSensors() {
        return Collections.singletonList(
                Sensor.TYPE_ACCELEROMETER
        );
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    public boolean isSupported() {
        if (supported == null) {
            if (MainActivity.getContext() != null) {
                sensorManager = (SensorManager) MainActivity.getContext().getSystemService(Context.SENSOR_SERVICE);
                boolean supported = true;
                for (int sensorType : getRequiredSensors()) {
                    List<Sensor> sensors = sensorManager.getSensorList(sensorType);
                    supported = (sensors.size() > 0) && supported;
                }
                this.supported = supported;
                return supported;
            }
        }
        return supported;
    }


    /**
     * Registers a listener and start listening
     * callback for accelerometer events
     */
    public void startListening(OrientationListener orientationListener) {
        final AppCompatActivity context = MainActivity.getContext();
        // register listener and start listening
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        running = true;
        for (int sensorType : getRequiredSensors()) {
            List<Sensor> sensors = sensorManager.getSensorList(sensorType);
            if (sensors.size() > 0) {
                sensor = sensors.get(0);
                running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL) && running;
            }
        }
        if (running) {
            listener = orientationListener;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {

        oldPitch = pitch;
        oldRoll = roll;
        oldBalance = balance;

        SensorManager.getRotationMatrix(R, I, event.values, MAG);

        // compute pitch, roll & balance

                Log.i("wach","4");
                SensorManager.remapCoordinateSystem(
                        R,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Y,
                        outR);

        SensorManager.getOrientation(outR, LOC);

        // normalize z on ux, uy
        tmp = (float) Math.sqrt(outR[8] * outR[8] + outR[9] * outR[9]);
        tmp = (tmp == 0 ? 0 : outR[8] / tmp);

        // LOC[0] compass
        pitch = (float) Math.toDegrees(LOC[1]);
        roll = -(float) Math.toDegrees(LOC[2]);
        balance = (float) Math.toDegrees(Math.asin(tmp));

        // calculating minimal sensor step
        if (oldRoll != roll || oldPitch != pitch || oldBalance != balance) {
            if (oldPitch != pitch) {
                minStep = Math.min(minStep, Math.abs(pitch - oldPitch));
            }
            if (oldRoll != roll) {
                minStep = Math.min(minStep, Math.abs(roll - oldRoll));
            }
            if (oldBalance != balance) {
                minStep = Math.min(minStep, Math.abs(balance - oldBalance));
            }
            if (refValues < MIN_VALUES) {
                refValues++;
            }
        }




        // propagation of the orientation
        Log.i("wach1",pitch+" "+roll+" "+balance);
        listener.onOrientationChanged(pitch, roll, balance);
    }


    /**
     * Return the minimal sensor step
     *
     * @return the minimal sensor step
     * 0 if not yet known
     */
    public float getSensibility() {
        if (refValues >= MIN_VALUES) {
            return minStep;
        } else {
            return 0;
        }
    }
}
