package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.threekilogram.objectbus.ObjectBus;
import com.threekilogram.objectbus.Threads;

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
            bus.start();
      }
}
