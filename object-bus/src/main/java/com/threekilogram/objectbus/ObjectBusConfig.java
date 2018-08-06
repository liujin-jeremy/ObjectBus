package com.threekilogram.objectbus;

import com.threekilogram.objectbus.message.Messengers;
import com.threekilogram.objectbus.schedule.Scheduler;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wuxio 2018-05-04:21:14
 */
public final class ObjectBusConfig {

      /**
       * 防止重复初始化
       */
      private static AtomicBoolean isInit = new AtomicBoolean();

      public static void init () {

            if(isInit.get()) {
                  return;
            }

            isInit.set(true);
            Scheduler.init();
            Messengers.init();
      }
}
