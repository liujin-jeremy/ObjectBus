package com.threekilogram.objectbus.runnable;

/**
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-10
 * @time: 22:37
 */

/**
 * 执行一组任务
 *
 * @author liujin
 */
public abstract class RunnableGroup implements Runnable {

      private Runnable[] mRunnableList;

      public RunnableGroup ( Runnable... runnableList ) {

            mRunnableList = runnableList;
      }

      @Override
      public void run ( ) {

            for( Runnable t : mRunnableList ) {
                  t.run();
            }
      }
}
