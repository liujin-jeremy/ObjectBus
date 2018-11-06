package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusRunnable;

/**
 * @author Liujin 2018-11-06:21:52
 */
public class SimplePoolBus {

      private RunnableContainer mContainer;

      public static SimplePoolBus newList ( ) {

            return new SimplePoolBus( new ListContainer() );
      }

      public static SimplePoolBus newList ( int size ) {

            return new SimplePoolBus( new FixSizeListContainer( size ) );
      }

      public static SimplePoolBus newQueue ( ) {

            return new SimplePoolBus( new QueueContainer() );
      }

      public static SimplePoolBus newQueue ( int size ) {

            return new SimplePoolBus( new FixSizeQueueContainer( size ) );
      }

      private SimplePoolBus ( RunnableContainer container ) {

            mContainer = container;
      }

      public void run ( Runnable runnable ) {

            boolean call = mContainer.remainSize() == 0;
            mContainer.add( new BusRunnable( mContainer, ObjectBus.RUN_IN_POOL_THREAD, runnable ) );
            if( call ) {
                  ObjectBus.loop( mContainer );
            }
      }
}
