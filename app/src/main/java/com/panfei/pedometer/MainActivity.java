package com.panfei.pedometer;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by suitian on 15/12/29.
 */
public class MainActivity extends Activity{

    public final String TAG = "MainActivity";
    private int mStepValue;
    private TextView stepsView;

    private StepService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();
            //设置内容和点击事件
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 0,intent , 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);
            builder.setAutoCancel(false)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("计步器正在工作")
                    .setContentText("谢谢支持")
                    .setContentIntent(contentIntent);
            builder.build();
            Notification notification = builder.getNotification();
            mService.startForeground(1234, notification);
            mService.registerCallback(mCallback);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private static final int STEPS_MSG = 1;

    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
    };

    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    stepsView.setText("" + mStepValue);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate");

        mStepValue = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        stepsView = (TextView)findViewById(R.id.steps);
        startService(new Intent(this, StepService.class));
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mStepValue = DataBaseManager.getInstance(MainActivity.this).queryCount();
                mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, mStepValue, 0));
            }
        });
        startService(new Intent(this, StepService.class));
        bindService(new Intent(this, StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();

    }
}
