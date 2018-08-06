package com.threekilogram.objectbus.schedule;

import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;

/**
 * 用于{@link Scheduler}取消任务
 *
 * @author wuxio 2018-05-03:9:25
 */
public class CancelTodo {

    private int                       tag;
    private WeakReference< Runnable > callbackRef;

    private int                       key;
    private WeakReference< Runnable > todoRunnableRef;


    public CancelTodo() {

    }


    void setTag(int tag) {

        this.tag = tag;
    }


    void setKey(int key) {

        this.key = key;
    }


    void setCallback(Runnable callback) {

        this.callbackRef = new WeakReference<>(callback);
    }


    void setTodoRunnable(Runnable todoRunnable) {

        this.todoRunnableRef = new WeakReference<>(todoRunnable);
    }


    public void cancel() {

        SparseArray< WeakReference< Runnable > > callbackRunnable = Scheduler.CALLBACK_RUNNABLE;
        SparseArray< Runnable > todoRunnable = Scheduler.RUNNABLE;

        try {
            if (callbackRef.get() != null) {
                callbackRunnable.remove(tag);
            }
        } catch (Exception e) {
            Log.e("cancel", "cancel:" + "nullPointer");
        }

        try {
            if (todoRunnableRef.get() != null) {
                todoRunnable.remove(key);
            }
        } catch (Exception e) {
            Log.e("cancel", "cancel:" + "nullPointer");
        }
    }


    void init() {

        tag = 0;
        callbackRef = null;
        key = 0;
        todoRunnableRef = null;
    }
}
