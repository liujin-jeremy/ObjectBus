package com.example.objectbus.bus;

import android.support.annotation.NonNull;

import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.schedule.Scheduler;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-05-03:16:16
 */
public class ObjectBus {

    private static final int GO       = 0b1;
    private static final int TO_UNDER = 0b10;


    private static final int MAIN_THREAD      = 0X1FFFF;
    private static final int EXECUTOR_THREAD  = 0X2FFFF;
    private static final int MESSENGER_THREAD = 0X4FFFF;
    private int currentThread;

    /**
     * how many station pass By
     */
    private AtomicInteger        mPassBy    = new AtomicInteger();
    private ArrayList< Command > mHowToPass = new ArrayList<>();

    private BusOnExecuteRunnable mBusOnExecuteRunnable = new BusOnExecuteRunnable();


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
            mPassBy.set(0);
        }
    }


    /**
     * @param command use command to run runnable
     */
    private void doCommand(Command command) {

        if (command.command == GO) {
            command.run();
            toNextStation();
            return;
        }

        if (command.command == TO_UNDER) {
            if (currentThread != EXECUTOR_THREAD) {
                mBusOnExecuteRunnable.setRunnable(command.getRunnable());
                Scheduler.todo(mBusOnExecuteRunnable);
            } else {
                command.run();
                toNextStation();
            }
        }
    }


    /**
     * run runnable on current thread
     *
     * @param task task todo
     * @return self
     */
    public ObjectBus go(@NonNull Runnable task) {

        mHowToPass.add(new Command(GO, task));
        return this;
    }


    public ObjectBus toUnder(@NonNull Runnable task) {

        mHowToPass.add(new Command(TO_UNDER, task));
        return this;
    }


    /**
     * start run bus
     */
    public void run() {

        toNextStation();
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


        public Runnable getRunnable() {

            return mRunnable;
        }
    }

    //============================ executor runnable ============================

    private class BusOnExecuteRunnable implements OnExecuteRunnable {

        private Runnable mRunnable;


        public void setRunnable(Runnable runnable) {

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
}
