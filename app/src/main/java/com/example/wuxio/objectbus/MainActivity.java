package com.example.wuxio.objectbus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.objectbus.bus.ObjectBus;
import com.example.objectbus.bus.OnAfterRunAction;
import com.example.objectbus.bus.OnBeforeRunAction;
import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.CancelTodo;
import com.example.objectbus.schedule.Scheduler;
import com.example.objectbus.schedule.run.AsyncThreadCallBack;
import com.example.objectbus.schedule.run.MainThreadCallBack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author wuxio
 */
public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

    private static final String TAG = "MainActivity";
    protected NavigationView mNavigationView;
    protected DrawerLayout   mDrawerLayout;
    protected TextView       mTextLog;
    protected ScrollView     mContainer;

    private String mLog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MainManager.getInstance().register(this);

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
    }


    private void initView() {

        mNavigationView = findViewById(R.id.navigationView);
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mTextLog = findViewById(R.id.text_log);
        mContainer = findViewById(R.id.container);

        mNavigationView.setNavigationItemSelectedListener(new MainNavigationItemClick());
    }


    private void closeDrawer() {

        mDrawerLayout.closeDrawer(Gravity.START);
    }


    private ObjectBus mLogBus = new ObjectBus();


    private void printLog(String log) {

        mLog = mLog + log;
        mLogBus.toMain(() -> {
            mTextLog.setText(mLog);
            mLogBus.clearRunnable();
        }).run();
    }


    class MainNavigationItemClick implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_00:
                    testSchedulerTodo();
                default:
                    break;
            }

            closeDrawer();
            return true;
        }
    }

    //============================ scheduler ============================


    public void testSchedulerTodo() {

        mLog = "";

        /* 以下会将任务带到后台执行  */

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                printLog(" todo 01 ");
                print(" todo 01 ");
            }
        });

        /* lambda 简化编程 */

        Scheduler.todo(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            printLog(" todo 02 ");
            print(" todo 02 ");
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                printLog(" todo 03 ");
                print(" todo 03 ");
            }
        });
    }


    public void testSchedulerTodoDelayed(View view) {

        mLog = "";

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

                print(" running; do something extra in pool ");
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

                print(" running; do something extra in pool ");
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


        /* message what 的奇偶性决定发送到哪个线程 */

        /* 1 is odd number,the message will send to main thread */

        Messengers.send(1, this);

        /* 2 is  even number, the message will send to a async thread*/

        Messengers.send(2, this);

    }


    public void testMessengerSendDelayed(View view) {

        print("send delayed message");

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

                print(" receive message 88");
            }
        }).registerMessage(87, new Runnable() {
            @Override
            public void run() {

                print(" receive message 87");
            }
        }).go(new Runnable() {
            @Override
            public void run() {

                print(" do finished ");

            }
        }).run();

        Messengers.send(88, 3000, bus);
        Messengers.send(87, 3000, bus);

    }


    public void testBusCallableList(View view) {

        ObjectBus bus = new ObjectBus();

        List< Callable< String > > callableList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

            int j = i;

            Callable< String > callable = new Callable< String >() {
                @Override
                public String call() throws Exception {

                    Thread.sleep(1000);

                    return String.valueOf(j);
                }
            };

            callableList.add(callable);

        }

        bus.toUnder(callableList, "CAll_LIST")
                .go(new Runnable() {
                    @Override
                    public void run() {

                        List< String > result = (List< String >) bus.get("CAll_LIST");

                        Log.i(TAG, "run:" + result);
                    }
                }).run();

    }


    public void testBusCallable(View view) {

        ObjectBus bus = new ObjectBus();

        Callable< String > callable = new Callable< String >() {
            @Override
            public String call() throws Exception {

                Thread.sleep(1000);

                return String.valueOf(1990);
            }
        };

        bus.toUnder(callable, "CAll")
                .go(new Runnable() {
                    @Override
                    public void run() {

                        String result = (String) bus.get("CAll");

                        Log.i(TAG, "run:" + result);
                    }
                }).run();

    }


    public void testBusRunnableList(View view) {

        ObjectBus bus = new ObjectBus();

        List< Runnable > runnableList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    try {
                        print("start");
                        Thread.sleep(1000);
                        print("end");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            runnableList.add(runnable);

        }

        bus.toUnder(runnableList)
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print("all finished");
                    }
                }).run();

    }


    public void testBusLazyGo(View view) {

        ObjectBus bus = new ObjectBus();

        bus.go(new Runnable() {
            @Override
            public void run() {

                print(" do something 01 ");
            }
        }).go(new OnBeforeRunAction< Runnable >() {
            @Override
            public void onBeforeRun(Runnable runnable) {

                print("before take to bus" + runnable);
                bus.take("Hello runnable", "key");
            }
        }, new Runnable() {
            @Override
            public void run() {

                String msg = (String) bus.get("key");

                print(msg + " get from bus ");
            }
        }, new OnAfterRunAction< Runnable >() {
            @Override
            public void onAfterRun(Object bus, Runnable runnable) {

                print("after run " + runnable);

            }
        }).run();
    }
}
