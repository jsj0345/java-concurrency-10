package thread.control;

import static util.MyLogger.log;

public class ThreadStateMain {

  public static void main(String[] args) throws InterruptedException {

    Thread thread = new Thread(new MyRunnable(), "myThread");
    log("myThread.state1 = " + thread.getState()); // NEW
    log("myThread.start()");
    thread.start();
    Thread.sleep(1000); // 예외 던지기 alt + enter
    log("myThread.state3 = " + thread.getState()); // TIMED_WAITING
    Thread.sleep(4000);
    log("myThread.state5 = " + thread.getState()); // TERMINATED
    log("end");



  }

  static class MyRunnable implements Runnable {

    @Override
    public void run() {

      try { // 예외 처리 단축키 ctrl + alt + T, alt + shift + 화살표 -> 코드 옮기는 단축키
        log("start");
        log("myThread.state2 = " + Thread.currentThread().getState()); // RUNNABLE
        log("sleep() start");
        Thread.sleep(3000);
        // 자고 있는 상태를 getState()로 찍고싶은데 이거는 다른 스레드가 찍어줘야함.
        // 상식적으로 자고 있는 스레드 본인이 자신을 찍어내는거 자체가 말이안된다.
        log("sleep() end");
        log("myThread.state4 = " + Thread.currentThread().getState()); // RUNNABLE

        log("end");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    }

  }

}
