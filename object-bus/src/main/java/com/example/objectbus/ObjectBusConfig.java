package com.example.objectbus;

import com.example.objectbus.message.Messengers;
import com.example.objectbus.schedule.Scheduler;

/**
 * @author wuxio 2018-05-04:21:14
 */
public final class ObjectBusConfig {

    public static void init() {

        Scheduler.init();
        Messengers.init();
    }
}
