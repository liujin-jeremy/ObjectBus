package com.example.objectbus.bus;

/**
 * 用于为一个runnable添加异常监听
 *
 * @author wuxio 2018-05-06:13:28
 */
public interface OnRunExceptionHandler < T extends Runnable > {

    /**
     * 当run 发生异常时调用 {@link com.example.objectbus.bus.ObjectBus.ExtraActionRunnable}
     *
     * @param t runnable
     * @param e 异常
     */
    void onException(T t, Exception e);

}
