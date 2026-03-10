package thread.start;

public class HelloRunnableMain {

  public static void main(String[] args) {

    System.out.println(Thread.currentThread().getName() + ": main() start");

    HelloRunnable runnable = new HelloRunnable();

    Thread thread = new Thread(runnable);

    /*
    public Thread(Runnable task) {
        this(null, null, 0, task, 0, null);
    }

    task -> 실제 실행할 Runnable 작업
     */
    thread.start();

    System.out.println(Thread.currentThread().getName() + ": main() end");

  }
}
