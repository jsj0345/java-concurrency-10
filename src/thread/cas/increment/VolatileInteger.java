package thread.cas.increment;

public class VolatileInteger implements IncrementInteger {

  volatile private int value;
  volatile private boolean flag;

  @Override
  public void increment() {
    value++;
  }

  @Override
  public int get() {
    return value;
  }

}
