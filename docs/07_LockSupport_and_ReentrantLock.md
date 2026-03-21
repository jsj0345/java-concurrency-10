# 고급 동기화

## 1. LockSupport1 

synchronized는 자바 1.0부터 제공되는 매우 편리한 기능이지만, 다음과 같은 한계가 있다.

**synchronized 단점**
- 무한 대기 : BLOCKED 상태의 스레드는 락이 풀릴 때 까지 무한 대기한다.

-> 특정 시간까지만 대기하는 타임아웃이 없다.
-> 중간에 인터럽트가 불가능하다.

- 공정성 : 락이 돌아왔을 때 BLOCKED 상태의 여러 스레드 중에 어떤 스레드가 락을 획득할 지 알 수 없다.

결국 더 유연하고, 더 세밀한 제어가 가능한 방법들이 필요하게 되었다.

이런 문제를 해결하기 위해 자바 1.5부터 java.util.concurrent라는 동시성 문제 해결을 위한 라이브러리 패키지가 추가 된다.

**LockSupport 기능**

LockSupport는 스레드를 WAITING 상태로 변경한다.
WAITING 상태는 누가 깨워주기 전까지는 계속 대기한다. 그리고 CPU 스케줄링에 들어가지 않는다.

LockSupport의 대표적인 기능은 다음과 같다.
- park() : 스레드를 WAITING 상태로 변경한다.
-> 스레드를 대기 상태로 둔다. 

- parkNanos(nanos) : 스레드를 나노초 동안만 TIMED_WAITING 상태로 변경한다.
-> 지정한 나노초가 지나면 TIMED_WAITING 상태에서 빠져나오고 RUNNABLE 상태로 변경된다.

- unpark(thread) : WAITING 상태의 대상 스레드를 RUNNABLE 상태로 변경한다.

```java
package thread.sync.lock;

import java.util.concurrent.locks.LockSupport;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class LockSupportMainV1 {

  public static void main(String[] args) {
    Thread thread1 = new Thread(new ParkTask(), "Thread-1");
    thread1.start();
    
    sleep(100);
    log("Thread-1 state : " + thread1.getState());
    
    //log("main -> unpark(Thread-1)");
    //LockSupport.unpark(thread1); // 1. unpark 사용
    //thread1.interrupt(); // 2. interrupt() 사용
  }
  
  static class ParkTask implements Runnable {
    @Override
    public void run() {
      log("park 시작");
      LockSupport.park(); 
      log("park 종료, state : " + Thread.currentThread().getState());
      log("인터럽트 상태 : " + Thread.currentThread().isInterrupted());
    }
  }

}
```

먼저 위와같이 주석을 걸어 둔 상태에서 코드를 실행해보자.

결과는 다음과 같다.

```text
19:08:56.447 [ Thread-1] park 시작
19:08:56.490 [     main] Thread-1 state : WAITING
```

지금은 park 메소드를 호출해서 계속 WAITING 상태가 된다.

이 상태를 해제해야 한다.

```java
package thread.sync.lock;

import java.util.concurrent.locks.LockSupport;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class LockSupportMainV1 {

  public static void main(String[] args) {
    Thread thread1 = new Thread(new ParkTask(), "Thread-1");
    thread1.start();
    
    sleep(100);
    log("Thread-1 state : " + thread1.getState());
    
    log("main -> unpark(Thread-1)");
    LockSupport.unpark(thread1); // 1. unpark 사용
    //thread1.interrupt(); // 2. interrupt() 사용
  }
  
  static class ParkTask implements Runnable {
    @Override
    public void run() {
      log("park 시작");
      LockSupport.park(); 
      log("park 종료, state : " + Thread.currentThread().getState());
      log("인터럽트 상태 : " + Thread.currentThread().isInterrupted());
    }
  }

}
```

결과는 이렇게 나온다.

```text
19:10:59.261 [ Thread-1] park 시작
19:10:59.347 [     main] Thread-1 state : WAITING
19:10:59.347 [     main] main -> unpark(Thread-1)
19:10:59.349 [ Thread-1] park 종료, state : RUNNABLE
19:10:59.356 [ Thread-1] 인터럽트 상태 : false
```

unpark() 메소드를 호출해서 WAITING 상태를 빠져 나온다.

- main 스레드가 Thread-1을 start() 하면 Thread-1은 RUNNABLE 상태가 된다.

- Thread-1은 Thread.park()를 호출한다. Thread-1은 RUNNABLE -> WAITING 상태가 된다.

- main 스레드가 Thread-1을 unpark()로 깨운다. Thread-1은 대기 상태에서 RUNNABLE 상태로 바뀐다.

LockSupport는 특정 스레드를 WAITING 상태로, 또 RUNNABLE 상태로 변경할 수 있다.
그런데 대기 상태로 바꾸는 LockSupport.park()는 매개변수가 없는데 실행 상태로 바꾸는
LockSupport.unpark(thread1)는 왜 특정 스레드를 지정하는 매개변수가 있을까?
왜냐하면 실행 중인 스레드는 LockSupport.park()를 호출해서 스스로 대기 상태에 빠질 수 있지만, 대기 상태의 스레드는 자신의 코드를 실행할 수 없기 때문이다.

**인터럽트 사용**

```java
package thread.sync.lock;

import java.util.concurrent.locks.LockSupport;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class LockSupportMainV1 {

  public static void main(String[] args) {
    Thread thread1 = new Thread(new ParkTest(), "Thread-1");
    thread1.start();

    sleep(100);
    log("Thread-1 state : " + thread1.getState());

    log("main -> unpark(Thread-1)");
    //LockSupport.unpark(thread1);
    thread1.interrupt();
  }

  static class ParkTest implements Runnable {

    @Override
    public void run() {
      log("park 시작");
      LockSupport.park();
      log("park 종료, state : " + Thread.currentThread().getState());
      log("인터럽트 상태 : " + Thread.currentThread().isInterrupted());

    }

  }

}
```

```text
19:17:27.272 [ Thread-1] park 시작
19:17:27.349 [     main] Thread-1 state : WAITING
19:17:27.349 [     main] main -> unpark(Thread-1)
19:17:27.350 [ Thread-1] park 종료, state : RUNNABLE
19:17:27.356 [ Thread-1] 인터럽트 상태 : true
```

실행 결과를 보면 스레드가 RUNNABLE 상태로 깨어난 것을 확인할 수 있고 해당 스레드의 인터럽트의 상태도 true인 것을 확인할 수 있다.
이처럼 WAITING 상태의 스레드는 인터럽트를 걸어서 중간에 깨울 수 있다.

## 2. LockSupport2

이번에는 스레드를 특정 시간 동안만 대기하는 parkNanos(nanos)를 호출해보자.

- parkNanos(nanos) : 스레드를 나노초 동안만 TIMED_WAITING 상태로 변경한다. 지정한 나노초가 지나면
TIMED_WAITING 상태에서 빠져나와서 RUNNABLE 상태로 변경된다.

```java
package thread.sync.lock;

import java.util.concurrent.locks.LockSupport;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class LockSupportMainV2 {
  public static void main(String[] args) {
    Thread thread1 = new Thread(new ParkTask(), "Thread-1");
    thread1.start();
    
    sleep(100);
    log("Thread-1 state : " + thread1.getState());
  }
  
  static class ParkTask implements Runnable {
    @Override
    public void run() {
      log("park 시작, 2초 대기");
      LockSupport.parkNanos(2000_000000);
      log("park 종료, state : " + Thread.currentThread().getState());
      log("인터럽트 상태 : " + Thread.currentThread().isInterrupted());
    }
  }
}
```

```text
19:33:56.654 [ Thread-1] park 시작, 2초 대기
19:33:56.730 [     main] Thread-1 state : TIMED_WAITING
19:33:58.671 [ Thread-1] park 종료, state : RUNNABLE
19:33:58.676 [ Thread-1] 인터럽트 상태 : false
```

결과를 보면 Thread-1이 실행되고 Thread-1은 parkNanos(2000_000000); 으로 인해 TIMED_WAITING 상태가 된다.

2초가 지나면 RUNNABLE 상태로 전환된다.

**인터럽트**
- BLOCKED 상태는 인터럽트가 걸려도 대기 상태를 빠져나오지 못한다. 여전히 BLOCKED 상태다.

- WAITING, TIMED_WAITING 상태는 인터럽트가 걸리면 대기 상태를 빠져나온다. 그래서 RUNNABLE 상태로 변한다.

**용도**
- BLOCKED 상태는 자바의 synchronized에서 락을 획득하기 위해 대기할 때 사용된다.
- WAITING, TIMED_WAITING 상태는 스레드가 특정 조건이나 시간동안 대기할 때 발생하는 상태다.
- WAITING 상태는 다양한 상황에서 사용된다. 예를 들어, Thread.join(), LockSupport.park(), Object.wait()와 같은 메서드 호출 시 WAITING 상태가 된다.

BLOCKED, WAITING, TIMED_WAITING 상태 모두 스레드가 대기하며, 실행 스케줄링에 들어가지 않기 때문에, 
CPU 입장에서 보면 실행하지 않는 비슷한 상태이다. 

- BLOCKED 상태는 synchronized 에서만 사용하는 특별한 대기 상태라고 이해하면 된다.
- WAITING, TIMED_WAITING 상태는 범용적으로 활용할 수 있는 대기 상태라고 이해하면 된다.

## 3. ReentrantLoack - 이론

**synchronized 단점**
- 무한 대기 : BLOCKED 상태의 스레드는 락이 풀릴 때 까지 무한 대기한다.
-> 특정 시간까지만 대기하는 타임아웃X
-> 중간에 인터럽트X

- 공정성 : 락이 돌아왔을 때 BLOCKED 상태의 여러 스레드 중에 어떤 스레드가 락을 획득할 지 알 수 없다.

```java
package java.util.concurrent.locks;

public interface Lock {
  void lock();
  void lockInterruptibly() throws InterruptedException;
  boolean tryLock();
  boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
  void unlock();
}
```

Lock 인터페이스는 동시성 프로그래밍에서 쓰이는 안전한 임계 영역을 위한 락을 구현하는데 사용된다.

Lock 인터페이스는 다음과 같은 메서드를 제공하고 대표적인 구현체로 ReentrantLock이 있다. 

void lock()
- 락을 획득한다. 만약 다른 스레드가 이미 락을 획득했다면, 락이 풀릴 때까지 현재 스레드는 대기(WAITING)한다. 
이 메서드는 인터럽트에 응답하지 않는다.

- 여기서 사용하는 락은 객체 내부에 있는 모니터 락이 아니라 Lock 인터페이스와 ReentrantLock이 제공하는 기능이다!

void lockInterruptibly()
- 락 획득을 시도하되, 다른 스레드가 인터럽트할 수 있도록 한다. 만약 다른 스레드가 이미 락을 획득했다면, 현재 스레드는 락을 획득할 때까지 대기한다.
대기 중에 인터럽트가 발생하면 InterruptedException이 발생하며 락 획득을 포기한다.

boolean tryLock()
- 락 획득을 시도하고, 즉시 성공 여부를 반환한다. 만약 다른 스레드가 이미 락을 획득했다면 false를 반환하고,
그렇지 않으면 락을 획득하고 true를 반환한다. 

boolean tryLock(long time, TimeUnit unit)
- 주어진 시간 동안 락 획득을 시도한다. 주어진 시간 안에 락을 획득하면 true를 반환한다.
주어진 시간이 지나도 락을 획득하지 못한 경우 false를 반환한다. 이 메서드는 대기 중 인터럽트가 발생하면
InterruptedException이 발생하며 락 획득을 포기한다.

void unlock()
- 락을 해제한다. 락을 해제하면 락 획득을 대기 중인 스레드 중 하나가 락을 획득할 수 있게 된다.
- 락을 획득한 스레드가 호출해야 하며, 그렇지 않으면 IllegalMonitorStateExcepion이 발생 할 수 있다.

참고 : lock() 메서드는 인터럽트에 응하지 않는다고 되어있는데 lock() 메서드의 설명을 보면 대기(WAITING) 상태인데 인터럽트에 응하지 않는다고 되어 있다.

왜 그런걸까? lock()을 호출해서 락을 얻기 위해 대기중인 스레드에 인터럽트가 발생하면 순간 대기 상태를 빠져나오는 것은 맞다.
그래서 아주 짧지만 WAITING -> RUNNABLE이 된다. 그런데 lock() 메서드 안에서 해당 스레드를 다시 WAITING 상태로 강제로 변경해버린다.
이런 원리로 인터럽트를 무시하는 것이다.

**공정성**

비공정 모드 특징
- 성능 우선: 락을 획득하는 속도가 빠르다.
- 선점 가능 : 새로운 스레드가 기존 대기 스레드보다 먼저 락을 획득할 수 있다.
- 기아 현상 가능성 : 특정 스레드가 계속해서 락을 획득하지 못할 수 있다.

공정 모드 특징
- 공정성 보장 : 대기 큐에서 먼저 대기한 스레드가 락을 먼저 획득한다.
- 기아 현상 방지 : 모든 스레드가 언젠가 락을 획득할 수 있게 보장된다.
- 성능 저하 : 락을 획득하는 속도가 느려질 수 있다.

## 4. ReentrantLock - 활용 
```java
package thread.sync;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccountV4 implements BankAccount {
  private int balance;
  
  private final Lock lock = new ReentrantLock();
  
  public BankAccountV4(int initialBalance) {
    this.balance = initialBalance;
  }
  
  @Override
  public boolean withdraw(int amount) {
    log("거래 시작 : " + getClass().getSimpleName());
    
    lock.lock(); // ReentrantLock 이용하여 lock을 걸기
    try {
      log("[검증 시작] 출금액 : " + amount + ", 잔액 : " + balance); 
      if(balance < amount) {
        log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
        return false;
      }
      log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
      sleep(1000);
      balance = balance - amount;
      log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);
    } finally {
      lock.unlock(); // ReentrantLock 이용하여 lock을 해제 
    }
    log("거래 종료");
    return true; 
  }
  
  @Override
  public int getBalance() {
    lock.lock(); 
    try {
      return balance;
    } finally {
      lock.unlock(); 
    }
  }
  
}
```

결과는 다음과 같다.

```text
20:46:18.866 [       t1] 거래 시작 : BankAccountV4
20:46:18.866 [       t2] 거래 시작 : BankAccountV4
20:46:18.879 [       t1] [검증 시작] 출금액 : 800, 잔액 : 1000
20:46:18.880 [       t1] [검증 완료] 출금액 : 800, 잔액 : 1000
20:46:19.334 [     main] t1 state : TIMED_WAITING
20:46:19.334 [     main] t2 state : WAITING
20:46:19.889 [       t1] [출금 완료] 출금액 : 800, 잔액 : 200
20:46:19.890 [       t1] 거래 종료
20:46:19.890 [       t2] [검증 시작] 출금액 : 800, 잔액 : 200
20:46:19.891 [       t2] [검증 실패]
20:46:19.895 [     main] 최종 잔액 : 200
```

- synchronized(this) 대신에 lock.lock()을 사용해서 락을 건다.
-> 여기서 this는 스레드가 현재 작업하러 들어간 인스턴스(객체) 주소를 의미한다. 
-> lock() -> unlock() 까지는 안전한 임계 영역이 된다.

- 임계 영역이 끝나면 반드시 락을 반납해야 한다. 그렇지 않으면 대기하는 스레드가 락을 얻지 못한다.
-> 따라서 lock.unlock()은 반드시 finally 블럭에 작성해야한다. 이렇게 하면 검증에 실패해서 중간에
return을 호출해도 또는 중간에 예상치 못한 예외가 발생해도 lock.unlock()이 반드시 호출된다.











