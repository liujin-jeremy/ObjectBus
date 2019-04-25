package tech.liujin.objectbus;

/**
 * @author Liujin 2019/2/23:11:04:35
 */
public interface StepTask extends Runnable {

      void setNext ( StepTask task );

      void start ( );

      void startNext ( );
}
