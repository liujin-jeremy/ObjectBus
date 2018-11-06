package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusRunnable;

/**
 * @author Liujin 2018-11-06:21:52
 */
public class SimplePoolBus {

      private final RunnableContainer mContainer;

      private transient boolean isRunning;

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

            mContainer.add( new BusRunnable( mContainer, ObjectBus.RUN_IN_POOL_THREAD, runnable ) );
            if( !isRunning ) {
                  isRunning = true;
                  ObjectBus.loop( mContainer );
            }
      }

      private class PoolBusRunnable extends BusRunnable {

            protected PoolBusRunnable (
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
                        isRunning = false;
                  }
            }
      }
}
