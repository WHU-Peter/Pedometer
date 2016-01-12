package com.panfei.pedometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by suitian on 16/1/6.
 */
public class DataBaseManager {

    private static final String TAG = "DataBaseManager";
    private volatile static DataBaseManager instance;
    private DataBaseHelper dataBaseHelper;
    private SQLiteDatabase db;
    private int count;


    private DataBaseManager(Context context){
        dataBaseHelper = new DataBaseHelper(context);

        db = dataBaseHelper.getWritableDatabase();
    }

    public static DataBaseManager getInstance(Context context){
        if (instance == null){
            synchronized (DataBaseManager.class){
                if (instance == null){
                    instance = new DataBaseManager(context);
                }
            }
        }

        return instance;
    }

    public synchronized void addData(long time, float sensor){
        ContentValues contentValues = new ContentValues();
        contentValues.put("time", time);
        contentValues.put("sensor", sensor);

        db.beginTransaction();  //开始事务
        try {
            long row = db.insert("sensorinfo",null,contentValues);
            db.setTransactionSuccessful();  //设置事务成功完成
        } finally {
            db.endTransaction();    //结束事务
        }
    }

    public void clearTable() throws Exception{
        synchronized (this){
            String sql = "DELETE FROM sensorinfo;";
            db.execSQL(sql);
            sql = "update sqlite_sequence set seq=0 where name='sensorinfo'";
            db.execSQL(sql);
        }
    }

    public synchronized int queryCount(){
        String sql = "select * from sensorinfo";
        Cursor c = db.rawQuery(sql, null);
        count = c.getCount();
        Log.e(TAG, "queryPedometerCount : " + count);
        c.close();
        return count;
    }

    public synchronized int queryPedometerCount(long start, long end){
        String sql = "select * from sensorinfo where time between " + start + " and " + end;
        Cursor c = db.rawQuery(sql, null);
        count = c.getCount();
        Log.e(TAG, "queryPedometerCount : " + count);
        c.close();
        return count;
    }

    public void closeDB() {
        db.close();
    }
}
