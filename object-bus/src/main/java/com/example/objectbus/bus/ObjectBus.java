package com.example.objectbus.bus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.SparseArray;

import com.example.objectbus.executor.AppExecutor;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;

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

    private static final String TAG = "ObjectBus";

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
     * current thread state
     */
    private static final int THREAD_MAIN     = 0X1FFFF;
    private static final int THREAD_EXECUTOR = 0X2FFFF;
    private int threadCurrent;

    /**
     * record  run State is resting
     */
    private static final int RUN_STATE_RUNNING        = 0X10EE;
    private static final int RUN_STATE_RESTING        = 0X11EE;
    private static final int RUN_STATE_RESTING_AWHILE = 0X100EE;
    private int runState;

    /**
     * how many station pass By
     */
    private AtomicInteger mPassBy = new AtomicInteger();

    /**
     * how to pass every station
     */
    private ArrayList< Command > mHowToPass = new ArrayList<>();

    /**
     * used do runnable at {@link com.example.objectbus.executor.AppExecutor},used by
     * {@link #COMMAND_TO_UNDER}
     */
    private ExecutorRunnable mExecutorRunnable;

    /**
     * used do runnable at MainThread with {@link #COMMAND_TO_MAIN},
     * {@link #stopRest()}:to stop bus rest
     * {@link TakeWhileRunnable#run()}: to top bus rest
     */
    private BusMessageListener mBusMessageManger;

    /**
     * take customs to bus,{@link #take(Object, String)},{@link #get(String)},{@link #getOff(String)}
     */
    private ArrayMap< String, Object > mExtras;

    /**
     * do nothing just take a rest ,{@link #COMMAND_TAKE_REST}
     */
    private RestRunnable mRestRunnable;


    public ObjectBus() {

    }

    //============================ core ============================


    /**
     * @return did how many task
     */
    public int getPassBy() {

        return mPassBy.get();
    }


    /**
     * to next station,
     */
    private void toNextStation() {

        int index = mPassBy.getAndAdd(1);
        if (index < mHowToPass.size()) {

            Command command = mHowToPass.get(index);
            doCommand(command);
        } else {

            // TODO: 2018-05-05 添加更多选项

            mHowToPass.clear();
            mPassBy.set(0);
        }
    }


    /**
     * @param command use command to run runnable
     */
    private void doCommand(Command command) {

        /* run runnable on current thread, current thread depends on before command run on which */

        if (command.command == COMMAND_GO) {

            Runnable runnable = command.getRunnable();
            runnable = wrapperRunnableIfHaveOnBusRunningListener(runnable);
            runnable.run();

            toNextStation();
            return;
        }

        /* run runnable on threadPool */

        if (command.command == COMMAND_TO_UNDER) {

            if (threadCurrent != THREAD_EXECUTOR) {

                /* not in pool change to pool */

                if (mExecutorRunnable == null) {
                    mExecutorRunnable = new ExecutorRunnable();
                }

                Runnable runnable = command.getRunnable();
                runnable = wrapperRunnableIfHaveOnBusRunningListener(runnable);
                mExecutorRunnable.setRunnable(runnable);
                AppExecutor.execute(mExecutorRunnable);
                threadCurrent = THREAD_EXECUTOR;
            } else {

                /* still in pool, run runnable */

                Runnable runnable = command.getRunnable();
                runnable = wrapperRunnableIfHaveOnBusRunningListener(runnable);
                runnable.run();
                toNextStation();
            }
            return;
        }

        /* run runnable on MainThread */

        if (command.command == COMMAND_TO_MAIN) {

            if (threadCurrent != THREAD_MAIN) {

                /* not on main, use messenger change to MainThread */

                if (mBusMessageManger == null) {
                    mBusMessageManger = new BusMessageListener();
                }

                BusMessageListener messenger = mBusMessageManger;
                Runnable runnable = command.getRunnable();
                runnable = wrapperRunnableIfHaveOnBusRunningListener(runnable);
                messenger.setRunnable(runnable);
                messenger.runOnMain();
                threadCurrent = THREAD_MAIN;
            } else {

                /* still on Main, runRunnable */

                command.getRunnable().run();
                toNextStation();
            }

            return;
        }

        /* bus take a rest */

        if (command.command == COMMAND_TAKE_REST) {

            runState = RUN_STATE_RESTING;

            /* did'nt toNextStation(), wait util stopRest() called */

            return;
        }

        /* bus take a while, then go on */

        if (command.command == COMMAND_TAKE_REST_AWHILE) {

            /* just take a while, when time up toNextStation(), or stopRest() called toNextStation() */

            runState = RUN_STATE_RESTING_AWHILE;
            command.getRunnable().run();

            //return;
        }

    }

    //============================ flow action ============================


    /**
     * run runnable on current thread;
     * if call {@link #toUnder(Runnable)} current thread will be
     * {@link com.example.objectbus.executor.AppExecutor} thread;
     * if call {@link #toMain(Runnable)} current thread will be main thread;
     *
     * @param runnable runnable to run
     * @return self
     */
    public ObjectBus go(@NonNull Runnable runnable) {

        mHowToPass.add(new Command(COMMAND_GO, runnable));
        return this;
    }

    //============================ 后台执行 ============================


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public ObjectBus toUnder(@NonNull Runnable runnable) {

        mHowToPass.add(new Command(COMMAND_TO_UNDER, runnable));
        return this;
    }

    //============================ callable 执行 ============================


    /**
     * to do callable on BackThread and save value
     *
     * @param callable need run
     * @param key      key for save
     * @param <T>      result type
     * @return self
     */
    public < T > ObjectBus toUnder(@NonNull Callable< T > callable, String key) {

        mHowToPass.add(new Command(
                COMMAND_CALLABLE,
                new CallableRunnable<>(callable, key))
        );
        return this;
    }

    //============================ 并发多任务后台执行 ============================


    /**
     * run list of runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param runnableList task to run
     * @return self
     */
    public ObjectBus toUnder(@NonNull List< Runnable > runnableList) {

        mHowToPass.add(new Command(
                COMMAND_MULTI_RUNNABLE,
                new ListRunnable(runnableList))
        );
        return this;
    }


    /**
     * to do callable on BackThread and save value
     *
     * @param callableList need run
     * @param key          key for save
     * @param <T>          result type
     * @return self
     */
    public < T > ObjectBus toUnder(@NonNull List< Callable< T > > callableList, String key) {

        mHowToPass.add(new Command(
                COMMAND_MULTI_CALLABLE,
                new ConcurrentRunnable<>(callableList, key))
        );
        return this;
    }

    //============================ 主线程任务 ============================


    /**
     * run runnable on main thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public ObjectBus toMain(@NonNull Runnable runnable) {

        mHowToPass.add(new Command(COMMAND_TO_MAIN, runnable));
        return this;
    }

    //============================ 额外任务 ============================


    /**
     * @param action   this will call after last runnable, before this runnable run,could do some
     *                 Initialize action to runnable
     * @param runnable runnable
     * @param <T>      type of runnable
     * @return self
     */
    public < T extends Runnable > ObjectBus go(
            OnBeforeRunAction< T > action,
            @NonNull T runnable) {

        return go(action, runnable, null);
    }


    /**
     * @param runnable       runnable runnable
     * @param afterRunAction this will call after runnable.run
     * @param <T>            type of runnable
     * @return self
     */
    public < T extends Runnable > ObjectBus go(
            @NonNull T runnable, OnAfterRunAction< T > afterRunAction) {

        return go(null, runnable, afterRunAction);
    }


    /**
     * @param initializeAction call after last runnable, before this runnable run
     * @param runnable         runnable
     * @param afterRunAction   this will call after runnable.run
     * @return self
     */
    public < T extends Runnable > ObjectBus go(
            OnBeforeRunAction< T > initializeAction,
            @NonNull T runnable,
            OnAfterRunAction< T > afterRunAction) {

        mHowToPass.add(new Command(
                COMMAND_GO,
                new ExtraActionRunnable(initializeAction, runnable, afterRunAction))
        );
        return this;
    }


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toUnder(
            OnBeforeRunAction< T > beforeRunAction,
            @NonNull T runnable) {

        return toUnder(beforeRunAction, runnable, null);
    }


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toUnder(
            @NonNull T runnable,
            OnAfterRunAction< T > afterRunAction) {

        return toUnder(null, runnable, afterRunAction);
    }


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toUnder(
            OnBeforeRunAction< T > initializeAction,
            @NonNull T runnable,
            OnAfterRunAction< T > afterRunAction) {

        mHowToPass.add(new Command(
                COMMAND_TO_UNDER,
                new ExtraActionRunnable(
                        initializeAction,
                        runnable,
                        afterRunAction))
        );
        return this;
    }


    /**
     * run runnable on main thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toMain(
            OnBeforeRunAction< T > initializeAction,
            @NonNull T runnable) {

        return toMain(initializeAction, runnable, null);
    }


    /**
     * run runnable on main thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toMain(
            @NonNull T runnable,
            OnAfterRunAction< T > afterRunAction) {

        return toMain(null, runnable, afterRunAction);
    }


    /**
     * run runnable on main thread
     *
     * @param runnable runnable to run
     * @return self
     */
    public < T extends Runnable > ObjectBus toMain(
            OnBeforeRunAction< T > initializeAction,
            @NonNull T runnable,
            OnAfterRunAction< T > afterRunAction) {

        mHowToPass.add(new Command(
                COMMAND_TO_MAIN,
                new ExtraActionRunnable(
                        initializeAction,
                        runnable,
                        afterRunAction))
        );
        return this;
    }

    //============================ 向外发送消息 ============================


    /**
     * send message on current thread
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus send(int what, OnMessageReceiveListener listener) {

        return sendDelayed(what, 0, null, listener);
    }


    /**
     * send message on current thread
     *
     * @param what     message what
     * @param extra    extra msg
     * @param listener receiver
     * @return self
     */
    public ObjectBus send(int what, Object extra, OnMessageReceiveListener listener) {

        return sendDelayed(what, 0, extra, listener);
    }


    /**
     * send message on current thread
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus sendDelayed(int what, int delayed, OnMessageReceiveListener listener) {

        return sendDelayed(what, delayed, null, listener);
    }


    /**
     * send message on current thread
     *
     * @param what     message what
     * @param extra    extra msg
     * @param listener receiver
     * @return self
     */
    @SuppressWarnings("WeakerAccess")
    public ObjectBus sendDelayed(int what, int delayed, Object extra, OnMessageReceiveListener listener) {

        mHowToPass.add(new Command(COMMAND_SEND, new SendRunnable(what, delayed, extra, listener)));
        return this;
    }

    //============================ 暂停/恢复 ============================


    /**
     * take a rest ,util {@link #stopRest()} called
     */
    public ObjectBus takeRest() {

        if (mRestRunnable == null) {
            mRestRunnable = new RestRunnable();
        }
        mHowToPass.add(new Command(COMMAND_TAKE_REST, mRestRunnable));
        return this;
    }


    /**
     * take a rest for a while ,util time up, or {@link #stopRest()} called
     *
     * @param millisecond time to rest
     * @return self
     */
    public ObjectBus takeRest(int millisecond) {

        mHowToPass.add(new Command(COMMAND_TAKE_REST_AWHILE, new TakeWhileRunnable(millisecond)));
        return this;
    }


    /**
     * when this called , if bus is resting , bus will go on
     */
    public void stopRest() {

        if (runState == RUN_STATE_RESTING || runState == RUN_STATE_RESTING_AWHILE) {

            mBusMessageManger.notifyBusStopRest();
        }
    }

    //============================ 开始执行任务 ============================


    /**
     * start run bus
     */
    public void run() {

        runState = RUN_STATE_RUNNING;
        toNextStation();
    }

    //============================ 添加/删除乘客操作 ============================


    private ArrayMap< String, Object > getExtras() {

        if (mExtras == null) {
            mExtras = new ArrayMap<>();
        }
        return mExtras;
    }


    /**
     * take extra to bus,could use key to get
     *
     * @param extra extra to bus
     * @param key   key
     */
    public void take(Object extra, String key) {

        getExtras().put(key, extra);
    }


    /**
     * get the extra
     *
     * @param key key
     * @return extra
     */
    @Nullable
    public Object get(String key) {

        return getExtras().get(key);
    }


    /**
     * get and remove extra
     *
     * @param key key
     * @return extra
     */
    @Nullable
    public Object getOff(String key) {

        return getExtras().remove(key);
    }

    //============================ bus listener ============================

    private OnBusRunningListener       mOnBusRunningListener;
    private BusRunningListenerRunnable mBusRunningListenerRunnable;


    public void setOnBusRunningListener(OnBusRunningListener onBusRunningListener) {

        mOnBusRunningListener = onBusRunningListener;
    }


    public OnBusRunningListener getOnBusRunningListener() {

        return mOnBusRunningListener;
    }


    /**
     * bus run runnable listener
     */
    public interface OnBusRunningListener {

        /**
         * before runnable start call
         *
         * @param bus      bus
         * @param runnable to run
         */
        void onRunnableStart(ObjectBus bus, Runnable runnable);

        /**
         * after runnable finish
         *
         * @param bus      bus
         * @param runnable finished
         */
        void onRunnableFinished(ObjectBus bus, Runnable runnable);

        /**
         * called when runnable exception
         *
         * @param bus      bus
         * @param runnable runnable get Exception
         * @param e        Exception
         */
        void onRunnableException(ObjectBus bus, Runnable runnable, Exception e);

    }


    /**
     * if have a {@link #mOnBusRunningListener} use {@link BusRunningListenerRunnable} wrapper {@code
     * runnable}
     *
     * @param runnable user's runnable
     * @return {@link BusRunningListenerRunnable} or runnable self
     */
    private Runnable wrapperRunnableIfHaveOnBusRunningListener(@NonNull Runnable runnable) {

        if (mOnBusRunningListener != null) {

            if (mBusRunningListenerRunnable == null) {
                mBusRunningListenerRunnable = new BusRunningListenerRunnable(mOnBusRunningListener);
            } else {

                mBusRunningListenerRunnable.changeListener(mOnBusRunningListener);
            }

            mBusRunningListenerRunnable.setRunnable(runnable);
            return mBusRunningListenerRunnable;
        }

        return runnable;
    }

    //============================ bus message register ============================

    private SparseArray< Runnable > mMessageReceiveRunnable = new SparseArray<>();


    /**
     * when {@link Messengers#send(int, OnMessageReceiveListener)} to bus ,bus will do the runnable
     *
     * @param what     msg what, nofity when what is even number (偶数), bus will run the runnable on the
     *                 threadPool, else will run the runnable on MainThread
     * @param runnable what to do when receive msg
     * @return self
     */
    public ObjectBus registerMessage(int what, Runnable runnable) {

        mMessageReceiveRunnable.put(what, runnable);
        return this;
    }


    /**
     * unRegister a message
     */
    public void unRegisterMessage(int what) {

        mMessageReceiveRunnable.delete(what);
    }


    @Override
    public void onReceive(int what) {

        /* when bus receive a message run the runnable register to what  */

        Runnable runnable = mMessageReceiveRunnable.get(what);

        if (runnable == null) {
            return;
        }

        runnable = wrapperRunnableNewIfHaveOnBusRunningListener(runnable);

        if (what % 2 == 0) {

            /* run on thread pool */

            AppExecutor.execute(runnable);
        } else {

            /* run on MainThread */

            mMessageReceiveRunnable.put(what, runnable);
            mBusMessageManger.runMessageReceiveRunnableOnMain(what);

        }
    }


    /**
     * if have a {@link #mOnBusRunningListener} use {@link BusRunningListenerRunnable} wrapper {@code
     * runnable},different from {@link #wrapperRunnableIfHaveOnBusRunningListener(Runnable)} this all use a
     * new {@link BusRunningListenerRunnable} to wrapper runnable, this is used for {@link #onReceive(int)}
     * to avoid multi thread safe question
     *
     * @param runnable user's runnable
     * @return {@link BusRunningListenerRunnable} or runnable self
     */
    private Runnable wrapperRunnableNewIfHaveOnBusRunningListener(Runnable runnable) {

        if (mOnBusRunningListener != null) {

            BusRunningListenerRunnable listenerRunnable = new BusRunningListenerRunnable
                    (mOnBusRunningListener);
            listenerRunnable.setRunnable(runnable);
            return listenerRunnable;
        }

        return runnable;
    }

    //============================ command for Bus run runnable ============================

    /**
     * record how to run runnable
     */
    private class Command {

        /**
         * one of {@link #COMMAND_GO} {@link #COMMAND_SEND} {@link #COMMAND_TAKE_REST}
         * {@link #COMMAND_TO_MAIN} {@link #COMMAND_TO_UNDER} {@link #COMMAND_TAKE_REST_AWHILE}
         *
         * bus use this command to decide what to do
         */
        private int      command;
        /**
         * the runnable if the user want to do,bus will run this with command
         */
        private Runnable mRunnable;


        Command(int command, @NonNull Runnable runnable) {

            this.command = command;
            mRunnable = runnable;
        }


        Runnable getRunnable() {

            return mRunnable;
        }
    }

    //============================ executor runnable  ============================

    /**
     * use take task to do in the {@link com.example.objectbus.executor.AppExecutor}
     *
     * {@link #mExecutorRunnable}
     */
    private class ExecutorRunnable implements Runnable {

        private Runnable mRunnable;


        void setRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        @Override
        public void run() {

            /* this will run on  AppExecutor */

            Runnable runnable = mRunnable;
            if (runnable != null) {
                runnable.run();
            }
            toNextStation();
        }
    }

    //============================ communicate ============================

    /**
     * bus use this to communicate with {@link Messengers}
     */
    private class BusMessageListener implements OnMessageReceiveListener {


        /**
         * when receive this msg, take bus to Main Thread to run
         */
        private static final int WHAT_MAIN = 3;
        /**
         * used with {@link #WHAT_MAIN},when receive {@link #WHAT_MAIN},run this runnable on mainThread
         */
        private Runnable mRunnable;


        void setRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        /**
         * run the {@link #mRunnable}at main
         */
        void runOnMain() {

            Messengers.send(WHAT_MAIN, this);
        }


        /**
         * when receive this msg take bus to last Command run Thread to go on
         */
        private static final int WHAT_STOP_REST    = 4;
        /**
         * when receive this msg means bus last command run on MainThread,to MainThread go on
         */
        private static final int WHAT_NEXT_AT_MAIN = 5;
        /**
         * when receive this msg means bus rest time up, need go on
         */
        private static final int WHAT_REST_TIME_UP = 6;


        /**
         * notify bus to next station if bus is RESTING, called from {@link #stopRest()},
         * when this called bus must in resting
         */
        void notifyBusStopRest() {

            runState = RUN_STATE_RUNNING;
            Messengers.send(WHAT_STOP_REST, this);
        }


        /**
         * notify bus to next station when bus is RESTING
         */
        void notifyBusStopRestAfter(int millisecond) {

            Messengers.send(WHAT_REST_TIME_UP, millisecond, this);
        }


        /**
         * call {@link #toNextStation()} on main thread
         */
        private void takeBusOnMainToNextStation() {

            Messengers.send(WHAT_NEXT_AT_MAIN, this);
        }


        private static final int WHAT_RUN_MESSAGE_RECEIVE_RUNNABLE_MAIN = 7;


        /**
         * to run message runnable at {@link #mMessageReceiveRunnable} on MainThread
         *
         * @param what msg what
         */
        void runMessageReceiveRunnableOnMain(int what) {

            Messengers.send(WHAT_RUN_MESSAGE_RECEIVE_RUNNABLE_MAIN, what, this);
        }


        @SuppressWarnings("UnnecessaryUnboxing")
        @Override
        public void onReceive(int what, Object extra) {

            if (what == WHAT_RUN_MESSAGE_RECEIVE_RUNNABLE_MAIN) {
                int index = ((Integer) extra).intValue();
                Runnable runnable = mMessageReceiveRunnable.get(index);
                if (runnable != null) {
                    runnable.run();
                }
            }
        }


        @Override
        public void onReceive(int what) {

            /* run runnable on main */

            if (what == WHAT_MAIN) {
                if (mRunnable != null) {
                    mRunnable.run();
                }
                toNextStation();
                return;
            }

            /* stop bus rest */

            if (what == WHAT_STOP_REST) {

                /* base on bus threadCurrent when him rest,to go on at that thread */

                /* this is running at Messengers#Thread not in threadPool or MainThread*/

                if (threadCurrent == THREAD_MAIN) {

                    takeBusOnMainToNextStation();
                } else {

                    AppExecutor.execute(ObjectBus.this::toNextStation);
                }

                return;
            }

            /* take bus to stop rest at main thread */

            if (what == WHAT_NEXT_AT_MAIN) {
                toNextStation();
                return;
            }

            /* time up to notify bus to go on */

            if (what == WHAT_REST_TIME_UP) {

                if (runState == RUN_STATE_RESTING_AWHILE) {
                    notifyBusStopRest();
                }
            }
        }
    }

    //============================ Send runnable ============================

    /**
     * used to send Message with {@link #COMMAND_SEND}
     * {@link #sendDelayed(int, int, Object, OnMessageReceiveListener)}
     *
     * @see Messengers#send(int, int, Object, OnMessageReceiveListener)
     */
    private class SendRunnable implements Runnable {

        private int                      what;
        private int                      delayed;
        private Object                   extra;
        private OnMessageReceiveListener receiveListener;


        SendRunnable(int what, int delayed, Object extra, @NonNull OnMessageReceiveListener receiveListener) {

            this.what = what;
            this.extra = extra;
            this.delayed = delayed;
            this.receiveListener = receiveListener;
        }


        @Override
        public void run() {

            /* send message */

            if (extra == null) {

                Messengers.send(what, delayed, receiveListener);
            } else {

                Messengers.send(what, delayed, extra, receiveListener);
            }
        }


        public int getWhat() {

            return what;
        }


        public int getDelayed() {

            return delayed;
        }


        public Object getExtra() {

            return extra;
        }


        public OnMessageReceiveListener getReceiveListener() {

            return receiveListener;
        }
    }

    //============================ rest Runnable ============================

    /**
     * used with {@link #COMMAND_TAKE_REST},
     * because {@link Command#Command(int, Runnable)} not null,so create a do Nothing runnable
     */
    private class RestRunnable implements Runnable {

        @Override
        public void run() {

            /* take a rest, do nothing */

        }
    }

    //============================ take a while Runnable ============================

    /**
     * used with {@link #COMMAND_TAKE_REST_AWHILE} ,
     * send a delayed message, to call {@link #stopRest()}
     */
    private class TakeWhileRunnable implements Runnable {

        private int delayed;


        TakeWhileRunnable(int delayed) {

            this.delayed = delayed;
        }


        @Override
        public void run() {

            mBusMessageManger.notifyBusStopRestAfter(delayed);
        }
    }

    //============================ BusRunningListenerRunnable ============================

    /**
     * when user {@link #setOnBusRunningListener(OnBusRunningListener)},use this to
     * {@link #wrapperRunnableIfHaveOnBusRunningListener(Runnable)} the user runnable to listen
     */
    private class BusRunningListenerRunnable implements Runnable {

        /**
         * {@link #setOnBusRunningListener(OnBusRunningListener)}
         */
        @NonNull
        OnBusRunningListener mOnBusRunningListener;
        /**
         * {@link Command#mRunnable}
         */
        @NonNull
        Runnable             mRunnable;


        BusRunningListenerRunnable(@NonNull OnBusRunningListener onBusRunningListener) {

            mOnBusRunningListener = onBusRunningListener;
        }


        void changeListener(OnBusRunningListener onBusRunningListener) {

            mOnBusRunningListener = onBusRunningListener;
        }


        void setRunnable(@NonNull Runnable runnable) {

            mRunnable = runnable;
        }


        @Override
        public void run() {

            /* run the runnable user set with the OnBusRunningListener */

            Runnable runnable = mRunnable;
            OnBusRunningListener listener = mOnBusRunningListener;

            listener.onRunnableStart(ObjectBus.this, runnable);

            try {

                runnable.run();
                listener.onRunnableFinished(ObjectBus.this, runnable);
            } catch (Exception e) {

                e.printStackTrace();
                listener.onRunnableException(ObjectBus.this, runnable, e);
            }
        }
    }

    //============================ list runnable ============================

    private class ListRunnable implements Runnable {

        private List< Runnable > mRunnableList;


        public ListRunnable(List< Runnable > runnableList) {

            mRunnableList = runnableList;
        }


        @Override
        public void run() {

            AppExecutor.execute(mRunnableList);
        }
    }

    //============================ callable Runnable ============================

    private class CallableRunnable < T > implements Runnable {

        private Callable< T > mCallable;
        private String        key;


        public CallableRunnable(Callable< T > callable, String key) {

            this.key = key;
            mCallable = callable;
        }


        @Override
        public void run() {

            T t = AppExecutor.submitAndGet(mCallable);
            take(t, key);
        }
    }

    //============================ multi runnable Concurrent ============================

    private class ConcurrentRunnable < T > implements Runnable {

        java.util.List< Callable< T > > mCallableList;
        private String key;


        ConcurrentRunnable(List< Callable< T > > callableList, String key) {

            mCallableList = callableList;
            this.key = key;
        }


        @Override
        public void run() {

            List< T > list = AppExecutor.submitAndGet(mCallableList);
            take(list, key);
        }
    }

    //============================ do extra action with runnable ============================

    /**
     * use with {@link #go(OnBeforeRunAction, Runnable)} to run init before runnable run
     *
     * @see OnBeforeRunAction
     */
    private class ExtraActionRunnable implements Runnable {

        private OnBeforeRunAction mOnBeforeRunAction;
        private Runnable          mRunnable;
        private OnAfterRunAction  mOnRunnableFinishAction;


        public ExtraActionRunnable(OnBeforeRunAction OnBeforeRunAction, Runnable
                runnable, OnAfterRunAction onRunnableFinishAction) {

            mOnBeforeRunAction = OnBeforeRunAction;
            mRunnable = runnable;
            mOnRunnableFinishAction = onRunnableFinishAction;
        }


        @SuppressWarnings("unchecked")
        @Override
        public void run() {

            if (mOnBeforeRunAction != null) {
                mOnBeforeRunAction.onBeforeRun(mRunnable);
            }
            mRunnable.run();

            if (mOnRunnableFinishAction != null) {
                mOnRunnableFinishAction.onAfterRun(ObjectBus.this, mRunnable);
            }
        }
    }
}
