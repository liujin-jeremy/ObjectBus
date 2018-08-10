package com.threekilogram.wuxio.objectbus;

import android.app.Application;

/**
 * @author wuxio 2018-05-03:8:07
 */
public class App extends Application {

      private static final String TAG = App.class.getSimpleName();

      @Override
      public void onCreate () {

            super.onCreate();

            ObjectBusConfig.init();
      }
}
