package com.panfei.pedometer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by suitian on 15/12/29.
 */
public class StepService extends Service {

    public final String TAG = "StepService";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private StepDetector mStepDetector;
    public StepDisplayer mStepDisplayer;

    private PowerManager.WakeLock wakeLock;
    private NotificationManager mNM;

    private int mSteps;


    public class StepBinder extends Binder {
        StepService getService() {
            return StepService.this;
        }
    }

    private final IBinder mBinder = new StepBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        super.onCreate();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        acquireWakeLock();
        mStepDetector = new StepDetector(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registerDetector();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        mStepDisplayer = new StepDisplayer();
        mStepDisplayer.addListener(mStepListener);
        mStepDetector.setStepLinstener(mStepDisplayer);

        Toast.makeText(this, "Pedometer started. ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e(TAG, "onStart");
        super.onStart(intent, startId);
        mStepDisplayer.notifyListener();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        unregisterDetector();

        mNM.cancel(R.string.app_name);
        wakeLock.release();

        super.onDestroy();

        mSensorManager.unregisterListener(mStepDetector);
        Toast.makeText(this, "Pedometer stopped. ", Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                StepService.this.unregisterDetector();
                StepService.this.registerDetector();
                wakeLock.release();
                acquireWakeLock();
            }
        }
    };

    private void registerDetector() {
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mStepDetector, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterDetector() {
        mSensorManager.unregisterListener(mStepDetector);
    }

    public void acquireWakeLock() {
        PowerManager pw = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pw.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    private StepDisplayer.Listener mStepListener = new StepDisplayer.Listener() {
        public void stepsChanged(int value) {
            mSteps = value;
            passValue();
        }
        public void passValue() {
            if (mCallback != null) {
                mCallback.stepsChanged(mSteps);
            }
        }
    };

    public interface ICallback {
        public void stepsChanged(int value);
    }

    private ICallback mCallback;

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        startService(new Intent(this, StepService.class));
    }
}
