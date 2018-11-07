package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.threekilogram.objectbus.bus.BusGroup;
import com.threekilogram.objectbus.bus.ObjectBus;
import com.threekilogram.objectbus.bus.RunnableContainer;
import com.threekilogram.objectbus.bus.SimplePoolBus;
import com.threekilogram.objectbus.executor.PoolExecutor;
import com.threekilogram.objectbus.executor.ScheduleExecutor;

/**
 * @author liujin
 */
public class MainActivity extends AppCompatActivity {

      private static final String TAG = MainActivity.class.getSimpleName();

      private ConstraintLayout mRoot;

      private ObjectBus         mObjectBus;
      private RunnableContainer mContainer;
      private SimplePoolBus     mSimplePoolBus;

      private transient int      mCount;
      private           BusGroup mBusGroup;

      @Override
      protected void onCreate ( Bundle savedInstanceState ) {

            super.onCreate( savedInstanceState );
            setContentView( R.layout.activity_main );
            initView();
            mObjectBus = new ObjectBus();
      }

      private void initView ( ) {

            mRoot = findViewById( R.id.root );
      }

      public void addMain ( View view ) {

            mObjectBus.toMain( new Task() );
      }

      public void toPool ( View view ) {

            mObjectBus.toPool( new Task() );
      }

      public void toMainDelayed ( View view ) {

            mObjectBus.toMain( 2000, new Task() );
      }

      public void toPoolDelayed ( View view ) {

            mObjectBus.toPool( 2000, new Task() );
      }

      public void run ( View view ) {

            mContainer = mObjectBus.run();
      }

      public void size ( View view ) {

            int i = mContainer.remainSize();
            log( String.valueOf( i ) );
      }

      private static void log ( String msg ) {

            Log.e(
                TAG,
                "log : "
                    + msg + " "
                    + System.currentTimeMillis() + " "
                    + Thread.currentThread().getName()
            );
      }

      public void isRunning ( View view ) {

            log( Boolean.toString( mContainer.remainSize() != 0 ) );
      }

      public void clearAll ( View view ) {

            mContainer.deleteAll();
      }

      public void clearOne ( View view ) {

            Task runnable = new Task();
            mObjectBus.toPool( runnable );
            mContainer.delete( runnable );
            log( "cancel " + runnable );
      }

      public void list ( View view ) {

            mObjectBus = new ObjectBus();
      }

      public void ifFalseFalse ( View view ) {

            mObjectBus.test(
                ( ) -> true,
                container -> {
                      container.deleteAll();
                }
            );
      }

      public void ifTrueFalse ( View view ) {

            mObjectBus.test(
                ( ) -> false,
                container -> {
                      container.deleteAll();
                }
            );
      }

      public void group ( View view ) {

            ObjectBus bus = new ObjectBus();

            if( mBusGroup == null ) {
                  mBusGroup = BusGroup.newQueue( 3, 3 );
                  //mBusGroup = BusGroup.newQueue(  3 );
                  //mBusGroup = BusGroup.newList( 3, 3 );
                  //mBusGroup = BusGroup.newList( 3 );
            }

            for( int i = 0; i < 10; i++ ) {
                  mCount++;
                  final int j = mCount;
                  bus.toPool( ( ) -> {

                        try {
                              log( "group : 后台执行任务 " + String.valueOf( j ) + " 中" );
                              Thread.sleep( 1000 );
                        } catch(InterruptedException e) {
                              e.printStackTrace();
                        }
                  } ).toMain( ( ) -> {

                        log( "group : 前台执行任务 " + String.valueOf( j ) + " 完成" );
                  } ).submit( mBusGroup );
            }
      }

      public void simple ( View view ) {

            if( mSimplePoolBus == null ) {
                  //mSimplePoolBus = SimplePoolBus.newQueue( 3 );
                  //mSimplePoolBus = SimplePoolBus.newQueue();
                  //mSimplePoolBus = SimplePoolBus.newList( 3 );
                  mSimplePoolBus = SimplePoolBus.newList();
            }
            for( int i = 0; i < 10; i++ ) {
                  mCount++;
                  int j = mCount;
                  mSimplePoolBus.run( ( ) -> {

                        try {
                              Thread.sleep( 2 * 1000 );
                        } catch(InterruptedException e) {
                              e.printStackTrace();
                        }

                        log( "simple task " + j );
                  } );
            }
      }

      public void schedule ( View view ) {

            ScheduleExecutor.scheduleAtFixedRate( ( ) -> {

                  PoolExecutor.execute( ( ) -> {

                        log( "schedule " + mCount++ );
                  } );
            }, 1000, 1000, 3 );
      }

      public void schedule2 ( View view ) {

            ScheduleExecutor.scheduleWithFixedDelay( ( ) -> {

                  PoolExecutor.execute( ( ) -> {

                        log( "schedule " + mCount++ );
                  } );
            }, 1000, 1000, 3 );
      }

      private class Task implements Runnable {

            @Override
            public void run ( ) {

                  log( "Task" );
            }
      }
}
