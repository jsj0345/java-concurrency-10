# 스레드 풀과 Executor 프레임워크

## 1. 스레드를 직접 사용할 때의 문제점
스레드를 직접 생성해서 사용하면 다음과 같은 3가지 문제가 있다.
- 스레드 생성 시간으로 인한 성능 문제 (여기서 생성은 객체 생성뿐만 아니라 start()까지 호출했을 때를 의미함.)
- 스레드 관리 문제
- Runnable 인터페이스의 불편함

**스레드 생성 비용으로 인한 성능 문제**
- 메모리 할당 : 각 스레드는 자신만의 호출 스택을 가지고 있어야 한다. 이 호출 스택은 스레드가
실행되는 동안 사용하는 메모리 공간이다. 따라서 스레드를 생성할 때는 이 호출 스택을 위한 메모리를 할당해야 한다.
- 운영체제 자원 사용 : 스레드를 생성하는 작업은 운영체제 커널 수준에서 이루어지며, 시스템 콜을 통해 처리된다.
이는 CPU와 메모리 리소스를 소모하는 작업이다. 
- 운영체제 스케줄러 설정 : 새로운 스레드가 생성되면 운영체제의 스케줄러는 이 스레드를 관리하고 실행 순서를 조정해야 한다.
이는 운영체제의 스케줄링 알고리즘에 따라 추가적인 오버헤드가 발생할 수 있다. 
-> 오버헤드는 실제 작업과는 상관없지만, 그 작업을 실행하기 위해 어쩔 수 없이 소모되는 CPU와 메모리 자원을 의미한다.
예를 들어, 결정 비용(알고리즘 연산)으로 생각하면 수 많은 스레드 중 누구를 CPU에 올릴지 스케줄링 알고리즘을 계산하는 데 
드는 CPU 시간을 의미함. 관리 비용으로는 스레드가 생길 때마다 그 정보를 기록하는 명세서를 만들고 업데이트하는 메모리와 CPU자원을 의미함.
교체 비용으로는 현재 작업 중인 스레드의 상태를 저장하고, 다음 스레드의 상태를 불러오는 '교체 작업' 자체에 드는 비용을 의미. 

**스레드 관리 문제**
서버의 CPU, 메모리 자원은 한정되어 있기 때문에, 스레드는 무한하게 만들 수 없다.
예를 들어서, 사용자의 주문을 처리하는 서비스라고 가정하자. 그리고 사용자의 주문이 들어올 때 마다 스레드를 만들어서
요청을 처리한다고 가정해보자. 서비스 마케팅을 위해 선착순 할인 이벤트를 진행한다고 가정해보자. 그러면 사용자가
갑자기 몰려들 수 있다. 평소 동시에 100개 정도의 스레드면 충분했는데, 갑자기 10000개의 스레드가 필요한 상황이 된다면
CPU, 메모리 자원이 버티지 못할 것이다. 
이런 문제를 해결하려면 우리 시스템이 버틸 수 있는, 최대 스레드의 수 까지만 스레드를 생성할 수 있게 관리해야 한다.

또한 이런 문제도 있다. 예를 들어 애플리케이션을 종료한다고 가정해보자.
이때 안전한 종료를 위해 실행 중인 스레드가 남은 작업은 모두 수행한 다음에 프로그램을 종료하고 싶다거나, 또는 급하게 종료해야 해서
인터럽트 등의 신호를 주고 스레드를 종료하고 싶다고 가정해보자.
이런 경우에도 스레드가 어딘가에 관리가 되어 있어야한다. 

**Runnable 인터페이스**
- 반환 값이 없다 : run() 메서드는 반환 값을 가지지 않는다. 따라서 실행 결과를 얻기 위해서는 별도의 메커니즘을
사용해야 한다. 쉽게 이야기해서 스레드의 실행 결과를 직접 받을 수 없다. 앞에서 공부한 SumTask의 예를 생각해보자.
스레드가 실행한 결과를 멤버 변수에 넣어두고, join()등을 사용해서 스레드가 종료되길 기다린다음에 멤버 변수에 보관한 값을 받아야 한다. 

- 예외 처리 : run() 메서드는 체크 예외를 던질 수 없다. 체크 예외의 처리는 메서드 내부에서 처리해야 한다.

**해결**
지금까지 얘기했던 문제들을 해결하려면 스레드를 생성하고 관리하는 풀이 필요하다.

- 스레드를 관리하는 스레드 풀에 스레드를 미리 필요한 만큼 만들어둔다.
- 스레드는 스레드 풀에서 대기하며 쉰다.
- 작업 요청이 온다.
- 스레드 풀에서 이미 만들어진 스레드를 하나 조회한다.
- 조회한 스레드1로 작업을 처리한다. 
- 스레드1은 작업을 완료한다.
- 작업을 완료한 스레드는 종료하는게 아니라, 다시 스레드 풀에 반납한다. 스레드1인 이후에 다시 재사용 될 수 있다. 

스레드 풀이라는 개념을 사용하면 스레드를 재사용할 수 있어서, 재사용시 스레드의 생성 시간을 절약할 수 있다.
그리고 스레드 풀에서 스레드가 관리되기 때문에 필요한 만큼만 스레드를 만들 수 있고, 또 관리할 수 있다.

## 2. Executor 프레임워크
자바의 Executor 프레임워크는 멀티스레딩 및 병렬 처리를 쉽게 사용할 수 있도록 돕는 기능의 모음이다.
이 프레임워크는 작업 실행의 관리 및 스레드 풀 관리를 효율적으로 처리해서 개발자가 직접 스레드를 생성하고 관리하는 복잡함을
줄여준다. 

**Executor 프레임워크의 주요 구성 요소**

Executor 인터페이스
```java
package java.util.concurrent;

public interface Executor {
  void execute(Runnable command);
}
```
- 가장 단순한 작업 실행 인터페이스로, execute(Runnable command) 메서드 하나를 가지고 있다.

```java
public interface ExecutorService extends Executor, AutoCloseable {
  <T> Future<T> submit(Callable<T> task);
  
  @Override
  default void close(){/*...*/}
}
```
- Executor 인터페이스를 확장해서 작업 제출과 제어 기능을 추가로 제공한다.
- 주요 메서드로는 submit(), close()가 있다.
- 더 많은 기능이 있지만 나머지 기능들은 뒤에서 알아보자.
- Executor 프레임워크를 사용할 때는 대부분 이 인터페이스를 사용한다.

**로그 출력 유틸리티 만들기**
먼저 Executor 프레임워크의 상태를 확인하기 위한 로그 출력 유틸리티를 만들어두자.

```java
package thread.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static util.MyLogger.log;

public abstract class ExecutorUtils {
  
  public static void printState(ExecutorService executorService) {
    if(executorService instanceof ThreadPoolExecutor poolExecutor) {
      int pool = poolExecutor.getPoolSize();
      int active = poolExecutor.getActiveCount();
      int queuedTasks = poolExecutor.getQueue().size();
      long completedTask = poolExecutor.getCompletedTaskCount();
      log("[pool = " + pool + ", active = " + active + ", queuedTasks = " + queuedTasks + ", completedTasks = " + completedTask + "]");
    } else {
      log(executorService); 
    }
  }
  
}
```
- pool : 스레드 풀에서 관리되는 스레드의 숫자
- active : 작업을 수행하는 스레드의 숫자
- queuedTasks : 큐에 대기중인 작업의 숫자
- completedTask : 완료된 작업의 숫자

```java
package thread.executor;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class RunnableTask implements Runnable {
  private final String name;
  private int sleepMs = 1000;
  
  public RunnableTask(String name) {
    this.name = name;
  }
  
  public RunnableTask(String name, int sleepMs) {
    this.name = name;
    this.sleepMs = sleepMs;
  }
  
  @Override
  public void run() {
    log(name + " 시작");
    sleep(sleepMs); // 작업 시간 시뮬레이션
    log(name + " 완료");
  }
}
```

```java
package thread.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQUeue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static thread.executor.ExecutorUtils.*;
import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ExecutorBasicMain {

  public static void main(String[] args) throws InterruptedException {
    ExecutorService es = new ThreadPoolExecutor(2,2,0,TimeUnit.MICROSECONDS
    , new LinkedBlockingQueue<>());
    log("== 초기 상태 ==");
    printState(es);
    es.execute(new RunnableTask("taskA"));
    es.execute(new RunnableTask("taskB"));
    es.execute(new RunnableTask("taskC"));
    es.execute(new RunnableTask("taskD"));
    log("== 작업 수행 중 ==");
    printState(es);
    
    sleep(3000);
    log("== 작업 수행 완료==");
    printState(es);
    
    es.close(); 
    
  }
  
}
```

ThreadPoolExecutor(ExecutorService)는 크게 2가지 요소로 구성되어 있다.
- 스레드 풀 : 스레드를 관리한다.
- BlockingQueue : 작업을 보관한다. 생산자-소비자 문제를 해결하기 위해 단순한 큐가 아니라,
BlockingQueue를 사용한다. 

생산자가 es.execute(new RunnableTask("taskA"))를 호출하면, RunnableTask("taskA") 인스턴스가
BlockingQueue에 보관된다. 
- 생산자 : es.execute(작업)을 호출하면 내부에서 BlockingQueue에 작업을 보관한다. main 스레드가 생산자가 된다.
- 소비자 : 스레드 풀에 있는 스레드가 소비자이다. 이후에 소비자 중에 하나가 BlockingQueue에 들어있는 작업을
받아서 처리한다. 

**ThreadPoolExecutor 생성자**
ThreadPoolExecutor의 생성자는 다음 속성을 사용한다.
- corePoolSize : 스레드 풀에서 관리되는 기본 스레드의 수
- maximumPoolSize : 스레드 풀에서 관리되는 최대 스레드 수
- keepAliveTime, TimeUnit unit : 기본 스레드 수를 초과해서 만들어진 스레드가 생존할 수 있는 대기시간이다.
이 시간 동안 처리할 작업이 없다면 초과 스레드는 제거된다.
- BlockingQueue workQueue : 작업을 보관할 블로킹 큐

코드를 실행하면 결과는 다음과 같다. 

```text
16:01:58.526 [     main] == 초기 상태 ==
16:01:58.551 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 0]
16:01:58.554 [     main] == 작업 수행 중 ==
16:01:58.554 [     main] [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
16:01:58.555 [pool-1-thread-1] taskA 시작
16:01:58.556 [pool-1-thread-2] taskB 시작
16:01:59.571 [pool-1-thread-2] taskB 완료
16:01:59.571 [pool-1-thread-1] taskA 완료
16:01:59.572 [pool-1-thread-2] taskC 시작
16:01:59.572 [pool-1-thread-1] taskD 시작
16:02:00.587 [pool-1-thread-1] taskD 완료
16:02:00.587 [pool-1-thread-2] taskC 완료
16:02:01.565 [     main] == 작업 수행 완료 ==
16:02:01.566 [     main] [pool=2, active = 0, queuedTasks = 0, completedTask = 4]
16:02:01.567 [     main] == shutdown 완료 ==
16:02:01.567 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 4]
```

결과를 나눠서 보자.

```text
16:01:58.526 [     main] == 초기 상태 ==
16:01:58.551 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 0]
```

- ThreadPoolExecutor를 생성한 시점에 스레드 풀에 스레드를 미리 만들어두지는 않는다.
- main 스레드가 es.execute("taskA ~ taskD")를 호출한다.
-> 참고로 당연한 이야기지만 main 스레드는 작업을 전달하고 기다리지 않는다. 전달한 작업은 다른 스레드가 실행할 것이다.
main 스레드는 작업을 큐에 보관까지만 하고 바로 다음 코드를 수행한다.
- taskA~D 요청이 블로킹 큐에 들어온다.
- 최초의 작업이 들어오면 이때 작업을 처리하기 위해 스레드를 만든다.
-> 참고로 스레드 풀에 스레드를 미리 만들어두지는 않는다.
- 작업이 들어올 때마다 corePoolSize의 크기 까지 스레드를 만든다.
-> 예를 들어서 최초 작업인 taskA가 들어오는 시점에 스레드1을 생성하고, 다음 작업인 taskB가 들어오는 시점에
스레드2를 생성한다. 
-> 이런 방식으로 corePoolSize에 지정한 수 만큼 스레드를 스레드 풀에 만든다. 여기서는 2를 설정했으므로
2개까지 만든다.
-> corePoolSize까지 스레드가 생성되고 나면, 이후에는 스레드를 생성하지 않고 앞서 만든 스레드를 재사용한다.

```text
16:01:58.554 [     main] == 작업 수행 중 ==
16:01:58.554 [     main] [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
```

스레드 풀의 스레드가 작업을 실행할 때, 스레드 풀에서 스레드를 꺼내는 것처럼 표현했지만,
실제로 꺼내는 것은 아니고, 스레드의 상태가 변경된다고 이해하면 된다. 그래서 여전히 pool=2로 유지된다. 

- 작업이 완료되면 스레드 풀에 스레드를 반납한다. 스레드를 반납하면 스레드는 대기 상태로 스레드 풀에 대기한다.
-> 참고로 실제 반납 되는게 아니라, 스레드의 상태가 변경된다고 이해하면 된다. 

- 반납된 스레드는 재사용된다.
- taskC, taskD의 작업을 처리하기 위해 스레드 풀에서 스레드를 꺼내 재사용한다.

- 작업이 완료되면 스레드는 다시 스레드 풀에서 대기한다.
```text
16:02:01.565 [     main] == 작업 수행 완료 ==
16:02:01.566 [     main] [pool=2, active = 0, queuedTasks = 0, completedTask = 4]
```

```text
16:02:01.567 [     main] == shutdown 완료 ==
16:02:01.567 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 4]
```

- close()를 호출하면 ThreadPoolExecutor가 종료된다. 이때 스레드 풀에 대기하는 ㅅ레드도 함께 제거된다.

## 3. Runnable의 불편함 

위에서 설명했던 대로 Runnable 인터페이스는 다음과 같은 두가지 문제때문에 불편하다.

- 반환 값이 없다 : run() 메서드는 반환 값을 가지지 않는다. 따라서 실행 결과를 얻기 위해서는 별도의 메커니즘을 사용해야 한다.
쉽게 이야기해서 스레드의 실행 결과를 직접 받을 수 없다. 앞에서 공부한 SumTask의 예를 생각해보자.
스레드가 실행한 결과를 멤버 변수에 넣어두고, join() 등을 사용해서 스레드가 종료되길 기다린 다음에 멤버 변수를 통해 값을 받아야 한다.
- 예외 처리 : run() 메서드는 체크 예외를 던질 수 없다. 체크 예외의 처리는 메서드 내부에서 처리해야 한다.

```java
package thread.executor.future;

import java.util.Random;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class RunnableMain {

  public static void main(String[] args) throws InterruptedException {
    MyRunnable task = new MyRunnable();
    Thread thread = new Thread(task, "Thread-1");
    thread.start();
    thread.join();
    int result = task.value;
    log("result value = " + result);
  }
  
  static class MyRunnable implements Runnable {
    int value;
    
    @Override
    public void run() {
      log("Runnable 시작");
      sleep(2000);
      value = new Random().nextInt(10);
      log("create value = " + value);
      log("Runnable 완료");
    }
  }
  
  
}
```

```text
16:48:16.178 [ Thread-1] Runnable 시작
16:48:18.197 [ Thread-1] create value = 7
16:48:18.198 [ Thread-1] Runnable 완료
16:48:18.198 [     main] result value = 7
```
- 무작위 값이므로 숫자의 결과는 다를 수 있다.
- 프로그램이 시작되면 Thread-1이라는 별도의 스레드를 하나 만든다.
- Thread-1이 수행하는 MyRunnable은 무작위 값을 하나 구한 다음에 value 필드에 보관한다.
- 클라이언트인 main 스레드가 이 별도의 스레드에서 만든 무작위 값을 얻어오려면 Thread-1이 종료될때까지 기다려야한다.
그래서 main 스레드는 join()을 호출해서 대기한다. 
- 이후에 main 스레드에서 MyRunnable 인스턴스의 value 필드를 통해 최종 무작위 값을 획득한다.

## 4. Future1 - 시작

**Runnable과 Callable 비교**
```java
package java.lang;

public interface Runnable {
  void run();
}
```
- Runnable의 run()은 반환 타입이 void다. 따라서 값을 반환할 수 없다.
- 예외가 선언되어 있지 않다. 따라서 해당 인터페이스를 구현하는 모든 메서드는 체크 예외를 던질 수 없다.
-> 참고로 자식은 부모의 예외 범위를 넘어설 수 없다. 부모에 예외가 선언되어 있지 않으므로 예외를 던질 수 없다.
-> 물론 런타임(비체크)예외는 제외다. 

Callable은 다음과 같다.
```java
package java.util.concurrent;

public interface Callable<V> {
  V call() throws Exception;
}
```
- java.util.concurrent에서 제공되는 기능이다.
- Callable의 call()은 반환 타입이 제네릭 V이다. 따라서 값을 반환할 수 있다.
- throws Exception 예외가 선언되어 있다. 따라서 해당 인터페이스를 구현하는 모든 메서드는 체크 예외인
Exception과 그 하위 예외를 모두 던질 수 있다.

**Callable과 Future 사용**

```java
package thread.executor.future;

import java.util.Random;
import java.util.concurrent.*;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class CallableMainV1 {

  public static void main(String[] args) throws ExecutionException,
      InterruptedException {
    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<Integer> future = es.submit(new MyCallable());
    Integer result = future.get();
    log("result value = " + result);
    es.close();
  }
  
  static class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() {
      log("Callable 시작");
      sleep(2000);
      int value = new Random().nextInt(10);
      log("create value = " + value);
      log("Callable 완료");
      return value; 
    }
  }
  
}
```

java.util.concurrent.Executors가 제공하는 newFixedThreadPool(size)을 사용하면
편리하게 ExecutorService를 생성할 수 있다.

기존 코드

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

ExecutorService es = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
```

편의 코드
```java
ExecutorService es = Executors.newFixedThreadPool(1);
```

실행 결과는 다음과 같다.
```text
19:37:32.490 [pool-1-thread-1] Callable 시작
19:37:34.505 [pool-1-thread-1] create value = 0
19:37:34.506 [pool-1-thread-1] Callable 완료
19:37:34.507 [     main] result value = 0
```

먼저 MyCallable을 구현하는 부분을 보자.
-> 숫자를 반환하므로 반환할 제네릭 타입을 <Integer>로 선언했다.
-> 구현은 Runnable 코드와 비슷한데, 유일한 차이는 결과를 필드에 담아두는 것이 아니라, 결과를 반환한다는 점이다. 

submit()
```java
<T> Future<T> submit(Callable<T> task);
```

ExecutorService가 제공하는 submit()을 통해 Callable을 작업으로 전달할 수 있다.
```java
Future<Integer> future = es.submit(new MyCallable());
```

MyCallable 인스턴스가 블로킹 큐에 전달되고, 스레드 풀의 스레드 중 하나가 이 작업을 실행할 것이다.
이때 작업의 처리 결과는 직접 반환되는 것이 아니라 Future라는 특별한 인터페이스를 통해 반환된다.

```java
Integer result = future.get();
```
future.get()을 호출하면 MyCallable의 call()이 반환된 결과를 받을 수 있다.
코드를 잘 보면 애매한 상황이 있다.

future.get()을 호출하는 요청 스레드(main)는 future.get()을 호출 했을 때 2가지 상황으로 나뉘게 된다.
- MyCallable 작업을 처리하는 스레드 풀의 스레드가 작업을 완료한 상황
- MyCallable 작업을 처리하는 스레드 풀의 스레드가 작업을 완료하지 못한 상황

future.get()을 호출했을 때 스레드 풀의 스레드가 작업을 완료했다면 반환 받을 결과가 있을 것이다.
그런데 아직 작업을 처리중이라면 어떻게 될까?

## 5. Future2 - 분석

```java
Future<Integer> future = es.submit(new MyCallable());
```
- submit()의 호출로 MyCallable의 인스턴스를 전달한다.
- 이때 submit()은 MyCallable.call()이 반환하는 무작위 숫자 대신에 Future를 반환한다.
- 생각해보면 MyCallable이 즉시 실행되어서 즉시 결과를 반환하는 것은 불가능하다. 왜냐하면 MyCallable은 
즉시 실행되는 것이 아니다. 스레드 풀의 스레드가 미래의 어떤 시점에 이 코드를 대신 실행해야 한다. 
- MyCallable.call() 메서드는 호출 스레드가 실행하는 것도 아니고, 스레드 풀의 다른 스레드가 실행하기 때문에
언제 실행이 완료되어서 결과를 반환할 지 알 수 없다. 
- 따라서 결과를 즉시 받는 것은 불가능하다. 이런 이유로 es.submit()은 MyCallable의 결과를 반환하는 대신에
MyCallable의 결과를 나중에 받을 수 있는 Future라는 객체를 대신 제공한다.
- 정리하면 Future은 전달한 작업의 미래이다. 이 객체를 통해 전달한 작업의 미래 결과를 받을 수 있다.

```java
package thread.executor.future;

import java.util.Random;
import java.util.concurrent.*;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class CallableMainV2 {
  
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    ExecutorService es = Executors.newFixedThreadPool(1);
    log("submit() 호출");
    Future<Integer> future = es.submit(new MyCallable());
    log("future 즉시 반환, future = " + future);
    
    log("future.get() [블로킹] 메서드 호출 시작 -> main 스레드 WAITING");
    Integer result = future.get();
    log("future.get() [블로킹] 메서드 호출 완료 -> , main 스레드 RUNNABLE");
    
    log("result value = " + result);
    log("future 완료, future = " + future);
    es.close();
  }
  
  static class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() {
      log("Callable 시작");
      sleep(2000);
      int value = new Random().nextInt(10);
      log("create value = " + value);
      log("Callable 완료");
      return value; 
    }
  }
  
}
```

```text
20:20:59.217 [     main] submit() 호출
20:20:59.223 [pool-1-thread-1] Callable 시작
20:20:59.224 [     main] future 즉시 반환, future = java.util.concurrent.FutureTask@c4437c4[Not completed, task = thread.executor.future.CallableMainV2$MyCallable@2e817b38]
20:20:59.224 [     main] future.get() [블로킹] 메서드 호출 시작 -> main 스레드 WAITING
20:21:01.241 [pool-1-thread-1] create value = 6
20:21:01.241 [pool-1-thread-1] Callable 완료
20:21:01.242 [     main] future.get() [블로킹] 메서드 호출 시작 -> main 스레드 RUNNABLE
20:21:01.242 [     main] result value = 6
20:21:01.242 [     main] future 완료, future = java.util.concurrent.FutureTask@c4437c4[Completed normally]
```

- 요청 스레드는 es.submit(taskA)를 호출하고 있는 중이다.
- ExecutorService는 전달한 taskA의 미래 결과를 알 수 있는 Future 객체를 생성한다.
-> Future는 인터페이스이다. 이때 생성되는 실제 구현체는 FutureTask다.
- 그리고 생성한 Future 객체 안에 taskA의 인스턴스를 보관한다.
- Future는 내부에 taskA 작업의 완료 여부와, 작업의 결과 값을 가진다.
- submit()을 호출한 경우 Future가 만들어지고, 전달한 작업인 taskA가 바로 블로킹 큐에 담기는 것이 아니라,
taskA를 감싸고 있는 Future가 대신 블로킹 큐에 담긴다.

```java
Future<Integer> future = es.submit(new MyCallable());
```

```text
20:20:59.224 [     main] future 즉시 반환, 
future = java.util.concurrent.FutureTask@c4437c4[Not completed, task = thread.executor.future.CallableMainV2$MyCallable@2e817b38]
```
- Future는 내부에 작업의 완료 여부와 작업의 결과 값을 가진다. 작업이 완료되지 않았기 때문에 아직은 결과 값이 없다. 
-> 로그를 보면 Future의 구현체는 FutureTask이다. 
-> Future의 상태는 Not Completed(미 완료)이고, 연관된 작업은 전달한 taskA(MyCallable 인스턴스)이다. 
- 여기서 중요한 핵심이 있는데, 작업을 전달할 때 생성된 Future는 즉시 반환된다는 점이다. 

```text
20:20:59.224 [     main] future 즉시 반환, future = java.util.concurrent.FutureTask@c4437c4[Not completed,
task = thread.executor.future.CallableMainV2$MyCallable@2e817b38]
20:20:59.224 [     main] future.get() [블로킹] 메서드 호출 시작 -> main 스레드 WAITING
```
- 생성한 Future를 즉시 반환하기 때문에 요청 스레드는 대기하지 않고, 자유롭게 본인의 다음 코드를 호출할 수 있다. 
-> 이것은 마치 Thread.start()를 호출한 것과 비슷하다. Thread.start()를 호출하면 스레드의 작업 코드가
별도의 스레드에서 실행된다. 요청 스레드는 대기하지 않고, 즉시 다음 코드를 호출할 수 있다.

```text
20:20:59.223 [pool-1-thread-1] Callable 시작
```
- 큐에 들어있는 Future[taskA]를 꺼내서 스레드 풀의 스레드1이 작업을 시작한다.
- 참고로 Future의 구현체인 FutureTask는 Runnable 인터페이스도 함께 구현하고 있다.
- 스레드1은 FutureTask의 run() 메서드를 수행한다.
- 그리고 run() 메서드가 taskA의 call() 메서드를 호출하고 그 결과를 받아서 처리한다.
-> FutureTask.run() -> MyCallable.call()

스레드1
- 스레드1은 taskA의 작업을 아직 처리중이다. 아직 완료하지는 않았다.

요청 스레드
- 요청 스레드는 Future 인스턴스의 참조를 가지고 있다.
- 그리고 언제든지 본인이 필요할 때 Future.get()을 호출해서 taskA 작업의 미래 결과를 받을 수 있다.
- 요청 스레드는 작업의 결과가 필요해서 future.get()을 호출한다.
-> Future에는 완료 상태가 있다. taskA의 작업이 완료되면 Future의 상태도 완료로 변경된다.
-> 그런데 여기서 taskA의 작업이 아직 완료되지 않았다. 따라서 Future도 완료 상태가 아니다.
- 요청 스레드가 future.get()을 호출하면 Future가 완료 상태가 될 때 까지 대기한다. 이때 요청 스레드의 상태는
RUNNABLE -> WAITING이 된다.

future.get()을 호출했을 때
- Future가 완료 상태 : Future가 완료 상태면 Future에 결과도 포함되어 있다.
이 경우 요청 스레드는 대기하지 않고, 값을 즉시 반환받을 수 있다.
- Future가 완료 상태가 아님 : taskA가 아직 수행되지 않았거나 또는 수행 중이라는 뜻이다. 이때는 어쩔 수 없이
요청 스레드가 결과를 받기 위해 대기해야 한다. 요청 스레드가 마치 락을 얻을 때처럼, 결과를 얻기 위해 대기한다.
이처럼 스레드가 어떤 결과를 얻기 위해 대기하는 것을 BLOCKING이라 한다. 

```text
20:21:01.241 [pool-1-thread-1] create value = 6
20:21:01.241 [pool-1-thread-1] Callable 완료
20:21:01.242 [     main] future.get() [블로킹] 메서드 호출 시작 -> main 스레드 RUNNABLE
```

요청 스레드
- 대기(WAITING)상태로 future.get()을 호출하고 대기중이다.

스레드1
- taskA의 작업을 완료한다.
- Future에 taskA의 반환 결과를 담는다.
- Future의 상태를 완료로 변경한다.
- 요청 스레드를 깨운다. 요청 스레드는 WAITING -> RUNNABLE 상태로 변한다. 

```text
20:21:01.242 [     main] future.get() [블로킹] 메서드 호출 시작 -> main 스레드 RUNNABLE
20:21:01.242 [     main] result value = 6
```

요청 스레드
- 요청 스레드는 RUNNABLE 상태가 되었다. 그리고 완료 상태의 Future에서 결과를 반환 받는다.
taskA의 결과가 Future에 담겨있다.

스레드1
- 작업을 마친 스레드1인 스레드 풀로 반환된다. RUNNABLE -> WAITING 
```text
20:21:01.242 [     main] future 완료, future = java.util.concurrent.FutureTask@c4437c4[Completed normally]
```
- Future의 인스턴스인 FutureTask를 보면 "Completed normally"로 정상 완료된 것을 확인할 수 있다.

**Future가 필요한 이유?**
어떤 의문이 생길 수 있다.
코드를 보자.

```java
public static void main(String[] args) {
  Future<Integer> future = es.submit(new MyCallable());
  future.get(); // 여기서 블로킹
}
```

ExecutorService를 설계할 때 지금처럼 복잡하게 Future를 반환하는게 아니라 다음과 같이 결과를 직접 받도록 설계하는게
더 단순하지 않을까?

```java
Integer result = es.submit(new MyCallable()); 
```
이렇게 설계하면 submit()을 호출할 때, 작업의 결과가 언제 나올지 알 수 없다.
따라서 작업의 결과를 받을 때까지 요청 스레드는 대기해야 한다.
그런데 이것은 Future를 사용할 때도 마찬가지다. 
Future만 즉시 반환 받을 뿐이지, 작업의 결과를 얻으려면 future.get()을 호출해야 한다.
그리고 이 시점에는 작업의 결과를 받을 때 까지 대기해야한다. 

## 6. Future3 - 활용

```java
package thread.executor.future;

import static util.MyLogger.log;

public class SumTaskMainV1 {

  public static void main(String[] args) throws InterruptedException {
    SumTask task1 = new SumTask(1,50);
    SumTask task2 = new SumTask(51, 100);
    Thread thread1 = new Thread(task1, "thread-1");
    Thread thread2 = new Thread(task2, "thread-2");
    
    thread1.start();
    thread2.start();
    
    //스레드가 종료될 때 까지 대기
    log("join() - main 스레드가 thread1, thread2 종료까지 대기");
    thread1.join();
    thread2.join();
    log("main 스레드 대기 완료");
    
    log("task1.result = " + task1.result);
    log("task2.result = " + task2.result);
    
    int sumAll = task1.result + task2.result;
    log("task1 + task2 = " + sumAll);
    log("End");
  }
  
  static class SumTask implements Runnable {
    int startValue;
    int endValue;
    int result = 0;
    
    public SumTask(int startValue, int endValue) {
      this.startValue = startValue;
      this.endValue = endValue;
    }
    
    @Override
    public void run() {
      log("작업 시작");
      
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      
      int sum = 0;
      for (int i = startValue; i <= endValue; i++) {
        sum += i;
      }
      result = sum;
      
      log("작업 완료 result = " + result); 
    }
  }
  
  
}
```
실행 결과는 다음과 같다.

```text
20:00:37.980 [ thread-2] 작업 시작
20:00:37.980 [ thread-1] 작업 시작
20:00:37.980 [     main] join() - main 스레드가 thread1, thread2 종료까지 대기
20:00:39.996 [ thread-2] 작업 완료 result = 3775
20:00:39.996 [ thread-1] 작업 완료 result = 1275
20:00:39.996 [     main] main 스레드 대기 완료
20:00:39.997 [     main] task1.result = 1275
20:00:39.997 [     main] task2.result = 3775
20:00:39.998 [     main] task1 + task2 = 5050
20:00:39.998 [     main] End
```

이 코드를 Callable과 ExecutorService로 처리해보자. 

```java
package thread.executor.future;

import java.util.concurrent.*;

import static util.MyLogger.log;

public class SumTaskMainV2 {

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    SumTask task1 = new SumTask(1, 50);
    SumTask task2 = new SumTask(51, 100);
    ExecutorService es = Executors.newFixedThreadPool(2);
    
    Future<Integer> future1 = es.submit(task1);
    Future<Integer> future2 = es.submit(task2);
    
    Integer sum1 = future1.get();
    Integer sum2 = future2.get(); 
    
    log("task1.result = " + sum1);
    log("task2.result = " + sum2);
    
    int sumAll = sum1 + sum2;
    log("task1 + task2 = " + sumAll);
    log("End");
    
    es.close(); 
  }
  
  static class SumTask implements Callable<Integer> {
    int startValue;
    int endValue;
    
    public SumTask(int startValue, int endValue) {
      this.startValue = startValue;
      this.endValue = endValue;
    }
    
    @Override
    public Integer call() throws InterruptedException {
      log("작업 시작");
      Thread.sleep(2000);
      int sum = 0;
      for(int i = startValue; i <= endValue; i++) {
        sum += i;
      }
      log("작업 완료 result = " + sum);
      return sum; 
    }
  }
  
}
```

실행 결과는 다음과 같다.

```text
20:09:15.833 [pool-1-thread-2] 작업 시작
20:09:15.833 [pool-1-thread-1] 작업 시작
20:09:17.858 [pool-1-thread-1] 작업 완료 result = 1275
20:09:17.858 [pool-1-thread-2] 작업 완료 result = 3775
20:09:17.859 [     main] task1.result = 1275
20:09:17.860 [     main] task2.result = 3775
20:09:17.861 [     main] task1 + task2 = 5050
20:09:17.862 [     main] End
```

ExecutorService와 Callable을 사용한 덕분에, 이전 코드보다 훨씬 직관적이고 깔끔하게 코드를 작성할 수 있었다.
특히 작업의 결과를 반환하고, 요청 스레드에서 그 결과를 바로 받아서 처리하는 부분이 매우 직관적이다. 
코드만 보면 마치 멀티스레드를 사용하지 않고, 단일 스레드 상황에서 일반적인 메서드를 호출하고 결과를 받는 것 처럼 느껴진다.

그리고 스레드를 생성하고, Thread.join()과 같은 스레드를 관리하는 코드도 모두 제거할 수 있었다.
추가로 Callable.call()은 throws InterruptedException과 같은 체크 예외도 던질 수 있다.

## 7. Future4 - 이유 
이제 Future가 필요한 이유를 코드를 통해 알아보자. 

Future를 반환 하는 코드
```java
Future<Integer> future1 = es.submit(task1); // 블로킹X
Future<Integer> future2 = es.submit(task2); // 블로킹X

Integer sum1 = future1.get();
Integer sum2 = future2.get();
```

Future 없이 결과를 직접 반환 하는 코드(가정)
```java
Integer sum1 = es.submit(task1); // 여기서 블로킹
Integer sum2 = es.submit(task2); // 여기서 블로킹 
```

Future 없을 때를 생각해보자.

먼저 ExecutorService가 Future 없이 결과를 직접 반환한다고 가정해보자.
- 요청 스레드는 task1을 ExecutorService에 요청하고 결과를 기다린다.
-> 작업 스레드가 작업을 수행하는데 2초가 걸린다.
-> 요청 스레드는 결과를 받을 때 까지 2초간 대기한다.
-> 요청 스레드는 2초 후에 결과를 받고 다음 라인을 수행한다. 

요청 스레드는 task2를 ExecutorService에 요청하고 결과를 기다린다.
-> 작업 스레드가 작업을 수행하는데 2초가 걸린다.
-> 요청 스레드는 결과를 받을 때 까지 2초간 대기한다.
-> 결과를 받고 요청 스레드가 다음 라인을 수행한다. 

이건 마치 다음 상황과 같다.
```java
public static void main(String[] args) {

  Thread thread1 = new Thread(task1, "Thread-1");
  thread1.start();
  thread1.join();

  Thread thread2 = new Thread(task2, "Thread-2");
  thread2.start();
  thread.join();
}
```

이러면 사실 일을 병렬로 처리하지 못하고 한개의 일이 끝나고 나서야 다른 일을 수행하는 것과 같다. 

그런데 만약에 Future를 반환한다면?
```java
Future<Integer> future1 = es.submit(task1); // 블로킹X
Future<Integer> future2 = es.submit(task2); // 블로킹X

Integer sum1 = future1.get();
Integer sum2 = future2.get();
```
요청 스레드는 task1을 ExecutorService에 요청한다.
-> 요청 스레드는 즉시 Future를 반환 받는다.
-> 작업 스레드1은 task1을 수행한다.

요청 스레드는 task2를 ExecutorService에 요청한다.
-> 요청 스레드는 즉시 Future를 반환 받는다.
-> 작업 스레드2는 task2를 수행한다. 

이렇게 되면 요청 스레드는 task1, task2를 동시에 요청할 수 있다.
따라서 두 작업은 동시에 수행된다.

- 이후에 요청 스레드는 future1.get()을 호출하며 대기한다.
-> 작업 스레드1이 작업을 진행하는 약 2초간 대기하고 결과를 받는다.

- 이후에 요청 스레드는 future2.get()을 호출하며 즉시 결과를 받는다.
-> 작업 스레드2는 이미 2초간 작업을 완료했다. 따라서 future2.get()은 거의 즉시 결과를 반환한다. 

이건 마치 다음과 같다.

```java
public static void main(String[] args) {
  Thread thread1 = new Thread(task1, "Thread-1");
  Thread thread2 = new Thread(task2, "Thread-2");
  
  thread1.start();
  thread2.start();
  
  thread1.join();
  thread2.join(); 
}
```

**Future를 잘못 활용한 예1**
```java
public static void main(String[] args) {
  Future<Integer> future1 = es.submit(task1); // non-blocking
  Integer sum1 = future1.get(); // blocking, 2초 대기
  
  Future<Integer> future2 = es.submit(task2); // non-blocking
  Integer sum2 = future2.get(); // blocking, 2초 대기 
}
```
- 요청 스레드가 작업을 하나 요청하고 그 결과를 기다린다. 그리고 그 다음에 다시 다음 요청 전달하고 결과를 기다린다.
- 총 4초의 시간이 걸린다.

**Future를 잘못 활용한 예2**
```java
Integer sum1 = es.submit(task1).get();
Integer sum2 = es.submit(task2).get();
```
- Future를 잘못 활용한 예1과 똑같은 코드이다. 대신에 submit()을 호출하고 그 결과를 변수에 담지 않고 바로 연결해서 get()을 호출한다.
- 총 4초의 시간이 걸린다.

SumTaskMainV2를 복사해서 SumTaskMainV2_Bad를 만들어보자. 

```java
package thread.executor.future;

import java.util.concurrent.*;

import static util.MyLogger.log;

public class SumTaskMainV2_Bad {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    SumTask task1 = new SumTask(1, 50);
    SumTask task2 = new SumTask(51, 100);

    ExecutorService es = Executors.newFixedThreadPool(2);

    Future<Integer> future1 = es.submit(task1);
    Integer sum1 = future1.get(); // 2초

    Future<Integer> future2 = es.submit(task2);
    Integer sum2 = future2.get(); // 2초


    log("task1.result = " + sum1);
    log("task2.result = " + sum2);

    int sumAll = sum1 + sum2;
    log("task1 + task2 = " + sumAll);
    log("End");

    es.close();

  }

  static class SumTask implements Callable<Integer> {
    int startValue;
    int endValue;

    public SumTask(int startValue, int endValue) {
      this.startValue = startValue;
      this.endValue = endValue;
    }

    @Override
    public Integer call() throws Exception {
      log("작업 시작");
      Thread.sleep(2000);
      int sum = 0;
      for(int i = startValue; i <= endValue; i++) {
        sum += i;
      }
      log("작업 완료 result = " + sum);
      return sum;
    }
  }

}
```

```text
20:39:06.180 [pool-1-thread-1] 작업 시작
20:39:08.193 [pool-1-thread-1] 작업 완료 result = 1275
20:39:08.194 [pool-1-thread-2] 작업 시작
20:39:10.201 [pool-1-thread-2] 작업 완료 result = 3775
20:39:10.202 [     main] task1.result = 1275
20:39:10.202 [     main] task2.result = 3775
20:39:10.203 [     main] task1 + task2 = 5050
20:39:10.203 [     main] End
```
총 4초의 시간이 걸린 것을 확인할 수 있다.

**정리**
- Future라는 개념이 없다면 결과를 받을 때 까지 요청 스레드는 아무일도 못하고 대기해야 한다. 따라서
다른 작업을 동시에 수행할 수도 없다.
- Future라는 개념 덕분에 요청 스레드는 대기하지 않고, 다른 작업을 수행할 수 있다. 예를 들어서 다른 작업을
더 요청할 수 있다. 그리고 모든 작업 요청이 끝난 다음에, 본인이 필요할 때 Future.get()을 호출해서 최종 결과를
받을 수 있다. 
- Future를 사용하는 경우 결과적으로 task1, task2를 동시에 요청할 수 있다.
두 작업을 바로 요청했기 때문에 작업을 동시에 제대로 수행할 수 있다.
- 즉, Future는 병렬 처리 및 결과 반환을 할 수 있는 것이 강점이다.

## 8. Future5 - 정리
Future는 작업의 미래 계산의 결과를 나타내며, 계산이 완료되었는지 확인하고, 완료될 때까지 기다릴 수 있는 기능을 제공한다.

Future 인터페이스
```java
package java.util.concurrent;

public interface Future<V> {
  
  boolean cancel(boolean mayInterruptIfRunning);
  boolean isCancelled();
  
  boolean isDone();
  
  V get() throws InterruptedException, ExecutionException;
  V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
  
  enum State {
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
  }
  
  default State state() {}
}
```

**boolean cancel(boolean mayInterruptIfRunning)**
- 기능 : 아직 완료되지 않은 작업을 취소한다. 
- 매개변수 : mayInterruptIfRunning
-> cancel(true) : Future를 취소 상태로 변경한다. 이때 작업이 실행중이라면 Thread.interrupt()를 호출해서 작업을 중단한다.
-> cancel(false) : Future를 취소 상태로 변경한다. 단 이미 실행 중인 작업을 중단하지는 않는다.
- 반환값 : 작업이 성공적으로 취소된 경우 true, 이미 완료되었거나 취소할 수 없는 경우 false
- 설명 : 작업이 실행 중이 아니거나 아직 시작되지 않았으면 취소하고, 실행 중인 작업의 경우
mayInterruptIfRunning이 true이면 중단을 시도한다.
- 참고 : 취소 상태의 Future에 Future.get()을 호출하면 CancellationException 런타임 예외가 발생한다.

**boolean isCancelled()**
- 기능 : 작업이 취소되었는지 여부를 확인한다.
- 반환값 : 작업이 취소된 경우 true, 그렇지 않은 경우 false
- 설명 : 이 메서드는 작업이 cancel() 메서드에 의해 취소된 경우에 true를 반환한다.

**boolean isDone()**
- 기능 : 작업이 완료되었는지 여부를 확인한다.
- 반환값 : 작업이 완료된 경우 true, 그렇지 않은 경우 false
- 설명 : 작업이 정상적으로 완료되었거나, 취소되었거나, 예외가 발생하여 종료된 경우에 true를 반환한다.

**State state()**
- 기능 : Future의 상태를 반환한다.
- RUNNING : 작업 실행 중
- SUCCESS : 성공 완료
- FAILED : 실패 완료
- CANCELLED : 취소 완료

**V get()**
- 기능 : 작업이 완료될 때까지 대기하고, 완료되면 결과를 반환한다.
- 반환값 : 작업의 결과
- 예외
-> InterruptedException : 대기 중에 현재 스레드가 인터럽트된 경우 발생
-> ExecutionException : 작업 계산 중에 예외가 발생한 경우 발생
- 설명 : 작업이 완료될 때까지 get()을 호출한 현재 스레드를 대기한다. 작업이 완료되면 결과를 반환한다.

**V get(long timeout, TimeUnit unit)**
- 기능 : get()과 같은데, 시간 초과되면 예외를 발생시킨다.
- 매개변수
-> timeout : 대기할 최대 시간
-> unit : timeout 매개변수의 시간 단위 지정 
- 반환값 : 작업의 결과
- 예외
-> InterruptedException : 대기 중에 현재 스레드가 인터럽트된 경우 발생 
-> ExecutionException : 계산 중에 예외가 발생한 경우 발생
-> TimeoutException : 주어진 시간 내에 완료되지 않은 경우 발생 
- 설명 : 지정된 시간 동안 결과를 기다린다. 시간이 초과되면 TimeoutException을 발생시킨다.

## 9. Future6 - 취소
cancel()이 어떻게 동작하는지 알아보자.

```java
package thread.executor.future;

import java.util.concurrent.*;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class FutureCancelMain {
  
  private static boolean mayInterruptedIfRunning = true;
  //private static boolean mayInterruptedIfRunning = false;

  public static void main(String[] args) {
    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<String> future = es.submit(new MyTask());
    log("Future.state : " + future.state());
    
    // 일정 시간 후 취소 시도
    sleep(3000);
    
    // cancel() 호출
    log("future.cancel(" + mayInterruptedIfRunning + ") 호출");
    boolean cancelResult1 = future.cancel(mayInterruptedIfRunning); 
    log("Future.state : " + future.state()); 
    log("cancel(" + mayInterruptedIfRunning + ") result : " + cancelResult1); // FAILED 
    
    // 결과 확인
    try {
      log("Future result : " + future.get());
    } catch (CancellationException e) {
      log("Future는 이미 취소 되었습니다.");
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    
    // Executor 종료
    es.close(); 
  }
  
  static class MyTask implements Callable<String> {
    
    @Override
    public String call() {
      try {
        for (int i = 0; i < 10; i++) {
          log("작업 중 : " + i);
          Thread.sleep(1000);
        }
      } catch (InterruptedException e) {
        log("인터럽트 발생");
        return "Interrupted";
      }
      
      return "Completed"; 
    }
    
  }
  
}
```

실행 결과는 다음과 같다.

```text
21:50:14.816 [pool-1-thread-1] 작업 중 : 0
21:50:14.816 [     main] Future.state : RUNNING
21:50:15.827 [pool-1-thread-1] 작업 중 : 1
21:50:16.833 [pool-1-thread-1] 작업 중 : 2
21:50:17.833 [     main] future.cancel(true) 호출
21:50:17.834 [pool-1-thread-1] 인터럽트 발생
21:50:17.834 [     main] Future.state : CANCELLED
21:50:17.842 [     main] cancel(true) result : true
21:50:17.842 [     main] Future는 이미 취소 되었습니다.
```

실행 결과를 보면 딱 맞아 떨어진다. true값은 그대로니까 Future는 캔슬됐고 
캔슬할땐 Interrupt()가 걸려있기 때문에 try 안에 있는 문장을 실행하다가 예외가 잡히므로 그 문장에 대한 로그가 안나오는 것이다.

만약에 mayInterruptedIfRunning이 false라면 어떤 결과가 나올까? 

```text
15:46:15.059 [     main] Future.state : RUNNING
15:46:15.059 [pool-1-thread-1] 작업 중 : 0
15:46:16.068 [pool-1-thread-1] 작업 중 : 1
15:46:17.070 [pool-1-thread-1] 작업 중 : 2
15:46:18.075 [pool-1-thread-1] 작업 중 : 3
15:46:18.076 [     main] future.cancel(false) 호출
15:46:18.077 [     main] Future.state : CANCELLED
15:46:18.085 [     main] cancel(false) result : true
15:46:18.085 [     main] Future는 이미 취소 되었습니다.
15:46:19.087 [pool-1-thread-1] 작업 중 : 4
15:46:20.100 [pool-1-thread-1] 작업 중 : 5
15:46:21.102 [pool-1-thread-1] 작업 중 : 6
15:46:22.110 [pool-1-thread-1] 작업 중 : 7
15:46:23.111 [pool-1-thread-1] 작업 중 : 8
15:46:24.120 [pool-1-thread-1] 작업 중 : 9
```
결과는 이렇게 나온다. 
일단 Cancel상태에서 get() 메소드를 호출하면 당연히 CancellationException 예외가 걸린다.
그런데 cancel(false) : true인 이유는 Future는 멈췄기 때문이고 작업은 계속 수행된다.
Thread.interrupt()를 걸지 않았기 때문이다. 

## 10. Future7 - 예외
Future.get()을 호출하면 작업의 결과값 뿐만 아니라, 작업 중에 발생한 예외도 받을 수 있다.

```java
package thread.executor.future;

import java.util.concurrent.*;
import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class FutureExceptionMain {

  public static void main(String[] args) {
    ExecutorService es = Executors.newFixedThreadPool(1);
    log("작업 전달");
    Future<Integer> future = es.submit(new ExCallable());
    sleep(1000); // 대기
    
    try {
      log("future.get() 호출 시도, future.state() : " + future.state());
      Integer result = future.get();
      log("result value = " + result);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      log("e = " + e);
      Throwable cause = e.getCause();
      log("cause = " + cause);
    }
    
    es.close(); 
  }
  
  static class ExCallable implements Callable<Integer> {
    @Override
    public Integer call() {
      log("Callable 실행, 예외 발생");
      throw new IllegalStateException("ex!"); 
    }
  } 
  
}
```

결과는 다음과 같다.

```text
18:59:16.944 [     main] 작업 전달
18:59:16.963 [pool-1-thread-1] Callable 실행, 예외 발생
18:59:17.972 [     main] future.get() 호출 시도, future.state() : FAILED
18:59:17.973 [     main] e = java.util.concurrent.ExecutionException: java.lang.IllegalStateException: ex!
18:59:17.973 [     main] cause = java.lang.IllegalStateException: ex!
```

여기서 잠깐 개념을 정립하고 가자. 예전에 예외처리를 배웠을때는 catch(Exception e)가 있고 어떤 메서드를 호출 했을 때,
그 메서드에서 throw new IOException("예외 메시지"); 라고 한다면 Exception e = new IOException("예외 메시지"); 라는 개념으로 봤다.
다형성이니까 성립한다.

그런데 위에것은 좀 다른게 ExecutionException e = new ExecutionException(new IllegalStateException("ex")); 
ExecutionException으로 한번 감 싼 다음에 예외를 넣는다. 

다음 코드를 참고하자.

```java
public ExecutionException(Throwable cause) {
  super(cause);
}
```

- 요청 스레드 : es.submit(new ExCallable())을 호출해서 작업을 전달한다.
- 작업 스레드 : ExCallable을 실행하는데, IllegalStateException 예외가 발생한다.
-> 작업 스레드는 Future에 발생한 예외를 담아둔다. 참고로 예외도 객체이다. 잡아서 필드에 보관할 수 있다.
-> 예외가 발생했으므로 Future의 상태는 FAILED가 된다.
- 요청 스레드 : 결과를 얻기 위해 future.get()을 호출한다.
-> Future의 상태가 FAILED면 ExecutionException 예외를 던진다.
-> 이 예외는 내부에 앞서 Future에 저장해둔 IllegalStateException을 포함하고 있다.
-> e.getCause()을 호출하면 작업에서 발생한 원본 예외를 받을 수 있다.

## 11. ExecutorService - 작업 컬렉션 처리
ExecutorService는 여러 작업을 한 번에 편리하게 처리하는 invokeAll(), invokeAny() 기능을 제공한다.

**작업 컬렉션 처리**
invokeAll()
```java
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;
```
- 모든 Callable 작업을 제출하고, 모든 작업이 완료될 때까지 기다린다. 

```java
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException;
```
- 지정된 시간 내에 모든 Callable 작업을 제출하고 완료될 때까지 기다린다.

invokeAny()

```java
<T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;
```
- 하나의 Callable 작업이 완료될 때까지 기다리고, 가장 먼저 완료된 작업의 결과를 반환한다.
- 완료되지 않은 나머지 작업은 취소한다.

```java
<T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException;
```
- 지정된 시간 내에 하나의 Callable 작업이 완료될 때까지 기다리고, 가장 먼저 완료된 작업의 결과를 반환한다.
- 완료되지 않은 나머지 작업은 취소한다.

먼저 예제를 시작하기 전에 특정 시간 대기하는 CallableTask를 만들자.

```java
package thread.executor;

import java.util.concurrent.Callable;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class CallableTask implements Callable<Integer> {
  
  private String name;
  private int sleepMs = 1000;
  
  public CallableTask(String name) {
    this.name = name;
  }
  
  public CallableTask(String name, int sleepMs) {
    this.name = name;
    this.sleepMs = sleepMs;
  }
  
  @Override
  public Integer call() throws Exception {
    log(name + " 실행");
    sleep(sleepMs);
    log(name + " 완료, return = " + sleepMs);
    return sleepMs;
  }
  
}
```

- Callable 인터페이스를 구현한다.
- 전달 받은 sleep 값 만큼 대기한다.
- sleep 값을 반환한다. 

**ExecutorService - invokeAll()**
```java
package thread.executor.future;

import thread.executor.CallableTask;

import java.util.List;
import java.util.concurrent.*;

import static util.MyLogger.log;

public class InvokeAllMain {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    ExecutorService es = Executors.newFixedThreadPool(10);
    
    CallableTask task1 = new CallableTask("task1", 1000);
    CallableTask task2 = new CallableTask("task2", 2000);
    CallableTask task3 = new CallableTask("task3", 3000);
    List<CallableTask> tasks = List.of(task1, task2, task3);
    
    List<Future<Integer>> futures = es.invokeAll(tasks);
    for(Future<Integer> future : futures) {
      Integer value = future.get();
      log("value = " + value);
    }
    
    es.close(); 
  }
  
}
```

```text
19:53:29.367 [pool-1-thread-1] task1 실행
19:53:29.367 [pool-1-thread-3] task3 실행
19:53:29.367 [pool-1-thread-2] task2 실행
19:53:30.375 [pool-1-thread-1] task1 완료
19:53:31.380 [pool-1-thread-2] task2 완료
19:53:32.386 [pool-1-thread-3] task3 완료
19:53:32.388 [     main] value = 1000
19:53:32.388 [     main] value = 2000
19:53:32.389 [     main] value = 3000
```

모든 task를 다 마쳤을때 결과물이 나온다. 

참고 : invokeAll()을 쓰면 submit도 자동으로 실행되는것. 

**동기 vs 비동기**
- 동기 : 어떤 일이 있으면 그 일의 결과가 나오고나서 다음 일을 실행.
- 비동기 : 어떤 일이 있더라도 일의 결과를 기다리지 않고 계속 일을 수행함. 

## 12. Future를 활용한 예제
먼저 다음 코드를 보고 어떤 문제가 있는지 실행 해보자. 

```java
package thread.executor.test;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class OldOrderService {

  public void order(String orderNo) {

    InventoryWork inventoryWork = new InventoryWork(orderNo);
    ShippingWork shippingWork = new ShippingWork(orderNo);
    AccountingWork accountingWork = new AccountingWork(orderNo);

    // 작업 요청
    Boolean inventoryResult = inventoryWork.call();
    Boolean shippingResult = shippingWork.call();
    Boolean accountingResult = accountingWork.call();

    // 결과 확인
    if (inventoryResult && shippingResult && accountingResult) {
      log("모든 주문 처리가 성공적으로 완료되었습니다.");
    } else {
      log("일부 작업이 실패했습니다.");
    }

  }

  static class InventoryWork {

    private final String orderNo;

    public InventoryWork(String orderNo) {
      this.orderNo = orderNo;
    }

    public Boolean call() {
      log("재고 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class ShippingWork {

    private final String orderNo;

    public ShippingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    public Boolean call() {
      log("배송 시스템 알림 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class AccountingWork {

    private final String orderNo;

    public AccountingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    public Boolean call() {
      log("회계 시스템 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }
}
```

```java
package thread.executor.test;

public class OldOrderServiceTestMain {

  public static void main(String[] args) {
    String orderNo = "Order#1234";
    OldOrderService orderService = new OldOrderService();
    orderService.order(orderNo);
  }

}
```

코드를 실행하면 결과는 다음과 같다. 

```text
20:48:07.135 [     main] 재고 업데이트 : Order#1234
20:48:08.143 [     main] 배송 시스템 알림 : Order#1234
20:48:09.155 [     main] 회계 시스템 업데이트 : Order#1234
20:48:10.169 [     main] 모든 주문 처리가 성공적으로 완료되었습니다.
```
sleep(1000);도 있고 스레드가 하나여서 동기식으로 처리된다. 이걸 비동기식으로 고치면서 병렬로 일을 처리해보자. 

```java
package thread.executor.test;

import java.util.concurrent.*;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class NewOrderService {

  private final ExecutorService es = Executors.newFixedThreadPool(3);

  public void order(String orderNo) throws ExecutionException, InterruptedException {

    // 작업 요청
    Future<Boolean> future1 = es.submit(new InventoryWork(orderNo));
    Future<Boolean> future2 = es.submit(new ShippingWork(orderNo));
    Future<Boolean> future3 = es.submit(new AccountingWork(orderNo));

    // 결과물 기다리기
    Boolean inventoryResult = future1.get();
    Boolean shippingResult = future2.get();
    Boolean accountingResult = future3.get();

    // 결과 확인
    if (inventoryResult && shippingResult && accountingResult) {
      log("모든 주문 처리가 성공적으로 완료되었습니다.");
    } else {
      log("일부 작업이 실패했습니다.");
    }

    es.close();

  }

  static class InventoryWork implements Callable<Boolean> {

    private final String orderNo;

    public InventoryWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("재고 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class ShippingWork implements Callable<Boolean> {

    private final String orderNo;

    public ShippingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("배송 시스템 알림 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class AccountingWork implements Callable<Boolean> {

    private final String orderNo;

    public AccountingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("회계 시스템 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }
}
```

먼저 비동기식 및 병렬로 일을 처리하고 반환형을 즉시 받기 위해 implements Callable 및 메서드를 재정의하고 Future를 활용했다.

```java
package thread.executor.test;

import java.util.concurrent.ExecutionException;

import static util.MyLogger.log;

public class NewOrderServiceTestMain {

  public static void main(String[] args) {
    String orderNo = "Order#1234";
    NewOrderService orderService = new NewOrderService();
    try {
      orderService.order(orderNo);
    } catch (InterruptedException e){
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      log("e = " + e);
      Throwable cause = e.getCause();
      log("cause = " + cause);
    }
    
  }

}
```

실행하면 결과는 다음과 같다. 

```text
20:50:45.257 [pool-1-thread-2] 배송 시스템 알림 : Order#1234
20:50:45.257 [pool-1-thread-3] 회계 시스템 업데이트 : Order#1234
20:50:45.257 [pool-1-thread-1] 재고 업데이트 : Order#1234
20:50:46.271 [     main] 모든 주문 처리가 성공적으로 완료되었습니다.
```

3초 걸리던 작업이 1초로 걸렸다. 비동기 및 병렬의 중요성을 알게 된 코드다. 




