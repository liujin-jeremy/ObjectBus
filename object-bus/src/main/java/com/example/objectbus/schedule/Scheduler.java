package com.example.objectbus.schedule;

import android.os.Message;
import android.util.SparseArray;

import com.example.objectbus.executor.AppExecutor;
import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.run.AsyncThreadCallBack;
import com.example.objectbus.schedule.run.MainThreadCallBack;

import java.lang.ref.WeakReference;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-04-30:0:54
 */
public class Scheduler {

    /**
     * 用于发送消息,进行任务调度
     */
    private static ScheduleTask sScheduleTask;

    /**
     * 用于有callback的任务的存储,该int起始11,每次增加2,
     * 用于{@link #CALLBACK_RUNNABLE}保存{@link MainThreadCallBack}
     */
    private static AtomicInteger sMainInteger;
    /**
     * 用于有callback的任务的存储,该int起始12,每次增加2,
     * 用于{@link #CALLBACK_RUNNABLE}保存{@link AsyncThreadCallBack}
     */
    private static AtomicInteger sOtherInteger;

    /**
     * 生成一个标记,{@link #RUNNABLE}使用该标记存储后台任务{@link Runnable}
     */
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();

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

    //============================ 后台任务不可取消 ============================


    /**
     * do something in ThreadPool
     *
     * @param runnable something
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable) {

        todoInternal(runnable, 0, null, null);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, MainThreadCallBack callBack) {

        todoInternal(runnable, 0, callBack, null);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in asyncThread
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, AsyncThreadCallBack callBack) {

        todoInternal(runnable, 0, callBack, null);
    }


    /**
     * do something in background,With a delayed
     *
     * @param runnable something
     * @param delayed  delayed time
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, int delayed) {

        todoInternal(runnable, delayed, null, null);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, int delayed, MainThreadCallBack callback) {

        todoInternal(runnable, delayed, callback, null);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback async thread callback
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, int delayed, AsyncThreadCallBack callback) {

        todoInternal(runnable, delayed, callback, null);
    }

    //============================ 后台任务,可取消 ============================


    /**
     * do something in background
     *
     * @param runnable something
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, CancelTodo cancelTodo) {

        todoInternal(runnable, 0, null, cancelTodo);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in mainThread
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, MainThreadCallBack callBack, CancelTodo cancelTodo) {

        todoInternal(runnable, 0, callBack, cancelTodo);
    }


    /**
     * do something in background,then do the callBack on mainThread
     *
     * @param runnable something do in background
     * @param callBack something do in asyncThread
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, AsyncThreadCallBack callBack, CancelTodo cancelTodo) {

        todoInternal(runnable, 0, callBack, cancelTodo);
    }


    /**
     * do something in background,With a delayed
     *
     * @param runnable something
     * @param delayed  delayed time
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(Runnable runnable, int delayed, CancelTodo cancelTodo) {

        todoInternal(runnable, delayed, null, cancelTodo);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback main thread callback
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(
            Runnable runnable,
            int delayed,
            MainThreadCallBack callback,
            CancelTodo cancelTodo) {

        todoInternal(runnable, delayed, callback, cancelTodo);
    }


    /**
     * do something in background,With a delayed,then do the callBack on mainThread
     *
     * @param runnable something
     * @param delayed  delayed time
     * @param callback async thread callback
     * @see #todoInternal(Runnable, int, Runnable, CancelTodo)
     */
    public static void todo(
            Runnable runnable,
            int delayed,
            AsyncThreadCallBack callback,
            CancelTodo cancelTodo) {

        todoInternal(runnable, delayed, callback, cancelTodo);
    }


    /**
     * do something in background,With a delayed(if delayed==0 do it now),then do the callBack on another
     * Thread (if has callBack),the thread depends on which runnable used,if {@link MainThreadCallBack}
     * used ,will call on mainThread,if {@link AsyncThreadCallBack}used ,will call on {@link Messengers}'s
     * thread,
     *
     * @param runnable   background task,use {@link OnExecuteRunnable}could
     *                   listen runnable execute station
     * @param delayed    delayed
     * @param callback   callBack to do when finish
     * @param cancelTodo container used for cancel {@code runnable}
     */
    private static void todoInternal(
            Runnable runnable,
            int delayed,
            Runnable callback,
            CancelTodo cancelTodo) {

        Runnable todoRunnable;

        if (cancelTodo != null) {
            cancelTodo.init();
        }

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

        int key = ATOMIC_INTEGER.addAndGet(1);
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

            Message msg = (Message) extra;

            /* 从delayed队列中,找出需要执行的任务 */
            /* try catch 是因为,外部会调用delayRunnable.remove(int);取消任务 */
            SparseArray< Runnable > runnable = RUNNABLE;
            try {

                Runnable needExecute = runnable.get(msg.arg1);
                if (needExecute != null) {
                    AppExecutor.execute(needExecute);
                }
                runnable.delete(msg.arg1);

            } catch (Exception e) {

                runnable.delete(msg.arg1);
            }
        }
    }

    //============================ callBack Runnable ============================

    /**
     * 包装任务,使其具有回调,后台执行完成之后,根据{@link #mTag}的奇偶性,
     * 在不同的线程回调监听{@link Messengers#send(int, OnMessageReceiveListener)},
     * 可以肯定的是,肯定不再执行任务的线程回调监听,如果需要在后台执行完之后,继续进行一些操作,
     * 请使用{@link OnExecuteRunnable},该接口具有执行情况的监听
     */
    private static class CallbackRunnable implements Runnable {

        private Runnable mRunnable;
        private int      mTag;


        CallbackRunnable(Runnable runnable, int tag) {

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
