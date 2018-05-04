package com.example.objectbus.bus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.example.objectbus.executor.AppExecutor;
import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.Scheduler;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-05-03:16:16
 */
public class ObjectBus {

    private static final String TAG = "ObjectBus";

    /**
     * command used for {@link Command} to how to do runnable
     */
    private static final int COMMAND_GO               = 0b1;
    private static final int COMMAND_TO_UNDER         = 0b10;
    private static final int COMMAND_TO_MAIN          = 0b100;
    private static final int COMMAND_SEND             = 0b1000;
    private static final int COMMAND_TAKE_REST        = 0b10000;
    private static final int COMMAND_TAKE_REST_AWHILE = 0b100000;

    /**
     * current thread state
     */
    private static final int THREAD_MAIN     = 0X1FFFF;
    private static final int THREAD_EXECUTOR = 0X2FFFF;
    private int threadCurrent;

    /**
     * record current is resting
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
     * used do runnable at {@link com.example.objectbus.executor.AppExecutor}
     */
    private BusOnExecuteRunnable mBusOnExecuteRunnable = new BusOnExecuteRunnable();

    /**
     * used do runnable at MainThread
     */
    private BusMessageListener mBusMessageListener = new BusMessageListener();

    /**
     * take customs to bus,{@link #takeAs(Object, String)}
     */
    private ArrayMap< String, Object > mExtras = new ArrayMap<>();

    /**
     * do nothing just take a rest ,wait notify
     */
    private RestRunnable mRestRunnable = new RestRunnable();


    public ObjectBus() {

    }


    /**
     * @return hoe many station pass by
     */
    public int getPassBy() {

        return mPassBy.get();
    }


    /**
     * to next station
     */
    private void toNextStation() {

        int index = mPassBy.getAndAdd(1);
        if (index < mHowToPass.size()) {

            Command command = mHowToPass.get(index);
            doCommand(command);
        } else {

            // TODO: 2018-05-03 how to set current value
            mPassBy.getAndAdd(-1);
        }
    }


    /**
     * @param command use command to run runnable
     */
    private void doCommand(Command command) {

        if (command.command == COMMAND_GO) {
            command.run();
            toNextStation();
            return;
        }

        if (command.command == COMMAND_TO_UNDER) {
            if (threadCurrent != THREAD_EXECUTOR) {
                mBusOnExecuteRunnable.setRunnable(command.getRunnable());
                Scheduler.todo(mBusOnExecuteRunnable);
                threadCurrent = THREAD_EXECUTOR;
            } else {
                command.run();
                toNextStation();
            }
            return;
        }

        if (command.command == COMMAND_TO_MAIN) {
            if (threadCurrent != THREAD_MAIN) {
                BusMessageListener messenger = mBusMessageListener;
                messenger.setRunnable(command.getRunnable());
                messenger.runOnMain();
                threadCurrent = THREAD_MAIN;
            } else {
                command.run();
                toNextStation();
            }

            return;
        }

        if (command.command == COMMAND_SEND) {
            command.run();
            toNextStation();
            return;
        }

        if (command.command == COMMAND_TAKE_REST) {
            runState = RUN_STATE_RESTING;
            /*  do nothing,wait for notify to go on */
            return;
        }

        if (command.command == COMMAND_TAKE_REST_AWHILE) {
            runState = RUN_STATE_RESTING_AWHILE;
            command.run();
            return;
        }
    }


    /**
     * run runnable on current thread;
     * if call {@link #toUnder(Runnable)} current thread will be
     * {@link com.example.objectbus.executor.AppExecutor} thread;
     * if call {@link #toMain(Runnable)} current thread will be main thread;
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus go(@NonNull Runnable task) {

        mHowToPass.add(new Command(COMMAND_GO, task));
        return this;
    }


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus toUnder(@NonNull Runnable task) {

        mHowToPass.add(new Command(COMMAND_TO_UNDER, task));
        return this;
    }


    /**
     * run runnable on main thread
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus toMain(@NonNull Runnable task) {

        mHowToPass.add(new Command(COMMAND_TO_MAIN, task));
        return this;
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus send(int what, OnMessageReceiveListener listener) {

        return sendDelayed(what, 0, null, listener);
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
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
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus sendDelayed(int what, int delayed, OnMessageReceiveListener listener) {

        return sendDelayed(what, delayed, null, listener);
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param extra    extra msg
     * @param listener receiver
     * @return self
     */
    public ObjectBus sendDelayed(int what, int delayed, Object extra, OnMessageReceiveListener listener) {

        mHowToPass.add(new Command(COMMAND_SEND, new SendRunnable(what, delayed, extra, listener)));
        return this;
    }


    /**
     * take a rest
     */
    public ObjectBus takeRest() {

        mHowToPass.add(new Command(COMMAND_TAKE_REST, mRestRunnable));
        return this;
    }


    public ObjectBus takeRest(int millisecond) {

        mHowToPass.add(new Command(COMMAND_TAKE_REST_AWHILE, new TakeWhileRunnable(millisecond)));
        return this;
    }


    public void stopRest() {

        if (runState == RUN_STATE_RESTING || runState == RUN_STATE_RESTING_AWHILE) {

            mBusMessageListener.notifyBusStopRest();
        }
    }


    /**
     * start run bus
     */
    public void run() {

        runState = RUN_STATE_RUNNING;
        toNextStation();
    }

    //============================ 添加乘客操作 ============================


    /**
     * take extra to bus,could use key to get
     *
     * @param extra extra to bus
     * @param key   key
     */
    public void takeAs(Object extra, String key) {

        mExtras.put(key, extra);
    }


    /**
     * get the extra
     *
     * @param key key
     * @return extra
     */
    @Nullable
    public Object get(String key) {

        return mExtras.get(key);
    }


    /**
     * get and remove extra
     *
     * @param key key
     * @return extra
     */
    @Nullable
    public Object off(String key) {

        return mExtras.remove(key);
    }

    //============================ command for Bus run runnable ============================

    /**
     * record how to run runnable
     */
    private class Command {

        private int      command;
        private Runnable mRunnable;


        Command(int command, @NonNull Runnable runnable) {

            this.command = command;
            mRunnable = runnable;
        }


        void run() {

            mRunnable.run();
        }


        Runnable getRunnable() {

            return mRunnable;
        }
    }

    //============================ executor runnable  ============================

    /**
     * use take task to do in the {@link com.example.objectbus.executor.AppExecutor}
     */
    private class BusOnExecuteRunnable implements OnExecuteRunnable {

        private Runnable mRunnable;


        void setRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        @Override
        public void onExecute() {

            Runnable runnable = mRunnable;
            if (runnable != null) {
                runnable.run();
            }
        }


        @Override
        public void onFinish() {

            toNextStation();
        }


        @Override
        public void onException(Exception e) {

            // TODO: 2018-05-03 how to handle exception

        }
    }

    //============================ main runnable ============================

    private class BusMessageListener implements OnMessageReceiveListener {

        private static final String TAG = "BusMessageListener";

        private static final int WHAT_MAIN         = 3;
        private static final int WHAT_STOP_REST    = 4;
        private static final int WHAT_NEXT_AT_MAIN = 5;
        private static final int WHAT_REST_TIME_UP = 6;

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
         * notify bus to next station when bus is RESTING
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


        private void takeBusAtMainToNext() {

            Messengers.send(WHAT_NEXT_AT_MAIN, this);
        }


        @Override
        public void onReceive(int what) {

            /* run runnable on main */

            if (what == WHAT_MAIN) {
                if (mRunnable != null) {
                    mRunnable.run();
                    toNextStation();
                }
                return;
            }

            /* stop bus rest */

            if (what == WHAT_STOP_REST) {

                /* base on bus threadCurrent when him rest,to go on at that thread */

                if (threadCurrent == THREAD_MAIN) {
                    takeBusAtMainToNext();
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

            if (what == WHAT_REST_TIME_UP) {

                if (runState == RUN_STATE_RESTING_AWHILE) {
                    notifyBusStopRest();
                }
            }
        }
    }

    //============================ Send runnable ============================

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

            if (extra == null) {

                Messengers.send(what, delayed, receiveListener);
            } else {

                Messengers.send(what, delayed, extra, receiveListener);
            }
        }
    }

    //============================ rest Runnable ============================

    private class RestRunnable implements Runnable {

        @Override
        public void run() {

            /* take a rest, do nothing */

        }
    }

    //============================ take a while Runnable ============================

    private class TakeWhileRunnable implements Runnable {

        private int delayed;


        public TakeWhileRunnable(int delayed) {

            this.delayed = delayed;
        }


        @Override
        public void run() {

            mBusMessageListener.notifyBusStopRestAfter(delayed);
        }
    }
}
