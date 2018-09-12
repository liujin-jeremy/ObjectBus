package com.threekilogram.objectbus.tools;

/**
 * 用于线程间制造屏障,在线程运行期间如果需要满足一定条件才能执行后面的代码那么可以使用{@link #pause()}暂停,
 * 当其他线程创建好条件之后调用{@link #resumeAll()}恢复所有暂停线程继续执行
 *
 * @author Liujin 2018-09-12:21:13
 */
public class Blocker {

      /**
       * 暂停线程,注意不要再主线程调用
       */
      public void pause ( ) {

            synchronized(this) {
                  try {
                        wait();
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }
            }
      }

      /**
       * 恢复所有暂停的线程{@link #pause()},
       * <p>
       * 注意:不能和{@link #pause()}同一个线程调用
       */
      public void resumeAll ( ) {

            synchronized(this) {
                  notifyAll();
            }
      }
}
