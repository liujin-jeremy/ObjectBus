package com.threekilogram.objectbus.executor;

import android.support.annotation.NonNull;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 该类用于执行定时任务
 *
 * @author Liujin 2018-11-06:20:55
 */
public class ScheduleExecutor {

      private ScheduleExecutor ( ) { }

      private static final ScheduledExecutorService SERVICE = new ScheduledThreadPoolExecutor(
          1,
          new ScheduleThreadFactory()
      );

      /**
       * 安排一个延迟任务
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param runnable 任务
       * @param delayed 延时时间
       */
      public static void schedule ( Runnable runnable, long delayed ) {

            SERVICE.schedule( runnable, delayed, TimeUnit.MILLISECONDS );
      }

      /**
       * 延时任务,可以获取结果
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param callable 任务
       * @param delayed 延时时间
       * @param <T> 结果类型
       *
       * @return 使用该类在未来获取结果
       */
      public static <T> ScheduledFuture<T> schedule ( Callable<T> callable, long delayed ) {

            return SERVICE.schedule( callable, delayed, TimeUnit.MILLISECONDS );
      }

      /**
       * 以固定频率执行任务
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param runnable 任务
       * @param delayed 延时时间
       * @param period 任务之间间隔速率
       *
       * @return {@link ScheduledFuture#cancel(boolean)}结束任务
       */
      public static ScheduledFuture<?> scheduleAtFixedRate (
          Runnable runnable, long delayed, long period ) {

            return SERVICE.scheduleAtFixedRate( runnable, delayed, period, TimeUnit.MILLISECONDS );
      }

      /**
       * 以固定频率执行固定数量任务
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param runnable 任务
       * @param delayed 延时时间
       * @param period 任务之间间隔速率
       *
       * @return {@link ScheduledFuture#cancel(boolean)}结束任务
       */
      public static ScheduledFuture<?> scheduleAtFixedRate (
          Runnable runnable, long delayed, long period, int count ) {

            CountRunnable countRunnable = new CountRunnable();
            countRunnable.mCount = count;
            countRunnable.mRunnable = runnable;
            ScheduledFuture<?> future = SERVICE
                .scheduleAtFixedRate( countRunnable, delayed, period, TimeUnit.MILLISECONDS );
            countRunnable.mFuture = future;
            return future;
      }

      /**
       * 以固定频率执行任务,上一个任务结束后才计时
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param runnable 任务
       * @param delayed 延时时间
       * @param delay 任务间隔
       *
       * @return {@link ScheduledFuture#cancel(boolean)}结束任务
       */
      public static ScheduledFuture<?> scheduleWithFixedDelay (
          Runnable runnable, long delayed, long delay ) {

            return SERVICE
                .scheduleWithFixedDelay( runnable, delayed, delay, TimeUnit.MILLISECONDS );
      }

      /**
       * 以固定频率执行固定数量任务,上一个任务结束后才计时
       * <p>
       * 注意:该类单独使用一个线程,不要再该线程执行具体任务,具体任务应该交给{@link MainExecutor}或者{@link
       * PoolExecutor}执行,此处执行的只是定时触发任务
       *
       * @param runnable 任务
       * @param delayed 延时时间
       * @param delay 任务间隔
       *
       * @return {@link ScheduledFuture#cancel(boolean)}结束任务
       */
      public static ScheduledFuture<?> scheduleWithFixedDelay (
          Runnable runnable, long delayed, long delay, int count ) {

            CountRunnable countRunnable = new CountRunnable();
            countRunnable.mCount = count;
            countRunnable.mRunnable = runnable;
            ScheduledFuture<?> future = SERVICE
                .scheduleWithFixedDelay( countRunnable, delayed, delay, TimeUnit.MILLISECONDS );
            countRunnable.mFuture = future;
            return future;
      }

      public static void shutDown ( ) {

            SERVICE.shutdown();
      }

      public static void shutDownNow ( ) {

            SERVICE.shutdownNow();
      }

      private static class ScheduleThreadFactory implements ThreadFactory {

            @Override
            public Thread newThread ( @NonNull Runnable r ) {

                  return new ScheduleThread( r );
            }
      }

      private static class ScheduleThread extends Thread {

            private static AtomicInteger sInt = new AtomicInteger();

            ScheduleThread ( Runnable target ) {

                  super( target );
                  setName( "ScheduleThread-" + sInt.getAndAdd( 1 ) );
                  setPriority( Thread.MAX_PRIORITY );
            }
      }

      /**
       * 用于{@link ScheduleExecutor#scheduleAtFixedRate(Runnable, long, long)}和{@link
       * ScheduleExecutor#scheduleWithFixedDelay(Runnable, long, long)}包装任务
       */
      private static class CountRunnable implements Runnable {

            ScheduledFuture<?> mFuture;
            int                mCount;
            Runnable           mRunnable;

            @Override
            public void run ( ) {

                  mCount--;
                  if( mCount == 0 ) {
                        mFuture.cancel( true );
                  }
                  if( mRunnable != null ) {
                        mRunnable.run();
                  }
            }
      }
}
