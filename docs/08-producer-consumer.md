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

