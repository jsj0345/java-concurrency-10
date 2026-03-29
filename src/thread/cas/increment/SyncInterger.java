package thread.cas.increment;

public class SyncInterger implements IncrementInteger {

  private int value;

  @Override
  public synchronized void increment() {
    value++;
  }

  @Override
  public synchronized int get() {
    return value;
  }

}
