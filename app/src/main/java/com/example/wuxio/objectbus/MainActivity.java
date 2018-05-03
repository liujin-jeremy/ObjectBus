package com.example.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.runnable.AsyncThreadCallBack;
import com.example.objectbus.runnable.MainThreadCallBack;
import com.example.objectbus.runnable.OnExecuteRunnable;
import com.example.objectbus.schedule.CancelTodo;
import com.example.objectbus.schedule.Scheduler;

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

    //============================ scheduler ============================


    public void testSchedulerTodo(View view) {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 01");
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 02");
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 03");
            }
        });
    }


    public void testSchedulerTodoDelayed(View view) {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed01");
            }
        }, 2000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed02");
            }
        }, 4000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed03");
            }
        }, 6000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed04");
            }
        }, 4000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed05");
            }
        }, 2000);
    }


    public void testSchedulerTodoMainCallBack(View view) {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back ");
            }
        }, new MainThreadCallBack() {
            @Override
            public void run() {

                print(" callback main ");
            }
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back delayed");
            }
        }, 3000, new MainThreadCallBack() {
            @Override
            public void run() {

                print(" callback main ");
            }
        });
    }


    public void testSchedulerTodoAsyncCallBack(View view) {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back ");
            }
        }, new AsyncThreadCallBack() {
            @Override
            public void run() {

                print(" callback async ");
            }
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back delayed");
            }
        }, 3000, new AsyncThreadCallBack() {
            @Override
            public void run() {

                print(" callback async ");
            }
        });
    }


    public void testSchedulerTodoWithListener(View view) {

        Scheduler.todo(new OnExecuteRunnable() {
            @Override
            public void onExecute() {

                print(" do something in pool ");
            }


            @Override
            public void onFinish() {

                print(" finish; do something extra in pool ");
            }


            @Override
            public void onException(Exception e) {

                print(" Exception! do something to rescue ");
            }
        });

        Scheduler.todo(new OnExecuteRunnable() {
            @Override
            public void onExecute() {

                print(" do something in pool ");
            }


            @Override
            public void onFinish() {

                print(" finish; do something extra in pool ");
            }


            @Override
            public void onException(Exception e) {

                print(" Exception! do something to rescue ");
            }
        }, 2000, new MainThreadCallBack() {
            @Override
            public void run() {

                print(" callback is different with 'OnExecuteRunnable' ");
            }
        });
    }


    private boolean flag;


    public void testSchedulerCancel(View view) {

        CancelTodo cancelTodo = new CancelTodo();
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo cancel");
            }
        }, 2000, cancelTodo);

        if (flag) {
            cancelTodo.cancel();
            Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
        }
        flag = !flag;
    }


    public static void print(String text) {

        Log.i(TAG, "print:" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text);
    }

    //============================ message ============================


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
