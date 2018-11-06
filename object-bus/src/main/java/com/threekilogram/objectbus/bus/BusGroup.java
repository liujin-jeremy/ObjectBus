package com.threekilogram.objectbus.bus;

import com.threekilogram.objectbus.bus.ObjectBus.BusRunnable;
import java.util.LinkedList;

/**
 * @author Liujin 2018-10-26:17:41
 */
public class BusGroup {

      private TaskIterator mTaskIterator;

      /**
       * 并发执行任务的数量
       */
      private transient int mCouldLoopCount = 3;

      /**
       * 创建一个任务组,先添加的先执行
       *
       * @param maxConcurrentCount 并发线程数量
       *
       * @return 任务组
       */
      public static BusGroup newList ( int maxConcurrentCount ) {

            return new BusGroup( new ListTaskIterator(), maxConcurrentCount );
      }

      /**
       * 创建一个任务组,先添加的先执行,有固定任务上限,到达上限移除最早添加的任务
       *
       * @param maxConcurrentCount 并发线程数量
       *
       * @return 任务组
       */
      public static BusGroup newList ( int maxCont, int maxConcurrentCount ) {

            return new BusGroup( new FixSizeListTaskIterator( maxCont ), maxConcurrentCount );
      }

      /**
       * 创建一个任务组,后添加的先执行
       *
       * @param maxConcurrentCount 并发线程数量
       *
       * @return 任务组
       */
      public static BusGroup newQueue ( int maxConcurrentCount ) {

            return new BusGroup( new QueueTaskIterator(), maxConcurrentCount );
      }

      /**
       * 创建一个任务组,后添加的先执行,有固定任务上限,到达上限移除最早添加的任务
       *
       * @param maxConcurrentCount 并发线程数量
       *
       * @return 任务组
       */
      public static BusGroup newQueue ( int maxCont, int maxConcurrentCount ) {

            return new BusGroup( new FixSizeQueueTaskIterator( maxCont ), maxConcurrentCount );
      }

      /**
       * @param taskIterator 按照什么顺序执行任务
       * @param concurrentCount 并发线程数量
       */
      private BusGroup ( TaskIterator taskIterator, int concurrentCount ) {

            mTaskIterator = taskIterator;
            mCouldLoopCount = concurrentCount;
      }

      /**
       * {@link ObjectBus#submit(BusGroup)}添加任务到队列执行
       *
       * @param container 一组任务
       */
      void addTask ( RunnableContainer container ) {

            if( mCouldLoopCount == 0 ) {
                  mTaskIterator.add( container );
            } else {
                  container.add( new LoopBusRunnable( container ) );
                  ObjectBus.loop( container );
                  mCouldLoopCount--;
            }
      }

      /**
       * 用于一组任务执行完成后继续执行下一组任务
       */
      private class LoopBusRunnable extends BusRunnable {

            private LoopBusRunnable ( RunnableContainer container ) {

                  super( container, ObjectBus.RUN_IN_CURRENT, null );
            }

            @Override
            public void run ( ) {

                  try {
                        RunnableContainer container = mTaskIterator.next();
                        container.add( new LoopBusRunnable( container ) );
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
