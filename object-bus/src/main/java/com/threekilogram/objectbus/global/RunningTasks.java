package com.threekilogram.objectbus.global;

/**
 * {@link RunningTask}的静态实现版本
 *
 * @author Liujin 2018-09-08:10:34
 */
public class RunningTasks {

      private static final RunningTask RUNNING_TASK = new RunningTask();

      /**
       * 测试是否正在执行该任务,如果没有执行那么添加该任务到执行列表,当下次调用时返回true,
       * 当执行完成后记得删除{@link #remove(String)}
       *
       * @param url 需要测试的任务
       *
       * @return true:正在执行
       */
      public static boolean isRunning ( String url ) {

            return RUNNING_TASK.isRunning( url );
      }

      /**
       * 测试是否该任务包含在任务列表中,并不会添加到任务列表中
       *
       * @param url 需要测试任务
       *
       * @return true:该任务正在执行
       */
      public static boolean containsOf ( String url ) {

            return RUNNING_TASK.containsOf( url );
      }

      /**
       * 删除已经完成的任务
       */
      public static void remove ( String url ) {

            RUNNING_TASK.remove( url );
      }

      /**
       * 测试是否正在执行该任务,如果没有执行那么添加该任务到执行列表,当下次调用时返回true,
       * 当执行完成后记得删除{@link #remove(String)}
       *
       * @param what 需要测试的任务
       *
       * @return true:正在执行
       */
      public static boolean isRunning ( int what ) {

            return RUNNING_TASK.isRunning( what );
      }

      /**
       * 测试是否该任务包含在任务列表中,并不会添加到任务列表中
       *
       * @param what 需要测试任务
       *
       * @return true:该任务正在执行
       */
      public static boolean containsOf ( int what ) {

            return RUNNING_TASK.containsOf( what );
      }

      /**
       * 删除已经完成的任务
       */
      public static void remove ( int what ) {

            RUNNING_TASK.remove( what );
      }

      public static void clear ( ) {

            RUNNING_TASK.clear();
      }
}
