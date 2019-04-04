package com.threekilogram.objectbus;

import java.util.concurrent.TimeUnit;

/**
 * @author Liujin 2019/2/23:11:19:26
 */
class ScheduledTask implements StepTask {

      /**
       * 延时
       */
      private long     mTimeMillDelayed;
      /**
       * 任务
       */
      private StepTask mStepTask;

      ScheduledTask ( long timeMillDelayed, StepTask stepTask ) {

            mTimeMillDelayed = timeMillDelayed;
            mStepTask = stepTask;
      }

      @Override
      public void run ( ) {

            mStepTask.start();
      }

      @Override
      public void setNext ( StepTask task ) {

            mStepTask.setNext( task );
      }

      @Override
      public void start ( ) {

            if( mTimeMillDelayed > 0 ) {

                  Threads.SCHEDULE.schedule( this, mTimeMillDelayed, TimeUnit.MILLISECONDS );
                  mTimeMillDelayed = -1;
            }
      }

      @Override
      public void startNext ( ) {

            if( mTimeMillDelayed > 0 ) {

                  Threads.SCHEDULE.schedule( this, mTimeMillDelayed, TimeUnit.MILLISECONDS );
                  mTimeMillDelayed = -1;
            }
      }
}
