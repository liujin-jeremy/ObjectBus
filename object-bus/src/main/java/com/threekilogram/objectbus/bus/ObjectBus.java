package com.threekilogram.objectbus.bus;

import android.support.v4.util.ArrayMap;
import com.threekilogram.objectbus.executor.MainThreadExecutor;
import com.threekilogram.objectbus.executor.PoolThreadExecutor;
import com.threekilogram.objectbus.message.Messengers;
import com.threekilogram.objectbus.message.OnMessageReceiveListener;
import com.threekilogram.objectbus.runnable.Executable;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:19
 */
public class ObjectBus {

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
      /**
       * 保存结果
       */
      private ArrayMap<String, Object> mResults;

      private ObjectBus ( RunnableContainer container ) {

            mRunnableContainer = container;
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务
       */
      public static ObjectBus newListActions ( ) {

            return new ObjectBus( new ListRunnableContainer() );
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务
       */
      public static ObjectBus newQueueActions ( ) {

            return new ObjectBus( new QueueRunnableContainer() );
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务,有固定任务上限
       */
      public static ObjectBus newFixSizeQueueActions ( int maxSize ) {

            return new ObjectBus( new FixSizeQueueRunnableContainer( maxSize ) );
      }

      /**
       * 保存一个变量
       *
       * @param key key
       * @param result 变量
       */
      public void setResult ( String key, Object result ) {

            if( mResults == null ) {
                  mResults = new ArrayMap<>();
            }
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
      public ObjectBus toMain ( Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            BusExecute execute = new BusExecute();
            execute.mThread = MAIN_THREAD;
            execute.mObjectBus = this;
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
      public ObjectBus toMain ( int delayed, Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            DelayExecute execute = new DelayExecute();
            execute.mThread = MAIN_THREAD;
            execute.mObjectBus = this;
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
      public ObjectBus toPool ( Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            BusExecute execute = new BusExecute();
            execute.mThread = POOL_THREAD;
            execute.mObjectBus = this;
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
      public ObjectBus toPool ( int delayed, Runnable runnable ) {

            if( runnable == null ) {
                  return this;
            }

            DelayExecute execute = new DelayExecute();
            execute.mThread = POOL_THREAD;
            execute.mObjectBus = this;
            execute.mRunnable = runnable;
            execute.mDelayed = delayed;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 如果{@code test}返回true,继续执行后面的任务,否则清除所有任务,注意在后台线程测试
       *
       * @param test 测试是否继续执行后面的任务
       *
       * @return bus
       */
      public ObjectBus ifTrue ( Predicate test ) {

            if( test == null ) {
                  return null;
            }
            PredicateExecute execute = new PredicateExecute();
            execute.mObjectBus = this;
            execute.mThread = POOL_THREAD;
            execute.mPredicate = test;
            execute.mResult = true;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 如果{@code test}返回false,继续执行后面的任务,否则清除所有任务,注意在后台线程测试
       *
       * @param test 测试是否继续执行后面的任务
       *
       * @return bus
       */

      public ObjectBus ifFalse ( Predicate test ) {

            if( test == null ) {
                  return null;
            }
            PredicateExecute execute = new PredicateExecute();
            execute.mObjectBus = this;
            execute.mThread = POOL_THREAD;
            execute.mPredicate = test;
            execute.mResult = false;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 循环取出下一个任务执行,直到所有任务执行完毕
       */
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
       * 开始执行所有任务
       */
      public void run ( ) {

            if( !isLooping.get() ) {
                  isLooping.set( true );
                  loop();
            }
      }

      /**
       * 根据返回值决定是否执行后面的任务
       */
      public interface Predicate {

            /**
             * 测试是否还继续执行任务
             *
             * @param bus bus
             *
             * @return true :
             */
            boolean test ( ObjectBus bus );
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
       * 如何放置任务,如何取出任务
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
      @SuppressWarnings("WeakerAccess")
      public static class BusExecute extends Executable {

            public static final int RUN_IN_MAIN_THREAD = 1;
            public static final int RUN_IN_POOL_THREAD = -1;

            /**
             * 用于通知任务完成,以便执行下一个任务
             */
            protected ObjectBus mObjectBus;
            /**
             * 指定执行位置
             */
            protected int mThread = RUN_IN_MAIN_THREAD;
            /**
             * 用户设置的任务
             */
            protected Runnable mRunnable;

            @Override
            public void onStart ( ) { }

            /**
             * 真正的执行任务
             */
            @Override
            public void onExecute ( ) {

                  mRunnable.run();
            }

            /**
             * 该方法会在任务执行完毕回调,如果任务没有执行完毕,不需要在此处调用{@link #finish()},
             * 当完成任务后在自己调用{@link #finish()},通知{@link #mObjectBus}执行下一个任务
             */
            @Override
            public void onFinish ( ) {

                  finish();
            }

            /**
             * 执行完任务之后,必须执行的操作,这样才能进行下一个任务
             */
            protected void finish ( ) {

                  mObjectBus.loop();
            }
      }

      /**
       * 处理延时任务
       */
      private static class DelayExecute extends BusExecute implements OnMessageReceiveListener {

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
                        execute.mObjectBus = mObjectBus;
                        execute.mRunnable = mRunnable;
                        MainThreadExecutor.execute( execute );
                  } else {

                        /* 需要完成后进行下一个任务,所以包装一下 */

                        BusExecute execute = new BusExecute();
                        execute.mObjectBus = mObjectBus;
                        execute.mRunnable = mRunnable;
                        PoolThreadExecutor.execute( execute );
                  }
            }
      }

      /**
       * 测试结果,如果结果不一致清除所有任务,如果一致继续执行任务
       */
      public static class PredicateExecute extends BusExecute {

            private Predicate mPredicate;
            private boolean   mResult;

            @Override
            public void onExecute ( ) {

                  boolean test = mPredicate.test( mObjectBus );

                  if( test != mResult ) {
                        mObjectBus.cancelAll();
                  }
            }
      }

      /**
       * 执行一组任务
       */
      public static class ListRunnable extends BusExecute {

            private Runnable[] mRunnableList;

            public ListRunnable ( Runnable... runnableList ) {

                  mRunnableList = runnableList;
                  mThread = RUN_IN_POOL_THREAD;
            }

            @Override
            public void onExecute ( ) {

                  for( Runnable t : mRunnableList ) {
                        t.run();
                  }
            }
      }

      /**
       * 执行一个任务,并保存结果
       */
      public static class CallableRunnable extends BusExecute {

            private Callable mCallable;
            private String   key;

            public CallableRunnable ( Callable callable, String key ) {

                  this.key = key;
                  mCallable = callable;
            }

            @Override
            public void onExecute ( ) {

                  try {
                        Object call = mCallable.call();
                        mObjectBus.setResult( key, call );
                  } catch(Exception e) {
                        e.printStackTrace();
                  }
            }
      }

      /**
       * 辅助执行一组任务并保存结果
       */
      public static class CallableListExecute extends BusExecute {

            private Callable[] mCallableList;
            private String     key;

            public CallableListExecute ( String key, Callable... callableList ) {

                  this.key = key;
                  mCallableList = callableList;
                  mThread = RUN_IN_POOL_THREAD;
            }

            @Override
            public void onExecute ( ) {

                  int size = mCallableList.length;
                  Object[] result = new Object[ size ];

                  for( int i = 0; i < size; i++ ) {

                        Callable c = mCallableList[ i ];

                        try {

                              Object call = c.call();
                              result[ i ] = call;
                        } catch(Exception e) {

                              e.printStackTrace();
                        }
                  }

                  if( key != null ) {

                        mObjectBus.setResult( key, result );
                  }

                  finish();
            }
      }
}
