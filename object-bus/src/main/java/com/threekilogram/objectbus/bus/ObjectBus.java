package com.threekilogram.objectbus.bus;

import android.support.v4.util.ArrayMap;
import com.threekilogram.objectbus.executor.MainExecutor;
import com.threekilogram.objectbus.executor.PoolExecutor;
import com.threekilogram.objectbus.executor.ScheduleExecutor;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 该类用于按照一定的顺序规则在不同线程之间执行已经添加的所有任务
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-08
 * @time: 15:19
 */
public class ObjectBus {

      /**
       * 线程任务标记,一个对应主线程,一个对应pool线程
       */
      public static final int RUN_IN_MAIN_THREAD = 1;
      public static final int RUN_IN_POOL_THREAD = -1;
      public static final int RUN_IN_CURRENT     = -2;

      /**
       * 所有任务保存的地方
       */
      private RunnableContainer        mRunnableContainer;
      /**
       * 保存结果
       */
      private ArrayMap<String, Object> mResults;

      /**
       * 循环取出下一个任务执行,直到所有任务执行完毕
       */
      protected static void loop ( RunnableContainer container ) {

            if( container == null ) {
                  return;
            }

            try {

                  BusRunnable executable = container.next();
                  executeBusRunnable( executable );
            } catch(Exception e) {
                  /* container empty */
            }
      }

      protected static void executeBusRunnable ( BusRunnable executable ) {

            if( executable.mThread == RUN_IN_MAIN_THREAD ) {

                  MainExecutor.execute( executable );
            } else if( executable.mThread == RUN_IN_POOL_THREAD ) {

                  PoolExecutor.execute( executable );
            } else {

                  executable.run();
            }
      }

      public ObjectBus ( ) {

            mRunnableContainer = new ListRunnableContainer();
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

            if( mResults == null ) {
                  return null;
            }

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

            if( mResults == null ) {
                  return null;
            }

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

            BusRunnable execute = new BusRunnable(
                mRunnableContainer, RUN_IN_MAIN_THREAD, runnable );
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

            DelayRunnable execute = new DelayRunnable(
                mRunnableContainer, RUN_IN_MAIN_THREAD, runnable, delayed );
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

            BusRunnable execute = new BusRunnable(
                mRunnableContainer, RUN_IN_POOL_THREAD, runnable );
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

            DelayRunnable execute = new DelayRunnable(
                mRunnableContainer, RUN_IN_POOL_THREAD, runnable, delayed );
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
      public ObjectBus test ( Predicate test, OnPredicateRunnable runnable ) {

            if( test == null ) {
                  return null;
            }
            PredicateRunnable execute = new PredicateRunnable(
                mRunnableContainer, test, runnable );
            mRunnableContainer.add( execute );

            return this;
      }

      /**
       * 执行{@link BusRunnable},在{@link BusRunnable#run()}中记得调用{@link #loop(RunnableContainer)}进行下一个任务
       *
       * @return bus
       */
      public ObjectBus to ( BusRunnable runnable ) {

            runnable.mRunnableContainer = mRunnableContainer;
            mRunnableContainer.add( runnable );

            return this;
      }

      /**
       * 开始按照规则执行所有已经添加的任务,如果已经开始执行了,那么不会再次开始执行
       * <p>
       * 注意:该框架同一时间只有一个任务执行,如果需要任务调用之后立即执行,那么请另外新建一个{@link ObjectBus}
       *
       * @return container 用于查看任务执行情况
       */
      public RunnableContainer run ( ) {

            loop( mRunnableContainer );
            RunnableContainer result = mRunnableContainer;
            mRunnableContainer = mRunnableContainer.create();
            return result;
      }

      /**
       * 提交到任务组执行
       */
      public RunnableContainer submit ( BusGroup group ) {

            group.addTask( mRunnableContainer );
            RunnableContainer result = mRunnableContainer;
            mRunnableContainer = mRunnableContainer.create();
            return result;
      }

      /**
       * 根据返回值决定是否执行{@link OnPredicateRunnable}
       */
      public interface Predicate {

            /**
             * 测试是否还继续执行任务
             *
             * @return true :
             */
            boolean test ( );
      }

      /**
       * 如何放置任务,如何取出任务
       */
      public interface RunnableContainer {

            /**
             * 添加任务
             *
             * @param runnable 任务
             */
            void add ( BusRunnable runnable );

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
             * 下一个任务,或者异常{@link ArrayIndexOutOfBoundsException},或者null,通常实现会删除该任务,并且返回删除的任务
             *
             * @return runnable
             */
            BusRunnable next ( );

            /**
             * 剩余任务数量
             *
             * @return 剩余任务
             */
            int remainSize ( );

            /**
             * 是否包含一个任务还没有执行
             *
             * @param runnable 需要测试的任务
             *
             * @return true: 包含该任务并且还没有执行
             */
            boolean containsOf ( Runnable runnable );

            /**
             * 创建自身
             *
             * @return 自身
             */
            RunnableContainer create ( );
      }

      @SuppressWarnings("WeakerAccess")
      private static class ListRunnableContainer implements RunnableContainer {

            protected final LinkedList<BusRunnable> mExecutes = new LinkedList<>();

            @Override
            public void delete ( Runnable runnable ) {

                  Iterator<BusRunnable> iterator = mExecutes.iterator();
                  while( iterator.hasNext() ) {
                        BusRunnable next = iterator.next();
                        if( next.mRunnable == runnable ) {
                              iterator.remove();
                        }
                  }
            }

            @Override
            public void deleteAll ( ) {

                  mExecutes.clear();
            }

            @Override
            public int remainSize ( ) {

                  return mExecutes.size();
            }

            @Override
            public boolean containsOf ( Runnable runnable ) {

                  for( BusRunnable next : mExecutes ) {
                        if( next.mRunnable == runnable ) {
                              return true;
                        }
                  }
                  return false;
            }

            @Override
            public void add ( BusRunnable execute ) {

                  mExecutes.add( execute );
            }

            @Override
            public BusRunnable next ( ) {

                  return mExecutes.pollFirst();
            }

            @Override
            public RunnableContainer create ( ) {

                  return new ListRunnableContainer();
            }
      }

      /**
       * 代理{@link #mRunnable},使其完成后可以调用下一个任务执行
       */
      @SuppressWarnings("WeakerAccess")
      public static class BusRunnable implements Runnable {

            protected RunnableContainer mRunnableContainer;
            protected int               mThread;
            protected Runnable          mRunnable;

            public BusRunnable ( int thread, Runnable runnable ) {

                  mThread = thread;
                  mRunnable = runnable;
            }

            protected BusRunnable (
                RunnableContainer container, int thread, Runnable runnable ) {

                  mRunnableContainer = container;
                  mThread = thread;
                  mRunnable = runnable;
            }

            @Override
            public void run ( ) {

                  if( mRunnable != null ) {
                        mRunnable.run();
                  }
                  loop( mRunnableContainer );
            }
      }

      /**
       * 处理延时任务
       */
      private static class DelayRunnable extends BusRunnable {

            private int mDelayed;

            DelayRunnable (
                RunnableContainer container, int thread, Runnable runnable,
                int delayed ) {

                  super( container, thread, runnable );
                  mDelayed = delayed;
            }

            @Override
            public void run ( ) {

                  ScheduleExecutor.schedule( ( ) -> {
                        executeBusRunnable(
                            new BusRunnable( mRunnableContainer, mThread, mRunnable ) );
                  }, mDelayed );
            }
      }

      /**
       * 测试结果,如果结果为true执行一个特殊任务
       */
      private static class PredicateRunnable extends BusRunnable {

            private Predicate           mPredicate;
            private OnPredicateRunnable mRunnable;

            PredicateRunnable (
                RunnableContainer container,
                Predicate predicate,
                OnPredicateRunnable runnable ) {

                  super( container, RUN_IN_CURRENT, null );
                  mPredicate = predicate;
                  mRunnable = runnable;
            }

            @Override
            public void run ( ) {

                  boolean test = mPredicate.test();

                  if( test ) {
                        if( mRunnable != null ) {
                              mRunnable.run( mRunnableContainer );
                        }
                  }
                  loop( mRunnableContainer );
            }
      }

      public interface OnPredicateRunnable {

            /**
             * {@link PredicateRunnable#mPredicate}的{@link Predicate#test()}通过执行任务
             *
             * @param container {@link BusRunnable#mRunnableContainer} 任务所在任务组
             */
            void run ( RunnableContainer container );
      }
}
