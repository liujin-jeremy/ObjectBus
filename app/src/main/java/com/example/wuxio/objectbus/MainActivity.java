package com.example.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.objectbus.bus.ObjectBus;
import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.CancelTodo;
import com.example.objectbus.schedule.Scheduler;
import com.example.objectbus.schedule.run.AsyncThreadCallBack;
import com.example.objectbus.schedule.run.MainThreadCallBack;

/**
 * @author wuxio
 */
public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

    private static final String TAG = "MainActivity";

    protected TextView mLogText01;
    protected TextView mLogText02;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MainManager.getInstance().register(this);

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
    }


    private void initView() {

        mLogText01 = findViewById(R.id.logText01);
        mLogText02 = findViewById(R.id.logText02);
    }

    //============================ scheduler ============================


    public void testSchedulerTodo(View view) {

        clearText(mLogText01);

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 01");
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 02", mLogText01);
            }
        });
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo 03", mLogText01);
            }
        });
    }


    public void testSchedulerTodoDelayed(View view) {

        clearText(mLogText01);

        print(" start delayed task ");

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed01", mLogText01);
            }
        }, 2000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed02", mLogText01);
            }
        }, 4000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed03", mLogText01);
            }
        }, 6000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed04", mLogText01);
            }
        }, 4000);
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo delayed05", mLogText01);
            }
        }, 2000);
    }


    public void testSchedulerTodoMainCallBack(View view) {

        clearText(mLogText01);

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

                print(" todo back delayed", mLogText01);
            }
        }, 3000, new MainThreadCallBack() {
            @Override
            public void run() {

                print(" callback main ", mLogText01);
            }
        });
    }


    public void testSchedulerTodoAsyncCallBack(View view) {

        clearText(mLogText01);

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back ", mLogText01);
            }
        }, new AsyncThreadCallBack() {
            @Override
            public void run() {

                print(" callback async ", mLogText01);
            }
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" todo back delayed", mLogText01);
            }
        }, 3000, new AsyncThreadCallBack() {
            @Override
            public void run() {

                print(" callback async ", mLogText01);
            }
        });
    }


    public void testSchedulerTodoWithListener(View view) {

        clearText(mLogText01);

        Scheduler.todo(new OnExecuteRunnable() {
            @Override
            public void onExecute() {

                print(" do something in pool ", mLogText01);
            }


            @Override
            public void onFinish() {

                print(" running; do something extra in pool ", mLogText01);
            }


            @Override
            public void onException(Exception e) {

                print(" Exception! do something to rescue ", mLogText01);
            }
        });

        Scheduler.todo(new OnExecuteRunnable() {
            @Override
            public void onExecute() {

                print(" do something in pool ", mLogText01);
            }


            @Override
            public void onFinish() {

                print(" running; do something extra in pool ", mLogText01);
            }


            @Override
            public void onException(Exception e) {

                print(" Exception! do something to rescue ", mLogText01);
            }
        }, 2000, new MainThreadCallBack() {
            @Override
            public void run() {

                print(" callback is different with 'OnExecuteRunnable' ", mLogText01);
            }
        });
    }


    private boolean flag;


    public void testSchedulerCancel(View view) {

        clearText(mLogText01);

        CancelTodo cancelTodo = new CancelTodo();
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                print(" test todo cancel", mLogText01);
            }
        }, 2000, cancelTodo);

        if (flag) {
            cancelTodo.cancel();
            Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
        }
        flag = !flag;
    }


    public static void clearText(TextView textView) {

        textView.setText("");
    }


    public synchronized static void print(String text, TextView textView) {

        String msg = ":" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text;
        Log.i(TAG, msg);

        CharSequence old = textView.getText();
        textView.post(() -> textView.setText(old + "\n" + msg));

    }


    public synchronized static void print(String text) {

        String msg = ":" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text;
        Log.i(TAG, msg);

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

        clearText(mLogText02);

        /* message what 的奇偶性决定发送到哪个线程 */

        /* 1 is odd number,the message will send to main thread */

        Messengers.send(1, this);

        /* 2 is  even number, the message will send to a async thread*/

        Messengers.send(2, this);

    }


    public void testMessengerSendDelayed(View view) {

        clearText(mLogText02);

        print("send delayed message");

        Messengers.send(3, 2000, this);
        Messengers.send(4, 2000, this);
    }


    public void testMessengerSendWithExtra(View view) {

        clearText(mLogText02);

        Messengers.send(5, " hello main ", this);
        Messengers.send(6, " hello main ", this);

        Messengers.send(7, 2000, " hello main ", this);
        Messengers.send(8, 2000, " hello main ", this);
    }


    public void testMessengerRemove(View view) {

        clearText(mLogText02);

        Messengers.send(9, 2000, " hello main ", this);
        Messengers.send(9, 2000, " hello main ", this);
        Messengers.send(9, 2000, " hello main ", this);

        Messengers.send(9, 2000, " hello mainManager ", MainManager.getInstance());
        Messengers.send(9, 2000, " hello mainManager ", MainManager.getInstance());
        Messengers.send(9, 2000, " hello mainManager ", MainManager.getInstance());

        if (flag) {
            Messengers.remove(9, this);
            Toast.makeText(this, "removed", Toast.LENGTH_SHORT).show();
        }

        flag = !flag;
    }

    //============================ bus ============================

    private boolean running = false;


    public void testBusGo(View view) {

        if (running) {
            return;
        }

        running = true;

        ObjectBus bus = new ObjectBus();

        bus.go(() -> print(" do task 01 @Main"))
                .toUnder(() -> {

                    print(" do task 02 @ThreadPools ");

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                })
                .go(() -> {

                    print(" do task 03 @ThreadPools ");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }).toMain(() -> print(" go back to @Main "))
                .run();
    }


    public void testBusGoWithParams(View view) {

        ObjectBus bus = new ObjectBus();

        bus.go(() -> {

            print(" do task 01 @Main");
            int j = 99 + 99;
            bus.take(j, "result");

        }).toUnder(() -> {

            print(" do task 02 @ThreadPools ");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Integer result = (Integer) bus.get("result");
            int k = result + 1002;
            bus.take(k, "result");

        }).go(() -> {

            print(" do task 03 @ThreadPools ");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Integer result = (Integer) bus.get("result");
            int l = result + 3000;
            bus.take(l, "result");

        }).toMain(() -> {

            Integer result = (Integer) bus.get("result");
            print(" go back to @Main , result: " + result);

        }).run();

    }


    ObjectBus bus = new ObjectBus();


    public void testBusControl(View view) {

        bus.go(new Runnable() {

            @Override
            public void run() {

                print(" do task 01 ");

            }

        }).takeRest()
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print(" after take a rest go on do task 02 ");

                    }
                }).run();

    }


    public void goOn(View view) {

        bus.stopRest();

    }


    public void testBusMessage(View view) {

        bus.go(new Runnable() {
            @Override
            public void run() {

                print(" do someThing  ");
            }
        }).send(158, bus, MainManager.getInstance())
                .takeRest()
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print(" rest finished ");
                    }
                })
                .run();

    }


    public void testBusMessageRegister(View view) {

        bus.go(new Runnable() {
            @Override
            public void run() {

                print(" do someThing  ");
            }
        }).registerMessage(88, new Runnable() {
            @Override
            public void run() {

                print(" receive message ");
            }
        }).go(new Runnable() {
            @Override
            public void run() {

                print(" do finished ");

            }
        }).run();

        Messengers.send(88, 3000, bus);

    }
}
