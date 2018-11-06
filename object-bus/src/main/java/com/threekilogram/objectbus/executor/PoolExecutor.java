package com.threekilogram.objectbus.executor;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 该类负责使用线程池执行后台任务
 *
 * @author wuxio 2018-04-30:1:06
 */
public class PoolExecutor {

      /**
       * thread pool,默认4线程,比正常优先级低两个级别
       */
      private static ThreadPoolExecutor sPoolExecutor;

      static {

            sPoolExecutor = new ThreadPoolExecutor(
                9,
                Integer.MAX_VALUE,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new AppThreadFactory()
            );
      }

      public static void setMaximumPoolSize ( int size ) {

            sPoolExecutor.setMaximumPoolSize( size );
      }

      public static int getMaximumPoolSize ( ) {

            return sPoolExecutor.getMaximumPoolSize();
      }

      public static void setCorePoolSize ( int size ) {

            sPoolExecutor.setCorePoolSize( size );
      }

      public static int getCorePoolSize ( ) {

            return sPoolExecutor.getCorePoolSize();
      }

      public static void setKeepAliveTime ( int time ) {

            sPoolExecutor.setKeepAliveTime( time, TimeUnit.SECONDS );
      }

      public static long getKeepAliveTime ( TimeUnit unit ) {

            return sPoolExecutor.getKeepAliveTime( unit );
      }

      /**
       * 获取线程池对象
       *
       * @return 线程池对象
       */
      public static ThreadPoolExecutor getPoolExecutor ( ) {

            return sPoolExecutor;
      }

      /**
       * 设置线程池对象
       */
      public static void setPoolExecutor ( ThreadPoolExecutor poolExecutor ) {

            sPoolExecutor = poolExecutor;
      }

      /**
       * 后台执行任务
       *
       * @param runnable 执行的任务
       */
      public static void execute ( @NonNull Runnable runnable ) {

            /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

            try {
                  sPoolExecutor.execute( runnable );
            } catch(Exception e) {
                  e.printStackTrace();
            }
      }

      /**
       * 后台执行一组任务,并且在当期线程等待执行完毕
       *
       * @param runnableList need to do
       */
      @SuppressWarnings("unchecked")
      public static void execute ( List<Runnable> runnableList ) {

            final CountDownLatch latch = new CountDownLatch( runnableList.size() );
            for( Runnable runnable : runnableList ) {

                  execute( ( ) -> {
                        runnable.run();
                        latch.countDown();
                  } );
            }
            try {
                  latch.await();
            } catch(InterruptedException e) {
                  e.printStackTrace();
            }
      }

      /**
       * 后台执行一组任务
       *
       * @param runnableList need to do
       */

      public static void executeInOrder ( final List<Runnable> runnableList ) {

            CountDownLatch latch = new CountDownLatch( 1 );
            execute( ( ) -> {

                  int size = runnableList.size();
                  for( int i = 0; i < size; i++ ) {

                        Runnable runnable = runnableList.get( i );
                        runnable.run();
                  }
                  latch.countDown();
            } );

            try {
                  latch.await();
            } catch(InterruptedException e) {
                  e.printStackTrace();
            }
      }

      /**
       * 后台执行任务,并获取结果
       *
       * @param callable callable
       * @param <T> type
       *
       * @return use {@link Future#get()} to get result
       */
      public static <T, C extends Callable<T>> Future<T> submit ( C callable ) {

            /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

            try {

                  return sPoolExecutor.submit( callable );
            } catch(Exception e) {
                  e.printStackTrace();
            }

            return null;
      }

      /**
       * 后台执行任务,并获取结果
       *
       * @param callable callable
       * @param <T> result type
       *
       * @return result, or null if Exception
       */
      public static <T, C extends Callable<T>> T submitAndGet ( C callable ) {

            /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

            try {

                  Future<T> future = sPoolExecutor.submit( callable );
                  return future.get();
            } catch(Exception e) {

                  e.printStackTrace();
            }

            return null;
      }

      /**
       * 后台执行一组任务,并获取结果
       *
       * @param callableList need to run
       * @param <T> result type
       *
       * @return use {@link CompletionService#take()} to get {@link Future#get()}
       */
      public static <T, C extends Callable<T>> CompletionService<T> submit (
          List<C> callableList ) {

            ExecutorCompletionService<T> completionService = new ExecutorCompletionService<>(
                sPoolExecutor );

            int size = callableList.size();
            for( int i = 0; i < size; i++ ) {

                  try {
                        Callable<T> callable = callableList.get( i );
                        completionService.submit( callable );
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }
            return completionService;
      }

      /**
       * 后台执行一组任务,并获取结果
       */
      public static <T, C extends Callable<T>> List<T> submitAndGet ( List<C> callableList ) {

            int size = callableList.size();
            final List<T> results = new ArrayList<>( size );
            final CountDownLatch latch = new CountDownLatch( size );

            for( int i = 0; i < size; i++ ) {
                  Callable<T> callable = callableList.get( i );
                  execute( ( ) -> {

                        try {
                              T call = callable.call();
                              synchronized(results) {
                                    results.add( call );
                              }
                        } catch(Exception e) {
                              e.printStackTrace();
                        }
                        latch.countDown();
                  } );
            }

            try {
                  latch.await();
            } catch(InterruptedException e) {
                  e.printStackTrace();
            }

            return results;
      }

      /**
       * 不再添加任务,执行完所有任务
       */
      public static void shutDown ( ) {

            if( !sPoolExecutor.isShutdown() ) {
                  sPoolExecutor.shutdownNow();
            }
      }

      /**
       * 立即停止,未执行的不再执行,正在执行的任务如果判断{@link Thread#isInterrupted()}可以得到true;如果有抛出异常的代码则任务结束
       */
      public static void shutDownNow ( ) {

            sPoolExecutor.shutdownNow();
      }

      public static class AppThreadFactory implements ThreadFactory {

            @Override
            public Thread newThread ( @NonNull Runnable r ) {

                  return new AppThread( r );
            }
      }

      public static class AppThread extends Thread {

            private static AtomicInteger sInt = new AtomicInteger();

            AppThread ( Runnable target ) {

                  super( target );
                  setName( "PoolThread-" + sInt.getAndAdd( 1 ) );
                  setPriority( Thread.NORM_PRIORITY - 2 );
            }
      }
}
