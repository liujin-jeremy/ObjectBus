package com.example.objectbus.executor;

/**
 * 监听{@link AppExecutor}执行进度的接口
 * @author wuxio 2018-05-03:10:23
 */
public interface OnExecuteListener {

    void onFinish();

    void onException(Exception e);
}
