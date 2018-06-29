package com.example.wuxio.objectbus;

import android.app.Application;

import com.example.objectbus.ObjectBusConfig;

/**
 * @author wuxio 2018-05-03:8:07
 */
public class App extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        ObjectBusConfig.init();
    }
}
