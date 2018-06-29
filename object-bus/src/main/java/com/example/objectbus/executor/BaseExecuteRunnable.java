package com.example.objectbus.executor;

/**
 * @author liujin
 */
public abstract class BaseExecuteRunnable implements OnExecuteRunnable {

      @Override
      public void run () {

            try {
                  onStart();
                  onExecute();
                  onFinish();
            } catch(Exception e) {
                  onException(e);
            }
      }
}
