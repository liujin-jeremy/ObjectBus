package com.example.objectbus;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * @author wuxio 2018-05-01:20:16
 * 该类用于两个类之间通信
 * 用于发送一个通知,主要用于后台任务处理完成之后,继续下一步任务,使用handler 1.解决方法栈过长,2.可以切换到主线程
 */
public class Messengers {

    private static SendHandler syncHandler;
    private static SendHandler mainHandler;

    private static final Random RANDOM = new Random();

    static {
        HandlerThread thread = new HandlerThread("Messengers");
        thread.start();
        syncHandler = new SendHandler(thread.getLooper());
        mainHandler = new SendHandler(Looper.getMainLooper());
    }

    /**
     * 发送一条空白消息
     *
     * @param what 标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param who  发送给谁
     */
    public static void send(int what, @NonNull OnMessageReceiveListener who) {

        send(what, null, who);
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
     * 发送一条空白消息,携带一个数据
     *
     * @param what    标识,如果是奇数,发送到主线程,如果时偶数,发送到后台线程处理,注意不要使用0作为标识
     * @param delayed 延时
     * @param extra   额外的信息
     * @param who     发送给谁
     */
    public static void send(int what, int delayed, Object extra, @NonNull OnMessageReceiveListener who) {

        int key = RANDOM.nextInt();

        final int judge = 2;

        Message obtain = Message.obtain();
        obtain.what = what;
        obtain.arg1 = key;

        if (what % judge == 0) {
            syncHandler.MESSAGE_HOLDER_ARRAY.put(key, new Holder(what, extra, who));
            syncHandler.sendMessageDelayed(obtain, delayed);
        } else {
            mainHandler.MESSAGE_HOLDER_ARRAY.put(key, new Holder(what, extra, who));
            mainHandler.sendMessageDelayed(obtain, delayed);
        }
    }


    /**
     * 移除一条消息
     *
     * @param what message 标识
     */
    public static void remove(int what) {

        final int judge = 2;
        if (what % judge == 0) {
            remove(what, syncHandler);
        } else {
            remove(what, mainHandler);
        }
    }


    /**
     * 先移除消息,然后发送一个移除holder的消息,移除holder,如果需要移除的消息正在运行中,
     * 但是还没有调用到{@link OnMessageReceiveListener#onReceive(int)},
     * 通过置空holder{@link Messengers#setHolderNull(int, SendHandler)}可以快速结束,
     * 但是如果已经处于调用{@link OnMessageReceiveListener#onReceive(int)},那就解决不了了,
     * 需要用户的{@link OnMessageReceiveListener#onReceive(int)}尽快执行
     */
    private static void remove(int what, SendHandler sendHandler) {

        sendHandler.removeMessages(what);
        sendHandler.sendRemoveMessage(what);
        setHolderNull(what, sendHandler);
    }


    /**
     * 此方法用于处理handler正在处理message,但是发出了remove message的命令,通过置为null,使其发生异常快速结束该处理
     */
    private static void setHolderNull(int what, SendHandler handler) {

        SparseArray< Holder > array = handler.MESSAGE_HOLDER_ARRAY;
        for (int i = 0; i < array.size(); i++) {
            int key = array.keyAt(i);
            Holder holder = array.get(key);
            try {
                if (holder.what == what) {
                    array.put(key, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //============================ send messenger async ============================

    /**
     * 发送消息的handler
     */
    private static class SendHandler extends Handler {

        private final SparseArray< Holder > MESSAGE_HOLDER_ARRAY = new SparseArray<>();


        /**
         * 不同的loop 消息送到不同的线程
         *
         * @param looper looper
         */
        private SendHandler(Looper looper) {

            super(looper);
        }


        public void sendRemoveMessage(int what) {

            Message message = obtainMessage();
            message.what = what - 1;
            message.arg2 = what - 1;
            sendMessage(message);
        }


        /**
         * 处理消息
         */
        @Override
        public void handleMessage(Message msg) {

            int what = msg.what;

            /* 此处处理移除message后的收尾工作 */

            if (what == msg.arg2) {

                what += 1;
                SparseArray< Holder > array = MESSAGE_HOLDER_ARRAY;
                for (int i = 0; i < array.size(); i++) {
                    int key = array.keyAt(i);
                    Holder holder = array.get(key);
                    try {
                        if (holder.what == what) {
                            array.remove(key);
                            i--;
                        }
                    } catch (Exception e) {
                        array.remove(key);
                        i--;
                    }
                }
                return;
            }

            /* 遍历Holder, 找出需要处理的holder */

            SparseArray< Holder > holderArray = MESSAGE_HOLDER_ARRAY;
            for (int i = 0; i < holderArray.size(); i++) {

                int key = holderArray.keyAt(i);

                /* 该标记用于,catch中continue还是break */

                boolean isFindHolder = false;

                /* 此处使用 try catch 是因为需要保证程序稳定,并且holder可能为null */

                try {
                    if (holderArray.get(key).what == what && key == msg.arg1) {

                        isFindHolder = true;

                        Object extra = holderArray.get(key).extra;

                        if (extra == null) {

                            OnMessageReceiveListener listener = holderArray.get(key).listener.get();
                            if (listener != null) {
                                listener.onReceive(what);
                            }
                        } else {

                            OnMessageReceiveListener listener = holderArray.get(key).listener.get();
                            if (listener != null) {
                                listener.onReceive(what, extra);
                            }
                        }

                        /* delete holder, holder is handled */

                        holderArray.delete(key);
                        break;
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                    holderArray.delete(key);
                    i--;

                    if (isFindHolder) {
                        break;
                    }
                }
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
