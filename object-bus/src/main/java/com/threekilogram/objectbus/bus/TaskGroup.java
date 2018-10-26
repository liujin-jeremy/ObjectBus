package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusExecute;
import com.threekilogram.objectbus.bus.ObjectBus.RunnableContainer;
import java.util.LinkedList;

/**
 * @author Liujin 2018-10-26:17:41
 */
public class TaskGroup {

      private           TaskIterator mTaskIterator;
      /**
       * 并发执行任务的数量
       */
      private transient int          mCouldLoopCount = 3;

      public static TaskGroup newList ( int maxConcurrentCount ) {

            return new TaskGroup( new ListTaskIterator(), maxConcurrentCount );
      }

      public static TaskGroup newFixSizeList ( int maxCont, int maxConcurrentCount ) {

            return new TaskGroup( new FixSizeListTaskIterator( maxCont ), maxConcurrentCount );
      }

      public static TaskGroup newQueue ( int maxConcurrentCount ) {

            return new TaskGroup( new QueueTaskIterator(), maxConcurrentCount );
      }

      public static TaskGroup newFixSizeQueue ( int maxCont, int maxConcurrentCount ) {

            return new TaskGroup( new FixSizeQueueTaskIterator( maxCont ), maxConcurrentCount );
      }

      /**
       * @param taskIterator 按照什么顺序执行任务
       * @param concurrentCount 并发线程数量
       */
      private TaskGroup ( TaskIterator taskIterator, int concurrentCount ) {

            mTaskIterator = taskIterator;
            mCouldLoopCount = concurrentCount;
      }

      /**
       * {@link ObjectBus#submit(TaskGroup)}添加任务到队列执行
       *
       * @param container 一组任务
       */
      void addTask ( RunnableContainer container ) {

            if( mCouldLoopCount == 0 ) {
                  mTaskIterator.add( container );
            } else {
                  container.add( new LoopBusExecute( container ) );
                  ObjectBus.loop( container );
                  mCouldLoopCount--;
            }
      }

      /**
       * 用于一组任务执行完成后继续执行下一组任务
       */
      private class LoopBusExecute extends BusExecute {

            private LoopBusExecute ( RunnableContainer container ) {

                  super( container, RUN_IN_CURRENT );
            }

            @Override
            public void onExecute ( ) {

                  try {
                        RunnableContainer container = mTaskIterator.next();
                        container.add( new LoopBusExecute( container ) );
                        ObjectBus.loop( container );
                        return;
                  } catch(Exception e) {
                        /* nothing */
                  }
                  mCouldLoopCount++;
            }
      }

      /**
       * 如何放置任务,如何取出任务
       */
      public interface TaskIterator {

            /**
             * 添加任务
             *
             * @param task 任务
             */
            void add ( RunnableContainer task );

            /**
             * 下一个任务,或者异常{@link ArrayIndexOutOfBoundsException},或者null,通常实现会删除该任务,并且返回删除的任务
             *
             * @return runnable
             */
            RunnableContainer next ( );
      }

      @SuppressWarnings("WeakerAccess")
      private static abstract class BaseTaskIterator implements TaskIterator {

            protected final LinkedList<RunnableContainer> mTasks = new LinkedList<>();
      }

      /**
       * 使用list保存任务,先添加的先执行
       */
      private static class ListTaskIterator extends BaseTaskIterator {

            @Override
            public void add ( RunnableContainer task ) {

                  mTasks.add( task );
            }

            @Override
            public RunnableContainer next ( ) {

                  return mTasks.pollFirst();
            }
      }

      /**
       * 使用list保存任务,先添加的先执行,有固定任务上限,到达上限之后再次添加会丢弃最先添加的任务
       */
      private static class FixSizeListTaskIterator extends BaseTaskIterator {

            private final int mFixSize;

            FixSizeListTaskIterator ( int fixSize ) {

                  mFixSize = fixSize;
            }

            @Override
            public void add ( RunnableContainer execute ) {

                  mTasks.add( execute );

                  if( mTasks.size() > mFixSize ) {
                        mTasks.pollFirst();
                  }
            }

            @Override
            public RunnableContainer next ( ) {

                  return mTasks.pollFirst();
            }
      }

      /**
       * 使用队列形式保存任务,后添加的先执行
       */
      private static class QueueTaskIterator extends BaseTaskIterator {

            @Override
            public void add ( RunnableContainer execute ) {

                  mTasks.add( execute );
            }

            @Override
            public RunnableContainer next ( ) {

                  return mTasks.pollLast();
            }
      }

      /**
       * 使用队列形式保存任务,后添加的先执行,有固定任务上限,到达上限之后再次添加会丢弃最先添加的任务
       */
      private static class FixSizeQueueTaskIterator extends BaseTaskIterator {

            private final int mFixSize;

            FixSizeQueueTaskIterator ( int fixSize ) {

                  mFixSize = fixSize;
            }

            @Override
            public void add ( RunnableContainer execute ) {

                  mTasks.add( execute );

                  if( mTasks.size() > mFixSize ) {
                        mTasks.pollFirst();
                  }
            }

            @Override
            public RunnableContainer next ( ) {

                  return mTasks.pollLast();
            }
      }
}
