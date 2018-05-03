package com.example.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.objectbus.Messengers;
import com.example.objectbus.OnMessageReceiveListener;
import com.example.objectbus.Scheduler;

/**
 * @author wuxio
 */
public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void testSchedulerTodo(View view) {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo ");
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo ");
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo ");
            }
        });
    }


    private void print(String text) {

        Log.i(TAG, "print:" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text);
    }


    public void testMessengerSend(View view) {

        Messengers.send(2, this);

    }


    @Override
    public void onReceive(int what, Object extra) {

    }


    @Override
    public void onReceive(int what) {

        Log.i(TAG, "onReceive:" + what);
    }
}
