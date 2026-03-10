package thread.start.test;

import static util.MyLogger.log;

public class CounterThread extends Thread {

  @Override
  public void run() {

    for(int i = 1; i < 6; i++) {
      try {
        sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      log("value: " + i);
    }

  }

}
