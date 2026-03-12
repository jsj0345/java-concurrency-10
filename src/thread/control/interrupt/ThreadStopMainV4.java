package thread.control.interrupt;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ThreadStopMainV4 {

  public static void main(String[] args) {

    MyTask task = new MyTask();
    Thread thread = new Thread(task, "work");
    thread.start();

    sleep(100);
    log("작업 중단 지시 thread.interrupt()");
    thread.interrupt();
    log("work 스레드 인터럽트 상태 1 = " + thread.isInterrupted());


  }

  static class MyTask implements Runnable {

    @Override
    public void run() {
        while(!Thread.interrupted()) { // 인터럽트 상태를 변경 O (실제로 정상 상태로 바꾸고 난 다음 값을 반환함 원래는 true였으니 true를 반환 상태는 정상으로 이미 바뀜)
          log("작업 중");
        }
        log("work 스레드 인터럽트 상태 2 = " + Thread.currentThread().isInterrupted());

        try {
          log("자원 정리");
          Thread.sleep(1000);
          log("자원 종료");
        } catch (InterruptedException e) {
          log("자원 정리 실패 - 자원 정리 중 인터럽트 발생");
          log("work 스레드 인터럽트 상태3 = " + Thread.currentThread().isInterrupted());
        }

        log("작업 종료");

      }

    }
}


