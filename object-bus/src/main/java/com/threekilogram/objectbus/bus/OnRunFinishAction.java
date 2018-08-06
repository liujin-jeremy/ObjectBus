package com.threekilogram.objectbus.bus;

/**
 * 在runnable.run之后执行
 *
 * @param <T> runnable
 * @author wuxio 2018-05-06:10:20
 */
public interface OnRunFinishAction < T extends Runnable > {

    /**
     * 懒执行,会在runnable 执行run之前执行
     *
     * @param bus      bus,use for take result
     * @param runnable runnable
     */
    void onRunFinished(Object bus, T runnable);
}
