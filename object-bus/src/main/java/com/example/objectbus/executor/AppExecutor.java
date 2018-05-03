package com.example.objectbus.executor;

import android.support.annotation.NonNull;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 该类负责使用线程池执行后台任务
 *
 * @author wuxio 2018-04-30:1:06
 */
public class AppExecutor {

    private static ThreadPoolExecutor sPoolExecutor;


    public static void init() {

        sPoolExecutor = new ThreadPoolExecutor(
                3,
                6,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                new AppThreadFactory()
        );
    }


    public static void init(ThreadPoolExecutor poolExecutor) {

        sPoolExecutor = poolExecutor;
    }


    @Deprecated
    public static void init(ThreadFactory threadFactory) {

        sPoolExecutor = new ThreadPoolExecutor(
                3,
                6,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                threadFactory
        );
    }


    /**
     * 后台执行任务,如果需要监听执行情况请使用{@link OnExecuteRunnable}
     *
     * @param runnable 执行的任务
     */
    public static void execute(Runnable runnable) {

        /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

        try {
            if (runnable == null) {
                return;
            }
            sPoolExecutor.execute(runnable);
        } catch (Exception e) {
            if (sPoolExecutor == null) {
                throw new RuntimeException(" you should  call init() first");
            } else {
                e.printStackTrace();
            }
        }
    }

    //============================ 配置类 ============================

    private static class AppThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(@NonNull Runnable r) {

            return new AppThread(r);
        }
    }

    private static class AppThread extends Thread {

        private static AtomicInteger sInt = new AtomicInteger();


        public AppThread(Runnable target) {

            super(target);
            setName("AppThread-" + sInt.getAndAdd(1));
            setPriority(Thread.NORM_PRIORITY - 1);
        }
    }
}
