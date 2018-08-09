package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.threekilogram.objectbus.bus.ObjectBus;
import com.threekilogram.objectbus.bus.ObjectBusStation;
import com.threekilogram.objectbus.bus.OnBeforeRunAction;
import com.threekilogram.objectbus.bus.OnRunExceptionHandler;
import com.threekilogram.objectbus.bus.OnRunFinishAction;
import com.threekilogram.objectbus.executor.PoolThreadExecutor;
import com.threekilogram.objectbus.message.Messengers;
import com.threekilogram.objectbus.message.OnMessageReceiveListener;
import com.threekilogram.objectbus.runnable.Executable;
import com.threekilogram.objectbus.schedule.CancelTodo;
import com.threekilogram.objectbus.schedule.Scheduler;
import com.threekilogram.objectbus.schedule.run.AsyncThreadCallBack;
import com.threekilogram.objectbus.schedule.run.MainThreadCallBack;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author wuxio
 */
public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

      private static final String TAG = "MainActivity";
      protected NavigationView mNavigationView;
      protected DrawerLayout   mDrawerLayout;
      protected TextView       mTextLog;
      protected ScrollView     mContainer;

      private String mLog = "";

      @Override
      protected void onCreate (Bundle savedInstanceState) {

            MainManager.getInstance().register(this);

            super.onCreate(savedInstanceState);
            super.setContentView(R.layout.activity_main);
            initView();
      }

      private void initView () {

            mNavigationView = findViewById(R.id.navigationView);
            mDrawerLayout = findViewById(R.id.drawerLayout);
            mTextLog = findViewById(R.id.text_log);
            mContainer = findViewById(R.id.container);

            mNavigationView.setNavigationItemSelectedListener(new MainNavigationItemClick());
      }

      private void closeDrawer () {

            mDrawerLayout.closeDrawer(Gravity.START);
      }

      public synchronized static void print (String text) {

            String msg = ":" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text;
            Log.i(TAG, msg);
      }

      private ObjectBus mLogBus = new ObjectBus();

      private void clearLogText () {

            mLog = "";
            mTextLog.setText("");
      }

      private synchronized void printLog (String log) {

            String s = log + " : " +
                " ThreadOn: " + Thread.currentThread().getName() + ";" +
                " timeAt: " + System.currentTimeMillis() + "\n";
            mLog = mLog + s;

            mLogBus.toMain(() -> {
                  mTextLog.setText(mLog);
                  mLogBus.clearRunnable();
            }).run();
      }

      private synchronized void printText (String text) {

            mLog = mLog + text;

            mLogBus.toMain(() -> {
                  mTextLog.setText(mLog);
                  mLogBus.clearRunnable();
            }).run();
      }

      private void addEnter () {

            mLog = mLog + "\n";
      }

      public void testExecutorRunnable () {

            PoolThreadExecutor.execute( new Runnable() {

                  @Override
                  public void run () {

                        printLog(" run ");
                        print(" run ");
                  }
            });
      }

      //============================ PoolThreadExecutor ============================

      public void testExecutorCallable () {

            Future<String> submit = PoolThreadExecutor.submit( new Callable<String>() {

                  @Override
                  public String call () throws Exception {

                        String s = " call At";
                        printLog(s);
                        print(s);
                        return "Hello";
                  }
            });

            /* 不要在主线程{submit.get()},否则主线程会阻塞 */

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        try {
                              String s = submit.get();
                              printLog(" getAt: " + s);
                              print(" getAt: " + s);
                        } catch(InterruptedException | ExecutionException e) {
                              e.printStackTrace();
                        }
                  }
            });
      }

      public void testExecutorCallableAndGet () {

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        /* submitAndGet 会阻塞调用线程,推荐和Scheduler配合,在后台读取结果 */

                        String get = PoolThreadExecutor.submitAndGet( new Callable<String>() {

                              @Override
                              public String call () throws Exception {

                                    String s = "Hello";
                                    printLog(s);
                                    print(s);
                                    return s;
                              }
                        });
                  }
            });
      }

      public void testExecutorRunnableList () {

            final int size = 4;
            List<Runnable> runnableList = new ArrayList<>(size);
            for(int i = 0; i < size; i++) {

                  final int j = i;

                  Runnable runnable = new Runnable() {

                        @Override
                        public void run () {

                              String s = "running " + j;
                              printLog(s);
                              print(s);
                        }
                  };
                  runnableList.add(runnable);
            }

            PoolThreadExecutor.execute( runnableList );
      }

      public void testExecutorCallableList () {

            /* 因为 submitAndGet 会阻塞调用线程,所以和Scheduler配合,在后台读取结果 */

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        final int size = 4;
                        List<Callable<String>> callableList = new ArrayList<>(size);
                        for(int i = 0; i < size; i++) {

                              final int j = i;
                              Callable<String> callable = new Callable<String>() {

                                    @Override
                                    public String call () throws Exception {

                                          String s = " calling " + j;
                                          printLog(s);
                                          print(s);
                                          return "Hello " + j;
                                    }
                              };

                              callableList.add(callable);
                        }

                        List<String> stringList = PoolThreadExecutor.submitAndGet( callableList );

                        int length = stringList.size();
                        for(int i = 0; i < length; i++) {
                              String s = stringList.get(i);
                              printLog(s);
                              print(s);
                        }

                        addEnter();
                        String s = "可以看出 callable 运行在其他线程;结果都在同一个线程读取";
                        printText(s);
                  }
            });
      }

      public void testSchedulerTodoWithListener ( ) {

            Scheduler.todo( new Executable() {

                  @Override
                  public void onStart ( ) {

                        String msg = "task start";
                        printLog( msg );
                        print( msg );
                  }

                  @Override
                  public void onExecute ( ) {

                        String msg = "task running";
                        printLog( msg );
                        print( msg );

                        try {
                              Thread.sleep( 1000 );
                        } catch(InterruptedException e) {
                              e.printStackTrace();
                        }

                        mFlag = !mFlag;
                        if( mFlag ) {
                              throw new RuntimeException( "null" );
                        }
                  }

                  @Override
                  public void onFinish ( ) {

                        String msg = "task finish";
                        printLog( msg );
                        print( msg );
                  }
            } );
      }

      //============================ scheduler ============================

      public void testSchedulerTodo () {


            /* 以下会将任务带到后台执行  */

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        printLog(" todo 01 ");
                        print(" todo 01 ");
                  }
            });

            /* lambda 简化编程 */

            Scheduler.todo(() -> {

                  try {
                        Thread.sleep(1000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }

                  printLog(" todo 02 ");
                  print(" todo 02 ");
            });

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        try {
                              Thread.sleep(2000);
                        } catch(InterruptedException e) {
                              e.printStackTrace();
                        }

                        printLog(" todo 03 ");
                        print(" todo 03 ");
                  }
            });
      }

      public void testSchedulerTodoDelayed () {

            Scheduler.todo(() -> {

                  String msg = " delayed01 ";
                  printLog(msg);
                  print(msg);
            }, 1000);

            Scheduler.todo(() -> {

                  String msg = " delayed02 ";
                  printLog(msg);
                  print(msg);
            }, 2000);

            Scheduler.todo(() -> {

                  String msg = " delayed03 ";
                  printLog(msg);
                  print(msg);
            }, 3000);

            Scheduler.todo(() -> {

                  String msg = " delayed04 ";
                  printLog(msg);
                  print(msg);
            }, 4000);

            Scheduler.todo(() -> {

                  String msg = " delayed05 ";
                  printLog(msg);
                  print(msg);
            }, 5000);
      }

      public void testSchedulerTodoMainCallBack () {

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        String msg = "back";
                        printLog(msg);
                        print(msg);
                  }
            }, new MainThreadCallBack() {

                  @Override
                  public void run () {

                        String msg = "callback";
                        printLog(msg);
                        print(msg);
                        addEnter();
                  }
            });

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        String msg = "back delayed";
                        printLog(msg);
                        print(msg);
                  }
            }, 1000, new MainThreadCallBack() {

                  @Override
                  public void run () {

                        String msg = "callback";
                        printLog(msg);
                        print(msg);
                  }
            });
      }

      public void testSchedulerTodoAsyncCallBack () {

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        String msg = "back";
                        printLog(msg);
                        print(msg);
                  }
            }, new AsyncThreadCallBack() {

                  @Override
                  public void run () {

                        String msg = "callback";
                        printLog(msg);
                        print(msg);
                        addEnter();
                  }
            });

            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        String msg = "back delayed";
                        printLog(msg);
                        print(msg);
                  }
            }, 1000, new AsyncThreadCallBack() {

                  @Override
                  public void run () {

                        String msg = "callback";
                        printLog(msg);
                        print(msg);
                  }
            });
      }

      private boolean mFlag;

      class MainNavigationItemClick implements NavigationView.OnNavigationItemSelectedListener {

            @Override
            public boolean onNavigationItemSelected ( @NonNull MenuItem item ) {

                  CharSequence text = mTextLog.getText();
                  clearLogText();

                  switch( item.getItemId() ) {

                        /* Scheduler */

                        case R.id.menu_00:
                              testSchedulerTodo();
                              break;
                        case R.id.menu_01:
                              testSchedulerTodoDelayed();
                              break;
                        case R.id.menu_02:
                              testSchedulerTodoMainCallBack();
                              break;
                        case R.id.menu_03:
                              testSchedulerTodoAsyncCallBack();
                              break;
                        case R.id.menu_04:
                              testSchedulerTodoWithListener();
                              break;
                        case R.id.menu_05:
                              testSchedulerCancel();
                              break;

                        /* PoolThreadExecutor */

                        case R.id.menu_06:
                              testExecutorRunnable();
                              break;
                        case R.id.menu_07:
                              testExecutorCallable();
                              break;
                        case R.id.menu_08:
                              testExecutorCallableAndGet();
                              break;
                        case R.id.menu_09:
                              testExecutorRunnableList();
                              break;
                        case R.id.menu_10:
                              testExecutorCallableList();
                              break;

                        /* Messenger */

                        case R.id.menu_11:
                              testMessengerSend();
                              break;
                        case R.id.menu_12:
                              testMessengerSendDelayed();
                              break;
                        case R.id.menu_13:
                              testMessengerSendWithExtra();
                              break;
                        case R.id.menu_14:
                              testMessengerRemove();
                              break;

                        /* ObjectBus */

                        case R.id.menu_15:
                              testBusGo();
                              break;
                        case R.id.menu_16:
                              testBusGoWithParams();
                              break;
                        case R.id.menu_17:
                              testBusTakeRest();
                              break;
                        case R.id.menu_18:
                              testBusStopRest( text );
                              break;
                        case R.id.menu_19:
                              testBusSend();
                              break;
                        case R.id.menu_20:
                              testBusRegisterMessage();
                              break;
                        case R.id.menu_21:
                              testBusCallable();
                              break;
                        case R.id.menu_22:
                              testBusCallableList();
                              break;
                        case R.id.menu_23:
                              testBusRunnableList();
                              break;
                        case R.id.menu_24:
                              testBusListener();
                              break;

                        /* ObjectBusStation */
                        case R.id.menu_25:
                              testBusStation();
                              break;

                        /* print all thread */

                        case R.id.menu_26:
                              getAllThreads();
                              break;
                        case R.id.menu_27:
                              printAllThreads();
                              break;

                        default:
                              break;
                  }

                  closeDrawer();
                  return true;
            }
      }

      public void testSchedulerCancel () {

            CancelTodo cancelTodo = new CancelTodo();
            Scheduler.todo(new Runnable() {

                  @Override
                  public void run () {

                        String msg = "running";
                        printLog(msg);
                        print(msg);
                  }
            }, 2000, cancelTodo);

            if(mFlag) {
                  cancelTodo.cancel();
                  Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
            } else {
                  String s = "2s 后";
                  printText(s);
                  addEnter();
                  print(s);
            }

            mFlag = !mFlag;
      }

      /* 实现 OnMessageReceiveListener, 以接收消息 */

      @Override
      public void onReceive (int what, Object extra) {

            String s = "MainActivity receive: " + what + " extra: " + extra;
            printLog(s);
            print(s);
            addEnter();
      }

      @Override
      public void onReceive (int what) {

            String s = "MainActivity receive: " + what;
            printLog(s);
            print(s);
            addEnter();
      }


      /* 如果一个类实现不了 OnMessageReceiveListener 接口,使用如下包装者模式,以实现通信 */

      private MessengerReceiver mMessengerReceiver = new MessengerReceiver(this);

      private static class MessengerReceiver implements OnMessageReceiveListener {

            private WeakReference<MainActivity> mReference;

            public MessengerReceiver (MainActivity activity) {

                  mReference = new WeakReference<>(activity);
            }

            @Override
            public void onReceive (int what, Object extra) {

                  /* try catch 因为 mReference.get() 可能会为null */

                  try {
                        String s = "MessengerReceiver receive: " + what + " extra: " + extra;
                        mReference.get().printLog(s);
                        print(s);
                        mReference.get().addEnter();
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }

            @Override
            public void onReceive (int what) {

                  /* try catch 因为 mReference.get() 可能会为null */

                  try {
                        String s = "MessengerReceiver receive: " + what;
                        mReference.get().printLog(s);
                        print(s);
                        mReference.get().addEnter();
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }
      }

      //============================ test messenger ============================

      public void testMessengerSend () {

            String s = "send message,消息what值的奇偶性决定在哪个线程回调";
            printText(s);
            print(s);
            addEnter();
            addEnter();

            /* message what 的奇偶性决定发送到哪个线程 */

            /* 1 is odd number,the message will send to main thread */

            Messengers.send(1, this);

            /* 2 is even number, the message will send to a Messenger thread*/

            Messengers.send(2, this);

            Messengers.send(1, mMessengerReceiver);
            Messengers.send(2, mMessengerReceiver);
      }

      public void testMessengerSendDelayed () {

            String s = "send delayed message";
            printText(s);
            print(s);
            addEnter();
            addEnter();

            Messengers.send(3, 2000, this);
            Messengers.send(4, 2000, this);

            Messengers.send(3, 2000, mMessengerReceiver);
            Messengers.send(4, 2000, mMessengerReceiver);
      }

      public void testMessengerSendWithExtra () {

            String s = "send message with extra";
            printText(s);
            print(s);
            addEnter();
            addEnter();

            Messengers.send(5, " hello ", this);
            Messengers.send(6, " hello ", this);

            Messengers.send(7, 2000, " hello ", this);
            Messengers.send(8, 2000, " hello ", this);

            Messengers.send(5, " hello ", mMessengerReceiver);
            Messengers.send(6, " hello ", mMessengerReceiver);

            Messengers.send(7, 2000, " hello ", mMessengerReceiver);
            Messengers.send(8, 2000, " hello ", mMessengerReceiver);
      }

      public void testMessengerRemove () {

            Messengers.send(9, 2000, " hello ", this);

            Messengers.send(9, 2000, " hello mainManager ", mMessengerReceiver);

            if(mFlag) {
                  Messengers.remove(9, this);
                  Toast.makeText(this, "removed", Toast.LENGTH_SHORT).show();
            } else {
                  String s = " 2s 后收到消息 ";
                  printText(s);
                  addEnter();
                  print(s);
            }

            mFlag = !mFlag;
      }

      //============================ mBus ============================

      private boolean running = false;

      public void testBusGo () {

            ObjectBus bus = new ObjectBus();

            bus.go(() -> {
                  String s = "task 01";
                  printLog(s);
                  print(s);
            }).toUnder(() -> {

                  try {
                        Thread.sleep(1000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }

                  String s = "task 02";
                  printLog(s);
                  print(s);
            }).go(() -> {

                  try {
                        Thread.sleep(1000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }

                  String s = "task 03";
                  printLog(s);
                  print(s);

                  try {
                        Thread.sleep(1000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }
            }).toMain(() -> {

                  String s = "task 04";
                  printLog(s);
                  print(s);

                  addEnter();
                  printText("在不同的线程上顺次执行");
            }).run();
      }

      public void testBusGoWithParams () {

            ObjectBus bus = new ObjectBus();

            bus.go(() -> {

                  int j = 99 + 99;
                  String s = "take " + j + " to Bus";
                  printLog(s);
                  print(s);
                  addEnter();

                  bus.take(j, "result");
            }).toUnder(() -> {

                  try {
                        Thread.sleep(2000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }

                  Integer result = (Integer) bus.get("result");

                  String s = "get from last Task " + result;
                  printLog(s);
                  print(s);

                  int k = result + 1002;
                  bus.take(k, "result");

                  s = "take " + k + " to Bus";
                  printLog(s);
                  print(s);
                  addEnter();
            }).go(() -> {

                  try {
                        Thread.sleep(1000);
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }

                  Integer result = (Integer) bus.get("result");
                  String s = "get from last Task " + result;
                  printLog(s);
                  print(s);

                  int l = result + 3000;
                  bus.take(l, "result");

                  s = "take " + l + " to Bus";
                  printLog(s);
                  print(s);
                  addEnter();
            }).toMain(() -> {

                  Integer result = (Integer) bus.get("result");
                  String s = "get final result " + result;
                  printLog(s);
                  print(s);
            }).run();
      }

      ObjectBus mBus = new ObjectBus();

      public void testBusTakeRest () {

            mBus.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = "do task 01";
                        printLog(s);
                        print(s);

                        addEnter();
                        printText("wait for stopRest() to call");
                  }
            }).takeRest()
                .go(new Runnable() {

                      @Override
                      public void run () {

                            String s = "after take a rest go on do task 02";
                            printLog(s);
                            print(s);
                      }
                }).run();
      }

      public void testBusStopRest (CharSequence text) {

            if(mBus.isResting()) {
                  printText(text.toString());
                  addEnter();
                  addEnter();
                  mBus.stopRest();
            }
      }

      public void testBusSend () {

            mBus.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = "do something";
                        printLog(s);
                        print(s);
                        s = "send a message";
                        printLog(s);
                        print(s);
                  }

                  /* send 一般用于bus运行过程中,向外发送一条消息触发一个事件,既可以直接继续运行,也可以等待其它事件运行完成后在运行(配合 takeRest ) */
            }).send(158, mBus, MainManager.getInstance())
                .takeRest()
                .go(new Runnable() {

                      @Override
                      public void run () {

                            String s = "all finished";
                            printLog(s);
                            print(s);
                      }
                }).run();
      }

      public void testBusRegisterMessage () {

            mBus.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = "do someThing";
                        printLog(s);
                        print(s);

                        addEnter();
                        s = "wait 3s";
                        printText(s);
                        addEnter();
                        addEnter();
                  }
            }).registerMessage(88, new Runnable() {

                  /* 88偶数:注册一个后台线程消息,在后台线程接收处理消息 */

                  @Override
                  public void run () {

                        String s = "receive message 88";
                        printLog(s);
                        print(s);

                        mBus.stopRest();
                  }
            }).registerMessage(87, new Runnable() {

                  /* 87奇数:注册一个主线程线程消息,在主线程接收处理消息 */

                  @Override
                  public void run () {

                        String s = "receive message 87";
                        printLog(s);
                        print(s);

                        mBus.stopRest();
                  }
            }).takeRest()
                .go(new Runnable() {

                      @Override
                      public void run () {

                            try {
                                  Thread.sleep(1000);
                            } catch(InterruptedException e) {
                                  e.printStackTrace();
                            }

                            addEnter();

                            String s = "do finished";
                            printLog(s);
                            print(s);
                      }
                }).run();

            Messengers.send(88, 3000, mBus);
            Messengers.send(87, 3000, mBus);
      }

      public void testBusCallable () {

            ObjectBus bus = new ObjectBus();

            Callable<String> callable = new Callable<String>() {

                  @Override
                  public String call () throws Exception {

                        return String.valueOf(1990);
                  }
            };

            /* 在后台执行Callable 并保存为"CALL",后续可以使用该key读取该数据 */

            /* toUnder(@NonNull Callable< T > callable, String key) 后台执行Callable 并保存为"CALL" */
            bus.toUnder(callable, "CAll")
               .go(new Runnable() {

                     @Override
                     public void run () {

                           /* 读取上一步操作保存的callable返回值 */

                           String result = "result: " + (String) bus.get("CAll") + " from Callable";
                           printLog(result);
                           print(result);
                     }
               }).run();
      }

      public void testBusCallableList () {

            ObjectBus bus = new ObjectBus();

            List<Callable<String>> callableList = new ArrayList<>();

            for(int i = 0; i < 5; i++) {

                  int j = i;

                  Callable<String> callable = new Callable<String>() {

                        @Override
                        public String call () throws Exception {

                              return String.valueOf("Hello " + j);
                        }
                  };

                  callableList.add(callable);
            }

            /* 在后台执行Callable列表 并保存为"CAll_LIST",后续可以使用该key读取该数据 */

            bus.toUnder(callableList, "CAll_LIST")
               .go(new Runnable() {

                     @SuppressWarnings("unchecked")
                     @Override
                     public void run () {

                           printText("callable list result:");
                           addEnter();
                           addEnter();

                           List<String> stringList = (List<String>) bus.get("CAll_LIST");

                           for(String s : stringList) {
                                 printLog(s);
                                 print(s);
                           }
                     }
               }).run();
      }

      public void testBusRunnableList () {

            ObjectBus bus = new ObjectBus();

            List<Runnable> runnableList = new ArrayList<>();

            for(int i = 0; i < 6; i++) {

                  final int j = i;

                  Runnable runnable = new Runnable() {

                        @Override
                        public void run () {

                              try {
                                    Thread.sleep(1000);
                              } catch(InterruptedException e) {
                                    e.printStackTrace();
                              }

                              String s = "task " + j + " finished";
                              printLog(s);
                              print(s);
                        }
                  };

                  runnableList.add(runnable);
            }

            bus.toUnder(runnableList)
               .go(new Runnable() {

                     @Override
                     public void run () {

                           addEnter();
                           String s = "All finished";
                           printLog(s);
                           print(s);

                           addEnter();
                           s = "runnable list 执行完毕之后,才执行后面操作";
                           printText(s);
                           print(s);
                     }
               }).run();
      }

      public void testBusListener () {

            ObjectBus bus = new ObjectBus();

            bus.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = " task 01 ";
                        printLog(s);
                        print(s);
                        addEnter();
                  }
            }).go(new OnBeforeRunAction<Runnable>() {

                  @Override
                  public void onBeforeRun (Runnable runnable) {

                        String s = " before task 02 ";
                        printLog(s);
                        print(s);
                        bus.take("Hello runnable", "key");
                  }
            }, new Runnable() {

                  @Override
                  public void run () {

                        mFlag = !mFlag;

                        if(mFlag) {
                              String msg = (String) bus.getOff("key");

                              String s = " task 02: " + " get from BeforeAction: " + msg;
                              printLog(s);
                              print(s);
                        } else {

                              String s = " task 02: running";
                              printLog(s);
                              print(s);
                              Toast.makeText(MainActivity.this, "Exception", Toast.LENGTH_SHORT)
                                   .show();
                              throw new RuntimeException("exception");
                        }
                  }
            }, new OnRunFinishAction<Runnable>() {

                  @Override
                  public void onRunFinished (Object bus, Runnable runnable) {

                        String s = " after task 02 ";
                        printLog(s);
                        print(s);
                  }
            }, new OnRunExceptionHandler() {

                  @Override
                  public void onException (Runnable runnable, Exception e) {

                        String s = " Exception ";
                        printLog(s);
                        print(s);
                  }
            }).run();
      }

      public void testBusStation () {

            ObjectBus bus00 = ObjectBusStation.getInstance().obtainBus();
            ObjectBus bus01 = ObjectBusStation.getInstance().obtainBus();
            ObjectBus bus02 = ObjectBusStation.getInstance().obtainBus();

            bus02.registerMessage(1, new Runnable() {

                  @Override
                  public void run () {

                        ObjectBus bus = ObjectBusStation.callNewBus();
                        bus.go(new Runnable() {

                              @Override
                              public void run () {

                                    String s = "do task 02 " + bus;
                                    printLog(s);
                                    print(s);
                              }
                        }).run();
                  }
            });

            bus00.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = "do task 01 " + bus00;
                        printLog(s);
                        print(s);

                        ObjectBusStation.recycle(bus00);
                        Messengers.send(1, bus02);
                  }
            }).run();

            bus01.go(new Runnable() {

                  @Override
                  public void run () {

                        String s = "do task 01 " + bus01;
                        printLog(s);
                        print(s);

                        ObjectBusStation.recycle(bus01);
                        Messengers.send(1, bus02);
                  }
            }).run();
      }

      public void getAllThreads () {

            Thread[] allThread = AllThreads.getAllThread();
            for(int i = 0; i < allThread.length; i++) {

                  Log.e(TAG, "getAllThreads : " + allThread[i]);
            }
      }

      public void printAllThreads () {

            AllThreads.printThreads();
      }
}
