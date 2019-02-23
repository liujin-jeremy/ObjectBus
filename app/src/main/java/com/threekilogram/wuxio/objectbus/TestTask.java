package com.threekilogram.wuxio.objectbus;

import static com.threekilogram.wuxio.objectbus.MsgUtils.getInfo;

/**
 * @author Liujin 2019/2/23:23:16:28
 */
public class TestTask implements Runnable {

      private String mWhat;
      private int    mNeedTime;

      public TestTask ( String what, int needTime ) {

            mWhat = what;
            mNeedTime = needTime;
      }

      @Override
      public void run ( ) {

            System.out.println( getInfo() + " " + mWhat + " 开始" );
            try {
                  Thread.sleep( mNeedTime );
            } catch(InterruptedException e) {
                  e.printStackTrace();
            }
            System.out.println( getInfo() + " " + mWhat + " 结束" );
      }
}
