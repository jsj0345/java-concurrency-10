package thread.sync.test;

import static util.MyLogger.log;

/*
다음 코드에서 MyTask의 run() 메서드는 두 스레드에서 동시에 실행한다.

다음 코드의 결과를 예측해보자.

그리고 localValue 지역 변수에 동시성 문제가 발생하는지 하지 않는지를 생각해보자.

-> 공용 자원이 아니라 동시성 문제 발생 X.
 */

public class SyncTest2Main {

  public static void main(String[] args) {
    MyCounter myCounter = new MyCounter();

    Runnable task = new Runnable() {
      @Override
      public void run() {
        myCounter.count();
      }
    };

    Thread thread1 = new Thread(task, "Thread-1");
    Thread thread2 = new Thread(task, "Thread-2");

    thread1.start();
    thread2.start();
  }

  static class MyCounter {

    public void count() {
      int localValue = 0;
      for (int i = 0; i < 1000; i++) {
        localValue = localValue + 1;
      }
      log("결과 : " + localValue);
    }

  }

}
