package com.threekilogram.objectbus.action;

import android.util.ArrayMap;
import com.threekilogram.objectbus.executor.MainThreadExecutor;
import com.threekilogram.objectbus.executor.PoolThreadExecutor;
import com.threekilogram.objectbus.message.Messengers;
import com.threekilogram.objectbus.message.OnMessageReceiveListener;
import com.threekilogram.objectbus.runnable.Executable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:19
 */
public class Actions {

      private static final int MAIN_THREAD = BusExecute.RUN_IN_MAIN_THREAD;
      private static final int POOL_THREAD = BusExecute.RUN_IN_POOL_THREAD;
      /**
       * 所有任务
       */
      private final RunnableContainer mRunnableContainer;
      /**
       * 是否正在运行
       */
      private final AtomicBoolean isLooping = new AtomicBoolean();
      /**
       * 是否暂停了
       */
      private final AtomicBoolean isPaused  = new AtomicBoolean();
      private ArrayMap<String, Object> mResults;

      private Actions ( RunnableContainer container ) {

            mRunnableContainer = container;
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务
       */
      public static Actions newListActions ( ) {

            return new Actions( new ListRunnableContainer() );
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务
       */
      public static Actions newQueueActions ( ) {

            return new Actions( new QueueRunnableContainer() );
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务,有固定任务上限
       */
      public static Actions newFixSizeQueueActions ( int maxSize ) {

            return new Actions( new FixSizeQueueRunnableContainer( maxSize ) );
      }

      /**
       * 保存一个变量
       *
       * @param key key
       * @param result 变量
       */
      public void setResult ( String key, Object result ) {

            mResults.put( key, result );
      }

      /**
       * 读取保存的变量
       *
       * @param key key
       * @param <T> 变量类型
       *
       * @return 变量, 如果没有该key返回null
       */
      @SuppressWarnings("unchecked")
      public <T> T getResult ( String key ) {

            Object result = mResults.get( key );

            return result == null ? null : (T) result;
      }

      /**
       * 读取保存的变量,并且移除该变量
       *
       * @param key key
       * @param <T> 变量类型
       *
       * @return 变量, 如果没有该key返回null
       */
      @SuppressWarnings("unchecked")
      public <T> T getResultOff ( String key ) {

            Object result = mResults.remove( key );

            return result == null ? null : (T) result;
      }

      /**
       * 主线程执行任务
       *
       * @param runnable 任务
       *
       * @return 链式调用
       */
      public Actions toMain ( Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            BusExecute execute = new BusExecute();
            execute.mThread = MAIN_THREAD;
            execute.mActions = this;
            execute.mRunnable = runnable;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 主线程执行任务
       *
       * @param runnable 任务
       *
       * @return 链式调用
       */
      public Actions toMain ( int delayed, Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            DelayExecute execute = new DelayExecute();
            execute.mThread = MAIN_THREAD;
            execute.mActions = this;
            execute.mRunnable = runnable;
            execute.mDelayed = delayed;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 线程池执行任务
       *
       * @param runnable 任务
       *
       * @return 链式调用
       */
      public Actions toPool ( Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            BusExecute execute = new BusExecute();
            execute.mThread = POOL_THREAD;
            execute.mActions = this;
            execute.mRunnable = runnable;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 线程池执行任务
       *
       * @param runnable 任务
       *
       * @return 链式调用
       */
      public Actions toPool ( int delayed, Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            DelayExecute execute = new DelayExecute();
            execute.mThread = POOL_THREAD;
            execute.mActions = this;
            execute.mRunnable = runnable;
            execute.mDelayed = delayed;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 开始执行所有任务
       */
      public void run ( ) {

            if( !isLooping.get() ) {
                  isLooping.set( true );
                  loop();
            }
      }

      private void loop ( ) {

            if( isPaused.get() ) {
                  return;
            }

            try {

                  BusExecute executable = mRunnableContainer.next();

                  if( executable.mThread == BusExecute.RUN_IN_MAIN_THREAD ) {

                        MainThreadExecutor.execute( executable );
                  } else {

                        PoolThreadExecutor.execute( executable );
                  }
            } catch(Exception e) {

                  isLooping.set( false );
            }
      }

      /**
       * 是否正在运行
       *
       * @return true:正在运行
       */
      public boolean isRunning ( ) {

            return isLooping.get();
      }

      /**
       * 清除所有任务
       */
      public void cancelAll ( ) {

            mRunnableContainer.deleteAll();
      }

      /**
       * 取消任务
       *
       * @param runnable 任务需要取消
       */
      @SuppressWarnings("SuspiciousMethodCalls")
      public void cancel ( Runnable runnable ) {

            if( runnable == null ) {
                  return;
            }

            mRunnableContainer.delete( runnable );
      }

      /**
       * 暂停任务
       */
      public void pause ( ) {

            if( isPaused.get() ) {
                  return;
            }

            isPaused.set( true );
      }

      /**
       * 恢复任务
       */
      public void resume ( ) {

            if( isPaused.get() ) {

                  isPaused.set( false );
                  loop();
            }
      }

      /**
       * 剩余任务数量
       */
      public int remainSize ( ) {

            return mRunnableContainer.remainSize();
      }

      // ========================= 内部类 =========================

      /**
       * 如何放置任务
       */
      public interface RunnableContainer {

            /**
             * 添加任务
             *
             * @param runnable 任务
             */
            void add ( BusExecute runnable );

            /**
             * 删除任务
             *
             * @param runnable 任务
             */
            void delete ( Runnable runnable );

            /**
             * 清除所有任务
             */
            void deleteAll ( );

            /**
             * 下一个任务,或者异常{@link ArrayIndexOutOfBoundsException},或者null
             *
             * @return runnable
             */
            BusExecute next ( );

            /**
             * 剩余任务数量
             *
             * @return 剩余任务
             */
            int remainSize ( );
      }

      /**
       * 使用list保存任务,先添加的先执行
       */
      private static class ListRunnableContainer implements RunnableContainer {

            private final LinkedList<BusExecute> mExecutes = new LinkedList<>();

            @Override
            public void add ( BusExecute execute ) {

                  mExecutes.add( execute );
            }

            @Override
            public void delete ( Runnable runnable ) {

                  try {
                        for( BusExecute execute : mExecutes ) {

                              if( execute.mRunnable == runnable ) {
                                    mExecutes.remove( execute );
                                    return;
                              }
                        }
                  } catch(Exception e) {

                        e.printStackTrace();
                  }
            }

            @Override
            public void deleteAll ( ) {

                  mExecutes.clear();
            }

            @Override
            public BusExecute next ( ) {

                  return mExecutes.pollFirst();
            }

            @Override
            public int remainSize ( ) {

                  return mExecutes.size();
            }
      }

      /**
       * 使用队列形式保存任务,后添加的先执行
       */
      private static class QueueRunnableContainer implements RunnableContainer {

            private final LinkedList<BusExecute> mExecutes = new LinkedList<>();

            @Override
            public void add ( BusExecute execute ) {

                  mExecutes.add( execute );
            }

            @Override
            public void delete ( Runnable runnable ) {

                  try {
                        for( BusExecute execute : mExecutes ) {

                              if( execute.mRunnable == runnable ) {
                                    mExecutes.remove( execute );
                                    return;
                              }
                        }
                  } catch(Exception e) {

                        e.printStackTrace();
                  }
            }

            @Override
            public void deleteAll ( ) {

                  mExecutes.clear();
            }

            @Override
            public BusExecute next ( ) {

                  return mExecutes.pollLast();
            }

            @Override
            public int remainSize ( ) {

                  return mExecutes.size();
            }
      }

      /**
       * 使用队列形式保存任务,后添加的先执行,有固定任务上线
       */
      private static class FixSizeQueueRunnableContainer implements RunnableContainer {

            private final LinkedList<BusExecute> mExecutes = new LinkedList<>();
            private final int mFixSize;

            public FixSizeQueueRunnableContainer ( int fixSize ) {

                  mFixSize = fixSize;
            }

            @Override
            public void add ( BusExecute execute ) {

                  mExecutes.add( execute );

                  if( mExecutes.size() > mFixSize ) {
                        mExecutes.pollFirst();
                  }
            }

            @Override
            public void delete ( Runnable runnable ) {

                  try {
                        for( BusExecute execute : mExecutes ) {

                              if( execute.mRunnable == runnable ) {
                                    mExecutes.remove( execute );
                                    return;
                              }
                        }
                  } catch(Exception e) {

                        e.printStackTrace();
                  }
            }

            @Override
            public void deleteAll ( ) {

                  mExecutes.clear();
            }

            @Override
            public BusExecute next ( ) {

                  return mExecutes.pollLast();
            }

            @Override
            public int remainSize ( ) {

                  return mExecutes.size();
            }
      }

      /**
       * 代理任务,使其完成后调用下一个任务
       */
      public static class BusExecute extends Executable {

            public static final int RUN_IN_MAIN_THREAD = 1;
            public static final int RUN_IN_POOL_THREAD = -1;

            /**
             * 用于通知任务完成,以便执行下一个任务
             */
            protected Actions mActions;
            /**
             * 指定执行位置
             */
            protected int mThread = RUN_IN_MAIN_THREAD;
            /**
             * 用户设置的任务
             */
            protected Runnable mRunnable;

            @Override
            public void onStart ( ) {

            }

            @Override
            public void onExecute ( ) {

                  mRunnable.run();
            }

            @Override
            public void onFinish ( ) {

                  finish();
            }

            protected void finish ( ) {

                  mActions.loop();
            }
      }

      /**
       * 处理延时任务
       */
      public static class DelayExecute extends BusExecute implements OnMessageReceiveListener {

            private int mDelayed;

            @Override
            public void onStart ( ) {

            }

            @Override
            public void onExecute ( ) {

                  /* 使用 12 会在messenger线程收到消息 */

                  final int what = 12;
                  Messengers.send( what, mDelayed, this );
            }

            @Override
            public void onFinish ( ) {

            }

            @Override
            public void onReceive ( int what, Object extra ) {

            }

            @Override
            public void onReceive ( int what ) {

                  /* 还在messenger线程,需要转发一下 */

                  if( mThread == RUN_IN_MAIN_THREAD ) {

                        /* 需要完成后进行下一个任务,所以包装一下 */

                        BusExecute execute = new BusExecute();
                        execute.mActions = mActions;
                        execute.mRunnable = mRunnable;
                        MainThreadExecutor.execute( execute );
                  } else {

                        /* 需要完成后进行下一个任务,所以包装一下 */

                        BusExecute execute = new BusExecute();
                        execute.mActions = mActions;
                        execute.mRunnable = mRunnable;
                        PoolThreadExecutor.execute( execute );
                  }
            }
      }
}
