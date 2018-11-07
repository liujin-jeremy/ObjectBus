package com.threekilogram.objectbus.global;

import android.support.v4.util.ArraySet;

/**
 * 线程安全的记录正在执行的任务,使用{@link #add(int)}测试任务,防止重复执行该任务
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-08-27
 * @time: 11:28
 */
public class RunningTask {

      private final ArraySet<Integer> mRunning = new ArraySet<>();

      /**
       * 测试是否正在执行该任务,如果没有执行那么添加该任务到执行列表,当下次调用时返回true,
       * 当执行完成后记得删除{@link #remove(String)}
       *
       * @param url 需要测试的任务
       */
      public void add ( String url ) {

            add( url.hashCode() );
      }

      /**
       * 测试是否正在执行该任务,如果没有执行那么添加该任务到执行列表,当下次调用时返回true,
       * 当执行完成后记得删除{@link #remove(String)}
       *
       * @param what 需要测试的任务
       */
      public void add ( int what ) {

            mRunning.add( what );
      }

      /**
       * 测试是否该任务包含在任务列表中,并不会添加到任务列表中
       *
       * @param url 需要测试任务
       *
       * @return true:该任务正在执行
       */
      public boolean containsOf ( String url ) {

            return containsOf( url.hashCode() );
      }

      /**
       * 测试是否该任务包含在任务列表中,并不会添加到任务列表中
       *
       * @param what 需要测试任务
       *
       * @return true:该任务正在执行
       */
      public boolean containsOf ( int what ) {

            return mRunning.contains( what );
      }

      /**
       * 删除已经完成的任务
       */
      public void remove ( String url ) {

            remove( url.hashCode() );
      }

      /**
       * 删除已经完成的任务
       */
      public void remove ( int what ) {

            mRunning.remove( what );
      }

      /**
       * 清除所有任务
       */
      public void clear ( ) {

            mRunning.clear();
      }
}
