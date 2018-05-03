package com.example.objectbus.runnable;

import com.example.objectbus.executor.OnExecuteListener;

/**
 * @author wuxio 2018-05-03:10:27
 */
public interface OnExecuteRunnable extends Runnable, OnExecuteListener {

    /**
     * 执行任务
     */
    void onExecute();

    /**
     * {@link #onExecute()}正常结束之后,调用,表明任务执行完毕
     */
    @Override
    void onFinish();

    /**
     * {@link #onExecute()}发生异常之后调用,表明任务执行期间发生异常
     *
     * @param e : 异常
     */
    @Override
    void onException(Exception e);

    /**
     * 默认的操作,如果需要复写,请按照如下形式复写
     */
    @Override
    default void run() {

        try {

            onExecute();
            onFinish();

        } catch (Exception e) {
            onException(e);
        }
    }
}
