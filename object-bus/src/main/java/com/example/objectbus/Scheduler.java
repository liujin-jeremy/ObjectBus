package com.example.objectbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

import com.example.objectbus.runnable.AsyncThreadCallBack;
import com.example.objectbus.runnable.MainThreadCallBack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

        sMainInteger = new AtomicInteger(11);
        sOtherInteger = new AtomicInteger(12);
        sScheduleTask = new ScheduleTask();
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

        RUNNABLE.add(runnable);
        sScheduleTask.sendMessage();
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     */
    public static void todo(Runnable runnable, MainThreadCallBack callBack) {

        int tag = sMainInteger.addAndGet(2);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callBack));
        RUNNABLE.add(new CallbackRunnable(runnable, tag));
        sScheduleTask.sendMessage();
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in asyncThread
     */
    public static void todo(Runnable runnable, AsyncThreadCallBack callBack) {

        int tag = sOtherInteger.addAndGet(2);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callBack));
        RUNNABLE.add(new CallbackRunnable(runnable, tag));
        sScheduleTask.sendMessage();
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

        /* 使用一个标识,标记延时任务 */

        Message obtain = Message.obtain();
        long time = System.currentTimeMillis() + delayed;

        /* 标记延时任务的执行时间 */
        obtain.arg1 = (int) (time >> 32);
        obtain.arg2 = (int) time;
        /* 标记延时任务的string */
        obtain.obj = runnable.toString();
        /* 一般认为时间相同,string一致,就可判断该任务时需要执行的延时任务 */

        DELAY_RUNNABLE.add(runnable);
        sScheduleTask.sendDelayedMessage(obtain, delayed);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     */
    public static void todo(Runnable runnable, int delayed, MainThreadCallBack callback) {

        if (delayed <= 0) {
            todo(runnable, callback);
            return;
        }

        int tag = sMainInteger.addAndGet(2);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callback));
        CallbackRunnable callbackRunnable = new CallbackRunnable(runnable, tag);

        Message obtain = Message.obtain();
        long time = System.currentTimeMillis() + delayed;
        obtain.arg1 = (int) (time >> 32);
        obtain.arg2 = (int) time;
        obtain.obj = callbackRunnable.toString();

        DELAY_RUNNABLE.add(callbackRunnable);
        sScheduleTask.sendDelayedMessage(obtain, delayed);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback async thread callback
     */
    public static void todo(Runnable runnable, int delayed, AsyncThreadCallBack callback) {

        if (delayed <= 0) {
            todo(runnable, callback);
            return;
        }

        int tag = sOtherInteger.addAndGet(2);
        CALLBACK_RUNNABLE.put(tag, new WeakReference<>(callback));
        CallbackRunnable callbackRunnable = new CallbackRunnable(runnable, tag);

        Message obtain = Message.obtain();
        long time = System.currentTimeMillis() + delayed;
        obtain.arg1 = (int) (time >> 32);
        obtain.arg2 = (int) time;
        obtain.obj = callbackRunnable.toString();

        DELAY_RUNNABLE.add(callbackRunnable);
        sScheduleTask.sendDelayedMessage(obtain, delayed);
    }

    // TODO: 2018-05-03 cancel 方法 使用外部传参'undo'

    //============================ schedule task to pool ============================

    private static class ScheduleTask implements OnMessageReceiveListener {

        static final int WHAT_MESSAGE         = 2;
        static final int WHAT_DELAYED_MESSAGE = 4;


        void sendMessage() {

            Messengers.send(WHAT_MESSAGE, this);
        }


        void sendMessage(int what) {

            Messengers.send(what, this);
        }


        void sendDelayedMessage(Message message, int delayed) {

            Messengers.send(WHAT_DELAYED_MESSAGE, delayed, message, this);
        }


        @Override
        public void onReceive(int what) {

            /* 此处处理的是没有带有callBack的非延时任务 */

            if (what == WHAT_MESSAGE) {

                /* send task to executor */

                while (RUNNABLE.size() > 0) {

                    /* 此处try..catch..因为外部可能会在该方法执行期间,执行RUNNABLE.remove(0);需要捕获异常*/

                    try {
                        Runnable runnable = RUNNABLE.get(0);
                        AppExecutor.execute(runnable);
                        RUNNABLE.remove(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

            /* 此处处理的是带有callBack的任务 */

            WeakReference< Runnable > reference = CALLBACK_RUNNABLE.get(what);
            Runnable callBack = reference.get();
            if (callBack != null) {
                callBack.run();
            }
            CALLBACK_RUNNABLE.delete(what);
        }


        @Override
        public void onReceive(int what, Object extra) {

            /* 此处处理的是没有带有callBack的延时任务 */

            if (what == WHAT_DELAYED_MESSAGE) {

                Message msg = (Message) extra;
                Object name = msg.obj;

                /* send delay task to executor */

                long currentTimeMillis = System.currentTimeMillis();

                /* 从delayed队列中,找出需要执行的任务 */
                try {
                    ArrayList< Runnable > delayRunnable = DELAY_RUNNABLE;
                    int size = delayRunnable.size();
                    for (int i = 0; i < size; i++) {

                        Runnable runnable = delayRunnable.get(i);

                        /* 先比较名字 */
                        if (!name.equals(runnable.toString())) {
                            continue;
                        }

                        /* 后比较时间 */
                        long src = msg.arg1;
                        src = (src << 32) | msg.arg2;

                        /* 如果时间已经到了,执行它 */
                        if (src <= currentTimeMillis) {
                            AppExecutor.execute(runnable);
                            delayRunnable.remove(i);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //============================ main thread handler ============================

    /**
     * 将任务发送给线程池执行
     */
    @Deprecated
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
            sScheduleTask.sendMessage(mTag);
        }
    }
}
