package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusRunnable;

/**
 * 该类用于后台执行一组任务,可以指定线程并发数量
 *
 * @author Liujin 2018-11-06:21:52
 */
public class SimplePoolBus {

      /**
       * 保存任务
       */
      private final     RunnableContainer mContainer;
      /**
       * 任务并发执行最大数量
       */
      private transient int               mConcurrentCount = 3;

      /**
       * 使用列表保存所有添加的任务,到达上限删除最先添加的任务,先添加先执行
       *
       * @return bus
       */
      public static SimplePoolBus newList ( ) {

            return new SimplePoolBus( new ListContainer() );
      }

      /**
       * 使用列表保存所有添加的任务,任务数量有上限,到达上限删除最先添加的任务,先添加先执行
       *
       * @param size 任务数量上限
       *
       * @return bus
       */
      public static SimplePoolBus newList ( int size ) {

            return new SimplePoolBus( new FixSizeListContainer( size ) );
      }

      /**
       * 使用列表保存所有添加的任务,任务数量有上限,到达上限删除最先添加的任务,后添加先执行
       *
       * @return bus
       */
      public static SimplePoolBus newQueue ( ) {

            return new SimplePoolBus( new QueueContainer() );
      }

      /**
       * 使用列表保存所有添加的任务,任务数量有上限,到达上限删除最先添加的任务,后添加先执行
       *
       * @param size 任务数量上限
       *
       * @return bus
       */
      public static SimplePoolBus newQueue ( int size ) {

            return new SimplePoolBus( new FixSizeQueueContainer( size ) );
      }

      /**
       * 创建
       */
      private SimplePoolBus ( RunnableContainer container ) {

            mContainer = container;
      }

      /**
       * 添加一个任务执行
       *
       * @param runnable 任务
       */
      public void run ( Runnable runnable ) {

            BusRunnable busRunnable = new PoolBusRunnable(
                mContainer,
                ObjectBus.RUN_IN_POOL_THREAD,
                runnable
            );

            if( mConcurrentCount <= 0 ) {
                  mContainer.add( busRunnable );
            } else {
                  ObjectBus.executeBusRunnable( busRunnable );
                  mConcurrentCount--;
            }
      }

      /**
       * 用于包装用户任务,用户任务执行完成后,拿取下一个任务
       */
      private class PoolBusRunnable extends BusRunnable {

            private PoolBusRunnable (
                RunnableContainer container, int thread, Runnable runnable ) {

                  super( container, thread, runnable );
            }

            @Override
            public void run ( ) {

                  if( mRunnable != null ) {
                        mRunnable.run();
                  }
                  try {
                        BusRunnable next = mContainer.next();
                        ObjectBus.executeBusRunnable( next );
                  } catch(Exception e) {
                        mConcurrentCount++;
                  }
            }
      }
}
