package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusExecute;
import com.threekilogram.objectbus.bus.ObjectBus.RunnableContainer;
import java.util.LinkedList;

/**
 * @author Liujin 2018-10-26:17:41
 */
public class TaskGroup {

      private LinkedList<RunnableContainer> mTasks = new LinkedList<>();

      /**
       * 并发执行任务的数量
       */
      private transient int mConcurrentCount = 3;

      public TaskGroup ( ) { }

      public TaskGroup ( int concurrentCount ) {

            mConcurrentCount = concurrentCount;
      }

      void addTask ( RunnableContainer container ) {

            if( mConcurrentCount == 0 ) {
                  mTasks.add( container );
            } else {
                  container.add( new LoopBusExecute( container ) );
                  ObjectBus.loop( container );
                  mConcurrentCount--;
            }
      }

      private class LoopBusExecute extends BusExecute {

            protected LoopBusExecute ( RunnableContainer container ) {

                  super( container, RUN_IN_CURRENT );
            }

            @Override
            public void onExecute ( ) {

                  try {
                        RunnableContainer container = mTasks.remove( 0 );
                        container.add( new LoopBusExecute( container ) );
                        ObjectBus.loop( container );
                        return;
                  } catch(Exception e) {
                        /* nothing */
                  }
                  mConcurrentCount++;
            }
      }
}
