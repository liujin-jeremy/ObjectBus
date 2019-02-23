package com.threekilogram.objectbus;

import java.util.concurrent.Executor;

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
      public ObjectBus to ( Runnable runnable, Executor executor ) {

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

            Task task = new Task( runnable, Threads.SINGLE );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus toComputation ( Runnable runnable ) {

            Task task = new Task( runnable, Threads.COMPUTATION );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus toIO ( Runnable runnable ) {

            Task task = new Task( runnable, Threads.IO );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus toNew ( Runnable runnable ) {

            Task task = new Task( runnable, Threads.NEW_THREAD );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus toAndroidMain ( Runnable runnable ) {

            Task task = new Task( runnable, Threads.ANDROID_MAIN );
            if( mCurrent != null ) {
                  mCurrent.setNext( task );
            } else {
                  mFirst = task;
            }
            mCurrent = task;
            return this;
      }

      public ObjectBus schedule ( Runnable runnable, long delayed, Executor executor ) {

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

            Task task = new Task( runnable, Threads.SINGLE );
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

      public ObjectBus scheduleToComputation ( Runnable runnable, long delayed ) {

            Task task = new Task( runnable, Threads.COMPUTATION );
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

      public ObjectBus scheduleToIO ( Runnable runnable, long delayed ) {

            Task task = new Task( runnable, Threads.IO );
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

      public ObjectBus scheduleToNew ( Runnable runnable, long delayed ) {

            Task task = new Task( runnable, Threads.NEW_THREAD );
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

      public ObjectBus scheduleToAndroidMain ( Runnable runnable, long delayed ) {

            Task task = new Task( runnable, Threads.ANDROID_MAIN );
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

      public void start ( ) {

            if( mFirst != null ) {
                  mFirst.start();
            }
      }
}
