package com.threekilogram.wuxio.objectbus;

import static com.threekilogram.wuxio.objectbus.MsgUtils.getInfo;

import com.threekilogram.objectbus.anth.ScheduledTask;
import com.threekilogram.objectbus.anth.Task;
import com.threekilogram.objectbus.anth.Threads;

/**
 * @author Liujin 2019/2/23:0:25:42
 */
public class Test {

      public static class TaskRun implements Runnable {

            private String mWhat;
            private int    mNeedTime;

            private TaskRun ( String what, int needTime ) {

                  mWhat = what;
                  mNeedTime = needTime;
            }

            @Override
            public void run ( ) {

                  System.out.println( getInfo() + " " + mWhat + " 开始" );
                  try {
                        Thread.sleep( mNeedTime );
                  } catch(InterruptedException e) {
                        e.printStackTrace();
                  }
                  System.out.println( getInfo() + " " + mWhat + " 结束" );
            }
      }

      public static void startTest ( ) {

            Task task0 = new Task( new TaskRun( "任务00", 1000 ), Threads.IO );
            Task task1 = new Task( new TaskRun( "任务01", 1000 ), Threads.COMPUTATION );
            task0.setNext( task1 );
            Task task2 = new Task( new TaskRun( "任务02", 1000 ), Threads.SINGLE );
            task1.setNext( task2 );
            Task task3 = new Task( new TaskRun( "任务03", 1000 ), Threads.NEW_THREAD );
            task2.setNext( task3 );
            Task task4 = new Task( new TaskRun( "任务4", 1000 ), Threads.ANDROID_MAIN );
            task3.setNext( task4 );
            Task task5 = new Task( new TaskRun( "任务5", 1000 ), Threads.IO );
            task4.setNext( task5 );
            Task task6 = new Task( new TaskRun( "任务6", 1000 ), Threads.COMPUTATION );
            ScheduledTask task6Wrapper = new ScheduledTask( 3000, task6 );
            task5.setNext( task6Wrapper );
            Task task7 = new Task( new TaskRun( "任务7", 1000 ), Threads.IO );
            task6.setNext( task7 );
            task0.start();
      }
}
