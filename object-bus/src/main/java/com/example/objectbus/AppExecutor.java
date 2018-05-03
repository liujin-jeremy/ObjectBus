package com.example.objectbus;

import android.support.annotation.NonNull;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
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


    public static void execute(Runnable runnable) {

        try {
            sPoolExecutor.execute(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(" you should  call init() first");
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

        private static transient int total = 0;


        public AppThread(Runnable target) {

            super(target);
            setName("AppThread-" + total);
            setPriority(Thread.NORM_PRIORITY - 1);
            total++;
        }
    }
}
