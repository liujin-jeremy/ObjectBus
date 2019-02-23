package com.threekilogram.objectbus.anth;

import java.util.concurrent.Executor;

/**
 * @author Liujin 2019/2/23:0:29:41
 */
public class Task implements StepTask {

      private Executor mWhich;
      private Runnable mRunnable;
      private StepTask mNext;

      public Task ( Runnable runnable, Executor which ) {

            mWhich = which;
            mRunnable = runnable;
      }

      @Override
      public void run ( ) {

            mRunnable.run();
      }

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
