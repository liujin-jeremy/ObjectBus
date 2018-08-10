package com.threekilogram.objectbus.bus;

import android.support.v4.util.ArrayMap;
import com.threekilogram.objectbus.executor.MainThreadExecutor;
import com.threekilogram.objectbus.executor.PoolThreadExecutor;
import com.threekilogram.objectbus.runnable.Executable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import tech.threekilogram.messengers.Messengers;
import tech.threekilogram.messengers.OnMessageReceiveListener;

/**
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:19
 */
public class ObjectBus {

      /**
       * 线程任务标记,一个对应主线程,一个对应pool线程
       */
      private static final int MAIN_THREAD = BusExecute.RUN_IN_MAIN_THREAD;
      private static final int POOL_THREAD = BusExecute.RUN_IN_POOL_THREAD;

      /**
       * 所有任务保存的地方
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

      /**
       * @param container 指定保存读取任务策略
       */
      private ObjectBus ( RunnableContainer container ) {

            mRunnableContainer = container;
      }

      /**
       * @return 使用list管理的任务集, 按照添加顺序执行任务
       */
      public static ObjectBus newList ( ) {

            return new ObjectBus( new ListRunnableContainer() );
      }

      /**
       * @return 使用队列管理的任务集, 后添加的先执行
       */
      public static ObjectBus newQueue ( ) {

            return new ObjectBus( new QueueRunnableContainer() );
      }

      /**
       * @return 使用队列管理的任务集, 后添加的先执行, 有固定任务上限
       */
      public static ObjectBus newFixSize ( int maxSize ) {

            return new ObjectBus( new FixSizeQueueRunnableContainer( maxSize ) );
      }

      /**
       * 保存一个变量,当执行一组任务时使用该方法保存临时结果,后面的操作可以读取该结果
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
       * 主线程执行任务,任务处理完毕之后调用{@link BusExecute#finish()}以进行下一个任务
       * <p>
       * 主要用于自己定置执行任务规则,记得调用{@link BusExecute#finish()}就行
       *
       * @param execute 任务
       *
       * @return 链式调用
       */
      public ObjectBus toMain ( BusExecute execute ) {

            if( execute == null ) {
                  return this;
            }

            execute.mThread = MAIN_THREAD;
            execute.mObjectBus = this;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * pool线程执行任务,任务处理完毕之后调用{@link BusExecute#finish()}以进行下一个任务
       * <p>
       * 主要用于自己定置执行任务规则,记得调用{@link BusExecute#finish()}就行
       *
       * @param execute 任务
       *
       * @return 链式调用
       */
      public ObjectBus toPool ( BusExecute execute ) {

            if( execute == null ) {
                  return this;
            }

            execute.mThread = POOL_THREAD;
            execute.mObjectBus = this;
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 如果{@code test}返回true,继续执行后面的任务,否则清除所有任务,注意在后台线程测试{@link Predicate}结果
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
       * 如果{@code test}返回false,继续执行后面的任务,否则清除所有任务,注意在后台线程测试{@link Predicate}结果
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
       * 剩余没有执行任务数量
       */
      public int remainSize ( ) {

            return mRunnableContainer.remainSize();
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
       * 根据返回值决定是否执行后面的任务{@link #ifTrue(Predicate)}{@link #ifFalse(Predicate)}
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
       * 代理{@link #mRunnable},使其完成后可以调用下一个任务执行
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
             * 指定执行线程
             */
            protected int mThread = RUN_IN_MAIN_THREAD;
            /**
             * 用户设置的任务,如果自定义任务,可以忽略该变量,重写{@link #onExecute()}添加自己的逻辑,
             * 记得完成所有操作后,调用{@link #finish()}通知{@link #mObjectBus}进行下一个任务
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

                  /* 此处不调用finish();因为任务还没有完成,需要接收到延时消息后才完成任务 */
            }

            @Override
            public void onReceive ( int what, Object extra ) {

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
}
