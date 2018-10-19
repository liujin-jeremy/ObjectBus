package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
            mObjectBus = ObjectBus.newList();
      }

      private void initView ( ) {

            mRoot = (ConstraintLayout) findViewById( R.id.root );
      }

      public void addMain ( View view ) {

            mObjectBus.toMain( new MainRunnable() );
      }

      public void toPool ( View view ) {

            mObjectBus.toPool( new MainRunnable() );
      }

      public void toMainDelayed ( View view ) {

            mObjectBus.toMain( 2000, new MainRunnable() );
      }

      public void toPoolDelayed ( View view ) {

            mObjectBus.toPool( 2000, new MainRunnable() );
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

            MainRunnable runnable = new MainRunnable();
            mObjectBus.toPool( runnable );
            mContainer.delete( runnable );
            log( "cancel " + runnable );
      }

      public void list ( View view ) {

            mObjectBus = ObjectBus.newList();
      }

      public void queue ( View view ) {

            mObjectBus = ObjectBus.newQueue();
      }

      public void fixSize ( View view ) {

            //mObjectBus = ObjectBus.newQueue( 3 );
            //mObjectBus = ObjectBus.newList( 3 );
      }

      public void ifFalseFalse ( View view ) {

            mObjectBus.ifFalse( bus -> false );
      }

      public void ifTrueFalse ( View view ) {

            mObjectBus.ifTrue( bus -> false );
      }

      private class MainRunnable implements Runnable {

            @Override
            public void run ( ) {

                  log( "MainRunnable" );
            }
      }
}
