package com.example.wuxio.objectbus;

import com.example.objectbus.message.OnMessageReceiveListener;

import java.lang.ref.WeakReference;

/**
 * @author wuxio 2018-05-03:19:11
 */
public class MainManager implements OnMessageReceiveListener {

    //============================ register ============================

    private WeakReference< MainActivity > mReference;


    public void register(MainActivity mainActivity) {

        mReference = new WeakReference<>(mainActivity);
    }

    //============================ message ============================


    @Override
    public void onReceive(int what, Object extra) {

        MainActivity activity = mReference.get();
        MainActivity.print("receive: " + what + " extra: " + extra + " main: " + activity);
    }


    @Override
    public void onReceive(int what) {

        MainActivity activity = mReference.get();
        MainActivity.print("receive: " + what + " " + activity);
    }

    //============================ singleTon ============================


    private MainManager() {

    }


    public static MainManager getInstance() {

        return SingletonHolder.INSTANCE;
    }


    private static class SingletonHolder {
        private static final MainManager INSTANCE = new MainManager();
    }
}
