package com.threekilogram.objectbus.global;

import android.support.v4.util.ArrayMap;

/**
 * 用于全局保存一些临时变量,省去在类中声明这些变量
 *
 * @author: Liujin
 * @version: V1.0
 * @date: 2018-09-03
 * @time: 22:10
 */
public class GlobalFields {

      public static ArrayMap<String, Object> sMembers = new ArrayMap<>();

      /**
       * 保存一个临时变量
       */
      public static void put ( String key, Object value ) {

            sMembers.put( key, value );
      }

      /**
       * 获取一个临时变量
       */
      @SuppressWarnings("unchecked")
      public static <V> V get ( String key ) {

            return (V) sMembers.get( key );
      }

      /**
       * 删除一个临时变量
       */
      @SuppressWarnings("unchecked")
      public static <V> V remove ( String key ) {

            return (V) sMembers.remove( key );
      }

      /**
       * 清除所有临时变量
       */
      public static void clear ( ) {

            sMembers.clear();
      }
}
