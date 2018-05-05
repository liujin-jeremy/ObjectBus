package com.example.objectbus.executor;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
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
    public static void execute(@NonNull Runnable runnable) {

        /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

        try {
            sPoolExecutor.execute(runnable);
        } catch (Exception e) {

            if (sPoolExecutor == null) {
                throw new RuntimeException(" you should  call init() first");
            } else {
                e.printStackTrace();
            }
        }
    }


    public static < T > Future< T > submit(Callable< T > callable) {

        /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

        try {

            return sPoolExecutor.submit(callable);
        } catch (Exception e) {

            if (sPoolExecutor == null) {
                throw new RuntimeException(" you should  call init() first");
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static < T > T submitAndGet(Callable< T > callable) {

        /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

        try {

            Future< T > future = sPoolExecutor.submit(callable);
            return future.get();
        } catch (Exception e) {

            if (sPoolExecutor == null) {
                throw new RuntimeException(" you should  call init() first");
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static < T > CompletionService< T > submit(List< Callable< T > > callableList) {

        ExecutorCompletionService< T > completionService = new ExecutorCompletionService<>(sPoolExecutor);

        int size = callableList.size();
        for (int i = 0; i < size; i++) {

            try {
                Callable< T > callable = callableList.get(i);
                completionService.submit(callable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return completionService;
    }


    public static < T > List< T > submitAndGet(List< Callable< T > > callableList) {

        ExecutorCompletionService< T > completionService = new ExecutorCompletionService<>(sPoolExecutor);

        int size = callableList.size();
        for (int i = 0; i < size; i++) {
            Callable< T > callable = callableList.get(i);
            completionService.submit(callable);
        }

        List< T > results = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {

            try {

                T t = completionService.take().get();
                results.add(t);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    //============================ 结束任务 ============================

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
