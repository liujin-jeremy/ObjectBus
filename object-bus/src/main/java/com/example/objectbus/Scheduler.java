package com.example.objectbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-04-30:0:54
 */
public class Scheduler {


    /**
     * 用于发送消息,进行任务调度
     */
    private static MainHandler sMainHandler;

    /**
     * 用于右callback的任务的存储标记
     */
    private static AtomicInteger sInteger;

    /**
     * 保存立即执行的任务
     */
    private static final transient ArrayList< Runnable > RUNNABLE = new ArrayList<>();

    /**
     * 保存延时执行的任务
     */
    private static final transient ArrayList< Runnable > DELAY_RUNNABLE = new ArrayList<>();

    /**
     * 保存具有主线程回调的任务
     */
    private static final transient SparseArray< WeakReference< Runnable > > CALLBACK_RUNNABLE =
            new SparseArray<>();


    private Scheduler() {

    }


    private static void initField() {

        sMainHandler = new MainHandler(Looper.getMainLooper());
        sInteger = new AtomicInteger(12);
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
     * 初始化
     */
    public static void init(ThreadFactory threadFactory) {

        initField();
        AppExecutor.init(threadFactory);
    }


    /**
     * do something in background
     *
     * @param runnable something
     */
    public static void todo(Runnable runnable) {

        RUNNABLE.add(runnable);
        sMainHandler.sendEmptyMessage(0);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     */
    public static void todo(Runnable runnable, Runnable callBack) {

        int tag = sInteger.addAndGet(1);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callBack));
        RUNNABLE.add(new CallbackRunnable(runnable, tag));
        sMainHandler.sendEmptyMessage(0);
    }


    /**
     * do something in background,With a delayed
     *
     * @param runnable something
     * @param delayed  delayed time
     */
    public static void todo(Runnable runnable, int delayed) {

        if (delayed <= 0) {
            todo(runnable);
            return;
        }

        Message obtain = Message.obtain();
        long time = System.currentTimeMillis() + delayed;
        obtain.arg1 = (int) (time >> 32);
        obtain.arg2 = (int) time;
        obtain.obj = runnable.toString();
        obtain.what = 1;

        DELAY_RUNNABLE.add(runnable);
        sMainHandler.sendMessageDelayed(obtain, delayed);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     */
    public static void todo(Runnable runnable, int delayed, Runnable callback) {

        if (delayed <= 0) {
            todo(runnable, callback);
            return;
        }

        int tag = sInteger.addAndGet(1);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callback));
        CallbackRunnable callbackRunnable = new CallbackRunnable(runnable, tag);

        Message obtain = Message.obtain();
        long time = System.currentTimeMillis() + delayed;
        obtain.arg1 = (int) (time >> 32);
        obtain.arg2 = (int) time;
        obtain.obj = callbackRunnable.toString();
        obtain.what = 1;

        DELAY_RUNNABLE.add(callbackRunnable);
        sMainHandler.sendMessageDelayed(obtain, delayed);
    }

    //============================ main thread handler ============================

    /**
     * 将任务发送给线程池执行
     */
    private static class MainHandler extends Handler {

        MainHandler(Looper looper) {

            super(looper);
        }


        @Override
        public void handleMessage(Message msg) {

            if (msg.what == 0) {

                /* send task to executor */

                while (RUNNABLE.size() > 0) {
                    Runnable runnable = RUNNABLE.get(0);
                    AppExecutor.execute(runnable);
                    RUNNABLE.remove(0);
                }
                return;
            }

            if (msg.what == 1) {

                /* send delay task to executor */

                long currentTimeMillis = System.currentTimeMillis();

                int size = DELAY_RUNNABLE.size();
                for (int i = 0; i < size; i++) {

                    Runnable runnable = DELAY_RUNNABLE.get(i);

                    if (!msg.obj.equals(runnable.toString())) {
                        continue;
                    }

                    long src = msg.arg1;
                    src = (src << 32) | msg.arg2;
                    if (src <= currentTimeMillis) {
                        AppExecutor.execute(runnable);
                        DELAY_RUNNABLE.remove(i);
                        return;
                    }
                }
                return;
            }

            int tag = msg.what;
            WeakReference< Runnable > reference = CALLBACK_RUNNABLE.get(tag);
            Runnable callBack = reference.get();
            if (callBack != null) {
                callBack.run();
            }
            CALLBACK_RUNNABLE.delete(tag);

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
            sMainHandler.sendEmptyMessage(mTag);
        }
    }
}
