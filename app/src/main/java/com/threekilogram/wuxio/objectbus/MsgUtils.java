package com.threekilogram.wuxio.objectbus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Liujin 2019/2/23:10:27:07
 */
public class MsgUtils {

      public static SimpleDateFormat sFormat = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss:SSS", Locale.CHINA );

      public static String getInfo ( ) {

            String name = Thread.currentThread().getName();
            long l = System.currentTimeMillis();
            String format = sFormat.format( new Date( l ) );
            return name + " " + format;
      }
}
