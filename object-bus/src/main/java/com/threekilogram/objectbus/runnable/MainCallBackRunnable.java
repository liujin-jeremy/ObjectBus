package com.threekilogram.objectbus.runnable;

import tech.threekilogram.messengers.Messengers;
import tech.threekilogram.messengers.OnMessageReceiveListener;

/**
 * 用于后台处理任务,之后发送结果到主线程
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-10
 * @time: 22:52
 */
public abstract class MainCallBackRunnable implements Runnable, OnMessageReceiveListener {

      /**
       * 当任务处理完毕之后可以调用该方法,设置结果,结果将会被传送到主线程{@link #onResult(Object)}方法
       *
       * @param result 结果
       */
      @SuppressWarnings("WeakerAccess")
      protected void setResult ( Object result ) {

            Messengers.send( 1, result, this );
      }

      @Override
      public final void onReceive ( int what, Object extra ) {

            onResult( extra );
      }

      /**
       * 主线程接收结果
       *
       * @param result 结果{@link #setResult(Object)}
       */
      @SuppressWarnings("WeakerAccess")
      protected abstract void onResult ( Object result );
}
