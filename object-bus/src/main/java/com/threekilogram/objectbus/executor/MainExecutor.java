package com.threekilogram.objectbus.executor;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * 用于在主线程执行任务
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:46
 */
public class MainExecutor {

      private static MainHandler sHandler = new MainHandler();

      public static void execute ( Runnable runnable ) {

            if( runnable == null ) {

                  return;
            }

            sHandler.handleRunnable( runnable );
      }

      public static void execute ( Runnable runnable, int delayed ) {

            if( runnable == null ) {

                  return;
            }

            sHandler.handleRunnable( runnable, delayed );
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

                  Runnable obj = (Runnable) msg.obj;
                  obj.run();
            }

            void handleRunnable ( Runnable runnable ) {

                  Message message = Message.obtain();
                  message.obj = runnable;
                  sendMessage( message );
            }

            void handleRunnable ( Runnable runnable, int delayed ) {

                  Message message = Message.obtain();
                  message.obj = runnable;
                  sendMessageDelayed( message, delayed );
            }
      }
}
