package com.example.objectbus.bus;

import android.support.v4.util.ArrayMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-05-10:11:21
 */
public class ObjectBusStation {

      //============================ singleTon ============================

      private ObjectBusStation () {

      }

      public static ObjectBusStation getInstance () {

            return SingletonHolder.INSTANCE;
      }

      private static class SingletonHolder {

            private static final ObjectBusStation INSTANCE = new ObjectBusStation();
      }

      //============================ care ============================

      private ArrayMap<Integer, ObjectBus> mEmptyBus = new ArrayMap<>();
      private AtomicInteger                mInteger  = new AtomicInteger();

      /**
       * @return new object bus
       */
      public ObjectBus obtainBus () {

            for(int i = 0; i < mEmptyBus.size(); i++) {
                  int key = mEmptyBus.keyAt(i);
                  ObjectBus bus = mEmptyBus.get(key);
                  if(bus != null) {
                        mEmptyBus.remove(key);
                        return bus;
                  }
            }

            return new ObjectBus();
      }

      /**
       * @param bus recycle bus
       */
      public void recycleBus (ObjectBus bus) {

            bus.initToNew();
            int key = mInteger.getAndAdd(1);
            mEmptyBus.put(key, bus);
      }

      //============================ static Simple method ============================

      /**
       * @return a new empty ObjectBus
       */
      public static ObjectBus callNewBus () {

            return getInstance().obtainBus();
      }

      /**
       * @param bus recycle bus
       */
      public static void recycle (ObjectBus bus) {

            getInstance().recycleBus(bus);
      }
}
