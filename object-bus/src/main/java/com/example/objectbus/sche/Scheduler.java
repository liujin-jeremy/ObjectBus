package com.example.objectbus.sche;

import android.os.Message;
import android.util.SparseArray;

import com.example.objectbus.AppExecutor;
import com.example.objectbus.Messengers;
import com.example.objectbus.OnMessageReceiveListener;
import com.example.objectbus.runnable.AsyncThreadCallBack;
import com.example.objectbus.runnable.MainThreadCallBack;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-04-30:0:54
 */
public class Scheduler {

    private static final String TAG = "Scheduler";

    /**
     * 用于发送消息,进行任务调度
     */
    //private static MainHandler sMainHandler;

    private static ScheduleTask sScheduleTask;

    /**
     * 用于有callback的任务的存储标记
     */
    private static AtomicInteger sMainInteger;
    private static AtomicInteger sOtherInteger;

    /**
     * 用于生成一个标记
     */
    private static Random sRandom;

    /**
     * 保存任务
     */
    static final transient SparseArray< Runnable > RUNNABLE = new SparseArray<>();

    /**
     * 保存回调
     */
    static final transient SparseArray< WeakReference< Runnable > > CALLBACK_RUNNABLE =
            new SparseArray<>();


    private Scheduler() {

    }


    private static void initField() {

        sMainInteger = new AtomicInteger(11);
        sOtherInteger = new AtomicInteger(12);
        sScheduleTask = new ScheduleTask();
        sRandom = new Random(66082619900012L);
    }


    /**
     * 初始化
     */
    public static void init() {

        initField();
        AppExecutor.init();
    }


    /**
     * 初始化
     */
    public static void init(ThreadPoolExecutor poolExecutor) {

        initField();
        AppExecutor.init(poolExecutor);
    }


    /**
     * do something in background
     *
     * @param runnable something
     */
    public static void todo(Runnable runnable) {

        todoInner(runnable, 0, null, null);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     */
    public static void todo(Runnable runnable, MainThreadCallBack callBack) {

        todoInner(runnable, 0, callBack, null);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in asyncThread
     */
    public static void todo(Runnable runnable, AsyncThreadCallBack callBack) {

        todoInner(runnable, 0, callBack, null);
    }


    /**
     * do something in background,With a delayed
     *
     * @param runnable something
     * @param delayed  delayed time
     */
    public static void todo(Runnable runnable, int delayed) {

        todoInner(runnable, delayed, null, null);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     */
    public static void todo(Runnable runnable, int delayed, MainThreadCallBack callback) {

        todoInner(runnable, delayed, callback, null);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback async thread callback
     */
    public static void todo(Runnable runnable, int delayed, AsyncThreadCallBack callback) {

        todoInner(runnable, delayed, callback, null);
    }


    /**
     * do something in background
     *
     * @param runnable something
     */
    public static void todo(Runnable runnable, CancelTodo cancelTodo) {

        todoInner(runnable, 0, null, cancelTodo);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     */
    public static void todo(Runnable runnable, MainThreadCallBack callBack, CancelTodo cancelTodo) {

        todoInner(runnable, 0, callBack, cancelTodo);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in asyncThread
     */
    public static void todo(Runnable runnable, AsyncThreadCallBack callBack, CancelTodo cancelTodo) {

        todoInner(runnable, 0, callBack, cancelTodo);
    }


    /**
     * do something in background,With a delayed
     *
     * @param runnable something
     * @param delayed  delayed time
     */
    public static void todo(Runnable runnable, int delayed, CancelTodo cancelTodo) {

        todoInner(runnable, delayed, null, cancelTodo);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     */
    public static void todo(
            Runnable runnable,
            int delayed,
            MainThreadCallBack callback,
            CancelTodo cancelTodo) {

        todoInner(runnable, delayed, callback, cancelTodo);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback async thread callback
     */
    public static void todo(
            Runnable runnable,
            int delayed,
            AsyncThreadCallBack callback,
            CancelTodo cancelTodo) {

        todoInner(runnable, delayed, callback, cancelTodo);
    }


    /**
     * do something in background,With a delayed(if delayed==0 do it now),then do the callBack on a Thread
     * (if has callBack)
     *
     * @param runnable   background task
     * @param delayed    delayed
     * @param callback   callBack to do when finish
     * @param cancelTodo container used for cancel {@code runnable}
     */
    private static void todoInner(Runnable runnable, int delayed, Runnable callback, CancelTodo cancelTodo) {

        Runnable todoRunnable;

        if (callback == null) {
            todoRunnable = runnable;
        } else {

            int tag;

            if (callback instanceof MainThreadCallBack) {

                tag = sMainInteger.addAndGet(2);
                CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callback));
                todoRunnable = new CallbackRunnable(runnable, tag);

            } else {

                tag = sOtherInteger.addAndGet(2);
                CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callback));
                todoRunnable = new CallbackRunnable(runnable, tag);

            }

            if (cancelTodo != null) {
                cancelTodo.setTag(tag);
                cancelTodo.setCallback(callback);
            }
        }

        /* 使用一个标识,标记延时任务 */
        Message obtain = Message.obtain();
        int key = sRandom.nextInt();
        obtain.arg1 = key;
        RUNNABLE.put(key, todoRunnable);

        /* 记录给cancelTodo */

        if (cancelTodo != null) {
            cancelTodo.setKey(key);
            cancelTodo.setTodoRunnable(todoRunnable);
        }

        sScheduleTask.sendDelayedMessage(obtain, delayed);
    }

    //============================ schedule task to pool ============================

    private static class ScheduleTask implements OnMessageReceiveListener {

        static final int WHAT_DELAYED_MESSAGE = 2;


        void sendMessage(int what) {

            Messengers.send(what, this);
        }


        void sendDelayedMessage(Message message, int delayed) {

            Messengers.send(WHAT_DELAYED_MESSAGE, delayed, message, this);
        }


        @Override
        public void onReceive(int what) {

            /* 此处处理的是callBack的任务 */
            /* try catch 是因为,外部会调用callbackRunnable.remove(int);取消任务,而持有的弱引用也可能会消失 */

            SparseArray< WeakReference< Runnable > > callbackRunnable = CALLBACK_RUNNABLE;
            try {

                callbackRunnable.get(what).get().run();
                callbackRunnable.delete(what);

            } catch (Exception e) {

                callbackRunnable.delete(what);
            }
        }


        @Override
        public void onReceive(int what, Object extra) {

            /* 此处处理的是后台任务 */

            if (what == WHAT_DELAYED_MESSAGE) {

                Message msg = (Message) extra;

                /* 从delayed队列中,找出需要执行的任务 */
                /* try catch 是因为,外部会调用delayRunnable.remove(int);取消任务 */
                SparseArray< Runnable > runnable = RUNNABLE;
                try {

                    Runnable needExecute = runnable.get(msg.arg1);
                    if (needExecute != null) {
                        AppExecutor.execute(needExecute);
                    }
                    runnable.remove(msg.arg1);

                } catch (Exception e) {

                    runnable.remove(msg.arg1);
                }
            }
        }
    }

    //============================ callBack Runnable ============================

    /**
     * 包装任务,使其具有回调,后台执行完成之后,调用主线程回调
     */
    private static class CallbackRunnable implements Runnable {

        private Runnable mRunnable;
        private int      mTag;


        public CallbackRunnable(Runnable runnable, int tag) {

            mRunnable = runnable;
            this.mTag = tag;
        }


        @Override
        public void run() {

            mRunnable.run();
            sScheduleTask.sendMessage(mTag);
        }
    }
}
