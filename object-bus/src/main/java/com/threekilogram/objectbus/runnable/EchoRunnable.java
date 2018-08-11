package com.threekilogram.objectbus.runnable;

import android.util.SparseArray;
import java.util.concurrent.atomic.AtomicInteger;
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
public abstract class EchoRunnable implements Runnable {

      private static Receiver sReceiver = new Receiver();

      /**
       * 当任务处理完毕之后可以调用该方法,设置结果,结果将会被传送到主线程{@link #onResult(Object)}方法
       *
       * @param result 结果
       */
      @SuppressWarnings("WeakerAccess")
      protected void setResult ( Object result ) {

            sReceiver.sendResult( this, result );
      }

      /**
       * 主线程接收结果
       *
       * @param result 结果{@link #setResult(Object)}
       */
      @SuppressWarnings("WeakerAccess")
      protected abstract void onResult ( Object result );

      /**
       * 辅助转发消息
       */
      private static class Receiver implements OnMessageReceiveListener {

            private AtomicInteger       index      = new AtomicInteger( 1 );
            private SparseArray<Holder> mCallBacks = new SparseArray<>();

            void sendResult ( EchoRunnable runnable, Object result ) {

                  Holder holder = new Holder( runnable, result );

                  int index = this.index.getAndAdd( 2 );
                  mCallBacks.put( index, holder );
                  Messengers.send( index, this );
            }

            @Override
            public void onReceive ( int what, Object extra ) {

                  Holder holder = mCallBacks.get( what );
                  if( holder != null ) {

                        mCallBacks.remove( what );
                        holder.mEchoRunnable.onResult( holder.mResult );
                  }
            }

            private class Holder {

                  private EchoRunnable mEchoRunnable;
                  private Object       mResult;

                  Holder ( EchoRunnable echoRunnable, Object result ) {

                        mEchoRunnable = echoRunnable;
                        mResult = result;
                  }
            }
      }
}
