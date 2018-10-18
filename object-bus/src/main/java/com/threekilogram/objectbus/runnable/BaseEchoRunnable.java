package com.threekilogram.objectbus.runnable;

import tech.threekilogram.executor.MainExecutor;

/**
 * 用于后台处理任务,之后发送结果到主线程
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-10
 * @time: 22:52
 */
public abstract class BaseEchoRunnable<V> implements Runnable {

      /**
       * 当任务处理完毕之后可以调用该方法,设置结果,结果将会被传送到主线程{@link #onResult(Object)}方法
       *
       * @param result 结果
       */
      @SuppressWarnings("WeakerAccess")
      protected void setResult ( final V result ) {

            MainExecutor.execute( new Runnable() {

                  @Override
                  public void run ( ) {

                        onResult( result );
                  }
            } );
      }

      /**
       * 当任务处理完毕之后可以调用该方法,设置结果,结果将会被传送到主线程{@link #onResult(Object)}方法
       *
       * @param runnable 设置结束任务
       */
      @SuppressWarnings("WeakerAccess")
      protected void setResult ( Runnable runnable ) {

            MainExecutor.execute( runnable );
      }

      /**
       * 主线程接收结果
       *
       * @param result 结果{@link #setResult(Object)}
       */
      @SuppressWarnings("WeakerAccess")
      protected abstract void onResult ( V result );
}
