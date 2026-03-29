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

**중간 정리 - 생산자 소비자 문제**
08-producer-consumer 마크다운 파일에 있던 BoundedQueue버전1부터 현재 위에서 정리했던 BoundedQueue버전5까지
잠깐 분석 하는 시간을 갖도록 하자. 

**BoundedQueueV1**
- 단순한 큐 자료 구조이다. 스레드를 제어할 수 없기 때문에, 버퍼가 가득 차거나, 버퍼에 데이터가 없는 한정된 버퍼 상황에서 문제가
발생한다. 
- 버퍼가 가득 찬 경우 : 생산자의 데이터를 버린다.
- 버퍼에 데이터가 없는 경우 : 소비자는 데이터를 획득 할 수 없다.

**BoundedQueueV2**
- 앞서 발생한 문제를 해결하기 위해 반복문을 사용해서 스레드를 대기하는 방법을 적용했다. 하지만
synchronized 임계 영역 안에서 락을 들고 대기하기 때문에, 다른 스레드가 임계 영역에 접근할 수 없는 문제가 발생했다.
결과적으로 나머지 스레드는 모두 BLOCKED 상태가 되고, 자바 스레드 세상이 멈추는 심각한 문제가 발생했다.

**BoundedQueueV3**
- synchronized와 함께 사용할 수 있는 wait(), notify(), notifyAll()을 사용해서 문제를 해결했다.
wait()를 사용하면 스레드가 대기할 때, 락을 반납하고 대기한다. 이후에 notify()를 호출하면 스레드가 깨어나면서 락 획득을 시도한다.
이때 락을 획득하면 RUNNABLE 상태가 되고, 락을 획득하지 못하면 락 획득을 대기하는 BLOCKED 상태가 된다.
- 이렇게 해서 스레드를 제어하는 큐 자료 구조를 만들 수 있었다. 생산자 스레드는 버퍼가 가득차면 버퍼에 여유가 생길 때 까지
대기한다. 소비자 스레드는 버퍼에 데이터가 없으면 버퍼에 데이터가 들어올 때 까지 대기한다.
- 이런 구현 덕분에 단순한 자료 구조를 넘어서 스레드까지 제어할 수 있는 자료 구조를 완성했다. 
- 이 방식의 단점은 스레드가 대기하는 대기 집합이 하나이기 때문에, 원하는 스레드를 선택해서 깨울 수 없다는 문제가
있었다. 예를 들어, 생산자는 데이터를 생산한 다음 대기하는 소비자를 깨워야 하는데, 대기하는 생산자를 깨울 수 있다.
따라서 비효율이 발생한다. 물론 이렇게 해도 비효율이 있을 뿐 로직은 모두 정상 작동한다.

**BoundedQueueV4**
- synchronized와 wait(), notify()를 사용해서 구현하면 스레드 대기 집합이 하나라는 단점이 있다.
이 단점을 극복하려면 스레드 대기 집합을 생산자 전용과 소비자 전용으로 나누어야 한다. 이렇게 하려면 Lock을 사용해야 한다.
- 여기서는 단순히 synchronized와 wait(), notify()를 사용해서 구현한 코드를 Lock을 사용하도록 변경했다.

**BoundedQueueV5**
- Lock(ReentrantLock)은 Condition이라는 스레드 대기 공간을 제공한다. 이 스레드 대기 공간을
원하는 만큼 따로 만들 수 있다.
-> productCond : 생산자 스레드를 위한 전용 대기 공간
-> consumerCond : 소비자 스레드를 위한 전용 대기 공간 

## 4. BlockingQueue 

BlockingQueue를 사용하도록 기존 코드를 변경해보자.

```java
package thread.bounded;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BoundedQueueV6_1 implements BoundedQueue {
  
  private BlockingQueue<String> queue;
  
  public BoundedQueueV6_1(int max) {
    queue = new ArrayBlockingQueue<>(max);
  }
  
  public void put(String data) {
    try {
      queue.put(data);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String take() {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public String toString() {
    return queue.toString(); 
  }
  
}
```
- BlockingQueue.put(data) : BoundedQueueV5.put() 과 같은 기능을 제공한다.
- BlockingQueue.take() : BoundedQueueV5.take() 와 같은 기능을 제공한다.

ArrayBlockingQueue.put() 의 코드를 살펴보자.

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockingQueue {

  final Object[] items;
  int count;
  ReentrantLock lock;
  Condition notEmpty; // 소비자 스레드가 대기하는 condition
  Condition notFull; // 생산자 스레드가 대기하는 condition

  public void put(E e) throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (count == items.length) {
        notFull.await(); 
      }
      enqueue(e);
    } finally {
      lock.unlock(); 
    }
  }
  
  private void enqueue(E e) {
    items[putIndex] = e;
    count++;
    notEmpty.signal(); 
  }

}
```

BoundMain에서 코드를 실행하면 결과는 다음과 같다.

```text
17:10:27.425 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV6_1 ==

17:10:27.435 [     main] 생산자 시작
17:10:27.477 [producer1] [생산 시도] data1 -> []
17:10:27.478 [producer1] [생산 완료] data1 -> [data1]
17:10:27.567 [producer2] [생산 시도] data2 -> [data1]
17:10:27.568 [producer2] [생산 완료] data2 -> [data1, data2]
17:10:27.670 [producer3] [생산 시도] data3 -> [data1, data2]

17:10:27.779 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
17:10:27.781 [     main] producer1: TERMINATED
17:10:27.782 [     main] producer2: TERMINATED
17:10:27.783 [     main] producer3: WAITING

17:10:27.784 [     main] 소비자 시작
17:10:27.789 [consumer1] [소비 시도]  ? <- [data1, data2]
17:10:27.791 [consumer1] [소비 완료] data1 <- [data2]
17:10:27.791 [producer3] [생산 완료] data3 -> [data2, data3]
17:10:27.903 [consumer2] [소비 시도]  ? <- [data2, data3]
17:10:27.904 [consumer2] [소비 완료] data2 <- [data3]
17:10:28.011 [consumer3] [소비 시도]  ? <- [data3]
17:10:28.012 [consumer3] [소비 완료] data3 <- []

17:10:28.118 [     main] 현재 상태 출력, 큐 데이터 : []
17:10:28.119 [     main] producer1: TERMINATED
17:10:28.120 [     main] producer2: TERMINATED
17:10:28.122 [     main] producer3: TERMINATED
17:10:28.123 [     main] consumer1: TERMINATED
17:10:28.126 [     main] consumer2: TERMINATED
17:10:28.127 [     main] consumer3: TERMINATED
17:10:28.129 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV6_1 ==
```

일부 로그 메시지가 안나오긴 했지만 결과는 거의 비슷하다. 

## 5. BlockingQueue - 기능

멀티스레드를 사용할 때는 응답성이 중요하다. 예를 들어서 대기 상태에 있어도, 고객이 중지 요청을 하거나,
또는 너무 오래 대기한 경우 포기하고 빠져나갈 수 있는 방법이 필요하다. 
생산자가 무언가 데이터를 생산하는데, 버퍼가 빠지지 않아서 너무 오래 대기해야 한다면, 무한정 기다리는 것 보다는 작업을 포기하고
고객분께는 "죄송합니다. 현재 시스템에 문제가 있습니다. 나중에 다시 시도해주세요." 라고 하는 것이 더 나은 선택일 것이다.

예를 들어 생산자는 서버에 상품을 주문하는 고객일 수 있다. 고객이 상품을 주문하면, 고객의 요청을 생산자 스레드가 받아서
중간에 있는 큐에 넣어준다고 가정하자. 소비자 스레드는 큐에서 주문 요청을 꺼내서 주문을 처리하는 스레드다.
만약에 선착순 할인 이벤트가 크게 성공해서 갑자기 주문이 폭주하면 주문을 만드는 생산자 스레드는 매우 바쁘게
주문을 큐에 넣게 된다. 

큐의 한계가 1000개라고 가정하자. 생산자 스레드는 순간적으로 1000개가 넘는 주문을 큐에 담았다. 
소비자 스레드는 한 번에 겨우 10개 정도의 주문만 처리할 수 있다. 이 상황에서 생산자 스레드는 계속 생산을 시도한다.
결국 소비가 생산을 따라가지 못하고, 큐가 가득 차게 된다. 

이런 상황이 되면 수 많은 생산자 스레드는 큐 앞에서 대기하게 된다. 결국 고객도 응답을 받지 못하고 무한 대기하게 된다.
고객 입장에서 무작정 무한 대기하고 결과도 알 수 없는 상황이 가장 나쁜 상황일 것이다.

이렇게 생산자 스레드가 큐에 데이터를 추가할 때 큐가 가득 찬 경우, 또는 큐에 데이터를 추가하기 위해 너무 오래 대기하는 경우에는
데이터 추가를 포기하고, 고객에게 주문 폭주로 너무 많은 사용자가 몰려서 요청을 처리할 수 없다거나, 또는
나중에 다시 시도해달라고 하는 것이 더 나은 선택일 것이다. 

이런 문제를 해결하기 위해 BlockingQueue는 각 상황에 맞는 다양한 메서드를 제공한다.

**Throws Exception - 대기 시 예외**
- add(e) : 지정된 요소를 큐에 추가하며, 큐가 가득 차면 IllegalStateException 예외를 던진다.
- remove() : 큐에서 요소를 제거하며 반환한다. 큐가 비어 있으면 NoSuchElementException 예외를 던진다.
- element() : 큐의 머리 요소를 반환하지만, 요소를 큐에서 제거하지 않는다. 큐가 비어 있으면 NoSuchElementException 예외를 던진다.

**Special Value - 대기 시 즉시 반환**
- offer(e) : 지정된 요소를 큐에 추가하려고 시도하며, 큐가 가득 차면 false를 반환한다.
- poll() : 큐에서 요소를 제거하고 반환한다. 큐가 비어 있으면 null을 반환한다.
- peek() : 큐의 머리 요소를 반환하지만, 요소를 큐에서 제거하지 않는다. 큐가 비어 있으면 null을 반환한다. 

**Blocks - 대기**
- put(e) : 지정된 요소를 큐에 추가할 때까지 대기한다. 큐가 가득 차면 공간이 생길 때까지 대기한다.
- take() : 큐에서 요소를 제거하고 반환한다. 큐가 비어 있으면 요소가 준비될 때까지 대기한다.

**Times Out - 시간 대기**
- offer(e, time, unit) : 지정된 요소를 큐에 추가하려고 시도하며, 지정된 시간 동안 큐가 비워지기를 기다리다가
시간이 초과되면 false를 반환한다. 
- poll(time, unit) : 큐에서 요소를 제거하고 반환한다. 큐에 요소가 없다면 지정된 시간 동안 요소가 준비되기를
기다리다가 시간이 초과되면 null을 반환한다.

```java
package thread.bounded;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static util.MyLogger.log;

public class BoundedQueueV6_2 implements BoundedQueue {

  private BlockingQueue<String> queue;

  public BoundedQueueV6_2(int max) {
    queue = new ArrayBlockingQueue<>(max);
  }

  public void put(String data) {
    boolean result = queue.offer(data);
    log("저장 결과 시도 = " + result);
  }

  public String take() {
    return queue.poll();
  }

  @Override
  public String toString() {
    return queue.toString();
  }

}
```

결과는 다음과 같다. 

```text
11:16:00.144 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV6_2 ==

11:16:00.148 [     main] 생산자 시작
11:16:00.167 [producer1] [생산 시도] data1 -> []
11:16:00.168 [producer1] 저장 시도 결과 = true
11:16:00.169 [producer1] [생산 완료] data1 -> [data1]
11:16:00.264 [producer2] [생산 시도] data2 -> [data1]
11:16:00.264 [producer2] 저장 시도 결과 = true
11:16:00.265 [producer2] [생산 완료] data2 -> [data1, data2]
11:16:00.374 [producer3] [생산 시도] data3 -> [data1, data2]
11:16:00.377 [producer3] 저장 시도 결과 = false
11:16:00.380 [producer3] [생산 완료] data3 -> [data1, data2]

11:16:00.485 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
11:16:00.486 [     main] producer1: TERMINATED
11:16:00.486 [     main] producer2: TERMINATED
11:16:00.487 [     main] producer3: TERMINATED

11:16:00.487 [     main] 소비자 시작
11:16:00.490 [consumer1] [소비 시도]  ? <- [data1, data2]
11:16:00.491 [consumer1] [소비 완료] data1 <- [data2]
11:16:00.594 [consumer2] [소비 시도]  ? <- [data2]
11:16:00.594 [consumer2] [소비 완료] data2 <- []
11:16:00.694 [consumer3] [소비 시도]  ? <- []
11:16:00.695 [consumer3] [소비 완료] null <- []

11:16:00.795 [     main] 현재 상태 출력, 큐 데이터 : []
11:16:00.796 [     main] producer1: TERMINATED
11:16:00.796 [     main] producer2: TERMINATED
11:16:00.796 [     main] producer3: TERMINATED
11:16:00.797 [     main] consumer1: TERMINATED
11:16:00.797 [     main] consumer2: TERMINATED
11:16:00.797 [     main] consumer3: TERMINATED
11:16:00.798 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV6_2 ==
```
실행 결과를 보면 11:16:00.374 [producer3] [생산 시도] data3 -> [data1, data2]
이 부분에서는 이미 Queue의 공간이 다 찼기 때문에 데이터를 더 넣지 못한다.
따라서, 저장 시도 결과는 false가 나오고 producer3 스레드는 큐에 데이터가 빠져 나올 때 까지 기다리지 않는다.
offer(data)는 성공하면 true를 반환하고, 버퍼가 가득 차면 즉시 false를 반환한다. 

소비자를 먼저 실행해보자. 

```text
11:22:09.491 [     main] == [소비자 먼저 실행] 시작, BoundedQueueV6_2 ==

11:22:09.494 [     main] 소비자 시작
11:22:09.503 [consumer1] [소비 시도]  ? <- []
11:22:09.511 [consumer1] [소비 완료] null <- []
11:22:09.614 [consumer2] [소비 시도]  ? <- []
11:22:09.615 [consumer2] [소비 완료] null <- []
11:22:09.718 [consumer3] [소비 시도]  ? <- []
11:22:09.719 [consumer3] [소비 완료] null <- []

11:22:09.828 [     main] 현재 상태 출력, 큐 데이터 : []
11:22:09.829 [     main] consumer1: TERMINATED
11:22:09.829 [     main] consumer2: TERMINATED
11:22:09.829 [     main] consumer3: TERMINATED

11:22:09.830 [     main] 생산자 시작
11:22:09.833 [producer1] [생산 시도] data1 -> []
11:22:09.833 [producer1] 저장 시도 결과 = true
11:22:09.834 [producer1] [생산 완료] data1 -> [data1]
11:22:09.947 [producer2] [생산 시도] data2 -> [data1]
11:22:09.948 [producer2] 저장 시도 결과 = true
11:22:09.948 [producer2] [생산 완료] data2 -> [data1, data2]
11:22:10.058 [producer3] [생산 시도] data3 -> [data1, data2]
11:22:10.059 [producer3] 저장 시도 결과 = false
11:22:10.059 [producer3] [생산 완료] data3 -> [data1, data2]

11:22:10.162 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
11:22:10.162 [     main] consumer1: TERMINATED
11:22:10.163 [     main] consumer2: TERMINATED
11:22:10.163 [     main] consumer3: TERMINATED
11:22:10.164 [     main] producer1: TERMINATED
11:22:10.164 [     main] producer2: TERMINATED
11:22:10.164 [     main] producer3: TERMINATED
11:22:10.165 [     main] == [소비자 먼저 실행] 종료, BoundedQueueV6_2 ==
```

take(), put()을 실행 할 때는 각각 큐에 공간이 있을 때 까지, 없을 때 까지를 기준으로 기다렸는데
offer(), poll() 메소드는 기다리는거 없이 있으면 꺼내오고 없으면 넣는 상황이 없으면 그대로 논리값을 반환한다. 

```java
package thread.bounded;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static util.MyLogger.log;

public class BoundedQueueV6_3 implements BoundedQueue {
    
  private BlockingQueue<String> queue;
  
  public BoundedQueueV6_3(int max) {
    queue = new ArrayBlockingQueue<>(max);
  }
  
  public void put(String data) {
    try {
      boolean result = queue.offer(data, 1, TimeUnit.NANOSECONDS);
      log("저장 시도 결과 = " + result);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String take() {
    try {
      return queue.poll(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public String toString() {
    return queue.toString(); 
  }
}
```

```text
11:38:08.685 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV6_3 ==

11:38:08.688 [     main] 생산자 시작
11:38:08.700 [producer1] [생산 시도] data1 -> []
11:38:08.701 [producer1] 저장 시도 결과 = true
11:38:08.702 [producer1] [생산 완료] data1 -> [data1]
11:38:08.808 [producer2] [생산 시도] data2 -> [data1]
11:38:08.808 [producer2] 저장 시도 결과 = true
11:38:08.808 [producer2] [생산 완료] data2 -> [data1, data2]
11:38:08.918 [producer3] [생산 시도] data3 -> [data1, data2]
11:38:08.919 [producer3] 저장 시도 결과 = false
11:38:08.919 [producer3] [생산 완료] data3 -> [data1, data2]

11:38:09.025 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
11:38:09.026 [     main] producer1: TERMINATED
11:38:09.027 [     main] producer2: TERMINATED
11:38:09.027 [     main] producer3: TERMINATED

11:38:09.028 [     main] 소비자 시작
11:38:09.030 [consumer1] [소비 시도]  ? <- [data1, data2]
11:38:09.030 [consumer1] [소비 완료] data1 <- [data2]
11:38:09.142 [consumer2] [소비 시도]  ? <- [data2]
11:38:09.143 [consumer2] [소비 완료] data2 <- []
11:38:09.245 [consumer3] [소비 시도]  ? <- []

11:38:09.354 [     main] 현재 상태 출력, 큐 데이터 : []
11:38:09.354 [     main] producer1: TERMINATED
11:38:09.355 [     main] producer2: TERMINATED
11:38:09.355 [     main] producer3: TERMINATED
11:38:09.356 [     main] consumer1: TERMINATED
11:38:09.356 [     main] consumer2: TERMINATED
11:38:09.357 [     main] consumer3: TIMED_WAITING
11:38:09.357 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV6_3 ==
11:38:11.261 [consumer3] [소비 완료] null <- []
```

코드를 보면 boolean result = queue.offer(data, 1, TimeUnit.NANOSECONDS); , return queue.poll(2, TimeUnit.SECONDS);
시간 안에 큐에 데이터가 잠깐이라도 없을때랑 있을때를 기다리는건데 계속 기다리는게 아니라 1ns, 2ns만큼만 기다린다.
근데 이 안에 조건을 만족하지 못해서 더이상 기다리지 않고 false값을 반환한다. 
