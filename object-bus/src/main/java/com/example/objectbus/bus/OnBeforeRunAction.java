package com.example.objectbus.bus;

/**
 * 在runnable.run之前执行延迟初始化,保证会在上一操作之后执行
 *
 * @param <T> runnable
 * @author wuxio 2018-05-06:10:20
 */
public interface OnBeforeRunAction < T extends Runnable > {

    /**
     * 懒执行,会在runnable 执行run之前执行
     *
     * @param runnable runnable
     */
    void onBeforeRun(T runnable);
}
