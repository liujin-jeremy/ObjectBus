package com.threekilogram.objectbus.runnable;

/**
 * @author liujin
 */
public abstract class Executable implements Runnable {

      @Override
      public void run ( ) {

            onStart();
            onExecute();
            onFinish();
      }

      /**
       * 任务开始,{@link #onExecute()}之前回调
       */
      public abstract void onStart ( );

      /**
       * 执行任务
       */
      public abstract void onExecute ( );

      /**
       * {@link #onExecute()}正常结束之后,调用,表明任务执行完毕
       */
      public abstract void onFinish ( );
}
