package thread.start.test;

import static util.MyLogger.log;

public class StartTest4Main {

  public static void main(String[] args) {

    ThreadA threadA = new ThreadA();

    ThreadB threadB = new ThreadB();

    Thread thread1 = new Thread(threadA,"Thread-A");
    thread1.start();

    Thread thread2 = new Thread(threadB,"Thread-B");
    thread2.start();

  }

  static class ThreadA implements Runnable {
    @Override
    public void run() {
      while(true) {

        try {
          log("A");
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

      }

    }
  }

  static class ThreadB implements Runnable {
    @Override
    public void run() {
      while(true) {

        try {
          log("B");
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

      }

    }

  }

}
