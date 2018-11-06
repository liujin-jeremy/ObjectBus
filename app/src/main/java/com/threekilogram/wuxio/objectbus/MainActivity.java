package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.threekilogram.objectbus.bus.BusGroup;
import com.threekilogram.objectbus.bus.ObjectBus;
import com.threekilogram.objectbus.bus.ObjectBus.RunnableContainer;

/**
 * @author liujin
 */
public class MainActivity extends AppCompatActivity {

      private static final String TAG = MainActivity.class.getSimpleName();

      private ConstraintLayout mRoot;

      private ObjectBus         mObjectBus;
      private RunnableContainer mContainer;

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
            //BusGroup busGroup = BusGroup.newList(  3 );
            //BusGroup busGroup = BusGroup.newFixSizeList( 3, 3 );
            //BusGroup busGroup = BusGroup.newQueue(  3 );
            BusGroup busGroup = BusGroup.newFixSizeQueue( 3, 3 );

            for( int i = 0; i < 10; i++ ) {
                  final int j = i;
                  bus.toPool( ( ) -> {

                        try {
                              log( "group : 后台执行任务 " + String.valueOf( j ) + " 完成" );
                              Thread.sleep( 1000 );
                        } catch(InterruptedException e) {
                              e.printStackTrace();
                        }
                  } ).toMain( ( ) -> {

                        log( "group : 前台执行任务 " + String.valueOf( j ) + " 完成" );
                  } ).submit( busGroup );
            }
      }

      private class Task implements Runnable {

            @Override
            public void run ( ) {

                  log( "Task" );
            }
      }
}
