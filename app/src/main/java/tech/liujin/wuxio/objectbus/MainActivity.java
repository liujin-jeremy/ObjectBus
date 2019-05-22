package tech.liujin.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.liujin.wuxio.objectbus.R;
import tech.liujin.objectbus.ObjectBus;
import tech.liujin.objectbus.Threads;
import tech.liujin.objectbus.timer.DelayTask;

public class MainActivity extends AppCompatActivity {

      private static final String    TAG = MainActivity.class.getSimpleName();
      private              DelayTask mDelayTask;

      @Override
      protected void onCreate ( Bundle savedInstanceState ) {

            super.onCreate( savedInstanceState );
            setContentView( R.layout.activity_main );
      }

      public void startTest ( View view ) {

            ObjectBus bus = new ObjectBus();
            bus.to( new TestTask( "任务1", 1000 ), Threads.SINGLE );
            bus.to( new TestTask( "任务2", 1000 ), Threads.COMPUTATION );
            bus.to( new TestTask( "任务3", 1000 ), Threads.IO );
            bus.to( new TestTask( "任务4", 1000 ), Threads.NEW_THREAD );
            bus.to( new TestTask( "任务5", 1000 ), Threads.ANDROID_MAIN );
            bus.schedule( new TestTask( "任务6", 1000 ), Threads.COMPUTATION, 3000 );
            bus.to( new TestTask( "任务7", 1000 ), Threads.SINGLE );
            bus.start();
      }

      public void testDelayTask ( View view ) {

            mDelayTask = new DelayTask(
                new TestTask(
                    "任务-定时",
                    0,
                    new Runnable() {

                          @Override
                          public void run ( ) {

                                Log.i( TAG, "run: " + mDelayTask.getRunCount() );
                          }
                    }
                ),
                Threads.SINGLE,
                1000,
                10
            );
            mDelayTask.start();
      }

      public void startTest01 ( View view ) {

            ObjectBus bus = new ObjectBus();
            bus.toSingle( new TestTask( "任务1", 1000 ) );
            bus.toComputation( new TestTask( "任务2", 1000 ) );
            bus.toIO( new TestTask( "任务3", 1000 ) );
            bus.toNew( new TestTask( "任务4", 1000 ) );
            bus.toAndroidMain( new TestTask( "任务5", 1000 ) );
            bus.scheduleToComputation( new TestTask( "任务6", 1000 ), 3000 );
            bus.toSingle( new TestTask( "任务7", 1000 ) );
            bus.scheduleToIO( new TestTask( "任务8", 1000 ), 3000 );
            bus.scheduleToSingle( new TestTask( "任务9", 1000 ), 3000 );
            bus.scheduleToAndroidMain( new TestTask( "任务10", 1000 ), 3000 );
            bus.start();
      }

      public void pause ( View view ) {

            if( mDelayTask != null ) {
                  mDelayTask.pause();
            }
      }

      public void stop ( View view ) {

            if( mDelayTask != null ) {
                  mDelayTask.stop();
            }
      }

      public void resume ( View view ) {

            if( mDelayTask != null ) {
                  mDelayTask.resume();
            }
      }

      public void delayReset ( View view ) {

            if( mDelayTask != null ) {
                  mDelayTask.reset();
                  mDelayTask.start();
            }
      }

      public void arrayDelay ( View view ) {

            final DelayTask task = new DelayTask(
                new TestTask(
                    "任务-定时",
                    0
                ),
                Threads.SINGLE,
                new int[]{ 1000, 2000, 3000, 4000 }
            );
            task.start();
      }
}
