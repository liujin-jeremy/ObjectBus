package com.threekilogram.objectbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
      private static final int          sCoreCount   = Runtime.getRuntime().availableProcessors();
      /**
       * 只有一个线程
       */
      public static final  StepExecutor SINGLE       = new TaskThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(),
          new ThreadsFactory( "single", Thread.NORM_PRIORITY )
      );
      /**
       * 最多只有cpu个数个线程,用于计算型任务
       */
      public static final  StepExecutor COMPUTATION  = new TaskThreadPoolExecutor(
          sCoreCount,
          sCoreCount,
          0L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(),
          new ThreadsFactory( "computation", Thread.NORM_PRIORITY )
      );
      /**
       * 最多有Integer.MAX_VALUE个线程执行任务,用于IO操作
       */
      public static final  StepExecutor IO           = new TaskThreadPoolExecutor(
          sCoreCount,
          Integer.MAX_VALUE,
          60,
          TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(),
          new ThreadsFactory( "io", Thread.NORM_PRIORITY - 1 )
      );
      /**
       * 总是使用新线程操作任务
       */
      public static final  StepExecutor NEW_THREAD   = new TaskThreadPoolExecutor(
          0,
          Integer.MAX_VALUE,
          0,
          TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(),
          new ThreadsFactory( "new", Thread.MIN_PRIORITY )
      );
      /**
       * android 主线程,UI线程
       */
      public static final  StepExecutor ANDROID_MAIN = new AndroidMainExecutor();

      /**
       * 时间调度,用于执行定时任务,不要再此线程中执行耗时任务,此线程应该只用于在一个时间点触发任务开始执行,耗时任务请使用{@link Threads#NEW_THREAD},
       * {@link Threads#SINGLE},{@link Threads#COMPUTATION},{@link Threads#IO}
       */
      public static final ScheduledThreadPoolExecutor SCHEDULE = new ScheduledThreadPoolExecutor(
          1,
          new ThreadsFactory( "schedule", Thread.MAX_PRIORITY )
      );

      /**
       * 按步骤执行的任务
       */
      public interface StepExecutor {

            /**
             * 任务执行之前回调
             *
             * @param t 执行任务的线程
             * @param r 任务
             */
            void beforeExecute ( Thread t, Runnable r );

            /**
             * 执行任务
             *
             * @param command 执行任务
             */
            void execute ( Runnable command );

            /**
             * 任务执行完毕后回调
             *
             * @param r 任务
             * @param t 异常, maybe null
             */
            void afterExecute ( Runnable r, Throwable t );
      }

      /**
       * thread pool executor handle runnable
       */
      private static class TaskThreadPoolExecutor extends ThreadPoolExecutor implements StepExecutor {

            private TaskThreadPoolExecutor (
                int corePoolSize,
                int maximumPoolSize,
                long keepAliveTime,
                TimeUnit unit,
                BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory ) {

                  super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory );
            }

            @Override
            public void beforeExecute ( Thread t, Runnable r ) {

                  super.beforeExecute( t, r );
            }

            @Override
            public void execute ( Runnable command ) {

                  super.execute( command );
            }

            @Override
            public void afterExecute ( Runnable r, Throwable t ) {

                  super.afterExecute( r, t );
                  if( r instanceof StepTask ) {
                        ( (Task) r ).startNext();
                  }
            }
      }

      /**
       * android 主线程执行任务
       */
      private static class AndroidMainExecutor implements StepExecutor {

            private MainHandler mMainHandler;

            private AndroidMainExecutor ( ) {

                  mMainHandler = new MainHandler( this );
            }

            @Override
            public void beforeExecute ( Thread t, Runnable r ) { }

            @Override
            public void execute ( @NonNull Runnable command ) {

                  Message message = mMainHandler.obtainMessage();
                  message.obj = command;
                  mMainHandler.sendMessage( message );
            }

            @Override
            public void afterExecute ( Runnable r, Throwable t ) {

                  if( r instanceof Task ) {
                        ( (Task) r ).startNext();
                  }
            }
      }

      /**
       * 主线程handler
       */
      private static class MainHandler extends Handler {

            private AndroidMainExecutor mExecutor;

            private MainHandler ( AndroidMainExecutor executor ) {

                  super( Looper.getMainLooper() );
                  mExecutor = executor;
            }

            @Override
            public void handleMessage ( Message msg ) {

                  Runnable command = (Runnable) msg.obj;
                  //mExecutor.beforeExecute( Thread.currentThread(), command );
                  Throwable throwable = null;
                  try {
                        command.run();
                  } catch(Exception e) {
                        e.printStackTrace();
                        throwable = e;
                  }
                  mExecutor.afterExecute( command, throwable );
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
}
