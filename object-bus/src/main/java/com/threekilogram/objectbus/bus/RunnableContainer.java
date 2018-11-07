package com.threekilogram.objectbus.bus;

/**
 * @author Liujin 2018-11-06:21:53
 */

import com.threekilogram.objectbus.bus.ObjectBus.BusRunnable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 如何放置任务,如何取出任务
 *
 * @author liujin
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

/**
 * @author Liujin 2018-11-06:21:59
 */
abstract class BaseRunnableContainer implements RunnableContainer {

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
}

/**
 * @author Liujin 2018-11-06:21:54
 */
@SuppressWarnings("WeakerAccess")
class ListContainer extends BaseRunnableContainer {

      ListContainer ( ) { }

      @Override
      public synchronized BusRunnable next ( ) {

            return mExecutes.pollFirst();
      }

      @Override
      public RunnableContainer create ( ) {

            return new ListContainer();
      }
}

class FixSizeListContainer extends ListContainer {

      private int mSize;

      FixSizeListContainer ( int size ) {

            mSize = size;
      }

      @Override
      public void add ( BusRunnable execute ) {

            super.add( execute );
            if( mExecutes.size() > mSize ) {
                  mExecutes.pollFirst();
            }
      }

      @Override
      public synchronized BusRunnable next ( ) {

            return super.next();
      }

      @Override
      public RunnableContainer create ( ) {

            return new FixSizeListContainer( mSize );
      }
}

/**
 * @author Liujin 2018-11-06:21:54
 */
@SuppressWarnings("WeakerAccess")
class QueueContainer extends BaseRunnableContainer {

      QueueContainer ( ) { }

      @Override
      public synchronized BusRunnable next ( ) {

            return mExecutes.pollLast();
      }

      @Override
      public RunnableContainer create ( ) {

            return new QueueContainer();
      }
}

class FixSizeQueueContainer extends QueueContainer {

      private int mSize;

      FixSizeQueueContainer ( int size ) {

            mSize = size;
      }

      @Override
      public void add ( BusRunnable execute ) {

            super.add( execute );
            if( mExecutes.size() > mSize ) {
                  mExecutes.pollFirst();
            }
      }

      @Override
      public synchronized BusRunnable next ( ) {

            return super.next();
      }

      @Override
      public RunnableContainer create ( ) {

            return new FixSizeQueueContainer( mSize );
      }
}