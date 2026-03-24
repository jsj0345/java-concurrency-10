# 생산자 소비자 문제

## 1. 생산자 소비자 문제

생산자 소비자 문제는 멀티스레드 프로그래밍에서 자주 등장하는 동시성 문제 중 하나로, 여러 스레드가 동시에 데이터를
생산하고 소비하는 상황을 다룬다.

멀티스레드의 핵심을 제대로 이해하려면 반드시 생산자 소비자 문제를 이해하고, 올바른 해결 방안도 함께 알아두어야 한다.

**기본 개념**
- 생산자 : 데이터를 생성하는 역할을 한다. 예를 들어, 파일에서 데이터를 읽어오거나 네트워크에서 데이터를 받아오는
스레드가 생산자 역할을 할 수 있다. 

- 소비자 : 생성된 데이터를 사용하는 역할을 한다. 예를 들어, 데이터를 처리하거나 저장하는 스레드가 
소비자 역할을 할 수 있다.

- 버퍼 : 생산자가 생성한 데이터를 일시적으로 저장하는 공간이다. 이 버퍼는 한정된 크기를 가지며, 생산자와
소비자가 이 버퍼를 통해 데이터를 주고받는다.

문제 상황 

- 생산자가 너무 빠를 때 : 버퍼가 가득 차서 더 이상 데이터를 넣을 수 없을 때까지 생산자가 데이터를 생성한다. 
버퍼가 가득 찬 경우 생산자는 버퍼에 빈 공간이 생길 때까지 기다려야 한다.

- 소비자가 너무 빠를 때 : 버퍼가 비어서 더 이상 소비할 데이터가 없을 때까지 소비자가 데이터를 처리한다.
버퍼가 비어있을 때 소비자는 버퍼에 새로운 데이터가 들어올 때까지 기다려야 한다.

이 문제는 다음 두 용어로 불린다.

- 생산자-소비자 문제 : 생산자-소비자 문제는, 생산자 스레드와 소비자 스레드가 특정 자원을 함께 생산하고, 소비하면서 발생하는 
문제이다.

- 한정된 버퍼 문제 : 이 문제는 결국 중간에 있는 버퍼의 크기가 한정되어 있기 때문에 발생한다.
따라서 한정된 버퍼 문제라고도 한다. 

## 2. 생산자 소비자 문제1

```java
package thread.bounded;

public interface BoundedQueue {
  void put(String data); // 버퍼에 데이터를 보관한다.
  
  String take(); // 버퍼에 보관된 값을 가져간다.
}
```

```java
package thread.bounded;

import java.util.ArrayDeque;
import java.util.Queue;

import static util.MyLogger.log;

public class BoundedQueueV1 implements BoundedQueue {
  
  private final Queue<String> queue = new ArrayDeque<>();
  
  private final int max;
  
  public BoundedQueueV1(int max) {
    this.max = max;
  }
  
  @Override
  public synchronized void put(String data) {
    if (queue.size() == max) {
      log("[put] 큐가 가득 참, 버림 : " + data);
      return;
    }
    queue.offer(data);
  }
  
  @Override
  public synchronized String take() {
    if (queue.isEmpty()) {
      return null;
    }
    return queue.poll();
  }
  
  @Override
  public String toString() {
    return queue.toString();
  }
  
}
```

```java
package thread.bounded;

import static util.MyLogger.log;

public class ProducerTask implements Runnable {
  
  private BoundedQueue queue;
  private String request;
  
  public ProducerTask(BoundedQueue queue, String request) {
    this.queue = queue;
    this.request = request;
  }
  
  @Override
  public void run() {
    log("[생산 시도] " + request + " -> " + queue);
    queue.put(request);
    log("[생산 완료] " + request + " -> " + queue);
  }
  
}
```

```java
package thread.bounded;

import static util.MyLogger.log;

public class ConsumerTask implements Runnable {
  private BoundedQueue queue;
  
  public ConsumerTask(BoundedQueue queue) {
    this.queue = queue;
  }
  
  @Override
  public void run() {
    log("[소비 시도] ? <- " + queue);
    String data = queue.take();
    log("[소비 완료] " + data + " <- " + queue);
  }
  
}
```

```java
package thread.bounded;

import java.util.ArrayList;
import java.util.List;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BoundedMain {

  public static void main(String[] args) {
    // 1. BoundedQueue 선택
    BoundedQueue queue = new BoundedQueueV1(2);
    
    // 2. 생산자, 소비자 실행 순서 선택
    producerFirst(queue); 
    //consumerFirst(queue);
  }
  
  private static void producerFirst(BoundedQueue queue) {
    log("== [생산자 먼저 실행] 시작, " + queue.getClass().getSimpleName() + "==");
    
    List<Thread> threads = new ArrayList<>();
    startProducer(queue, threads);
    printAllState(queue, threads);
    startConsumer(queue, threads);
    printAllState(queue, threads);
    
    log("== [생산자 먼저 실행] 종료, " + queue.getClass().getSimpleName() + "==");
  }
  
  private static void consumerFirst(BoundedQueue queue) {
    log("== [소비자 먼저 실행] 시작, " + queue.getClass().getSimpleName() + "==");
    
    List<Thread> threads = new ArrayList<>();
    startConsumer(queue, threads);
    printAllState(queue, threads);
    startProducer(queue, threads);
    printAllState(queue, threads);
    
    log("== [소비자 먼저 실행] 종료, " + queue.getClass().getSimpleName() + "==");
  }
  
  private static void startProducer(BoundedQueue queue, List<Thread> threads) {
    System.out.println();
    log("생산자 시작");
    for(int i = 1; i <= 3; i++) {
      Thread producer = new Thread(new ProducerTask(queue, "data"+i), "producer"+i);
      threads.add(producer);
      producer.start();
      sleep(100); 
    }
  }
  
  private static void startConsumer(BoundedQueue queue, List<Thread> threads) {
    System.out.println();
    log("소비자 시작"); 
    for(int i = 1; i<=3; i++) {
      Thread consumer = new Thread(new ConsumerTask(queue), "consumer"+i);
      threads.add(consumer);
      consumer.start();
      sleep(100); 
    }
  }
  
}
```

코드를 실행하면 결과는 다음과 같다.

```text
21:08:46.302 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV1 ==

21:08:46.308 [     main] 생산자 시작
21:08:46.330 [producer1] [생산 시도] data1 -> []
21:08:46.332 [producer1] [생산 완료] data1 -> [data1]
21:08:46.429 [producer2] [생산 시도] data2 -> [data1]
21:08:46.429 [producer2] [생산 완료] data2 -> [data1, data2]
21:08:46.541 [producer3] [생산 시도] data3 -> [data1, data2]
21:08:46.541 [producer3] [put] 큐가 가득 참, 버림 : data3
21:08:46.541 [producer3] [생산 완료] data3 -> [data1, data2]

21:08:46.652 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
21:08:46.652 [     main] producer1: TERMINATED
21:08:46.652 [     main] producer2: TERMINATED
21:08:46.652 [     main] producer3: TERMINATED

21:08:46.655 [     main] 소비자 시작
21:08:46.657 [consumer1] [소비 시도]  ? <- [data1, data2]
21:08:46.657 [consumer1] [소비 완료] data1 <- [data2]
21:08:46.768 [consumer2] [소비 시도]  ? <- [data2]
21:08:46.768 [consumer2] [소비 완료] data2 <- []
21:08:46.878 [consumer3] [소비 시도]  ? <- []
21:08:46.878 [consumer3] [소비 완료] null <- []

21:08:46.990 [     main] 현재 상태 출력, 큐 데이터 : []
21:08:46.990 [     main] producer1: TERMINATED
21:08:46.990 [     main] producer2: TERMINATED
21:08:46.990 [     main] producer3: TERMINATED
21:08:46.990 [     main] consumer1: TERMINATED
21:08:46.990 [     main] consumer2: TERMINATED
21:08:46.990 [     main] consumer3: TERMINATED
21:08:46.990 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV1 ==
```

위 결과를 분석해보자.

```text
21:08:46.302 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV1 ==

21:08:46.308 [     main] 생산자 시작
21:08:46.330 [producer1] [생산 시도] data1 -> []
```

이 부분에서 producer1이라는 스레드가 lock을 획득하고 data1을 ArrayDeque에 넣는다.

넣기 전이니까 처음엔 빈 배열이 맞다. 

```text
21:08:46.332 [producer1] [생산 완료] data1 -> [data1]
```

이제 넣었으니 [data1]이 나온다. 이후에, producer1 스레드는 락을 반납하고 producer2 스레드가 락을 획득한다.

획득하고 나서 다음과 같은 과정을 거친다.

```text
21:08:46.429 [producer2] [생산 시도] data2 -> [data1]
21:08:46.429 [producer2] [생산 완료] data2 -> [data1, data2]
```

락을 반납하고 producer3 스레드에게 락을 넘긴다. 

```text
21:08:46.541 [producer3] [생산 시도] data3 -> [data1, data2]
21:08:46.541 [producer3] [put] 큐가 가득 참, 버림 : data3
21:08:46.541 [producer3] [생산 완료] data3 -> [data1, data2]
```

ArrayDeque에 넣을 수 있는 데이터는 2개이므로 더이상 데이터를 넣지 못한다.
이러면 data3을 강제로 버려야한다. data3을 버리지 않으려면 어떻게 해야할까?
data3을 버리지 않는 대안은, 큐에 빈 공간이 생길 때 까지 p3 스레드가 기다리는 것이다. 언젠가는 소비자 스레드가
실행되어서 큐의 데이터를 가져갈 것이고, 큐에 빈 공간이 생기게 된다. 이때 큐에 데이터를 보관하는 것이다.
그럼 어떻게 기다릴까?

단순하게 생각하면 생산자 스레드가 반복문을 사용해서 큐에 빈 공간이 생기는지 주기적으로 체크한 다음에, 만약 빈 공간이 없다면
sleep()을 짧게 사용해서 잠시 대기하고, 깨어난 다음에 다시 반복문에서 큐의 빈 공간을 체크하는 식으로 구현하면 될 것 같다.

이제 생산자 관련 스레드들은 실행을 완료해서 다음과 같은 메시지가 나온다.
```text
21:08:46.652 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
21:08:46.652 [     main] producer1: TERMINATED
21:08:46.652 [     main] producer2: TERMINATED
21:08:46.652 [     main] producer3: TERMINATED
```

소비자 스레드를 보도록 하자. 

```text
21:08:46.655 [     main] 소비자 시작
21:08:46.657 [consumer1] [소비 시도]  ? <- [data1, data2]
21:08:46.657 [consumer1] [소비 완료] data1 <- [data2]
```

consumer1이라는 스레드가 락을 획득한 후, ArrayDeque에서 data1을 갖고온다.
이후에 consumer2라는 스레드가 락을 획득 한 다음에 ArrayDeque에서 data2를 갖고온다.

```text
21:08:46.768 [consumer2] [소비 시도]  ? <- [data2]
21:08:46.768 [consumer2] [소비 완료] data2 <- []
```

이제 consumer3이 데이터를 갖고와야한다. 락을 획득한 후에 어떻게 됐는지 보자. 

```text
21:08:46.878 [consumer3] [소비 시도]  ? <- []
21:08:46.878 [consumer3] [소비 완료] null <- []
```

빼올 데이터가 없다. 소비자 입장에서 큐에 데이터가 없다면 기다리는 것도 대안이다.
null을 받지 않는 대안은, 큐에 데이터가 추가될 때 까지 consumer3 스레드가 기다리는 것이다. 
언젠가는 생산자 스레드가 실행되어서 큐에 데이터를 추가할 것이다.
물론 생산자 스레드가 계속해서 데이터를 생산한다는 가정이 필요하다.
그럼 어떻게 기다릴 수 있을까?
단순하게 생각하면 소비자 스레드가 반복문을 사용해서 큐에 데이터가 있는지 주기적으로 체크한 다음에, 만약 데이터가 없다면
sleep()을 짧게 사용해서 대기하고, 깨어난 다음에 다시 반복문에서 큐에 데이터가 있는지 체크하는식으로 구현하면 될 것 같다.

이번에는 반대로 소비자 코드를 먼저 실행해보자.

```java
package thread.bounded;

import java.util.ArrayList;
import java.util.List;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BoundedMain {

  public static void main(String[] args) {
    // 1. BoundedQueue 선택
    BoundedQueue queue = new BoundedQueueV1(2);
    
    // 2. 생산자, 소비자 실행 순서 선택
    //producerFirst(queue); 
    consumerFirst(queue); // 소비자 먼저 실행 
  }
  
  private static void producerFirst(BoundedQueue queue) {
    log("== [생산자 먼저 실행] 시작, " + queue.getClass().getSimpleName() + "==");
    
    List<Thread> threads = new ArrayList<>();
    startProducer(queue, threads);
    printAllState(queue, threads);
    startConsumer(queue, threads);
    printAllState(queue, threads);
    
    log("== [생산자 먼저 실행] 종료, " + queue.getClass().getSimpleName() + "==");
  }
  
  private static void consumerFirst(BoundedQueue queue) {
    log("== [소비자 먼저 실행] 시작, " + queue.getClass().getSimpleName() + "==");
    
    List<Thread> threads = new ArrayList<>();
    startConsumer(queue, threads);
    printAllState(queue, threads);
    startProducer(queue, threads);
    printAllState(queue, threads);
    
    log("== [소비자 먼저 실행] 종료, " + queue.getClass().getSimpleName() + "==");
  }
  
  private static void startProducer(BoundedQueue queue, List<Thread> threads) {
    System.out.println();
    log("생산자 시작");
    for(int i = 1; i <= 3; i++) {
      Thread producer = new Thread(new ProducerTask(queue, "data"+i), "producer"+i);
      threads.add(producer);
      producer.start();
      sleep(100); 
    }
  }
  
  private static void startConsumer(BoundedQueue queue, List<Thread> threads) {
    System.out.println();
    log("소비자 시작"); 
    for(int i = 1; i<=3; i++) {
      Thread consumer = new Thread(new ConsumerTask(queue), "consumer"+i);
      threads.add(consumer);
      consumer.start();
      sleep(100); 
    }
  }
  
}
```

실행 결과는 다음과 같다.

```text
20:22:23.447 [     main] == [소비자 먼저 실행] 시작, BoundedQueueV1 ==

20:22:23.447 [     main] 소비자 시작
20:22:23.447 [consumer1] [소비 시도]  ? <- []
20:22:23.462 [consumer1] [소비 완료] null <- []
20:22:23.559 [consumer2] [소비 시도]  ? <- []
20:22:23.559 [consumer2] [소비 완료] null <- []
20:22:23.668 [consumer3] [소비 시도]  ? <- []
20:22:23.668 [consumer3] [소비 완료] null <- []

20:22:23.778 [     main] 현재 상태 출력, 큐 데이터 : []
20:22:23.778 [     main] consumer1: TERMINATED
20:22:23.778 [     main] consumer2: TERMINATED
20:22:23.778 [     main] consumer3: TERMINATED

20:22:23.778 [     main] 생산자 시작
20:22:23.782 [producer1] [생산 시도] data1 -> []
20:22:23.782 [producer1] [생산 완료] data1 -> [data1]
20:22:23.888 [producer2] [생산 시도] data2 -> [data1]
20:22:23.888 [producer2] [생산 완료] data2 -> [data1, data2]
20:22:23.999 [producer3] [생산 시도] data3 -> [data1, data2]
20:22:23.999 [producer3] [put] 큐가 가득 참, 버림 : data3
20:22:23.999 [producer3] [생산 완료] data3 -> [data1, data2]

20:22:24.110 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
20:22:24.110 [     main] consumer1: TERMINATED
20:22:24.110 [     main] consumer2: TERMINATED
20:22:24.110 [     main] consumer3: TERMINATED
20:22:24.110 [     main] producer1: TERMINATED
20:22:24.112 [     main] producer2: TERMINATED
20:22:24.112 [     main] producer3: TERMINATED
20:22:24.112 [     main] == [소비자 먼저 실행] 종료, BoundedQueueV1 ==
```

왜 이런 결과가 나올까? 일단 처음에 ArrayDeque에 쌓이는 데이터가 없으니까 당연히 비어 있을 것이다.
그래서 소비자 스레드가 먼저 실행되면 꺼낼게 없으니까 계속 빈 ArrayDeque만 나온다.
synchronized로 인하여 임계 영역에서는 한 스레드만 락을 획득해서 접근이 가능하므로 스레드가 볼 일을 다 봤으면
락을 반납하고 다른 스레드가 접근 하도록 해야한다. 이렇게되면 소비자들은 아무것도 하는게 없다.

이제 생산자 스레드 같은 경우에는, 데이터를 넣고 락을 반납하여 다음 생산자 스레드가 또 데이터를 넣는다.
데이터는 최대 2개까지만 쌓이니까 강제로 생산자3 스레드는 데이터를 버려야 하는 상황이 발생한다.
이러한 문제들을 해결하기 위해 다르게 구현해보자.

## 3. 생산자 소비자 문제2 
```java
package thread.bounded;

import java.util.ArrayDeque;
import java.util.Queue;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BoundedQueueV2 implements BoundedQueue {
    private final Queue<String> queue = new ArrayDeque<>();
    private final int max;
    
    public BoundedQueueV2(int max) {
      this.max = max;
    }
    
    public synchronized void put(String data) {
      while(queue.size() == max) {
        log("[put] 큐가 가득 참, 생산자 대기");
        sleep(1000);
      }
      queue.offer(data); 
    }
    
    public synchronized String take() {
      while(queue.isEmpty()) {
        log("[take] 큐에 데이터가 없음, 소비자 대기");
        sleep(1000);
      }
      return queue.poll();
    }
    
    @Override
    public String toString() {
      return queue.toString(); 
    }
  
}
```

**put(data) - 데이터를 버리지 않는 대안**
data3을 버리지 않는 대안은, 큐가 가득 찼을 때, 큐에 빈 공간이 생길 때 까지, 생산자 스레드가 기다리면 된다.
언젠가 소비자 스레드가 실행되어서 큐의 데이터를 가져갈 것이고, 그러면 큐에 데이터를 넣을 수 있는 공간이 생기게 된다.
그럼 어떻게 기다릴 수 있을까?
여기서는 생산자 스레드가 반복문을 사용해서 큐에 빈 공간이 생기는지 주기적으로 체크한다. 만약 빈 공간이 없다면
sleep()을 사용해서 잠시 대기하고, 깨어난 다음에 다시 반복문에서 큐의 빈 공간을 체크하는 식으로 구현했다.

**take() - 큐에 데이터가 없다면 기다리자**
소비자 입장에서 큐에 데이터가 없다면 기다리는 것도 대안이다.
큐에 데이터가 없을 때 null을 받지 않는 대안은, 큐에 데이터가 추가될 때 까지 소비자 스레드가 기다리는 것이다.
언젠가는 생산자 스레드가 실행되어서 큐의 데이터를 추가할 것이고, 큐에 데이터가 생기게 된다.
물론 생산자 스레드가 계속해서 데이터를 생산한다는 가정이 필요하다.

그럼 어떻게 기다릴 수 있을까?
여기서는 소비자 스레드가 반복문을 사용해서 큐에 데이터가 있는지 주기적으로 체크한 다음에, 만약 데이터가 없다면
sleep()을 사용해서 잠시 대기하고, 깨어난 다음에 다시 반복문에서 큐에 데이터가 있는지 체크하는 식으로 구현했다.

main 메소드에서 BoundedQueue queue = new BoundedQueueV2(2); 
producerFirst(queue); 를 실행해보자. 

결과는 다음과 같다.

```text
20:49:12.775 [     main] == [생산자 먼저 실행] 시작, BoundedQueueV2 ==

20:49:12.789 [     main] 생산자 시작
20:49:12.802 [producer1] [생산 시도] data1 -> []
20:49:12.802 [producer1] [생산 완료] data1 -> [data1]
20:49:12.902 [producer2] [생산 시도] data2 -> [data1]
20:49:12.902 [producer2] [생산 완료] data2 -> [data1, data2]
20:49:13.013 [producer3] [생산 시도] data3 -> [data1, data2]
20:49:13.013 [producer3] [put] 큐가 가득 참, 생산자 대기

20:49:13.124 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
20:49:13.124 [     main] producer1: TERMINATED
20:49:13.124 [     main] producer2: TERMINATED
20:49:13.124 [     main] producer3: TIMED_WAITING

20:49:13.124 [     main] 소비자 시작
20:49:13.128 [consumer1] [소비 시도]  ? <- [data1, data2]
20:49:13.234 [consumer2] [소비 시도]  ? <- [data1, data2]
20:49:13.337 [consumer3] [소비 시도]  ? <- [data1, data2]

20:49:13.437 [     main] 현재 상태 출력, 큐 데이터 : [data1, data2]
20:49:13.437 [     main] producer1: TERMINATED
20:49:13.438 [     main] producer2: TERMINATED
20:49:13.438 [     main] producer3: TIMED_WAITING
20:49:13.439 [     main] consumer1: BLOCKED
20:49:13.439 [     main] consumer2: BLOCKED
20:49:13.439 [     main] consumer3: BLOCKED
20:49:13.440 [     main] == [생산자 먼저 실행] 종료, BoundedQueueV2 ==
20:49:14.018 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:15.031 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:16.038 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:17.051 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:18.055 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:19.057 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:20.064 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:21.077 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:22.089 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:23.101 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:24.115 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:25.122 [producer3] [put] 큐가 가득 참, 생산자 대기
20:49:26.131 [producer3] [put] 큐가 가득 참, 생산자 대기
```

반대로, 생산자가 아닌 소비자를 먼저 실행 시켜보자. 

```text
20:50:35.001 [     main] == [소비자 먼저 실행] 시작, BoundedQueueV2 ==

20:50:35.001 [     main] 소비자 시작
20:50:35.015 [consumer1] [소비 시도]  ? <- []
20:50:35.015 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:35.126 [consumer2] [소비 시도]  ? <- []
20:50:35.235 [consumer3] [소비 시도]  ? <- []

20:50:35.336 [     main] 현재 상태 출력, 큐 데이터 : []
20:50:35.343 [     main] consumer1: TIMED_WAITING
20:50:35.344 [     main] consumer2: BLOCKED
20:50:35.344 [     main] consumer3: BLOCKED

20:50:35.345 [     main] 생산자 시작
20:50:35.348 [producer1] [생산 시도] data1 -> []
20:50:35.449 [producer2] [생산 시도] data2 -> []
20:50:35.550 [producer3] [생산 시도] data3 -> []

20:50:35.650 [     main] 현재 상태 출력, 큐 데이터 : []
20:50:35.650 [     main] consumer1: TIMED_WAITING
20:50:35.651 [     main] consumer2: BLOCKED
20:50:35.651 [     main] consumer3: BLOCKED
20:50:35.651 [     main] producer1: BLOCKED
20:50:35.652 [     main] producer2: BLOCKED
20:50:35.652 [     main] producer3: BLOCKED
20:50:35.653 [     main] == [소비자 먼저 실행] 종료, BoundedQueueV2 ==
20:50:36.029 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:37.040 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:38.045 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:39.046 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:40.052 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
20:50:41.068 [consumer1] [take] 큐에 데이터 없음, 소비자 대기
```

어떻게 된걸까? 결과를 분석해보자.
producer 스레드를 먼저 실행했다고 생각을 하자.
이렇게 되면 큐에 데이터는 2개까지 쌓이다가 producer3 스레드가 큐가 가득차서 대기한다.
메인 스레드는 다른 스레드들한테 일을 하라고 시킬뿐, 본인의 일을 계속한다.
그래서 스레드의 상태가 producer1,2는 끝났지만 3은 TIMED_WAITING 상태가 유지된다.

이러한 상황에서 consumer1,2,3 스레드가 실행이 되는데 producer3이 락을 갖고 있으므로
synchronized로 인해 임계 영역에 접근을 못한다. 따라서, 소비시도만 나온다.
그리고나서 상태값의 결과는 BLOCKED가 뜬다. 이 문제를 해결하려면 producer3 스레드는 
어차피 큐에 데이터가 지금 당장 쌓일 일이 없으니까 잠시 대기상태로 변하고 다른 스레드(소비 스레드)가
실행되어야 한다. 이럴때 Object 클래스의 wait, notify 메소드를 활용하면 된다.

메소드를 활용하기 전에 소비자 스레드를 먼저 실행하면 당연히 데이터가 없으니 계속 consumer1 스레드는 멈춘 상태에 있고
consumer2,3 스레드는 소비 시도만하고 정작 쓰지는 못한다. 왜냐면 consumer1이 lock을 갖고 있기 때문이다.
그래서 상태값은 TIMED_WAITING, BLOCKED, BLOCKED가 출력되고 producer 스레드들은 consumer1 스레드가
락을 갖고 있기 때문에 생산 시도만하고 정작 생산은 못한다. 이제 wait, notify 메소드를 활용해보자.

## 4. 생산자 소비자 문제3 - wait, notify 활용

**wait(), notify()**
Object.wait()
- 현재 스레드가 가진 락을 반납하고 대기(WAITING)한다.
- 현재 스레드를 대기(WAITING) 상태로 전환한다. 이 메서드는 현재 스레드가 synchronized 블록이나
메서드에서 락을 소유하고 있을 때만 호출할 수 있다. 호출한 스레드는 락을 반납하고, 다른 스레드가 해당 락을 획득할 수 있도록 한다.
이렇게 대기 상태로 전환된 스레드는 다른 스레드가 notify() 또는 notifyAll()을 호출할 때까지 대기 상태를 유지한다. 

Object.notify()
- 대기 중인 스레드 중 하나를 깨운다.
- 이 메서드는 synchronized 블록이나 메서드에서 호출되어야 한다. 깨운 스레드는 락을 다시 획득할 기회를 얻게 된다.
만약 대기 중인 스레드가 여러 개라면, 그 중 하나만이 깨워지게 된다.

Object.notifyAll()
- 대기 중인 모든 스레드를 깨운다.
- 이 메서드 역시 synchronized 블록이나 메서드에서 호출되어야 하며, 모든 대기 중인 스레드가 락을 획득할 수 있는 기회를 얻게 된다.
이 방법은 모든 스레드를 깨워야 할 필요가 있는 경우에 유용하다. 

직접 메소드를 활용하여 코드를 작성해보자.

```java
package thread.bounded;

import java.util.ArrayDeque;
import java.util.Queue;

import static util.MyLogger.log;

public class BoundedQueueV3 implements BoundedQueue {
  
  private final Queue<String> queue = new ArrayDeque<>();
  private final int max;
  
  public BoundedQueueV3(int max) {
    this.max = max;
  }
  
  public synchronized void put(String data) {
    while(queue.size() == max) {
      log("[put] 큐가 가득 참, 생산자 대기");
      try {
        wait(); // RUNNABLE -> WAITING , 락 반납
        log("[put] 생산자 깨어남");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    
    queue.offer(data);
    log("[put] 생산자 데이터 저장, notify() 호출");
    notify(); // 대기 스레드, WAIT -> BLOCKED
  }
  
  public synchronized String take() {
    while(queue.isEmpty()) {
      log("[take] 큐에 데이터가 없음, 소비자 대기");
      try {
        wait();
        log("[take] 소비자 깨어남");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    
    String data = queue.poll();
    log("[take] 소비자 데이터 획득, notify() 호출");
    notify();
    return data; 
  }
  
  @Override
  public String toString() {
    return queue.toString(); 
  }
  
}
```

**put(data) - wait(), notify()**
- synchronized를 통해 임계 영역을 설정한다. 생산자 스레드는 락 획득을 시도한다.
- 락을 획득한 생산자 스레드는 반복문을 사용해서 큐에 빈 공간이 생기는지 주기적으로 체크한다. 만약 빈 공간이 없다면
Object.wait()를 사용해서 대기한다. 참고로 대기할 때 락을 반납하고 대기한다. 그리고 대기상태에서 깨어나면, 다시 반복문에서 큐의 빈 공간을 체크한다.
- wait()를 호출해서 대기하는 경우 RUNNABLE -> WAITING 상태가 된다.
- 생산자가 데이터를 큐에 저장하고 나면 notify()를 통해 저장된 데이터가 있다고 대기하는 스레드에 알려주어야 한다.

**take() - wait(), notify()**
- synchronized를 통해 임계 영역을 설정한다. 소비자 스레드는 락 획득을 시도한다.
- 락을 획득한 소비자 스레드는 반복문을 사용해서 큐에 데이터가 있는지 주기적으로 체크한다. 
만약 데이터가 없다면 Object.wait()를 사용해서 대기한다. 참고로 대기할 때 락을 반납하고 대기한다. 
- 대기하는 경우 RUNNABLE -> WAITING 상태가 된다. 




