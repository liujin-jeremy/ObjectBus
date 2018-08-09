package com.threekilogram.objectbus.bus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.SparseArray;
import com.threekilogram.objectbus.executor.PoolThreadExecutor;
import com.threekilogram.objectbus.message.Messengers;
import com.threekilogram.objectbus.message.OnMessageReceiveListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 该类穿梭于线程之间,顺序执行各种操作
 *
 * @author wuxio 2018-05-03:16:16
 */
public class ObjectBus implements OnMessageReceiveListener {

      /**
       * command used for {@link Command} to how to do runnable
       */
      private static final int COMMAND_GO               = 0b1;
      private static final int COMMAND_SEND             = 0b1;
      private static final int COMMAND_TO_UNDER         = 0b10;
      private static final int COMMAND_CALLABLE         = 0b10;
      private static final int COMMAND_MULTI_CALLABLE   = 0b10;
      private static final int COMMAND_MULTI_RUNNABLE   = 0b10;
      private static final int COMMAND_TO_MAIN          = 0b100;
      private static final int COMMAND_TAKE_REST        = 0b10000;
      private static final int COMMAND_TAKE_REST_AWHILE = 0b100000;

      /**
       * record run State if is resting
       */
      private static final int RUN_STATE_RUNNING        = 0X10EE;
      private static final int RUN_STATE_RESTING        = 0X11EE;
      private static final int RUN_STATE_RESTING_AWHILE = 0X100EE;
      private int runState;

      private static final int THREAD_EXECUTOR = 0X10EEE;
      private static final int THREAD_MAIN     = 0X11EEE;
      private int threadCurrent;

      /**
       * how many station pass By
       */
      private AtomicInteger mPassBy = new AtomicInteger();

      /**
       * how to pass every station
       */
      private final ArrayList<Command> mHowToPass = new ArrayList<>();

      /**
       * used do runnable at {@link PoolThreadExecutor},used by {@link
       * #COMMAND_TO_UNDER}
       */
      private ExecutorRunnable mExecutorRunnable;

      /**
       * used do runnable at MainThread with {@link #COMMAND_TO_MAIN}, {@link #stopRest()}:to stop
       * bus rest {@link TakeWhileRunnable#run()}: to top bus rest
       */
      private BusMessenger mBusMessageManager;

      /**
       * take customs to bus,{@link #take(Object, String)},{@link #get(String)},{@link
       * #getOff(String)}
       */
      private ArrayMap<String, Object> mExtras;

      /**
       * do nothing just take a rest ,{@link #COMMAND_TAKE_REST},because {@link Command#Command(int,
       * Runnable)} not null,so need a runnable to take place
       */
      private RestRunnable mRestRunnable;

      public ObjectBus () {

      }

      //============================ init to original ============================

      /**
       * 将状态还原为初始值
       */
      public void initToNew () {

            clearRunnable();
            clearMessageReceiveRunnable();
            clearPassenger();
      }

      //============================ core ============================

      /**
       * @return did how many task
       */
      public int getPassBy () {

            return mPassBy.get();
      }

      /**
       * to next station
       */
      private void toNextStation () {

            synchronized(mHowToPass) {

                  int size = mHowToPass.size();

                  if(size <= 0) {
                        return;
                  }

                  int index = mPassBy.getAndAdd(1);
                  if(index < size) {

                        Command command = mHowToPass.get(index);
                        doCommand(command);
                  } else {

                        mPassBy.getAndAdd(-1);
                  }
            }
      }

      /**
       * 推荐所有任务执行完成后,清除所有任务(因为可能存在内存泄漏,在所有任务完成后放弃对任务的引用,释放资源), 如果任务没有完成调用该方法,任务也会清除掉,需要用户自己保证所有任务已经执行完成,在调用该方法
       */
      public void clearRunnable () {

            synchronized(mHowToPass) {
                  mHowToPass.clear();
                  mPassBy.set(0);
            }
      }

      /**
       * @param command use command to run runnable
       */
      private void doCommand (Command command) {

            /* run runnable on current thread, current thread depends on before command run on which */

            if(command.command == COMMAND_GO) {

                  if(threadCurrent == THREAD_EXECUTOR) {

                        command.command = COMMAND_TO_UNDER;
                  } else if(threadCurrent == THREAD_MAIN) {

                        command.command = COMMAND_TO_MAIN;
                  } else {

                        Runnable runnable = command.getRunnable();
                        runnable.run();
                        toNextStation();
                        return;
                  }
            }

            /* run runnable on threadPool */

            if(command.command == COMMAND_TO_UNDER) {

                  /* not in pool change to pool */

                  if(mExecutorRunnable == null) {
                        mExecutorRunnable = new ExecutorRunnable();
                  }

                  Runnable runnable = command.getRunnable();
                  mExecutorRunnable.setRunnable(runnable);
                  PoolThreadExecutor.execute( mExecutorRunnable );
                  threadCurrent = THREAD_EXECUTOR;
                  return;
            }

            /* run runnable on MainThread */

            if(command.command == COMMAND_TO_MAIN) {

                  /* not on main, use messenger change to MainThread */

                  if(mBusMessageManager == null) {
                        mBusMessageManager = new BusMessenger();
                  }

                  BusMessenger messenger = mBusMessageManager;
                  Runnable runnable = command.getRunnable();
                  messenger.runOnMain(runnable);
                  threadCurrent = THREAD_MAIN;
                  return;
            }

            /* bus take a rest */

            if(command.command == COMMAND_TAKE_REST) {

                  runState = RUN_STATE_RESTING;

                  /* did'nt toNextStation(), wait util stopRest() called */

                  return;
            }

            /* bus take a while, then go on */

            if(command.command == COMMAND_TAKE_REST_AWHILE) {

                  /* just take a while, when time up toNextStation(), or stopRest() called toNextStation() */

                  runState = RUN_STATE_RESTING_AWHILE;
                  command.getRunnable().run();

                  //return;
            }
      }

      //============================ flow action ============================

      /**
       * run runnable on current thread; if call {@link #toUnder(Runnable)} current thread will be
       * {@link PoolThreadExecutor} thread; if call {@link
       * #toMain(Runnable)} current thread will be main thread;
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public ObjectBus go (@NonNull Runnable runnable) {

            mHowToPass.add(new Command(COMMAND_GO, runnable));
            return this;
      }

      //============================ 后台执行 ============================

      /**
       * run runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public ObjectBus toUnder (@NonNull Runnable runnable) {

            mHowToPass.add(new Command(COMMAND_TO_UNDER, runnable));
            return this;
      }

      //============================ mCallable 执行 ============================

      /**
       * call callable on current thread,and save result
       *
       * @param callable to call
       * @param key use this key to save result
       * @param <T> result type
       * @param <C> callable
       *
       * @return self
       */
      public <T, C extends Callable<T>> ObjectBus go (@NonNull C callable, @NonNull String key) {

            mHowToPass.add(new Command(COMMAND_GO, new CallableSelfCallRunnable<>(callable, key)));
            return this;
      }

      /**
       * call callableList on current thread,and save result
       *
       * @param callableList to call
       * @param key use this key to save result
       * @param <T> result type
       * @param <C> callable
       *
       * @return self
       */
      public <T, C extends Callable<T>> ObjectBus go (
          @NonNull List<C> callableList, @NonNull String key) {

            mHowToPass.add(
                new Command(COMMAND_GO, new CallableListSelfCallRunnable<>(callableList, key)));
            return this;
      }

      /**
       * to do mCallable on BackThread and save value
       *
       * @param callable need run
       * @param key key for save
       * @param <T> result type
       *
       * @return self
       */
      public <T, C extends Callable<T>> ObjectBus toUnder (@NonNull C callable, String key) {

            mHowToPass.add(
                new Command(
                    COMMAND_CALLABLE,
                    new CallableRunnable<>(callable, key)
                )
            );
            return this;
      }

      //============================ 并发多任务后台执行 ============================

      /**
       * run list of runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnableList task to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toUnder (@NonNull List<T> runnableList) {

            mHowToPass.add(new Command(
                               COMMAND_MULTI_RUNNABLE,
                               new ListRunnable<>(runnableList)
                           )
            );
            return this;
      }

      /**
       * to do mCallable on BackThread and save every result value as list
       *
       * @param callableList need run
       * @param key key for save
       * @param <T> result type
       *
       * @return self
       */
      public <T, C extends Callable<T>> ObjectBus toUnder (
          @NonNull List<C> callableList,
          String key) {

            mHowToPass.add(new Command(
                               COMMAND_MULTI_CALLABLE,
                               new ConcurrentRunnable<>(callableList, key)
                           )
            );
            return this;
      }

      //============================ 主线程任务 ============================

      /**
       * run runnable on main thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public ObjectBus toMain (@NonNull Runnable runnable) {

            mHowToPass.add(new Command(COMMAND_TO_MAIN, runnable));
            return this;
      }

      //============================ 额外任务 ============================

      /**
       * @param action this will call after last runnable, before this runnable run,could do some
       * Initialize action to runnable
       * @param runnable runnable
       * @param <T> type of runnable
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus go (
          OnBeforeRunAction<T> action,
          @NonNull T runnable) {

            return go(action, runnable, null);
      }

      /**
       * @param runnable runnable runnable
       * @param afterRunAction this will call after runnable.run
       * @param <T> type of runnable
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus go (
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction) {

            return go(null, runnable, afterRunAction);
      }

      /**
       * @param initializeAction call after last runnable, before this runnable run
       * @param runnable runnable
       * @param afterRunAction this will call after runnable.run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus go (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction) {

            return go(initializeAction, runnable, afterRunAction, null);
      }

      /**
       * @param initializeAction call after last runnable, before this runnable run
       * @param runnable runnable
       * @param afterRunAction this will call after runnable.run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus go (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction,
          OnRunExceptionHandler handler) {

            mHowToPass.add(new Command(
                               COMMAND_GO,
                               new ExtraActionRunnable(initializeAction, runnable, afterRunAction, handler)
                           )
            );
            return this;
      }

      /**
       * run runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toUnder (
          OnBeforeRunAction<T> beforeRunAction,
          @NonNull T runnable) {

            return toUnder(beforeRunAction, runnable, null);
      }

      /**
       * run runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toUnder (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction) {

            return toUnder(initializeAction, runnable, afterRunAction, null);
      }

      /**
       * run runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toUnder (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction,
          OnRunExceptionHandler<T> handler) {

            mHowToPass.add(new Command(
                               COMMAND_TO_UNDER,
                               new ExtraActionRunnable<>(
                                   initializeAction,
                                   runnable,
                                   afterRunAction,
                                   handler
                               )
                           )
            );
            return this;
      }

      /**
       * run runnable on {@link PoolThreadExecutor} thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toUnder (
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction ) {

            return toUnder( null, runnable, afterRunAction );
      }

      /**
       * run runnable on main thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toMain (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable) {

            return toMain(initializeAction, runnable, null);
      }

      /**
       * run runnable on main thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toMain (
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction) {

            return toMain(null, runnable, afterRunAction);
      }

      /**
       * run runnable on main thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toMain (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction) {

            return toMain(initializeAction, runnable, afterRunAction, null);
      }

      /**
       * run runnable on main thread
       *
       * @param runnable runnable to run
       *
       * @return self
       */
      public <T extends Runnable> ObjectBus toMain (
          OnBeforeRunAction<T> initializeAction,
          @NonNull T runnable,
          OnRunFinishAction<T> afterRunAction,
          OnRunExceptionHandler<T> handler) {

            mHowToPass.add(new Command(
                               COMMAND_TO_MAIN,
                               new ExtraActionRunnable<>(
                                   initializeAction,
                                   runnable,
                                   afterRunAction,
                                   handler
                               )
                           )
            );
            return this;
      }

      //============================ 向外发送消息 ============================

      /**
       * send message on current thread
       *
       * @param what message what
       * @param listener receiver
       *
       * @return self
       */
      public ObjectBus send (int what, OnMessageReceiveListener listener) {

            return sendDelayed(what, 0, null, listener);
      }

      /**
       * send message on current thread
       *
       * @param what message what
       * @param extra extra msg
       * @param listener receiver
       *
       * @return self
       */
      public ObjectBus send (int what, Object extra, OnMessageReceiveListener listener) {

            return sendDelayed(what, 0, extra, listener);
      }

      /**
       * send message on current thread
       *
       * @param what message what
       * @param listener receiver
       *
       * @return self
       */
      public ObjectBus sendDelayed (int what, int delayed, OnMessageReceiveListener listener) {

            return sendDelayed(what, delayed, null, listener);
      }

      /**
       * send message on current thread
       *
       * @param what message what
       * @param extra extra msg
       * @param listener receiver
       *
       * @return self
       */
      @SuppressWarnings("WeakerAccess")
      public ObjectBus sendDelayed (
          int what, int delayed, Object extra, OnMessageReceiveListener listener) {

            mHowToPass
                .add(new Command(COMMAND_SEND, new SendRunnable(what, delayed, extra, listener)));
            return this;
      }

      //============================ 暂停/恢复 ============================

      /**
       * take a rest ,util {@link #stopRest()} called
       */
      public ObjectBus takeRest () {

            if(mRestRunnable == null) {
                  mRestRunnable = new RestRunnable();
            }
            mHowToPass.add(new Command(COMMAND_TAKE_REST, mRestRunnable));
            return this;
      }

      /**
       * take a rest for a while ,util time up, or {@link #stopRest()} called
       *
       * @param millisecond time to rest
       *
       * @return self
       */
      public ObjectBus takeRest (int millisecond) {

            mHowToPass
                .add(new Command(COMMAND_TAKE_REST_AWHILE, new TakeWhileRunnable(millisecond)));
            return this;
      }

      /**
       * when this called , if bus is resting , bus will go on
       */
      public synchronized void stopRest () {

            if(runState == RUN_STATE_RESTING || runState == RUN_STATE_RESTING_AWHILE) {

                  if(mBusMessageManager == null) {
                        mBusMessageManager = new BusMessenger();
                  }
                  mBusMessageManager.notifyBusStopRest();
            }
      }

      public boolean isResting () {

            return (runState == RUN_STATE_RESTING || runState == RUN_STATE_RESTING_AWHILE);
      }

      public boolean isRestingAWhile () {

            return (runState == RUN_STATE_RESTING_AWHILE);
      }

      //============================ 开始执行任务 ============================

      /**
       * start run bus
       */
      public void run () {

            final String mainThreadName = "main";

            /* 标记当前线程 */
            if(mainThreadName.equals(Thread.currentThread().getName())) {
                  threadCurrent = THREAD_MAIN;
            } else {
                  threadCurrent = THREAD_EXECUTOR;
            }

            /* 标记当前运行状态 */
            runState = RUN_STATE_RUNNING;

            /* 开始 */
            toNextStation();
      }

      //============================ 添加/删除乘客操作 ============================

      /**
       * lazy init
       *
       * @return {@link #mExtras}
       */
      private ArrayMap<String, Object> getExtras () {

            if(mExtras == null) {
                  mExtras = new ArrayMap<>();
            }
            return mExtras;
      }

      /**
       * take extra to bus,could use key to get
       *
       * @param extra extra to bus
       * @param key key
       */
      public void take (Object extra, String key) {

            getExtras().put(key, extra);
      }

      /**
       * get the extra
       *
       * @param key key
       *
       * @return extra
       */
      @Nullable
      public Object get (String key) {

            return getExtras().get(key);
      }

      /**
       * get and remove extra
       *
       * @param key key
       *
       * @return extra
       */
      @Nullable
      public Object getOff (String key) {

            return getExtras().remove(key);
      }

      /**
       * 清除所有乘客
       */
      public void clearPassenger () {

            if(mExtras != null) {
                  mExtras.clear();
            }
      }

      //============================ bus message register ============================

      private SparseArray<Runnable> mMessageReceiveRunnable;

      /**
       * when {@link Messengers#send(int, OnMessageReceiveListener)} to bus ,bus will do the
       * runnable
       *
       * @param what msg what, nofity when what is even number (偶数), bus will run the runnable on
       * the threadPool, else will run the runnable on MainThread
       * @param runnable what to do when receive msg
       *
       * @return self
       */
      public ObjectBus registerMessage (int what, Runnable runnable) {

            if(mMessageReceiveRunnable == null) {
                  mMessageReceiveRunnable = new SparseArray<>();
            }
            mMessageReceiveRunnable.put(what, runnable);
            return this;
      }

      /**
       * unRegister a message
       */
      public void unRegisterMessage (int what) {

            if(mMessageReceiveRunnable == null) {
                  mMessageReceiveRunnable = new SparseArray<>();
            }
            mMessageReceiveRunnable.delete(what);
            Messengers.remove(what, this);
      }

      /**
       * 清除所有注册的消息的行动
       */
      public void clearMessageReceiveRunnable () {

            if(mMessageReceiveRunnable != null) {
                  mMessageReceiveRunnable.clear();
            }
      }

      @Override
      public void onReceive (int what, Object extra) {

      }

      @Override
      public void onReceive (int what) {

            if(mMessageReceiveRunnable == null) {
                  return;
            }

            /* when bus receive a message run the runnable register to what  */

            Runnable runnable = mMessageReceiveRunnable.get(what);

            if(runnable == null) {
                  return;
            }

            final int judge = 2;

            if(what % judge == 0) {

                  /* run on thread pool */

                  PoolThreadExecutor.execute( runnable );
            } else {

                  /* run on MainThread */

                  runnable.run();
            }
      }

      //============================ command for Bus run runnable ============================

      /**
       * record how to run runnable
       */
      private class Command {

            /**
             * one of {@link #COMMAND_GO} {@link #COMMAND_SEND} {@link #COMMAND_TAKE_REST} {@link
             * #COMMAND_TO_MAIN} {@link #COMMAND_TO_UNDER} {@link #COMMAND_TAKE_REST_AWHILE}
             * <p>
             * bus use this command to decide what to do
             */
            private int      command;
            /**
             * the runnable the user want to do,bus will run runnable with command
             */
            private Runnable mRunnable;

            Command (int command, @NonNull Runnable runnable) {

                  this.command = command;
                  mRunnable = runnable;
            }

            Runnable getRunnable () {

                  return mRunnable;
            }
      }

      //============================ executor runnable  ============================

      /**
       * use take task to do in the {@link PoolThreadExecutor}
       * <p>
       * {@link #mExecutorRunnable}
       */
      private class ExecutorRunnable implements Runnable {

            private Runnable mRunnable;

            void setRunnable (Runnable runnable) {

                  mRunnable = runnable;
            }

            @Override
            public void run () {

                  /* this will run on  PoolThreadExecutor */

                  Runnable runnable = mRunnable;
                  if(runnable != null) {
                        runnable.run();
                  }
                  toNextStation();
            }
      }

      //============================ communicate ============================

      /**
       * bus use this to communicate with {@link Messengers}
       */
      private class BusMessenger implements OnMessageReceiveListener {

            /**
             * when receive this msg, take bus to Main Thread to run
             */
            private static final int WHAT_MAIN = 3;
            /**
             * used with {@link #WHAT_MAIN},when receive {@link #WHAT_MAIN},run this runnable on
             * mainThread
             */
            private Runnable mRunnable;

            void setRunnable (Runnable runnable) {

                  mRunnable = runnable;
            }

            /**
             * run the {@link #mRunnable}at main
             */
            void runOnMain (Runnable runnable) {

                  setRunnable(runnable);
                  Messengers.send(WHAT_MAIN, this);
            }

            /**
             * when receive this msg take bus to last Command run Thread to go on
             */
            private static final int WHAT_STOP_REST    = 4;
            /**
             * when receive this msg means bus rest time up, need go on
             */
            private static final int WHAT_REST_TIME_UP = 6;

            /**
             * notify bus to next station if bus is RESTING, called from {@link #stopRest()}, when
             * this called bus must in resting
             */
            void notifyBusStopRest () {

                  runState = RUN_STATE_RUNNING;
                  Messengers.send(WHAT_STOP_REST, this);
            }

            /**
             * notify bus to next station when bus is RESTING
             */
            void notifyBusStopRestAfter (int millisecond) {

                  Messengers.send(WHAT_REST_TIME_UP, millisecond, this);
            }

            @Override
            public void onReceive (int what, Object extra) {

            }

            @Override
            public void onReceive (int what) {

                  /* run runnable on main */

                  if(what == WHAT_MAIN) {
                        if(mRunnable != null) {
                              mRunnable.run();
                        }
                        toNextStation();
                        return;
                  }

                  /* stop bus rest */

                  if(what == WHAT_STOP_REST) {

                        /* this is running at Messengers#Thread not in threadPool or MainThread*/
                        /* if before bus rest it run on pool thread ,goto pool; if on main thread ,go to main */
                        toNextStation();
                        return;
                  }

                  /* time up to notify bus to go on */

                  if(what == WHAT_REST_TIME_UP) {

                        if(runState == RUN_STATE_RESTING_AWHILE) {
                              notifyBusStopRest();
                        }
                  }
            }
      }

      //============================ Send runnable ============================

      /**
       * used to send Message with {@link #COMMAND_SEND} {@link #sendDelayed(int, int, Object,
       * OnMessageReceiveListener)}
       *
       * @see Messengers#send(int, int, Object, OnMessageReceiveListener)
       */
      private class SendRunnable implements Runnable {

            private int                                     what;
            private int                                     delayed;
            private Object                                  extra;
            private WeakReference<OnMessageReceiveListener> mListenerWeakReference;

            SendRunnable (
                int what, int delayed, Object extra,
                @NonNull OnMessageReceiveListener receiveListener) {

                  this.what = what;
                  this.extra = extra;
                  this.delayed = delayed;
                  this.mListenerWeakReference = new WeakReference<>(receiveListener);
            }

            @Override
            public void run () {

                  /* send message */

                  OnMessageReceiveListener who = mListenerWeakReference.get();

                  if(who == null) {
                        return;
                  }

                  if(extra == null) {

                        Messengers.send(what, delayed, who);
                  } else {

                        Messengers.send(what, delayed, extra, who);
                  }
            }
      }

      //============================ rest Runnable ============================

      /**
       * used with {@link #COMMAND_TAKE_REST}, because {@link Command#Command(int, Runnable)} not
       * null,so create a do Nothing runnable
       */
      private class RestRunnable implements Runnable {

            @Override
            public void run () {

                  /* take a rest, do nothing */

            }
      }

      //============================ take a while Runnable ============================

      /**
       * used with {@link #COMMAND_TAKE_REST_AWHILE} , send a delayed message, to call {@link
       * #stopRest()}
       */
      private class TakeWhileRunnable implements Runnable {

            private int delayed;

            TakeWhileRunnable (int delayed) {

                  this.delayed = delayed;
            }

            @Override
            public void run () {

                  if(mBusMessageManager == null) {
                        mBusMessageManager = new BusMessenger();
                  }
                  mBusMessageManager.notifyBusStopRestAfter(delayed);
            }
      }

      //============================ list runnable ============================

      private class ListRunnable<T extends Runnable> implements Runnable {

            private List<T> mRunnableList;

            public ListRunnable (List<T> runnableList) {

                  mRunnableList = runnableList;
            }

            @Override
            public void run () {

                  PoolThreadExecutor.execute( mRunnableList );
            }
      }

      //============================ mCallable Runnable ============================

      private class CallableRunnable<T, C extends Callable<T>> implements Runnable {

            private C      mCallable;
            private String key;

            public CallableRunnable (C callable, String key) {

                  this.key = key;
                  mCallable = callable;
            }

            @Override
            public void run () {

                  T t = PoolThreadExecutor.submitAndGet( mCallable );
                  take(t, key);
            }
      }

      private class ConcurrentRunnable<T, C extends Callable<T>> implements Runnable {

            List<C> mCallableList;
            private String key;

            ConcurrentRunnable (List<C> callableList, String key) {

                  mCallableList = callableList;
                  this.key = key;
            }

            @Override
            public void run () {

                  List<T> list = PoolThreadExecutor.submitAndGet( mCallableList );
                  take(list, key);
            }
      }

      //============================ callable self call runnable ============================

      private class CallableSelfCallRunnable<T, C extends Callable<T>> implements Runnable {

            private C      mCallable;
            private String key;

            public CallableSelfCallRunnable (C callable, String key) {

                  mCallable = callable;
                  this.key = key;
            }

            @Override
            public void run () {

                  try {
                        T call = mCallable.call();
                        if(key != null) {
                              take(call, key);
                        }
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }
      }

      private class CallableListSelfCallRunnable<T, C extends Callable<T>> implements Runnable {

            private List<C> mCallableList;
            private String  key;

            public CallableListSelfCallRunnable (List<C> callableList, String key) {

                  mCallableList = callableList;
                  this.key = key;
            }

            @Override
            public void run () {

                  int size = mCallableList.size();
                  List<T> result = new ArrayList<>(size);

                  for(int i = 0; i < size; i++) {
                        C c = mCallableList.get(i);
                        try {
                              T call = c.call();
                              result.add(call);
                        } catch(Exception e) {
                              e.printStackTrace();
                        }
                  }

                  if(key != null) {
                        take(result, key);
                  }
            }
      }

      //============================ do extra action with runnable ============================

      /**
       * use with {@link #go(OnBeforeRunAction, Runnable, OnRunFinishAction,
       * OnRunExceptionHandler)}
       */
      private class ExtraActionRunnable<T extends Runnable> implements Runnable {

            private OnBeforeRunAction<T>     mOnBeforeRunAction;
            private T                        mRunnable;
            private OnRunFinishAction<T>     mOnRunnableFinishAction;
            private OnRunExceptionHandler<T> mOnRunExceptionHandler;

            public ExtraActionRunnable (
                OnBeforeRunAction<T> onBeforeRunAction,
                T runnable,
                OnRunFinishAction<T> onRunFinishAction,
                OnRunExceptionHandler<T> onRunExceptionHandler) {

                  mOnBeforeRunAction = onBeforeRunAction;
                  mRunnable = runnable;
                  mOnRunnableFinishAction = onRunFinishAction;
                  mOnRunExceptionHandler = onRunExceptionHandler;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void run () {

                  T runnable = mRunnable;

                  try {
                        if(mOnBeforeRunAction != null) {
                              mOnBeforeRunAction.onBeforeRun(runnable);
                        }

                        runnable.run();

                        if(mOnRunnableFinishAction != null) {
                              mOnRunnableFinishAction.onRunFinished(ObjectBus.this, runnable);
                        }
                  } catch(Exception e) {

                        e.printStackTrace();
                        if(mOnRunExceptionHandler != null) {
                              mOnRunExceptionHandler.onException(runnable, e);
                        }
                  }
            }
      }
}
