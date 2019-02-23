package com.threekilogram.objectbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工具类
 *
 * @author Liujin 2019/2/22:23:01:36
 */
public class Threads {

      /**
       * 根据cpu计算核心数
       */
      private static final int      sCoreCount   = Runtime.getRuntime().availableProcessors();
      /**
       * 只有一个线程
       */
      public static final  Executor SINGLE       = new TaskThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(),
          new ThreadsFactory( "single", Thread.NORM_PRIORITY )
      );
      /**
       * 最多只有cpu个数个线程
       */
      public static final  Executor COMPUTATION  = new TaskThreadPoolExecutor(
          sCoreCount,
          sCoreCount,
          0L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(),
          new ThreadsFactory( "computation", Thread.NORM_PRIORITY )
      );
      /**
       * 最多有Integer.MAX_VALUE个线程执行任务
       */
      public static final  Executor IO           = new TaskThreadPoolExecutor(
          sCoreCount,
          Integer.MAX_VALUE,
          60,
          TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(),
          new ThreadsFactory( "io", Thread.NORM_PRIORITY - 1 )
      );
      /**
       * 总使用新线程
       */
      public static final  Executor NEW_THREAD   = new TaskThreadPoolExecutor(
          0,
          Integer.MAX_VALUE,
          0,
          TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(),
          new ThreadsFactory( "new", Thread.MIN_PRIORITY )
      );
      /**
       * android 主线程
       */
      public static final  Executor ANDROID_MAIN = new AndroidExecutor();

      /**
       * 时间调度
       */
      public static final ScheduledThreadPoolExecutor SCHEDULE = new ScheduledThreadPoolExecutor(
          1,
          new ThreadsFactory( "schedule", Thread.MAX_PRIORITY )
      );

      /**
       * thread pool executor handle runnable
       */
      private static class TaskThreadPoolExecutor extends ThreadPoolExecutor {

            private TaskThreadPoolExecutor (
                int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> workQueue ) {

                  super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue );
            }

            private TaskThreadPoolExecutor (
                int corePoolSize,
                int maximumPoolSize,
                long keepAliveTime,
                TimeUnit unit,
                BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory ) {

                  super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory );
            }

            private TaskThreadPoolExecutor (
                int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler ) {

                  super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler );
            }

            private TaskThreadPoolExecutor (
                int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler ) {

                  super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler );
            }

            @Override
            protected void beforeExecute ( Thread t, Runnable r ) {

                  super.beforeExecute( t, r );
            }

            @Override
            protected void afterExecute ( Runnable r, Throwable t ) {

                  super.afterExecute( r, t );
                  if( r instanceof StepTask ) {
                        ( (Task) r ).startNext();
                  }
            }
      }

      /**
       * set thread name
       */
      private static class ThreadsFactory implements ThreadFactory {

            /**
             * 线程前缀
             */
            private String        mThreadNamePre;
            /**
             * 线程优先级
             */
            private int           mPriority;
            /**
             * 统计线程数量
             */
            private AtomicInteger mCount = new AtomicInteger();

            ThreadsFactory ( String threadNamePre, int priority ) {

                  mThreadNamePre = threadNamePre;
                  mPriority = priority;
            }

            @Override
            public Thread newThread ( @NonNull Runnable r ) {

                  Thread thread = new Thread( r );
                  thread.setName( mThreadNamePre + "-" + mCount.getAndAdd( 1 ) );
                  thread.setPriority( mPriority );
                  return thread;
            }
      }

      /**
       * android 主线程执行任务
       */
      private static class AndroidExecutor implements Executor {

            private MainHandler mMainHandler = new MainHandler();

            @Override
            public void execute ( @NonNull Runnable command ) {

                  Message message = mMainHandler.obtainMessage();
                  message.obj = command;
                  mMainHandler.sendMessage( message );
            }
      }

      /**
       * 主线程
       */
      private static class MainHandler extends Handler {

            private MainHandler ( ) {

                  super( Looper.getMainLooper() );
            }

            @Override
            public void handleMessage ( Message msg ) {

                  Runnable command = (Runnable) msg.obj;
                  command.run();
                  if( command instanceof Task ) {
                        ( (Task) command ).startNext();
                  }
            }
      }
}
