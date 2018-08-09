package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.threekilogram.objectbus.action.Actions;

/**
 * @author liujin
 */
public class Main2Activity extends AppCompatActivity {

      private static final String TAG = Main2Activity.class.getSimpleName();
      private ConstraintLayout mRoot;

      private Actions mActions;

      @Override
      protected void onCreate ( Bundle savedInstanceState ) {

            super.onCreate( savedInstanceState );
            setContentView( R.layout.activity_main2 );
            initView();
            mActions = Actions.newListActions();
      }

      private void initView ( ) {

            mRoot = (ConstraintLayout) findViewById( R.id.root );
      }

      public void addMain ( View view ) {

            mActions.toMain( new MainRunnable() );
      }

      public void toPool ( View view ) {

            mActions.toPool( new MainRunnable() );
      }

      public void toMainDelayed ( View view ) {

            mActions.toMain( 2000, new MainRunnable() );
      }

      public void toPoolDelayed ( View view ) {

            mActions.toPool( 2000, new MainRunnable() );
      }

      public void run ( View view ) {

            mActions.run();
      }

      public void size ( View view ) {

            int i = mActions.remainSize();
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

            log( Boolean.toString( mActions.isRunning() ) );
      }

      public void pause ( View view ) {

            mActions.pause();
      }

      public void resume ( View view ) {

            mActions.resume();
      }

      public void clearAll ( View view ) {

            mActions.cancelAll();
      }

      public void clearOne ( View view ) {

            MainRunnable runnable = new MainRunnable();
            mActions.toPool( runnable );
            mActions.cancel( runnable );
            log( "cancel " + runnable );
      }

      public void list ( View view ) {

            if( mActions != null ) {
                  mActions.cancelAll();
            }
            mActions = Actions.newListActions();
      }

      public void queue ( View view ) {

            if( mActions != null ) {
                  mActions.cancelAll();
            }
            mActions = Actions.newQueueActions();
      }

      public void fixSize ( View view ) {

            if( mActions != null ) {
                  mActions.cancelAll();
            }
            mActions = Actions.newFixSizeQueueActions( 3 );
      }

      private class MainRunnable implements Runnable {

            @Override
            public void run ( ) {

                  log( this.toString() );
            }
      }
}
