package com.threekilogram.objectbus.executor;

import com.threekilogram.objectbus.executor.PoolExecutor.AppThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Liujin 2018-11-06:20:55
 */
public class ScheduleExecutor {

      private ScheduleExecutor ( ) { }

      private static final ScheduledExecutorService SERVICE = new ScheduledThreadPoolExecutor(
          1,
          new AppThreadFactory()
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

      public static ScheduledFuture<?> scheduleWithFixedDelay (
          Runnable runnable, long delayed, long delay ) {

            return SERVICE
                .scheduleWithFixedDelay( runnable, delayed, delay, TimeUnit.MILLISECONDS );
      }
}
