package com.threekilogram.objectbus.executor;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 该类负责使用线程池执行后台任务
 *
 * @author wuxio 2018-04-30:1:06
 */
public class PoolThreadExecutor {

      /**
       * thread pool, 核心3线程,最多12线程,默认比正常优先级低一个级别
       */
      private static ThreadPoolExecutor sPoolExecutor;

      static {

            /* init self */
            init();
      }

      private static void init ( ) {

            /* 防止重复初始化 */

            if( sPoolExecutor != null ) {
                  return;
            }

            sPoolExecutor = new ThreadPoolExecutor(
                3,
                12,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new AppThreadFactory()
            );
      }

      /**
       * 后台执行任务,如果需要监听执行情况请使用{@link com.threekilogram.objectbus.runnable.Executable}
       *
       * @param runnable 执行的任务
       */
      public static void execute ( @NonNull Runnable runnable ) {

            /* 使用try..catch 增加程序健壮性,防止线程意外结束 */

            try {
                  sPoolExecutor.execute( runnable );
            } catch(Exception e) {

                  if( sPoolExecutor == null ) {
                        throw new RuntimeException( " you should  call init() first" );
                  } else {
                        e.printStackTrace();
                  }
            }
      }

      /**
       * submit a callable
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

                  if( sPoolExecutor == null ) {
                        throw new RuntimeException( " you should  call init() first" );
                  } else {
                        e.printStackTrace();
                  }
            }

            return null;
      }

      /**
       * submit a callable, and return result
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

                  if( sPoolExecutor == null ) {
                        throw new RuntimeException( " you should  call init() first" );
                  } else {
                        e.printStackTrace();
                  }
            }

            return null;
      }

      /**
       * submit a list of callable to run
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
       * submit a list of runnable to run at same time
       *
       * @param runnableList need to do
       */
      @SuppressWarnings("unchecked")
      public static <T extends Runnable> void execute ( List<T> runnableList ) {

            ExecutorCompletionService completionService =
                new ExecutorCompletionService( sPoolExecutor );

            int size = runnableList.size();
            for( int i = 0; i < size; i++ ) {

                  Runnable runnable = runnableList.get( i );
                  completionService.submit( runnable, null );
            }

            for( int i = 0; i < size; i++ ) {

                  try {

                        completionService.take().get();
                  } catch(Exception e) {

                        e.printStackTrace();
                  }
            }
      }

      public static <T, C extends Callable<T>> List<T> submitAndGet ( List<C> callableList ) {

            ExecutorCompletionService<T> completionService = new ExecutorCompletionService<>(
                sPoolExecutor );

            int size = callableList.size();
            for( int i = 0; i < size; i++ ) {
                  Callable<T> callable = callableList.get( i );
                  completionService.submit( callable );
            }

            List<T> results = new ArrayList<>( size );

            for( int i = 0; i < size; i++ ) {

                  try {

                        T t = completionService.take().get();
                        results.add( t );
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }

            return results;
      }

      //============================ 结束任务 ============================

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

      //============================ 配置类 ============================

      private static class AppThreadFactory implements ThreadFactory {

            @Override
            public Thread newThread ( @NonNull Runnable r ) {

                  return new AppThread( r );
            }
      }

      private static class AppThread extends Thread {

            private static AtomicInteger sInt = new AtomicInteger();

            AppThread ( Runnable target ) {

                  super( target );
                  setName( "PoolThread-" + sInt.getAndAdd( 1 ) );
                  setPriority( Thread.NORM_PRIORITY - 1 );
            }
      }
}
