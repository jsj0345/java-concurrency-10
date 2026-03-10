package thread.start.test;

import static util.MyLogger.*;

public class StartTest1Main {

  public static void main(String[] args) {

    CounterThread counterThread = new CounterThread();

    counterThread.start();

  }

}
