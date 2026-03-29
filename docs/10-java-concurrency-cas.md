# CAS

## 1. 동기화와 원자적 연산

컴퓨터 과학에서 사용하는 원자적 연산의 의미는 해당 연산이 더 이상 나눌 수 없는 단위로 수행된다는 것을 의미한다.
즉, 원자적 연산은 중단되지 않고, 다른 연산과 간섭 없이 완전히 실행되거나 전혀 실행 되지 않는 성질을 가지고 있다.
쉽게 이야기해서 멀티스레드 상황에서 다른 스레드의 간섭 없이 안전하게 처리되는 연산이라는 뜻이다.

예를 들어서 volatile int i = 0;이 있으면 i = 1은 둘로 쪼갤 수 없는 원자적 연산이다
이 연산은 오른쪽에 있는 1의 값을 왼쪽의 i 변수에 대입만 하는거라 쪼갤 수 없다.

하지만 이런 경우라면? ex) i = i + 1; 
1. 이거는 오른쪽에 있는 i의 값을 읽는다.
2. 읽은 값에 1을 더한다.
3. 더한 값을 왼쪽의 i 변수에 대입한다.

원자적 연산은 멀티스레드 상황에서 문제가 발생하지 않는다.
하지만 원자적 연산이 아닌 경우에는 synchronized 블럭이나 Lock등을 사용해서 안전한 임계 영역을 만들어야 한다.

순서대로 실행 하는 경우와 동시에 실행 하는 경우를 살펴보자.

예를 들어보자. 처음에 i = 0이라고 가정하자.

```text
스레드1 : i = i + 1 연산 수행
스레드1 : i의 값을 읽는다. i는 0이다.
스레드1 : 읽은 0에 1을 더해서 1을 만든다.
스레드1 : 더한 1을 왼쪽의 i 변수에 대입한다.
결과 : i의 값은 1이다.

스레드2 : i = i + 1 연산 수행
스레드2 : i의 값을 읽는다. i는 1이다.
스레드2 : 읽은 1에 1을 더해서 2를 만든다.
스레드2 : 더한 2를 왼쪽의 i 변수에 대입한다.
결과 : i의 값은 2이다.
```
2개의 스레드가 각각 한 번 연산을 수행했으므로 i의 값은 0 -> 2.

동시에 실행 되는 경우를 보자.

```text
처음에 i = 0이락 가정하겠다.

스레드1 : i = i + 1 연산 수행
스레드2 : i = i + 1 연산 수행
스레드1 : i의 값을 읽는다. i는 0이다.
스레드2 : i의 값을 읽는다. i는 0이다.
스레드1 : 읽은 0에 1을 더해서 1을 만든다.
스레드2 : 읽은 0에 1을 더해서 1을 만든다.
스레드1 : 더한 1을 왼쪽의 i 변수에 대입한다.
스레드2 : 더한 1을 왼쪽의 i 변수에 대입한다.
결과 : i의 값은 1이다. 
```

## 2. 원자적 연산 

```java
package thread.cas.increment;

public interface IncrementInteger {
  void increment();
  
  int get(); 
}
```
- IncrementInteger는 값을 증가하는 기능을 가진 숫자 기능을 제공하는 인터페이스다.
- increment() : 값을 하나 증가
- get() : 값을 조회

```java
package thread.cas.increment;

public class BasicInteger implements IncrementInteger {
  
  private int value;
  
  @Override
  public void increment() {
    value++;
  }
  
  @Override
  public int get() {
    return value;
  }
  
}
```

- increment()를 호출하면 value++를 통해서 값을 하나 증가시킨다.
-> value 값은 인스턴스의 필드이기 때문에, 여러 스레드가 공유할 수 있다. 이렇게 공유 가능한 자원에
++와 같은 원자적이지 않은 연산을 사용하면 멀티스레드 상황에 문제가 될 수 있다.

```java
package thread.cas.increment;

import java.util.ArrayList;
import java.util.List;

import static util.ThreadUtils.sleep;

public class IncrementThreadMain {
  
  public static final int THREAD_COUNT = 1000;

  public static void main(String[] args) throws InterruptedException {
    test(new BasicInteger()); 
  }
  
  private static void test(IncrementInteger incrementInteger) throws InterruptedException {
    
    Runnable runnable = new Runnable() {
      
      @Override
      public void run() {
        sleep(10); // 너무 빨리 실행되기 때문에, 다른 스레드와 동시 실행을 위해 잠깐 쉬었다가 실행
        incrementInteger.increment();
      }
      
    };
    
    List<Thread> threads = new ArrayList<>();
    for(int i = 0; i < THREAD_COUNT; i++) {
      Thread thread = new Thread(runnable);
      threads.add(thread);
      thread.start();
    }
    
    for(Thread thread : threads) {
      thread.join();
    }
    
    int result = incrementInteger.get();
    System.out.println(incrementInteger.getClass().getSimpleName() + " result: " + result);
    
  }
  
}
```

결과는 다음과 같다.

```text
BasicInteger result : 985
```

왜 위와 같이 결과가 나올까? 예를 들어서, Thread300, Thread301이 value에 동시에 접근했다면
현재 value값이 300인데 둘다 300에 1을 더해서 301을 나오게 하는 것이다. 공용자원의 값이 2가 올라야하는데
1밖에 안오르는 문제가 생겨서 그렇다. 

**원자적 연산 - volatile, synchronized**
```java
package thread.cas.increment;

public class VolatileInteger implements IncrementInteger {

  private volatile int value;

  @Override
  public void increment() {
    value++;
  }

  @Override
  public int get() {
    return value;
  }

}
```

IncrementThreadMain에 test(new VolatileInteger());를 추가하고 실행해보자..
```java
public static void main(String[] args) throws InterruptedException {
    test(new BasicInteger());
    test(new volatileInteger());
}
```

결과는 다음과 같다.
```text
VolatileInteger result : 986
```

실행 결과를 보면 VolatileInteger도 여전히 1000이 아니라 더 작은 숫자가 나온다.
volatile은 여러 CPU 사이에 발생하는 캐시 메모리와 메인 메모리가 동기화 되지 않는 문제를 해결할 뿐이다.

다음과 같이 SyncInteger 클래스를 만들고 synchronized를 적용해서 안전한 임계 영역을 만들어보자.

```java
package thread.cas.increment;

public class SyncInteger implements IncrementInteger {
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
```

```java
public static void main(String[] args) throws InterruptedException {
    test(new BasicInteger());
    test(new volatileInteger());
    test(new SyncInteger());
}
```

```text
SyncInterger result : 1000
```
synchronized를 통해 안전한 임계 영역을 만들고 value++ 연산을 수행했더니 정확히 1000이라는 결과가 나왔다.
1000개의 스레드가 안전하게 value++ 연산을 수행한 것이다.

## 2. 원자적 연산 - AtomicInteger
자바는 앞서 만든 SyncInteger와 같이 멀티스레드 상황에서 안전하게 증가 연산을 수행할 수 있는
AtomicInteger라는 클래스를 제공한다. 이름 그대로 원자적인 Integer라는 뜻이다. 

```java
package thread.cas.increment;

import java.util.concurrent.atomic.AtomicInteger;

public class MyAtomicInteger implements IncrementInteger {
  
  AtomicInteger atomicInteger = new AtomicInteger(0);
  
  @Override
  public void increment() {
    atomicInteger.incrementAndGet();
  }
  
  @Override
  public int get() {
    return atomicInteger.get();
  }
}
```

- new AtomicInteger(0) : 초기값을 지정한다. 생략하면 0부터 시작한다.
- incrementAndGet() : 값을 하나 증가하고 증가된 결과를 반환한다.
- get() : 현재 값을 반환한다.

```java
public static void main(String[] args) throws InterruptedException {
    test(new BasicInteger());
    test(new volatileInteger());
    test(new SyncInteger());
    test(new MyAtomicInteger());
}
```

```text
MyAtomicInteger result : 1000
```

실행 결과를 보면 AtomicInteger를 사용하면 MyAtomicInteger의 결과도 1000인 것을 확인할 수 있다.
1000개의 스레드가 안전하게 증가 연산을 수행한 것이다.

이제 AtomicInteger를 한번 간략하게 구현해보자.

```java
package thread.cas.increment;

public class IncrementPerformanceMain {
  
  public static final long COUNT = 100_000_000;

  public static void main(String[] args) {
    test(new BasicInteger());
    test(new volatileInteger());
    test(new SyncInteger());
    test(new MyAtomicInteger());
  }
  
  private static void test(IncrementInteger incrementInteger) {
    long startMs = System.currentTimeMillis();
    for(long i = 0; i < COUNT; i++) {
      incrementInteger.increment();
    }
    long endMs = System.currentTimeMillis();
    System.out.println(incrementInteger.getClass().getSimpleName() + ": ms=" + (endMs - startMs));
  }
  
}
```

결과는 다음과 같다. 

```text
BasicInteger: ms=195
VolatileInteger: ms=888
SyncInterger: ms=2018
MyAtomicInteger: ms=774
```

**BasicInteger**
- 가장 빠르다.
- CPU 캐시를 적극 사용한다. CPU 캐시의 위력을 알 수 있다.
- 안전한 임계 영역도 없고, volatile도 사용하지 않기 때문에 멀티스레드 상황에는 사용할 수 없다.
- 단일 스레드가 사용하는 경우에 효율적이다.

**VolatileInteger**
- volatile을 사용해서 CPU 캐시를 사용하지 않고 메인 메모리를 사용한다.
- 안전한 임계 영역이 없기 때문에 멀티스레드 상황에는 사용할 수 없다.
- 단일 스레드가 사용하기에는 BasicInteger보다 느리다. 

**SyncInteger**
- synchronized를 사용한 안전한 임계 영역이 있기 때문에 멀티스레드 상황에도 안전하게 사용할 수 있다.
- MyAtomicInteger보다 성능이 느리다.

**MyAtomicInteger**
- 자바가 제공하는 AtomicInteger를 사용한다. 멀티스레드 상황에 안전하게 사용할 수 있다.
- 성능도 synchronized, Lock(ReentrantLock)을 사용하는 경우보다 1.5 ~ 2배 정도 빠르다.

## 3. CAS 연산1

**락 기반 방식의 문제점**
SyncInteger와 같은 클래스는 데이터를 보호하기 위해 락을 사용한다.
여기서 말하는 락은 synchronized, Lock(ReentrantLock) 등을 사용하는 것을 말한다. 
락은 특정 자원을 보호하기 위해 스레드가 해당 자원에 대한 접근하는 것을 제한한다. 
락이 걸려 있는 동안 다른 스레드들은 해당 자원에 접근할 수 없고, 락이 해제될 때까지 대기해야 한다.

**CAS**
이런 문제를 해결하기 위해 락을 걸지 않고 원자적인 연산을 수행할 수 있는 방법이 있는데 이것을 CAS(Compare-And-Set)연산이라 한다.
이 방법은 락을 사용하지 않기 때문에 락 프리 기법이라한다.
참고로 CAS 연산은 락을 완전히 대체하는 것은 아니고, 작은 단위의 일부 영역에 적용할 수 있다.
기본은 락을 사용하고, 특별한 경우에 CAS를 적용할 수 있다고 생각하면 된다.

코드를 통해 CAS 연산을 알아보자.

```java
package thread.cas;

import java.util.concurrent.atomic.AtomicInteger;

public class CasMainV1 {

  public static void main(String[] args) {
    AtomicInteger atomicInteger = new AtomicInteger(0);
    System.out.println("start value = " + atomicInteger.get());
    
    boolean result1 = atomicInteger.compareAndSet(0, 1);
    System.out.println("result1 = " + result1 + ", value = " + atomicInteger.get());
    
    boolean result2 = atomicInteger.compareAndSet(0, 1);
    System.out.println("result2 = " + result2 + ", value = " + atomicInteger.get());
  }
  
}
```

- new AtomicInteger(0) : 내부에 있는 기본 숫자 값을 0으로 설정한다.
- 자바는 AtomicXxx의 compareAndSet() 메서드를 통해 CAS 연산을 지원한다.

결과는 다음과 같다.

```text
start value = 0
result1 = true, value = 1
result2 = false, value = 1
```

**compareAndSet(0, 1)**
atomicInteger가 가지고 있는 값이 현재 0이면 이 값을 1로 변경하라는 매우 단순한 메서드이다.
- 만약 atomicInteger의 값이 현재 0이라면 atomicInteger의 값은 1로 변경된다. 이 경우 true를 반환한다.
- 만약 atomicInteger의 값이 현재 0이라면 atomicInteger의 값은 변경되지 않는다. 이 경우 false를 반환한다.

근데 잘 보면 값을 확인하고 비교 한 다음에 변경하는 작업이 하나의 연산으로 끝나지 않는다.
- 먼저 메인 메모리에 있는 값을 확인
- 해당 값이 기대하는 값(0)이라면 원하는 값(1)으로 변경한다.

**CPU 하드웨어의 지원**
CAS 연산은 이렇게 원자적이지 않은 두 개의 연산을 CPU 하드웨어 차원에서 특별하게 하나의 원자적인 연산으로
묶어서 제공하는 기능이다. 이것은 소프트웨어가 제공하는 기능이 아니라 하드웨어가 제공하는 기능이다.
대부분의 현대 CPU들은 CAS 연산을 위한 명령어를 제공한다.

CPU는 다음 두 과정을 묶어서 하나의 원자적인 명령으로 만들어버린다. 따라서 중간에 다른 스레드가 개입할 수 없다.

1. 참조값으로 접근한 변수의 값을 확인한다.
2. 읽은 값이 0이면 1로 변경한다. 

CPU는 두 과정을 하나의 원자적인 명령으로 만들기 위해 1번과 2번 사이에 다른 스레드가 참조값의 값을 변경하지 못하게 막는다.
참고로 1번과 2번 사이의 시간은 CPU 입장에서 보면 아주 잠깐 찰나의 순간이다. 그래서 성능에 큰 영향을 끼치지 않는다. 

## 4. CAS 연산2
어떤 값을 하나 증가하는 value++ 연산은 원자적 연산이 아니다.
이 연산은 다음과 같다. (i = i + 1;)

이 연산은 다음 순서로 나누어 실행된다. i의 초기 값은 0으로 가정하겠다.
1. 오른쪽에 있는 i의 값을 읽는다. i의 값은 0이다.
2. 읽은 0에 1을 더해서 1을 만든다.
3. 더한 1을 왼쪽의 i 변수에 대입한다. 

1번과 3번 연산 사이에 다른 스레드가 i의 값을 변경할 수 있기 때문에, 문제가 될 수 있다.
따라서 value++ 연산을 여러 스레드에서 사용한다면, 락을 건 다음에 값을 증가해야 한다. 

CAS 연산을 활용해서 락 없이 값을 증가하는 기능을 만들어보자.

```java
package thread.cas;

import java.util.concurrent.atomic.AtomicInteger;

import static util.MyLogger.log;

public class CasMainV2 {

  public static void main(String[] args) {
    AtomicInteger atomicInteger = new AtomicInteger(0);
    System.out.println("start value = " + atomicInteger.get());
    
    // incrementAndGet 구현
    int resultValue1 = incrementAndGet(atomicInteger);
    System.out.println("resultValue1 = " + resultValue1);
    
    int resultValue2 = incrementAndGet(atomicInteger);
    System.out.println("resultValue2 = " + resultValue2);
  }
  
  private static int incrementAndGet(AtomicInteger atomicInteger) {
    int getValue; 
    boolean result;
    do {
      getValue = atomicInteger.get();
      log("getValue : " + getValue);
      result = atomicInteger.compareAndSet(getValue, getValue + 1);
      log("result : " + result);
    } while (!result);
    
    return getValue + 1; 
  }
}
```

CAS 연산을 사용하면 여러 스레드가 같은 값을 사용하는 상황에서도 락을 걸지 않고, 안전하게 값을 증가할 수 있다.
여기서는 락을 걸지 않고 CAS 연산을 사용해서 값을 증가했다.
- getValue = atomicInteger.get()을 사용해서 value 값을 읽는다.
- compareAndSet(getValue, getValue + 1)을 사용해서, 방금 읽은 value 값이 메모리의 value 값과
같다면 value 값을 하나 증가한다. 여기서 CAS 연산을 사용한다.
- 만약 CAS 연산이 성공한다면 true를 반환하고 do~while문을 빠져나간다.
- 만약 CAS 연산을 실패한다면 false를 반환하고 do~while문을 다시 시작한다. 

```text
start value = 0
14:23:57.157 [     main] getValue : 0
14:23:57.163 [     main] result : true
result = 1
14:23:57.167 [     main] getValue : 1
14:23:57.167 [     main] result : true
result2 = 2
```

지금은 main 스레드 하나로 순서대로 실행되기 때문에 CAS 연산이 실패하는 상황을 볼 수 없다.
우리가 기대하는 실패하는 상황은 연산의 중간에 다른 스레드가 값을 변경해버리는 것이다.
멀티스레드로 실행해서 CAS 연산이 실패하는 경우에 어떻게 작동하는지 알아보자.

## 5. CAS 연산3
멀티스레드를 사용해서 중간에 다른 스레드가 먼저 값을 증가시켜 버리는 경우를 알아보자.
그리고 CAS 연산이 실패하는 경우에 어떻게 되는지 알아보자.
이 경우에도 값을 정상적으로 증가시킬 수 있을까?

```java
package thread.cas;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class CasMainV3 {
  
  private static final int THREAD_COUNT = 2;

  public static void main(String[] args) throws InterruptedException {
    AtomicInteger atomicInteger = new AtomicInteger(0);
    System.out.println("start value = " + atomicInteger.get());
    
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        incrementAndGet(atomicInteger);
      }
    };
    
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < THREAD_COUNT; i++) {
      Thread thread = new Thread(runnable);
      threads.add(thread);
      thread.start();
    }
    
    for(Thread thread : threads) {
      thread.join();
    }
    
    int result = atomicInteger.get();
    System.out.println(atomicInteger.getClass().getSimpleName() + " resultValue : " + result);
    
  }
  
  private static int incrementAndGet(AtomicInteger atomicInteger) {
    int getValue;
    boolean result;
    do {
      getValue = atomicInteger.get();
      sleep(100); // 스레드 동시 실행을 위한 대기
      log("getValue : " + getValue);
      result = atomicInteger.compareAndSet(getValue, getValue + 1);
      log("result : " + result);
    } while (!result); 
    
    return getValue + 1; 
  }
  
}
```

결과는 다음과 같다.

```text
start value = 0
16:28:11.736 [ Thread-1] getValue : 0
16:28:11.736 [ Thread-0] getValue : 0
16:28:11.739 [ Thread-1] result : true
16:28:11.739 [ Thread-0] result : false
16:28:11.853 [ Thread-0] getValue : 1
16:28:11.853 [ Thread-0] result : true
AtomicInteger resultValue : 2
```

결과를 보면 좀 의아한 것이 있다. 처음엔 getValue가 0이다. 그런데 16:28:11.739 [ Thread-1] result : true
16:28:11.739 [ Thread-0] result : false 여기서 각 스레드의 반환값이 서로 다르다. 
왜 다를까? 두 스레드가 동시에 incrementAndGet에 접근을 해서 getValue의 값은 0인 것을 확인했지만
스레드 1번이 먼저 compareAndSet을 호출해서 값을 증가 시켰기 때문에 스레드0번이 봤을땐 getValue의 현재값이 
처음에 읽었던 getValue값과 달라서 while문을 한번 더 실행하고나서 값을 바꾸는 것이다. 어쨌든 바꾸는 과정중에
읽었던 값이랑 달라서 false를 반환한다. 

**CAS(Compare-And-Swap)와 락(Lock) 방식의 비교**

락(Lock) 방식
- 비관적 접근법
- 데이터에 접근하기 전에 항상 락을 획득
- 다른 스레드의 접근을 막음
- "다른 스레드가 방해할 것이다"라고 가정

CAS(Compare-And-Swap) 방식
- 낙관적 접근법
- 락을 사용하지 않고 데이터에 바로 접근
- 충돌이 발생하면 그때 재시도
- "대부분의 경우 충돌이 없을 것이다" 라고 가정

정리하면 충돌이 많이 없는 경우에 CAS 연산이 빠른 것을 확인할 수 있다.
그럼 충돌이 많이 발생하지 않는 연산은 어떤 것이 있을까? 언제 CAS 연산을 사용하면 좋을까?
사실 간단한 CPU 연산은 너무 빨리 처리되기 때문에 충돌이 자주 발생하지 않는다.

## 6. CAS 락 구현1
CAS는 단순한 연산 뿐만 아니라, 락을 구현하는데 사용할 수도 있다.
synchronized, Lock(ReentrantLock) 없이 CAS를 활용해서 락을 구현해보자.
먼저 CAS의 필요성을 이해하기 위해 CAS 없이 직접 락을 구현해보자.

```java
package thread.cas.spinLock;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class SpinLockBad {
  private volatile boolean lock = false;
  
  public void lock() {
    log("락 획득 시도");
    while(true) {
      if (!lock) { // 1. 락 사용 여부 확인
        sleep(100); // 문제 상황 확인용, 스레드 대기
        lock = true; // 2. 락의 값 변경
        break; // while 탈출
      } else {
        // 락을 획득할 때 까지 스핀 대기(바쁜 대기) 한다.
        log("락 획득 실패 - 스핀 대기");
      }
    }
    log("락 획득 완료");
  }
  
  public void unlock() {
    lock = false;
    log("락 반납 완료"); 
  }
  
}
```

- 스레드가 락을 획득하면 lock의 값이 true가 된다.
- 스레드가 락을 반납하면 lock의 값이 false가 된다.

```java
package thread.cas.spinlock;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class SpinLockMain {
  public static void main(String[] args) {
    SpinLockBad spinLock = new SpinLockBad();

    Runnable task = new Runnable() {
      @Override
      public void run() {
        spinLock.lock();
        try {
          // critical section
          log("비즈니스 로직 실행");
        } finally {
          spinLock.unlock();
        }
      }
    };

    Thread t1 = new Thread(task, "Thread-1");
    Thread t2 = new Thread(task, "Thread-2");

    t1.start();
    t2.start();
  }
}
```

코드를 실행하면 결과는 다음과 같다.

```text
16:42:56.643 [ Thread-1] 락 획득 시도
16:42:56.643 [ Thread-2] 락 획득 시도
16:42:56.756 [ Thread-2] 락 획득 완료
16:42:56.756 [ Thread-1] 락 획득 완료
16:42:56.756 [ Thread-2] 비즈니스 로직 실행
16:42:56.756 [ Thread-1] 비즈니스 로직 실행
16:42:56.757 [ Thread-2] 락 반납 완료
16:42:56.757 [ Thread-1] 락 반납 완료
```

결과를 보면 좀 이상한걸 볼 수 있다. 
원래 예상했던 결과는 스레드가 락을 획득하고 로직을 실행 후, 반납을 완료 한 다음에 다른 스레드가 과정을 완료하고 반납해야 한다.
그런데 둘 다 락을 획득했다. 왜 그런걸까?
이건 앞에서 수없이 봤지만 동시성 문제로 인해 false값을 먼저 읽고 락을 획득 한 것이다.
이걸 어떻게 해결 해야할까? CAS 연산을 이용해보자. 










