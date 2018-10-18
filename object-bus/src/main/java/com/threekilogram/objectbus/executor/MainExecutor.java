package com.threekilogram.objectbus.executor;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于在主线程执行任务
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:46
 */
public class MainExecutor {

      private static MainHandler           sHandler     = new MainHandler();
      private static AtomicInteger         sIndexCreate = new AtomicInteger();
      private static SparseArray<Runnable> sRunnable    = new SparseArray<>();

      public static void execute ( Runnable runnable ) {

            if( runnable == null ) {

                  return;
            }

            int index = sIndexCreate.getAndAdd( 1 );
            sRunnable.put( index, runnable );

            sHandler.handleRunnable( index );
      }

      public static void execute ( Runnable runnable, int delayed ) {

            if( runnable == null ) {

                  return;
            }

            int index = sIndexCreate.getAndAdd( 1 );
            sRunnable.put( index, runnable );

            sHandler.handleRunnable( index, delayed );
      }

      /**
       * 处理主线程任务
       */
      private static class MainHandler extends Handler {

            private MainHandler ( ) {

                  super( Looper.getMainLooper() );
            }

            @Override
            public void handleMessage ( Message msg ) {

                  int index = msg.what;
                  Runnable runnable = sRunnable.get( index );

                  if( runnable != null ) {

                        sRunnable.delete( index );
                        runnable.run();
                  }
            }

            void handleRunnable ( int index ) {

                  sendEmptyMessage( index );
            }

            void handleRunnable ( int index, int delayed ) {

                  sendEmptyMessageDelayed( index, delayed );
            }
      }
}
