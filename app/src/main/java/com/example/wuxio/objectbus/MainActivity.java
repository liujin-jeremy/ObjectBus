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


    @Override
    public void onReceive(int what, Object extra) {

        print("receive: " + what + " extra: " + extra);
    }


    @Override
    public void onReceive(int what) {

        print("receive: " + what);
    }


    public void testMessengerSend(View view) {

        /* message what 的奇偶性决定发送到哪个线程 */

        /* 1 is odd number,the message will send to main thread */

        Messengers.send(1, this);

        /* 2 is  even number, the message will send to a async thread*/

        Messengers.send(2, this);

    }


    public void testMessengerSendDelayed(View view) {

        Messengers.send(3, 2000, this);
        Messengers.send(4, 2000, this);
    }


    public void testMessengerSendWithExtra(View view) {

        Messengers.send(5, " hello main ", this);
        Messengers.send(6, " hello main ", this);

        Messengers.send(7, 2000, " hello main ", this);
        Messengers.send(8, 2000, " hello main ", this);
    }


    public void testMessengerRemove(View view) {

        Messengers.send(9, 2000, " hello main ", this);

        if (flag) {
            Messengers.remove(9);
            Toast.makeText(this, "removed", Toast.LENGTH_SHORT).show();
        }

        flag = !flag;
    }
}
