package tech.liujin.objectbus;

import tech.liujin.objectbus.Threads.StepExecutor;

/**
 * @author Liujin 2019/2/22:23:21:08
 */
public class ObjectBus {

      private Object   mObj;
      private StepTask mCurrent;
      private StepTask mFirst;

      /**
       * @param runnable 需要执行的任务
       * @param executor 在哪个线程执行任务 {@link Threads#SINGLE},
       *     {@link Threads#COMPUTATION},{@link Threads#IO},
       *     {@link Threads#NEW_THREAD},
       *     {@link Threads#ANDROID_MAIN}
       *
       * @return self
       */
      public ObjectBus to ( Runnable runnable, StepExecutor executor ) {

            Task task = new Task( runnable, executor );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus toSingle ( Runnable runnable ) {

            to( runnable, Threads.SINGLE );
            return this;
      }

      public ObjectBus toComputation ( Runnable runnable ) {

            to( runnable, Threads.COMPUTATION );
            return this;
      }

      public ObjectBus toIO ( Runnable runnable ) {

            to( runnable, Threads.IO );
            return this;
      }

      public ObjectBus toNew ( Runnable runnable ) {

            to( runnable, Threads.NEW_THREAD );
            return this;
      }

      public ObjectBus toAndroidMain ( Runnable runnable ) {

            to( runnable, Threads.ANDROID_MAIN );
            return this;
      }

      public ObjectBus schedule ( Runnable runnable, StepExecutor executor, long delayed ) {

            Task task = new Task( runnable, executor );
            if( delayed < 0 ) {
                  delayed = 0;
            }
            ScheduledTask scheduledTask = new ScheduledTask( delayed, task );
            if( mCurrent != null ) {
                  mCurrent.setNext( scheduledTask );
            } else {
                  mFirst = scheduledTask;
            }
            mCurrent = scheduledTask;
            return this;
      }

      public ObjectBus scheduleToSingle ( Runnable runnable, long delayed ) {

            schedule( runnable, Threads.SINGLE, delayed );
            return this;
      }

      public ObjectBus scheduleToComputation ( Runnable runnable, long delayed ) {

            schedule( runnable, Threads.COMPUTATION, delayed );
            return this;
      }

      public ObjectBus scheduleToIO ( Runnable runnable, long delayed ) {

            schedule( runnable, Threads.IO, delayed );
            return this;
      }

      public ObjectBus scheduleToNew ( Runnable runnable, long delayed ) {

            schedule( runnable, Threads.NEW_THREAD, delayed );
            return this;
      }

      public ObjectBus scheduleToAndroidMain ( Runnable runnable, long delayed ) {

            schedule( runnable, Threads.ANDROID_MAIN, delayed );
            return this;
      }

      public void start ( ) {

            if( mFirst != null ) {
                  mFirst.start();
            }
      }

      /**
       * 保存一个变量
       *
       * @param obj 需要保存的变量
       */
      public void saveObj ( Object obj ) {

            mObj = obj;
      }

      /**
       * 获取保存的变量
       *
       * @return 保存的变量
       */
      public Object getObj ( ) {

            return mObj;
      }
}
