# 생산자 소비자 문제2

## 1. Lock Condition 
생산자가 생산자를 깨우고, 소비자가 소비자를 깨우는 비효율 문제를 어떻게 해결할 수 있을까?

해결 방안으로는 생산자 스레드는 데이터를 생성하고, 대기중인 소비자 스레드에게 알려주어야 한다.
반대로 소비자 스레드는 데이터를 소비하고, 대기중인 생산자 스레드에게 알려주면 된다.
결국 생산자 스레드가 대기하는 대기 집합과, 소비자 스레드가 대기하는 대기 집합을 둘로 나누면 된다. 
그리고 생산자 스레드가 데이터를 생산하면 소비자 스레드가 대기하는 대기 집합에만 알려주고, 소비자 스레드가
데이터를 소비하면 생산자 스레드가 대기하는 대기 집합에만 알려주면 되는 것이다. 
이렇게 생산자용, 소비자용 대기 집합을 서로 나누어 분리하면 비효율 문제를 깔끔하게 해결할 수 있다.
그럼 대기 집합을 어떻게 분리할 수 있을까? 바로 앞서 학습한 Lock, ReentrantLock을 사용하면 된다.

코드를 작성해보자.

```java
package thread.bounded;

import java.util.ArrayDeque;
import java.util.Queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static util.MyLogger.log;

public class BoundedQueueV4 implements BoundedQueue {
  
  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  
  private final Queue<String> queue = new ArrayDeque<>();
  private final int max;
  
  public BoundedQueueV4(int max) {
    this.max = max;
  }
  
  public void put(String data) {
    lock.lock();
    try {
      while(queue.size() == max) {
        log("[put] 큐가 가득 참, 생산자 대기");
        try {
          condition.await();
          log("[put] 생산자 깨어남");
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      queue.offer(data);
      log("[put] 생산자 데이터 저장, signal() 호출");
      condition.signal();
    } finally {
      lock.unlock();
    }
  }
  
  public String take() {
    lock.lock();
    try {
      while (queue.isEmpty()) {
        log("[take] 큐에 데이터가 없음, 소비자 대기");
        
        try {
          condition.await();
          log("[take] 소비자 깨어남");
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      String data = queue.poll();
      log("[take] 소비자 데이터 획득, signal() 호출");
      condition.signal();
      return data; 
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public String toString() {
    return queue.toString(); 
  }
}
```

**Condition**
Condition condition = lock.newCondition();
Condition은 ReentrantLock을 사용하는 스레드가 대기하는 스레드 대기 공간이다.
lock.newCondition() 메서드를 호출하면 스레드 대기 공간이 만들어진다. Lock(ReentrantLock)의 스레드 
대기 공간은 이렇게 만들 수 있다. 
참고로 Object.wait()에서 사용한 스레드 대기 공간은 모든 객체 인스턴스가 내부에 기본으로 가지고 있다.
반면에 Lock(ReentrantLock)을 사용하는 경우 이렇게 스레드 대기 공간을 직접 만들어서 사용해야 한다. 

**condition.await()**
Object.wait()와 유사한 기능이다. 지정한 Condition에 현재 스레드를 대기(WAITING) 상태로 보관한다.
이때 ReentrantLock에서 획득한 락을 반납하고 대기 상태로 condition에 보관된다.

**condition.signal()**
Object.notify()와 유사한 기능이다. 지정한 condition에서 대기중인 스레드를 하나 깨운다.
깨어난 스레드는 condition에서 빠져나온다. 

코드를 실행해보자. (생산자를 먼저 실행한다.)

```text
18:32:02.652 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV4 ==

18:32:02.655 [     main] 생산자 시작
18:32:02.669 [producer1] [생산 시도] data1 -> []
18:32:02.669 [producer1] [put] 생산자 데이터 저장, signal() 호출
18:32:02.670 [producer1] [생산 완료] data1 -> [data1]
18:32:02.777 [producer2] [생산 시도] data2 -> [data1]
18:32:02.778 [producer2] [put] 생산자 데이터 저장, signal() 호출
18:32:02.778 [producer2] [생산 완료] data2 -> [data1, data2]
18:32:02.880 [producer3] [생산 시도] data3 -> [data1, data2]
18:32:02.881 [producer3] [put] 큐가 가득 참, 생산자 대기

18:32:02.982 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
18:32:02.983 [     main] producer1: TERMINATED
18:32:02.983 [     main] producer2: TERMINATED
18:32:02.983 [     main] producer3: WAITING

18:32:02.984 [     main] 소비자 시작
18:32:02.987 [consumer1] [소비 시도]  ? <- [data1, data2]
18:32:02.987 [consumer1] [take] 소비자 데이터 획득, signal() 호출
18:32:02.987 [producer3] [put] 생산자 깨어남
18:32:02.988 [consumer1] [소비 완료] data1 <- [data2]
18:32:02.988 [producer3] [put] 생산자 데이터 저장, signal() 호출
18:32:02.989 [producer3] [생산 완료] data3 -> [data2, data3]
18:32:03.096 [consumer2] [소비 시도]  ? <- [data2, data3]
18:32:03.097 [consumer2] [take] 소비자 데이터 획득, signal() 호출
18:32:03.097 [consumer2] [소비 완료] data2 <- [data3]
18:32:03.196 [consumer3] [소비 시도]  ? <- [data3]
18:32:03.197 [consumer3] [take] 소비자 데이터 획득, signal() 호출
18:32:03.198 [consumer3] [소비 완료] data3 <- []

18:32:03.311 [     main] 현재 상태 출력, 큐 데이터 : []
18:32:03.311 [     main] producer1: TERMINATED
18:32:03.312 [     main] producer2: TERMINATED
18:32:03.312 [     main] producer3: TERMINATED
18:32:03.312 [     main] consumer1: TERMINATED
18:32:03.313 [     main] consumer2: TERMINATED
18:32:03.313 [     main] consumer3: TERMINATED
18:32:03.314 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV4 ==
```

앞에서 Object 클래스에서 이용한 wait(), notify()랑 같은 원리이다. 
아직 condition(스레드 대기 공간)을 분리하지 않았기 때문에 기존과 같다.

## 2. 생산자 소비자 대기 공간 분리

```java
package thread.bounded;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static util.MyLogger.log;

public class BoundedQueueV5 implements BoundedQueue {
  
  private final Lock lock = new ReentrantLock();
  private final Condition producerCond = lock.newCondition(); // 생산자 스레드 대기 집합 생성
  private final Condition consumerCond = lock.newCondition(); // 소비자 스레드 대기 집합 생성
  
  private final Queue<String> queue = new ArrayDeque<>();
  private final int max;
  
  public BoundedQueueV5(int max) {
    this.max = max;
  }
  
  @Override
  public void put(String data) {
    lock.lock();
    try {
      while(queue.size() == max) {
        log("[put] 큐가 가득 참, 생산자 대기");
        try {
          producerCond.await();
          log("[put] 생산자 깨어남");
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      queue.offer(data);
      log("[put] 생산자 데이터 저장, consumerCond.signal() 호출");
      consumerCond.signal();
    } finally {
      lock.unlock(); 
    }
  }
  
  @Override
  public String take() {
    lock.lock();
    try {
      while(queue.isEmpty()) {
        log("[take] 큐에 데이터가 없음, 소비자 대기");
        try {
          consumerCond.await();
          log("[take] 소비자 깨어남");
        } catch (InterruptedException e) {
          throw new RuntimeException(e); 
        }
      }
      String data = queue.poll();
      log("[take] 소비자 데이터 획득, producerCond.signal() 호출");
      producerCond.signal();
      return data;
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public String toString() {
    return queue.toString(); 
  }
      
  
}
```
위 코드에서 lock.newCondition()을 두 번 호출해서 ReentrantLock을 사용하는 스레드 대기 공간을 2개 만들었다.

```java
import java.util.concurrent.locks.ReentrantLock;

private final ReentrantLock lock = new ReentrantLock();
private final Condition producerCond = lock.newCondition();
private final Condition consumerCond = lock.newCondition();
```

**Condition 분리**
- consumerCond : 소비자를 위한 스레드 대기 공간
- producerCond : 생산자를 위한 스레드 대기 공간

**put(data) - 생산자 스레드가 호출**
- 큐가 가득 찬 경우 : producerCond.await()를 호출해서 생산자 스레드를 생산자 전용 스레드 대기 공간에 보관한다.
- 데이터를 저장한 경우 : 생산자가 데이터를 생산하면 큐에 데이터가 추가된다. 따라서 소비자를 깨우는 것이 좋다.
consumerCond.signal()를 호출해서 소비자 전용 스레드 대기 공간에 신호를 보낸다.
이렇게 하면 대기중인 소비자 스레드가 하나 깨어나서 데이터를 소비 할 수 있다.

**take() - 소비자 스레드가 호출**
- 큐가 빈 경우 : consumerCond.await()를 호출해서 소비자 스레드를 소비자 전용 스레드 대기 공간에 보관한다.
- 데이터를 소비한 경우 : 소비자가 데이터를 소비한 경우 큐에 여유 공간이 생긴다. 
따라서 생산자를 깨우는 것이 좋다. producerCond.signal()를 호출해서 생산자 전용 스레드 대기 공간에 신호를 보낸다. 
이렇게 하면 대기중인 생산자 스레드가 하나 깨어나서 데이터를 추가할 수 있다.

생산자를 먼저 실행해서 결과를 보자. 

```text
19:01:07.449 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV5 ==

19:01:07.453 [     main] 생산자 시작
19:01:07.469 [producer1] [생산 시도] data1 -> []
19:01:07.470 [producer1] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:01:07.470 [producer1] [생산 완료] data1 -> [data1]
19:01:07.563 [producer2] [생산 시도] data2 -> [data1]
19:01:07.564 [producer2] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:01:07.564 [producer2] [생산 완료] data2 -> [data1, data2]
19:01:07.666 [producer3] [생산 시도] data3 -> [data1, data2]
19:01:07.667 [producer3] [put] 큐가 가득 참, 생산자 대기

19:01:07.781 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
19:01:07.782 [     main] producer1: TERMINATED
19:01:07.782 [     main] producer2: TERMINATED
19:01:07.782 [     main] producer3: WAITING

19:01:07.783 [     main] 소비자 시작
19:01:07.786 [consumer1] [소비 시도]  ? <- [data1, data2]
19:01:07.787 [consumer1] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:01:07.787 [producer3] [put] 생산자 깨어남
19:01:07.788 [consumer1] [소비 완료] data1 <- [data2]
19:01:07.788 [producer3] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:01:07.788 [producer3] [생산 완료] data3 -> [data2, data3]
19:01:07.899 [consumer2] [소비 시도]  ? <- [data2, data3]
19:01:07.900 [consumer2] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:01:07.900 [consumer2] [소비 완료] data2 <- [data3]
19:01:08.002 [consumer3] [소비 시도]  ? <- [data3]
19:01:08.002 [consumer3] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:01:08.003 [consumer3] [소비 완료] data3 <- []

19:01:08.102 [     main] 현재 상태 출력, 큐 데이터 : []
19:01:08.103 [     main] producer1: TERMINATED
19:01:08.104 [     main] producer2: TERMINATED
19:01:08.104 [     main] producer3: TERMINATED
19:01:08.105 [     main] consumer1: TERMINATED
19:01:08.105 [     main] consumer2: TERMINATED
19:01:08.106 [     main] consumer3: TERMINATED
19:01:08.106 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV5 ==
```

소비자를 먼저 실행 한 결과도 보자. 

```text
19:02:19.354 [     main] == [소비자 먼저 실행] 시작, BoundedQueueV5 ==

19:02:19.357 [     main] 소비자 시작
19:02:19.365 [consumer1] [소비 시도]  ? <- []
19:02:19.365 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
19:02:19.466 [consumer2] [소비 시도]  ? <- []
19:02:19.466 [consumer2] [take] 큐에 데이터 없음, 소비자 대기
19:02:19.573 [consumer3] [소비 시도]  ? <- []
19:02:19.574 [consumer3] [take] 큐에 데이터 없음, 소비자 대기

19:02:19.688 [     main] 현재 상태 출력, 큐 데이터 : []
19:02:19.697 [     main] consumer1: WAITING
19:02:19.698 [     main] consumer2: WAITING
19:02:19.698 [     main] consumer3: WAITING

19:02:19.699 [     main] 생산자 시작
19:02:19.701 [producer1] [생산 시도] data1 -> []
19:02:19.702 [producer1] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:02:19.702 [consumer1] [take] 소비자 깨어남
19:02:19.703 [producer1] [생산 완료] data1 -> [data1]
19:02:19.703 [consumer1] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:02:19.704 [consumer1] [소비 완료] data1 <- []
19:02:19.806 [producer2] [생산 시도] data2 -> []
19:02:19.807 [producer2] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:02:19.807 [consumer2] [take] 소비자 깨어남
19:02:19.807 [producer2] [생산 완료] data2 -> [data2]
19:02:19.808 [consumer2] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:02:19.809 [consumer2] [소비 완료] data2 <- []
19:02:19.916 [producer3] [생산 시도] data3 -> []
19:02:19.917 [producer3] [put] 생산자 데이터 저장, consumerCond.signal() 호출
19:02:19.917 [consumer3] [take] 소비자 깨어남
19:02:19.917 [producer3] [생산 완료] data3 -> [data3]
19:02:19.917 [consumer3] [take] 소비자 데이터 획득, producerCond.signal() 호출
19:02:19.918 [consumer3] [소비 완료] data3 <- []

19:02:20.022 [     main] 현재 상태 출력, 큐 데이터 : []
19:02:20.022 [     main] consumer1: TERMINATED
19:02:20.023 [     main] consumer2: TERMINATED
19:02:20.023 [     main] consumer3: TERMINATED
19:02:20.024 [     main] producer1: TERMINATED
19:02:20.024 [     main] producer2: TERMINATED
19:02:20.024 [     main] producer3: TERMINATED
19:02:20.025 [     main] == [소비자 먼저 실행] 종료, BoundedQueueV5 ==
```

이전에 wait(), notify() 메소드를 생각해보자. 임의의 스레드를 깨웠다.
하지만 위 결과는 생산자와 소비자를 구별하는 집합에 스레드를 따로 넣고 그 집합에 가서 깨운다.
즉, 코드의 효율성이 올라가서 깔끔하게 작업이 실행된다. 

**Object.notify() vs Condition.signal()**
Object.notify()
- 대기 중인 스레드 중 임의의 하나를 선택해서 깨운다. 스레드가 깨어나는 순서는 정의되어 있지 않으며,
JVM 구현에 따라 다르다. 보통은 먼저 들어온 스레드가 먼저 수행되지만 구현에 따라 다를 수 있다.
- synchronized 블록 내에서 모니터 락을 가지고 있는 스레드가 호출해야 한다.

Condition.signal()
- 대기 중인 스레드 중 하나를 깨우며, 일반적으로 FIFO 순서로 깨운다. 이 부분은 자바 버전과 구현에 따라
달라질 수 있지만, 보통 Condition 구현은 Queue 구조를 사용하기 때문에 FIFO 순서로 깨운다.
- ReentrantLock을 가지고 있는 스레드가 호출해야 한다. 

## 3. 스레드의 대기
**synchronized 대기**
- 대기 1 : 락 획득 대기
-> BLOCKED 상태로 락 획득 대기
-> synchronized를 시작할 때 락이 없으면 대기
-> 다른 스레드가 synchronized를 빠져나갈 때 대기가 풀리며 락 획득 시도

- 대기 2 : wait() 대기
-> WAITING 상태로 대기
-> wait()를 호출 했을 때 스레드 대기 집합에서 대기
-> 다른 스레드가 notify()를 호출 했을 때 빠져나감

소비자1 스레드, 소비자2 스레드, 소비자3 스레드가 있는데 소비자1 스레드가 먼저 락을 획득하여
데이터를 꺼내오거나 큐에 데이터가 없으면 스레드 대기 집합에서 대기할 것이다.
소비자2 스레드, 소비자3 스레드는 소비자1 스레드가 작업을 하고 있는 동안 임계 영역에 들어가지 못한다.
그런데 이렇게되면 소비자2,소비자3 스레드는 어딘가에 보관되어야 한다. BLOCKED 상태의 스레드를 보관하는 곳이 락 대기 집합이다.
이후에 소비자1 스레드는 큐에 데이터가 없으니 스레드 대기 집합으로 이동하고 락을 반납한 다음에 락 대기 집합에서 임의의 락이 락을 획득하과서
작업을 수행한다. 소비자2,3 스레드도 큐에 데이터가 없는 것을 확인하고 스레드 대기 집합에서 대기한다.
이제 생산자1 스레드가 데이터를 넣고 notify();를 호출 하여 스레드 대기 집합에 있는 임의의 스레드를 깨운다.
이때 깨어난 스레드가 소비자1 스레드라면 소비자 1스레드는 깨어나고나서 바로 작업을 수행하는 것이 아니라 락 대기 집합으로 다시 이동한다.
왜냐면 아직 락을 획득하지 못했기 때문이다. 지금은 생산자1 스레드가 락을 갖고 있기 때문에 생산자1 스레드가 락을 반납해야 RUNNABLE 상태로 바뀐다.
즉, 스레드 대기 집합, 락 대기 집합이라는 두개의 관문을 통과해야 RUNNABLE 상태로 바뀌는 것이다. 

**synchronized vs ReentrantLock 대기**

synchronized와 마찬가지로 Lock(ReentrantLock)도 2가지 단계의 대기 상태가 존재한다.
먼저 synchronized 대기를 보자.

synchronized

- 대기 1: 모니터 락 획득 대기
-> 자바 객체 내부의 락 대기 집합에서 관리
-> BLOCKED 상태로 락 획득 대기
-> synchronized를 시작할 때 락이 없으면 대기 
-> 다른 스레드가 synchronized를 빠져나갈 때 락을 획득 시도, 락을 획득하면 락 대기 집합을 빠져나감

- 대기2 : wait() 대기
-> wait()를 호출 했을 때 자바 객체 내부의 스레드 대기 집합에서 관리
-> WAITING 상태로 대기
-> 다른 스레드가 notify()를 호출 했을 때 스레드 대기 집합을 빠져나감

ReentrantLock 

- 대기1 : ReentrantLock 락 획득 대기
-> ReentrantLock의 대기 큐에서 관리
-> WAITING 상태로 락 획득 대기
-> lock.lock()을 호출 했을 때 락이 없으면 대기 
-> 다른 스레드가 lock.unlock()을 호출 해쓸 때 대기가 풀리며 락 획득 시도, 락을 획득하면 대기 큐를 빠져나감

- 대기2 : await() 대기
-> condition.await()를 호출 했을 때, condition 객체의 스레드 대기 공간에서 관리
-> WAITING 상태로 대기
-> 다른 스레드가 condition.signal()을 호출 했을 때 condition 객체의 스레드 대기 공간에서 나감

