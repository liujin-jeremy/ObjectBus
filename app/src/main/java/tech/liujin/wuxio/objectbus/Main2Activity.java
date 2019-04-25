package tech.liujin.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.liujin.wuxio.objectbus.R;
import tech.liujin.objectbus.ObjectBus;
import tech.liujin.objectbus.Threads;

public class Main2Activity extends AppCompatActivity {

      @Override
      protected void onCreate ( Bundle savedInstanceState ) {

            super.onCreate( savedInstanceState );
            setContentView( R.layout.activity_main2 );
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

      private void test00 ( ) {

            ObjectBus bus = new ObjectBus();
            bus.to(
                ( ) -> {
                      // 执行的任务第0步
                },
                Threads.COMPUTATION
            ).to(
                ( ) -> {
                      // 执行的任务第1步
                },
                Threads.IO
            ).to(
                ( ) -> {
                      // 执行的任务第2步
                },
                Threads.SINGLE
            ).to(
                ( ) -> {
                      // 执行的任务第3步
                },
                Threads.ANDROID_MAIN
            ).start();
      }

      private void test01 ( ) {

            ObjectBus bus = new ObjectBus();
            bus.toComputation(
                ( ) -> {
                      // 执行的任务第0步
                }
            ).toIO(
                ( ) -> {
                      // 执行的任务第1步
                }
            ).toSingle(
                ( ) -> {
                      // 执行的任务第2步
                }
            ).toAndroidMain(
                ( ) -> {
                      // 执行的任务第3步
                }
            ).start();
      }

      private void test02 ( ) {

            ObjectBus bus = new ObjectBus();
            bus.schedule(
                ( ) -> {
                      // 延时任务
                },
                Threads.SINGLE,
                1000
            ).start();
      }

      private void test03 ( ) {

            ObjectBus bus = new ObjectBus();
            bus.toSingle(
                ( ) -> {
                      // 任务第0步
                }
            ).schedule(
                ( ) -> {
                      // 延时任务,任务第2步
                },
                Threads.SINGLE,
                1000
            ).toAndroidMain(
                ( ) -> {
                      // 任务第3步
                }
            ).start();
      }

      private void test04 ( ) {

            ObjectBus bus = new ObjectBus();
            bus.scheduleToSingle(
                ( ) -> {
                      // 任务第0步
                },
                1000
            ).scheduleToIO(
                ( ) -> {
                      // 延时任务,任务第2步
                },
                1000
            ).scheduleToComputation(
                ( ) -> {
                      // 任务第3步
                },
                1000
            ).scheduleToAndroidMain(
                ( ) -> {
                      // 任务第4步
                },
                1000
            ).start();
      }
}
