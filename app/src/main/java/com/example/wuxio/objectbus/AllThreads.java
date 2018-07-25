package com.example.wuxio.objectbus;

import android.util.Log;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-07-22
 * @time: 21:47
 */
public class AllThreads {

      public static ThreadGroup findOutRootThreadGroup () {

            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();

            while(rootGroup.getParent() != null) {
                  rootGroup = rootGroup.getParent();
            }

            return rootGroup;
      }

      public static Thread[] getThreadsInGroup (ThreadGroup group) {

            if(group == null) {
                  return null;
            }

            int activeCount = group.activeCount();
            Thread[] threads = new Thread[activeCount];
            group.enumerate(threads, true);
            return threads;
      }

      public static void print () {

            ThreadGroup threadGroup = findOutRootThreadGroup();
            threadGroup.list();
      }

      public static Thread[] getAllThread () {

            ThreadGroup rootGroup = findOutRootThreadGroup();
            return getThreadsInGroup(rootGroup);
      }

      public static void printThreads () {

            Set<Entry<Thread, StackTraceElement[]>> entries = Thread.getAllStackTraces().entrySet();

            for(Entry<Thread, StackTraceElement[]> entry : entries) {
                  Log.e("threads", entry.getKey().toString());
            }
      }
}
