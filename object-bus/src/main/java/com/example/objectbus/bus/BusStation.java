package com.example.objectbus.bus;

import android.util.SparseArray;

/**
 * @author wuxio 2018-05-10:11:21
 */
public class BusStation {

    //============================ singleTon ============================


    private BusStation() {

    }


    public static BusStation getInstance() {

        return SingletonHolder.INSTANCE;
    }


    private static class SingletonHolder {
        private static final BusStation INSTANCE = new BusStation();
    }

    //============================ care ============================

    private SparseArray< ObjectBus >      mEmptyBus    = new SparseArray<>();


    public void obtainBus() {

        if (mEmptyBus.size() > 0) {
            ObjectBus bus = mEmptyBus.get(0);
            bus.clearRunnable();
        }
    }
}
