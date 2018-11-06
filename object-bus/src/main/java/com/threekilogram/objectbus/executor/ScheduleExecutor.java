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
 * @author Liujin 2018-11-06:20:55
 */
public class ScheduleExecutor {

      private ScheduleExecutor ( ) { }

      private static final ScheduledExecutorService SERVICE = new ScheduledThreadPoolExecutor(
          1,
          new ScheduleThreadFactory()
      );

      public static void schedule ( Runnable runnable, long delayed ) {

            SERVICE.schedule( runnable, delayed, TimeUnit.MILLISECONDS );
      }

      public static <T> ScheduledFuture<T> schedule ( Callable<T> callable, long delayed ) {

            return SERVICE.schedule( callable, delayed, TimeUnit.MILLISECONDS );
      }

      public static ScheduledFuture<?> scheduleAtFixedRate (
          Runnable runnable, long delayed, long period ) {

            return SERVICE.scheduleAtFixedRate( runnable, delayed, period, TimeUnit.MILLISECONDS );
      }

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

      public static ScheduledFuture<?> scheduleWithFixedDelay (
          Runnable runnable, long delayed, long delay ) {

            return SERVICE
                .scheduleWithFixedDelay( runnable, delayed, delay, TimeUnit.MILLISECONDS );
      }

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
