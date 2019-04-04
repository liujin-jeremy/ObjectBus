package com.threekilogram.objectbus;

import com.threekilogram.objectbus.Threads.StepExecutor;

/**
 * @author Liujin 2019/2/23:0:29:41
 */
class Task implements StepTask {

      private StepExecutor mWhich;
      private Runnable     mRunnable;
      private StepTask     mNext;

      Task ( Runnable runnable, StepExecutor which ) {

            mWhich = which;
            mRunnable = runnable;
      }

      @Override
      public void run ( ) {

            mRunnable.run();
      }

      @Override
      public void setNext ( StepTask next ) {

            mNext = next;
      }

      @Override
      public void start ( ) {

            mWhich.execute( this );
      }

      @Override
      public void startNext ( ) {

            if( mNext != null ) {
                  mNext.start();
            }
      }
}
