package thread.control;

import thread.start.HelloRunnable; // alt + enter 자동 import

import static util.MyLogger.log;

public class ThreadInfoMain {

  public static void main(String[] args) {
    // main 스레드
    Thread mainThread = Thread.currentThread();
    log("mainThread = " + mainThread);
    log("mainThread.threadId()=" + mainThread.threadId());
    log("mainThread.getName()=" + mainThread.getName());
    log("mainThread.getPriority()=" + mainThread.getPriority()); // 우선순위가 높을 수록 많이 실행된다. (높다고 해서 항상 많이 실행되는건 아님)
    log("mainThread.getThreadGroup()=" + mainThread.getThreadGroup());
    log("mainThread.getState()=" + mainThread.getState()); // 스레드가 실행 될 수 있는 상태(RUNNABLE)

    //myThread 스레드
    Thread myThread = new Thread(new HelloRunnable(), "myThread");
    log("myThread = " + myThread);
    log("myThread.threadId()=" + myThread.threadId());
    log("myThread.getName()=" + myThread.getName());
    log("myThread.getPriority()=" + myThread.getPriority()); // 우선순위가 높을 수록 많이 실행된다. (높다고 해서 항상 많이 실행되는건 아님)
    log("myThread.getThreadGroup()=" + myThread.getThreadGroup());
    log("myThread.getState()=" + myThread.getState()); // 스레드가 실행 될 수 있는 상태(RUNNABLE)

  }


}
