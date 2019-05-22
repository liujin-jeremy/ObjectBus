package tech.liujin.objectbus.timer;

import android.support.annotation.IntDef;
import java.util.concurrent.TimeUnit;
import tech.liujin.objectbus.Threads;
import tech.liujin.objectbus.Threads.StepExecutor;

/**
 * @author Liujin 2019/5/20:11:46:21
 */
public class DelayTask implements Runnable {

      public static final int PREPARED = 0;
      public static final int RUNNING  = 1;
      public static final int PAUSED   = 2;
      public static final int FINISHED = 3;

      @State
      private volatile int mState = PREPARED;

      private int          mRunCount;
      private Runnable     mRunnable;
      private Iterator     mIterator;
      private StepExecutor mWhich;

      @IntDef(value = { PREPARED, RUNNING, PAUSED, FINISHED })
      public @interface State { }

      /**
       * @param runnable 任务
       * @param which 运行线程
       * @param delayedMilliseconds 延时时间
       */
      public DelayTask ( Runnable runnable, StepExecutor which, int delayedMilliseconds ) {

            this( runnable, which, delayedMilliseconds, 1 );
      }

      /**
       * @param runnable 任务
       * @param which 运行线程
       * @param delayedMilliseconds 延时时间
       * @param count 运行次数
       */
      public DelayTask ( Runnable runnable, StepExecutor which, int delayedMilliseconds, int count ) {

            mRunnable = runnable;
            mWhich = which;
            mIterator = new FixDelayedIterator( delayedMilliseconds, count );
      }

      /**
       * @param runnable 任务
       * @param which 运行线程
       * @param delayedMilliseconds 任务延时间隔数组
       */
      public DelayTask ( Runnable runnable, StepExecutor which, int[] delayedMilliseconds ) {

            mRunnable = runnable;
            mWhich = which;
            mIterator = new ArrayDelayedIterator( delayedMilliseconds );
      }

      @Override
      public void run ( ) {

            if( mRunnable != null ) {
                  mWhich.execute( mRunnable );
                  mRunCount++;
            }
            if( mState == PAUSED ) {
                  return;
            }
            startNext();
      }

      /**
       * 开始
       */
      public void start ( ) {

            startNext();
      }

      /**
       * 结束运行
       */
      public void stop ( ) {

            while( mIterator.hasNext() ) {
                  /* do nothing */
                  mIterator.next();
            }
            startNext();
      }

      /**
       * 暂停运行
       */
      public void pause ( ) {

            mState = PAUSED;
      }

      /**
       * 恢复运行
       */
      public void resume ( ) {

            if( mState == PAUSED ) {
                  mState = RUNNING;
                  startNext();
            }
      }

      /**
       * 重置状态,从起始重新开始
       */
      public void reset ( ) {

            mIterator.reset();
      }

      private void startNext ( ) {

            if( mIterator.hasNext() ) {
                  int next = mIterator.next();
                  Threads.SCHEDULE.schedule( this, next, TimeUnit.MILLISECONDS );
                  mState = RUNNING;
                  return;
            }
            mState = FINISHED;
      }

      /**
       * @return 获取运行状态
       */
      @State
      public int getState ( ) {

            return mState;
      }

      /**
       * @return 获取运行次数
       */
      public int getRunCount ( ) {

            return mRunCount;
      }

      /**
       * 获取延时时间
       */
      public interface Iterator {

            boolean hasNext ( );

            int next ( );

            void reset ( );
      }

      /**
       * 固定间隔延时
       */
      public class FixDelayedIterator implements Iterator {

            private int mCount;
            private int mCurrent;
            private int mDelayed;

            public FixDelayedIterator ( int delayed, int count ) {

                  mCount = count;
                  mDelayed = delayed;
            }

            @Override
            public boolean hasNext ( ) {

                  return mCurrent < mCount;
            }

            @Override
            public int next ( ) {

                  mCurrent++;
                  return mDelayed;
            }

            @Override
            public void reset ( ) {

                  mCurrent = 0;
            }
      }

      /**
       * 自定义间隔延时
       */
      public class ArrayDelayedIterator implements Iterator {

            private int   mIndex;
            private int[] mDelayed;

            public ArrayDelayedIterator ( int[] delayed ) {

                  mDelayed = delayed;
            }

            @Override
            public boolean hasNext ( ) {

                  return mIndex < mDelayed.length;
            }

            @Override
            public int next ( ) {

                  return mDelayed[ mIndex++ ];
            }

            @Override
            public void reset ( ) {

                  mIndex = 0;
            }
      }
}
