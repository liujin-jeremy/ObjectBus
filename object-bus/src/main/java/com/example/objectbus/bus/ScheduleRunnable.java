package com.example.objectbus.bus;

import android.support.annotation.NonNull;

import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.schedule.Scheduler;

/**
 * @author wuxio 2018-05-03:16:43
 */
class ScheduleRunnable implements Runnable {


    private ScheduleExecuteRunnable mRunnable;
    private ObjectBus               mObjectBus;


    public ScheduleRunnable(@NonNull Runnable runnable) {

        mRunnable = new ScheduleExecuteRunnable(runnable);
    }


    @Override
    public void run() {

        Scheduler.todo(mRunnable);

    }


    private class ScheduleExecuteRunnable implements OnExecuteRunnable {

        private Runnable mRunnable;


        public ScheduleExecuteRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        @Override
        public void onExecute() {

            mRunnable.run();
        }


        @Override
        public void onFinish() {

        }


        @Override
        public void onException(Exception e) {

        }
    }
}
