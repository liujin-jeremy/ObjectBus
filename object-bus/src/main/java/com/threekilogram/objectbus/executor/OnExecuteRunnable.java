package com.threekilogram.objectbus.executor;

import com.threekilogram.objectbus.schedule.run.AsyncThreadCallBack;
import com.threekilogram.objectbus.schedule.run.MainThreadCallBack;

/**
 * 该监听和{@link MainThreadCallBack}与{@link AsyncThreadCallBack}不同,它可以在任务执行完成后,继续在线程执行一些操作, 而{@link
 * MainThreadCallBack}与{@link AsyncThreadCallBack}, 会切换到{@link com.threekilogram.objectbus.message.Messengers}的线程上执行,后台线程执行结束了
 *
 * @author wuxio 2018-05-03:10:27
 */
public interface OnExecuteRunnable extends Runnable {

      /**
       * 任务开始,{@link #onExecute()}之前回调
       */
      void onStart ();

      /**
       * 执行任务
       */
      void onExecute ();

      /**
       * {@link #onExecute()}正常结束之后,调用,表明任务执行完毕
       */
      void onFinish ();

      /**
       * {@link #onExecute()}发生异常之后调用,表明任务执行期间发生异常
       *
       * @param e : 异常
       */
      void onException (Exception e);

      /**
       * 默认的操作,如果需要复写,请按照如下形式复写
       */
      @Override
      void run ();
}
