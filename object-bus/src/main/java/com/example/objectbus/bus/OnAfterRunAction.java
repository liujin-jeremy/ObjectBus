package com.example.objectbus.bus;

/**
 * 在runnable.run之后执行
 *
 * @param <T> runnable
 * @author wuxio 2018-05-06:10:20
 */
public interface OnAfterRunAction < T extends Runnable > {

    /**
     * 懒执行,会在runnable 执行run之前执行
     *
     * @param bus      bus
     * @param runnable runnable
     */
    void onAfterRun(Object bus, T runnable);
}
