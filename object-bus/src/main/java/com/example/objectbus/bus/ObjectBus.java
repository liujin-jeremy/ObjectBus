package com.example.objectbus.bus;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * @author wuxio 2018-05-03:16:16
 */
public class ObjectBus {


    /**
     * how many station pass By
     */
    private int mPassBy;

    private ArrayList< Runnable > mHowToPass;


    public ObjectBus() {

    }


    public int getPassBy() {

        return mPassBy;
    }


    public ObjectBus go(@NonNull Runnable task) {

        mHowToPass.add(task);
        return this;
    }


    public ObjectBus toUnder(@NonNull Runnable task) {

        return this;
    }

}
