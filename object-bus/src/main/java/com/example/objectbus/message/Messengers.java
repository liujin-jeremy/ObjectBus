package com.example.objectbus.message;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-05-01:20:16
 * 该类用于两个类之间通信
 * 用于发送一个通知,主要用于后台任务处理完成之后,继续下一步任务,使用handler 1.解决方法栈过长,2.可以切换到主线程
 */
public class Messengers {

    private static SendHandler sSendHandler;
    private static SendHandler sMainHandler;

    private static final Object LOCK = new Object();

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();


    public static void init() {

        HandlerThread thread = new HandlerThread("Messengers");
        thread.start();
        sSendHandler = new SendHandler(thread.getLooper());
        sMainHandler = new SendHandler(Looper.getMainLooper());
    }


    public static void init(Looper looper) {

        sSendHandler = new SendHandler(looper);
        sMainHandler = new SendHandler(Looper.getMainLooper());
    }


    /**
     * 发送一条空白消息
     *
     * @param what 标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param who  发送给谁
     */
    public static void send(int what, @NonNull OnMessageReceiveListener who) {

        send(what, 0, null, who);
    }


    /**
     * 发送一条空白消息
     *
     * @param what 标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param who  发送给谁
     */
    public static void send(int what, int delayed, @NonNull OnMessageReceiveListener who) {

        send(what, delayed, null, who);
    }


    /**
     * 发送一条空白消息,携带一个数据
     *
     * @param what 标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param who  发送给谁
     */
    public static void send(int what, Object extra, @NonNull OnMessageReceiveListener who) {

        send(what, 0, extra, who);
    }


    /**
     * 发送一条消息,携带一个数据
     *
     * @param what    标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param delayed 延时
     * @param extra   额外的信息
     * @param who     发送给谁
     */
    public static void send(int what, int delayed, Object extra, @NonNull OnMessageReceiveListener who) {

        final int judge = 2;
        SendHandler sendHandler;
        if (what % judge == 0) {
            sendHandler = sSendHandler;
        } else {
            sendHandler = sMainHandler;
        }

        SparseArray< Holder > array = sendHandler.MESSAGE_HOLDER_ARRAY;
        int key = ATOMIC_INTEGER.addAndGet(1);
        Message obtain = Message.obtain();

        synchronized (LOCK) {
            while (array.get(key) != null) {
                key = ATOMIC_INTEGER.addAndGet(1);
            }
            obtain.arg1 = key;
            array.put(key, new Holder(what, extra, who));
        }

        obtain.what = what;
        sendHandler.sendMessageDelayed(obtain, delayed);
    }


    /**
     * 移除一条消息
     *
     * @param what message 标识
     */
    public static void remove(int what, OnMessageReceiveListener listener) {

        final int judge = 2;
        if (what % judge == 0) {
            clearListener(what, listener, sSendHandler);
        } else {
            clearListener(what, listener, sMainHandler);
        }
    }


    /**
     * clear the listener , make him not receive message
     *
     * @param what     message what
     * @param listener listener
     * @param handler  which handler listener at
     */
    private static void clearListener(int what, OnMessageReceiveListener listener, SendHandler handler) {

        SparseArray< Holder > array = handler.MESSAGE_HOLDER_ARRAY;
        int size = array.size();
        for (int i = 0; i < size; i++) {

            int key = array.keyAt(i);
            Holder holder = array.get(key);

            if (holder != null && holder.what == what && holder.listener.get() == listener) {
                holder.listener.clear();
            }
        }
    }

    //============================ send messenger async ============================

    /**
     * 发送消息的handler
     */
    private static class SendHandler extends Handler {

        final SparseArray< Holder > MESSAGE_HOLDER_ARRAY = new SparseArray<>();


        /**
         * 不同的loop 消息送到不同的线程
         *
         * @param looper looper
         */
        private SendHandler(Looper looper) {

            super(looper);
        }


        /**
         * 处理消息
         */
        @Override
        public void handleMessage(Message msg) {

            /* 此处处理发送消息的工作 */

            SparseArray< Holder > holderArray = MESSAGE_HOLDER_ARRAY;
            Holder holder = holderArray.get(msg.arg1);
            if (holder != null) {

                OnMessageReceiveListener listener = holder.listener.get();
                if (listener != null) {
                    Object extra = holder.extra;
                    if (extra == null) {
                        listener.onReceive(holder.what);
                    } else {
                        listener.onReceive(holder.what, extra);
                    }
                }

                /* when holder is not null, must delete it */

                holderArray.delete(msg.arg1);
            }
        }
    }

    //============================ holder ============================

    /**
     * 记录message信息
     */
    private static class Holder {

        private int    what;
        private Object extra;

        /**
         * 使用弱引用,防止泄漏
         */
        private WeakReference< OnMessageReceiveListener > listener;


        Holder(int what, Object extra, OnMessageReceiveListener listener) {

            this.what = what;
            this.extra = extra;
            this.listener = new WeakReference<>(listener);
        }
    }
}
