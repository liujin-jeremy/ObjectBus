package com.example.objectbus.bus;

/**
 * @param <T> 用于延迟创建runnable,当上一个操作完成后才创建runnable 执行
 * @author wuxio 2018-05-06:12:25
 */
public interface LazyInitializeRunnableAction < T extends Runnable > {

    /**
     * 延迟创建runnable,上一个操作完成后才创建runnable 执行
     *
     * @return runnable
     */
    T onLazyInitialize();

}
