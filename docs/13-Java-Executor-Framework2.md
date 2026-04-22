# 스레드 풀과 Executor 프레임워크2

## 1. ExecutorService의 종료
고객의 주문을 처리하는 서버를 운영중이라고 가정하자.
만약 서버 기능 업데이트를 위해서 서버를 재시작해야 한다고 가정해보자.
이때 서버 애플리케이션이 고객의 주문을 처리하고 있는 도중에 갑자기 재시작 된다면, 해당 고객의 주문이 제대로 진행
되지 못할 것이다.
가장 이상적인 방향은 새로운 주문 요청은 막고, 이미 진행중인 주문은 모두 완료한 다음에 서버를 재시작 하는 것이 가장 좋을 것이다.
이처럼 서비스를 안정적으로 종료하는 것도 매우 중요하다.
이렇게 문제 없이 우아하게 종료하는 방식을 graceful shutdown이라 한다.

이런 관점에서 ExecutorService의 종료에 대해서 알아보자.

**ExecutorService의 종료 메서드**
ExecutorService에는 종료와 관련된 다양한 메서드가 존재한다.

서비스 종료
- void shutdown()
-> 새로운 작업을 받지 않고, 이미 제출된 작업을 모두 완료한 후에 종료한다.
-> Non Blocking Method(이 메서드를 호출한 스레드는 대기하지 않고 즉시 다음 코드를 호출한다.)

- List<Runnable> shutdownNow()
-> 실행 중인 작업을 중단하고, 대기 중인 작업을 반환하며 즉시 종료한다.
-> 실행 중인 작업을 중단하기 위해 인터럽트를 발생시킨다.
-> 논 블로킹 메서드

서비스 상태 확인
- boolean isShutdown()
-> 서비스가 종료되었는지 확인한다.
- boolean isTerminated()
-> shutdown(), shutdownNow() 호출 후, 모든 작업이 완료되었는지 확인한다.

작업 완료 대기
- boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
-> 서비스 종료시 모든 작업이 완료될 때까지 대기한다. 이때 지정된 시간까지만 대기한다.
-> 블로킹 메서드

close()
- close()는 자바 19부터 지원하는 서비스 종료 메서드이다. 이 메서드는 shutdown()과 같다고 생각하면 된다.
-> 더 정확히는 shutdown()을 호출하고, 작업이 완료되거나 인터럽트가 발생할 때 까지 무한정 반복 대기 한다.
-> 호출한 스레드에 인터럽트가 발생해도 shutdownNow()를 호출한다. 

**shutdown() - 처리중인 작업이 없는 경우**
- shutdown()을 호출한다.
- ExecutorService는 새로운 요청을 거절한다.
-> 거절 시 기본적으로 java.util.concurrent.RejectedExecutionException 예외가 발생한다.
- 스레드 풀의 자원을 정리한다.

**shutdown() - 처리중인 작업이 있는 경우**
- shutdown()을 호출한다.
- ExecutorService는 새로운 요청을 거절한다.
- 스레드 풀의 스레드는 처리중인 작업을 완료한다.
- 스레드 풀의 스레드는 큐에 남아있는 작업도 모두 꺼내서 완료한다.
- 모든 작업을 완료하면 자원을 정리한다.
- 결과적으로 처리중이던 taskA, taskB는 물론이고 큐에 대기중이던 taskC, taskD도 완료된다.

**shutdownNow() - 처리중인 작업이 있는 경우**
- shutdownNow()를 호출한다.
- ExecutorService는 새로운 요청을 거절한다.
- 큐를 비우면서, 큐에 있는 작업을 모두 꺼내서 컬렉션으로 반환한다.
-> List<Runnable> runnables = es.shutdownNow()
- 작업 중인 스레드에 인터럽트가 발생한다.
-> 작업 중인 taskA, taskB는 인터럽트가 걸린다.
-> 큐에 대기중인 taskC, taskD는 수행되지 않는다.
- 작업을 완료하면 자원을 정리한다. 

## 2. ExecutorService 우아한 종료 - 구현
shutdown()을 호출해서 이미 들어온 모든 작업을 다 처리하고 서비스를 우아하게 종료하는것이 가장 이상적이지만,
갑자기 요청이 너무 많이 들어와서 큐에 대기중인 작업이 너무 많아 작업 완료가 어렵거나, 작업이 너무 오래 걸리거나,
또는 버그가 발생해서 특정 작업이 끝나지 않을 수 있다. 이렇게 되면 서비스가 너무 늦게 종료되거나,
종료되지 않는 문제가 발생할 수 있다.

이럴 때는 보통 우아하게 종료하는 시간을 정한다. 예를 들어서, 60초까지는 작업을 다 처리할 수 있게 기다리는 것이다.
그리고 60초가 지나면, 무언가 문제가 있다고 가정하고 shutdownNow()를 호출해서 작업들을 강제로 종료한다.

close()

close()의 경우 이렇게 구현되어 있다. shutdown()을 호출하고, 하루를 기다려도 작업이 완료되지 않으면
shutdownNow()를 호출한다. 그리고 대부분 하루를 기다릴 수는 없을 것이다.

방금 설명한데로 우선은 shutdown()을 통해 우아한 종료를 시도하고, 10초간 종료되지 않으면 shutdownNow()를
통해 강제 종료하는 방식을 구현해보자. 

```java
package thread.executor;

import java.util.concurrent.*;

import static thread.executor.ExecutorUtils.printState;
import static util.MyLogger.log;

public class ExecutorShutdownMain {

  public static void main(String[] args) throws InterruptedException {
    ExecutorService es = Executors.newFixedThreadPool(2);
    es.execute(new RunnableTask("taskA"));
    es.execute(new RunnableTask("taskB"));
    es.execute(new RunnableTask("taskC"));
    es.execute(new RunnableTask("longTask", 100_000));
    printState(es);
    log("== shutdown 시작 ==");
    shutdownAndAwaitTermination(es);
    log("== shutdown 완료 ==");
    printState(es);
  }
  
  static void shutdownAndAwaitTermination(ExecutorService es) {
    es.shutdown(); // non-blocking, 새로운 작업을 받지 않는다. 처리 중이거나, 큐에 이미 대기중인 작업은 처리한다.
    // 이후에 풀의 스레드를 종료한다.
    
    try {
      // 이미 대기중인 작업을 모두 완료할 때 까지 10초 기다린다.
      log("서비스 정상 종료 시도");
      if(!es.awaitTermination(10, TimeUnit.SECONDS)) {
        // 정상 종료가 너무 오래 걸리면..
        log("서비스 정상 종료 실패 -> 강제 종료 시도");
        es.shutdownNow();
        // 작업이 취소될 때 까지 대기한다.
        if(!es.awaitTermination(10, TimeUnit.SECONDS)) {
          log("서비스가 종료되지 않았습니다.");
        }
      }
    } catch (InterruptedException ex) {
      // awaitTermination()으로 대기중인 현재 스레드가 인터럽트 될 수 있다. 
      es.shutdownNow(); 
    }
  }
  
}
```

결과는 다음과 같다.

```text
17:16:55.028 [     main] [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
17:16:55.028 [pool-1-thread-1] taskA 시작
17:16:55.028 [pool-1-thread-2] taskB 시작
17:16:55.032 [     main] == shutdown 시작
17:16:56.043 [pool-1-thread-1] taskA 완료
17:16:56.043 [pool-1-thread-1] taskC 시작
17:16:56.043 [pool-1-thread-2] taskB 완료
17:16:56.044 [pool-1-thread-2] longTask 시작
17:16:57.050 [pool-1-thread-1] taskC 완료
17:17:05.042 [     main] 서비스 정상 종료 실패 -> 강제 종료 시도
17:17:05.042 [pool-1-thread-2] 인터럽트 발생, sleep interrupted
17:17:05.043 [     main] == shutdown 완료
17:17:05.044 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 4]
```

왜 이런 결과가 나왔을까? 

```java
public static void main(String[] args) {
  ExecutorService es = Executors.newFixedThreadPool(2);
  es.execute(new RunnableTask("taskA"));
  es.execute(new RunnableTask("taskB"));
  es.execute(new RunnableTask("taskC"));
  es.execute(new RunnableTask("longTask", 100_000));
}
```

먼저 위에서 실행해야 할 task들을 다 담는데 스레드 풀에는 스레드가 2개만 있으므로 2개의 task만 처리한다.
그래서 다음과 같은 결과가 나온다.
```text
17:16:55.028 [     main] [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
```
일단 2개의 task를 처리해야하니까 BlockingQueue에는 2개의 Task들만 남는다.
스레드들이 작업하고 있는 task들은 2개여서 active = 2가 나온다. 이제 작업들을 수행하는데 
taskA, taskB, taskC는 sleep(1000)이여서 1초면 작업이 끝난다. 그런데 longTask 같은 경우에는 
100초를 기다려야하므로 일이 끝나지 않는다. 

```text
shutdownAndAwaitTermination(es);
```
여기서 shutdownAndAwaitTermination(es); 를 통해 shutdown을 실행한다. 
이때는 task들을 더 받지 않는다. 
longTask를 처리 중이니까 일을 더 하지 않고 10초동안 일이 끝나는지를 대기한다.
만약에 일이 종료가 안되면 shutdownNow()를 실행해서 일을 강제로 종료한다.  
그리고 예외메시지를 띄운다. 

```text
es.shutdown();
```
- 새로운 작업을 받지 않는다. 처리 중이거나, 큐에 이미 대기중인 작업은 처리한다. 이후에 풀의 스레드를 종료한다.
- shutdown()은 블로킹 메서드가 아니다. 서비스가 종료될 때 까지 main 스레드가 대기하지 않는다.
main 스레드는 바로 다음 코드를 호출한다. 

```text
if(!es.awaitTermination(10, TimeUnit.SECONDS)) {}
```
- 블로킹 메서드이다.
- main 스레드는 대기하며 서비스 종료를 10초간 기다린다.
-> 만약 10초안에 모든 작업이 완료된다면 true를 반환한다.
- 여기서 taskA, taskB, taskC의 수행이 완료된다. 그런데 longTask는 10초가 지나도
완료되지 않았다. (따라서, false를 반환한다.)

**서비스 종료 실패**
그런데 마지막에 강제 종료인 es.shutdownNow()를 호출한 다음에 왜 10초간 또 기다릴까?
shutdownNow()가 작업 중인 스레드에 인터럽트를 호출하는 것은 맞다. 인터럽트를 호출하더라도 여러가지 이유로
작업에 시간이 걸릴 수 있다. 인터럽트 이후에 자원을 정리하는 어떤 간단한 작업을 수행할 수 도 있다.
이런 시간을 기다려주는 것이다. 

## 3. Executor 스레드 풀 관리 - 코드
Executor 프레임워크가 어떤식으로 스레드를 관리하는지 깊이있게 알아보자.

ExecutorService의 기본 구현체인 ThreadPoolExecutor의 생성자는 다음 속성을 사용한다.
- corePoolSize : 스레드 풀에서 관리되는 기본 스레드의 수
- maximumPoolSize : 스레드 풀에서 관리되는 최대 스레드 수
- keepAliveTime, TimeUnit unit : 기본 스레드 수를 초과해서 만들어진 초과 스레드가 생존할 수 있는 대기 시간,
이 시간 동안 처리할 작업이 없다면 초과 스레드는 제거된다. 
- BlockingQueue workQueue : 작업을 보관할 블로킹 큐

corePoolSize와 maximumPoolSize의 차이를 알아보기 위해 간단한 예제를 만들어보자.

```java
public class ExecutorUtils {
  
  public static void printState(ExecutorService executorService, String taskName) {
    if(executorService instanceof ThreadPoolExecutor poolExecutor) {
      int pool = poolExecutor.getPoolSize();
      int active = poolExecutor.getActiveCount();
      int queue = poolExecutor.getQueue().size();
      long completedTask = poolExecutor.getCompletedTaskCount();
      log(taskName + " -> [pool = " + pool + ", active = " + active + ", queuedTasks = " 
          + queued + ", completedTasks = " + completedTask);
    } else {
      log(taskName + " -> " + executorService); 
    }
  }
  
}
```
- printState() 메서드를 하나 오버로딩 했다. 단순히 taskName을 출력하는 부분이 추가되었다.
- 중복된 부분을 제거할 수 있지만, 기본 코드를 유지하기 위해 그대로 복사해서 약간만 수정했다.

```java
package thread.executor.poolsize;

import thread.executor.RunnableTask;

import java.util.concurrent.*;

import static thread.executor.ExecutorUtils.printState;
import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class PoolSizeMainV1 {

  public static void main(String[] args) throws InterruptedException {
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(2);
    ExecutorService es = new ThreadPoolExecutor(2, 4, 3000, TimeUnit.MILLISECONDS, workQueue);
    printState(es);
    
    es.execute(new RunnableTask("task1"));
    printState(es, "task1");
    
    es.execute(new RunnableTask("task2"));
    printState(es, "task2");
    
    es.execute(new RunnableTask("task3"));
    printState(es, "task3");
    
    es.execute(new RunnableTask("task4"));
    printState(es, "task4");
    
    es.execute(new RunnableTask("task5"));
    printState(es, "task5");
    
    es.execute(new RunnableTask("task6"));
    printState(es, "task6");
    
    try {
      es.execute(new RunnableTask("task7"));
    } catch (RejectedExecutionException e) {
      log("task7 실행 거절 예외 발생 : " + e);
    }
    
    sleep(3000);
    log("== 작업 수행 완료 ==");
    printState(es);
    
    sleep(3000);
    log("== maximumPoolSize 대기 시간 초과 ==");
    printState(es);
    
    es.close();
    log("== shutdown 완료 ==");
    printState(es);
  }
}
```

결과는 다음과 같다.

```text
15:37:36.520 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 0]
15:37:36.530 [pool-1-thread-1] task1 시작
15:37:36.551 [     main] task1 -> [pool=1, active = 1, queuedTasks = 0, completedTask = 0]
15:37:36.552 [     main] task2 -> [pool=2, active = 2, queuedTasks = 0, completedTask = 0]
15:37:36.552 [pool-1-thread-2] task2 시작
15:37:36.553 [     main] task3 -> [pool=2, active = 2, queuedTasks = 1, completedTask = 0]
15:37:36.554 [     main] task4 -> [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
15:37:36.555 [     main] task5 -> [pool=3, active = 3, queuedTasks = 2, completedTask = 0]
15:37:36.555 [pool-1-thread-3] task5 시작
15:37:36.556 [     main] task6 -> [pool=4, active = 4, queuedTasks = 2, completedTask = 0]
15:37:36.556 [pool-1-thread-4] task6 시작
15:37:36.557 [     main] task7 실행 거절 예외 발생 : java.util.concurrent.RejectedExecutionException: Task thread.executor.RunnableTask@579bb367 rejected from java.util.concurrent.ThreadPoolExecutor@12edcd21[Running, pool size = 4, active threads = 4, queued tasks = 2, completed tasks = 0]
15:37:37.536 [pool-1-thread-1] task1 완료
15:37:37.537 [pool-1-thread-1] task3 시작
15:37:37.567 [pool-1-thread-4] task6 완료
15:37:37.567 [pool-1-thread-3] task5 완료
15:37:37.567 [pool-1-thread-2] task2 완료
15:37:37.567 [pool-1-thread-4] task4 시작
15:37:38.543 [pool-1-thread-1] task3 완료
15:37:38.574 [pool-1-thread-4] task4 완료
15:37:39.558 [     main] == 작업 수행 완료 ==
15:37:39.558 [     main] [pool=4, active = 0, queuedTasks = 0, completedTask = 6]
15:37:42.566 [     main] == maximumPoolSize 대기 시간 초과 ==
15:37:42.567 [     main] [pool=2, active = 0, queuedTasks = 0, completedTask = 6]
15:37:42.567 [     main] == shutdown 완료 ==
15:37:42.568 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 6]
```

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(2);
ExecutorService es = new ThreadPoolExecutor(2, 4, 3000, TimeUnit.MILLISECONDS, workQueue); 
```

- 작업을 보관할 블로킹 큐의 구현체로 ArrayBlockingQueue(2)를 사용했다. 사이즈를 2로 설정했으므로 최대 2개까지 작업을 큐에 보관할 수 있다.
- corePoolSize=2, maximumPoolSize=4를 사용해서 기본 스레드는 2개, 최대 스레드는 4개로 설정했다.
-> 스레드 풀에 기본 2개의 스레드를 운영한다. 요청이 너무 많거나 급한 경우 스레드 풀은 최대 4개까지 스레드를 증가시켜서 사용할 수 있다.
-> 이렇게 기보 스레드 수를 초과해서 만들어진 스레드를 초과 스레드라 한다.

- 3000, TimeUnit.MILLISECONDS
-> 초과 스레드는 생존할 수 있는 대기 시간을 뜻한다. 이 시간 동안 초과 스레드가 처리할 작업이 없다면 초과
스레드는 제거된다. 
-> 여기서는 3000 밀리초(3초)를 설정했으므로, 초과 스레드가 3초간 작업을 하지 않고 대기한다면 초과 스레드는 스레드 풀에서 제거된다.

## 4. Executor 스레드 풀 관리 - 분석
**실행 분석**
- task1 작업을 요청한다.
- Executor는 스레드 풀에 스레드가 core 사이즈 만큼 있는지 확인한다.
-> core 사이즈 만큼 없다면 스레드를 하나 생성한다.
-> 작업을 처리하기 위해 스레드를 하나 생성했기 때문에 작업을 큐에 넣을 필요 없이, 해당 스레드가 바로 작업을 처리한다.

```text
15:37:36.530 [pool-1-thread-1] task1 시작
15:37:36.551 [     main] task1 -> [pool=1, active = 1, queuedTasks = 0, completedTask = 0]
```
- 새로 만들어진 스레드1이 task1을 수행한다. 

```text
15:37:36.552 [     main] task2 -> [pool=2, active = 2, queuedTasks = 0, completedTask = 0]
15:37:36.552 [pool-1-thread-2] task2 시작
```

- task2를 요청한다.
- Executor는 스레드 풀에 스레드가 core 사이즈 만큼 있는지 확인한다.
-> 아직 core 사이즈 만큼 없으므로 스레드를 하나 생성한다.
- 새로 만들어진 스레드2가 task2를 처리한다.

```text
15:37:36.553 [     main] task3 -> [pool=2, active = 2, queuedTasks = 1, completedTask = 0]
```
- task3 작업을 요청한다.
- Executor는 스레드 풀에 스레드가 core 사이즈 만큼 있는지 확인한다.
- core 사이즈 만큼 스레드가 이미 만들어져 있고, 스레드 풀에 사용할 수 있는 스레드가 없으므로 이 경우 큐에 작업을 보관한다. 

```text
15:37:36.554 [     main] task4 -> [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
```
- task4 작업을 요청한다.
- Executor는 스레드 풀에 스레드가 core 사이즈 만큼 있는지 확인한다.
- core 사이즈 만큼 스레드가 이미 만들어져 있고, 스레드 풀에 사용할 수 있는 스레드가 없으므로 이 경우 큐에 작업을 보관한다.

```text
15:37:36.555 [     main] task5 -> [pool=3, active = 3, queuedTasks = 2, completedTask = 0]
15:37:36.555 [pool-1-thread-3] task5 시작
```
- task5 작업을 요청한다.
- Executor는 스레드 풀에 스레드가 core 사이즈 만큼 있는지 확인한다. -> core 사이즈 만큼 있다.
- Executor는 큐에 보관을 시도한다. -> 큐가 가득 찼다.

큐가 가득차면 긴급 상황이다. 대기하는 작업이 꽉 찰 정도로 요청이 많다는 뜻이다. 
이 경우 Executor는 max(maximumPoolSize) 사이즈까지 초과 스레드를 만들어서 작업을 수행한다.
- core=2 : 기본 스레드는 최대 2개
- max=4 : 기본 스레드 2개에 초과 스레드 2개 합계 총 4개 가능
- Executor는 초과 스레드인 스레드3을 만든다.
- 작업을 처리하기 위해 스레드를 하나 생성했기 때문에 작업을 큐에 넣을 필요 없이, 해당 스레드가 바로 작업을 처리한다. 
-> 참고로 이 경우 큐가 가득찼기 때문에 큐에 넣는 것도 불가능하다.
- 스레드3이 task5를 처리한다.

```text
15:37:36.556 [     main] task6 -> [pool=4, active = 4, queuedTasks = 2, completedTask = 0]
15:37:36.556 [pool-1-thread-4] task6 시작
```
- task6 작업을 요청한다.
- 큐가 가득찼다.
- Executor는 초과 스레드인 스레드4를 만들어서 task6을 처리한다.
-> 큐가 가득찼기 때문에 작업을 큐에 넣는 것은 불가능하다.

```text
15:37:36.557 [     main] task7 실행 거절 예외 발생 : java.util.concurrent.RejectedExecutionException
: Task thread.executor.RunnableTask@579bb367 rejected from java.util.concurrent.ThreadPoolExecutor@12edcd21
[Running, pool size = 4, active threads = 4, queued tasks = 2, completed tasks = 0]
```

- task7 작업을 요청한다.
- 큐가 가득찼다.
- 스레드 풀의 스레드도 max 사이즈 만큼 가득찼다.
- RejectedExecutionException이 발생한다.

```text
15:37:42.566 [     main] == maximumPoolSize 대기 시간 초과 ==
15:37:42.567 [     main] [pool=2, active = 0, queuedTasks = 0, completedTask = 6]
```
- 스레드3, 스레드4와 같은 초과 스레드들은 지정된 시간까지 작업을 하지 않고 대기하면 제거된다. 긴급한 작업들이 끝난 것으로 이해하면 된다.
- 여기서는 지정한 3초간 스레드3, 스레드4가 작업을 진행하지 않았기 때문에 스레드 풀에서 제거된다.
- 참고로 초과 스레드가 작업을 처리할 때 마다 시간은 계속 초기화 된다.
-> 작업 요청이 계속 들어온다면 긴급한 상황이 끝난 것이 아니다. 따라서 긴급한 상황이 끝날 때 까지는 초과 스레드를 살려두는 것이 많은 스레드를 사용해서
작업을 더 빨리 처리할 수 있다. 

**정리 - Executor 스레드 풀 관리**
1. 작업을 요청하면 core 사이즈 만큼 스레드를 만든다.
2. core 사이즈를 초과하면 큐에 작업을 넣는다.
3. 큐를 초과하면 max 사이즈 만큼 스레드를 만든다. 임시로 사용되는 초과 스레드가 생성된다.
-> 큐가 가득차서 큐에 넣을 수도 없다. 초과 스레드가 바로 수행해야 한다.
4. max 사이즈를 초과하면 요청을 거절한다. 예외가 발생한다.
-> 큐도 가득차고, 풀에 최대 생성 가능한 스레드 수도 가득 찼다. 작업을 받을 수 없다.

**스레드 미리 생성하기**
응답시간이 아주 중요한 서버라면, 서버가 고객의 처음 요청을 받기 전에 스레드를 스레드 풀에 미리 생성해두고 싶을 수 있다.
스레드를 미리 생성해두면, 처음 요청에서 사용되는 스레드의 생성 시간을 줄일 수 있다.
ThreadPoolExecutor.prestartAllCoreThreads()를 사용하면 기본 스레드를 미리 생성 할 수 있다.
참고로 ExecutorService는 이 메서드를 제공하지 않는다. 

```java
package thread.executor;

import java.util.concurrent.*;

import static thread.executor.ExecutorUtils.*;
import static util.ThreadUtils.sleep;

public class PrestartPoolMain {

  public static void main(String[] args) {
    ExecutorService es = Executors.newFixedThreadPool(1000);
    printState(es);
    
    ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) es;
    poolExecutor.prestartAllCoreThreads();
    printState(es); 
  }
  
}
```

## 5. Executor 전략 - 고정 풀 전략
**Executor 스레드 풀 관리 - 다양한 전략**
ThreadPoolExecutor를 사용하면 스레드 풀에 사용되는 숫자와 블로킹 큐등 다양한 속성을 조절할 수 있다. 
- corePoolSize : 스레드 풀에서 관리되는 기본 스레드의 수
- maximumPoolSize : 스레드 풀에서 관리되는 최대 스레드 수
- keepAliveTime, TimeUnit unit : 기본 스레드 수를 초과해서 만들어진 스레드가 생존할 수 있는 대기 시간,
이 시간 동안 처리할 작업이 없다면 초과 스레드는 제거된다. 
- BlockingQueue workQueue : 작업을 보관할 블로킹 큐 

이런 속성들을 조절하면 자신에게 맞는 스레드 풀 전략을 사용할 수 있다.

자바는 Executors 클래스를 통해 3가지 기본 전략을 제공한다.
- newSingleThreadPool() : 단일 스레드 풀 전략
- newFixedThreadPool(nThreads) : 고정 스레드 풀 전략
- newCachedThreadPool() : 캐시 스레드 풀 전략 

newSingleThreadPool() : 단일 스레드 풀 전략
- 스레드 풀에 기본 스레드 1개만 사용한다.
- 큐 사이즈에 제한이 없다. (LinkedBlockingQueue)
- 주로 간단히 사용하거나, 테스트 용도로 사용한다.

```text
new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, 
                       new LinkedBlockingQueue<Runnable>()); 
```
고정 스레드 풀 전략과 캐시 스레드 풀 전략을 자세히 알아보자.

**Executor 스레드 풀 관리 - 고정 풀 전략**
newFixedThreadPool(nThreads)
- 스레드 풀에 nThreads 만큼의 기본 스레드를 생성한다. 초과 스레드는 생성하지 않는다.
- 큐 사이즈에 제한이 없다. (LinkedBlockingQueue)
- 스레드 수가 고정되어 있기 때문에 CPU, 메모리 리소스가 어느정도 예측 가능한 안정적인 방식이다. 

코드를 실행해보자.
```java
package thread.executor.poolsize;

import thread.executor.RunnableTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static thread.executor.ExecutorUtils.printState;
import static util.MyLogger.log;

public class PoolSizeMainV2 {

  public static void main(String[] args) throws InterruptedException {
    
    ExecutorService es = Executors.newFixedThreadPool(2);
    // ExecutorService es = new ThreadPoolExecutor(2,2, 0L
    // , TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    log("pool 생성");
    printState(es); 
    
    for (int i = 1; i <= 6; i++) {
      String taskName = "task" + i;
      es.execute(new RunnableTask(taskName));
      printState(es, taskName);
    }
    es.close();
    log("== shutdown 완료 =="); 
    
  }
  
}
```
코드를 실행한 결과는 다음과 같다.
```text
16:09:59.731 [     main] pool 생성
16:09:59.757 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 0]
16:09:59.767 [pool-1-thread-1] task1 시작
16:09:59.787 [     main] task1 -> [pool=1, active = 1, queuedTasks = 0, completedTask = 0]
16:09:59.788 [     main] task2 -> [pool=2, active = 2, queuedTasks = 0, completedTask = 0]
16:09:59.788 [pool-1-thread-2] task2 시작
16:09:59.788 [     main] task3 -> [pool=2, active = 2, queuedTasks = 1, completedTask = 0]
16:09:59.789 [     main] task4 -> [pool=2, active = 2, queuedTasks = 2, completedTask = 0]
16:09:59.789 [     main] task5 -> [pool=2, active = 2, queuedTasks = 3, completedTask = 0]
16:09:59.790 [     main] task6 -> [pool=2, active = 2, queuedTasks = 4, completedTask = 0]
16:10:00.783 [pool-1-thread-1] task1 완료
16:10:00.783 [pool-1-thread-1] task3 시작
16:10:00.799 [pool-1-thread-2] task2 완료
16:10:00.799 [pool-1-thread-2] task4 시작
16:10:01.793 [pool-1-thread-1] task3 완료
16:10:01.793 [pool-1-thread-1] task5 시작
16:10:01.809 [pool-1-thread-2] task4 완료
16:10:01.809 [pool-1-thread-2] task6 시작
16:10:02.796 [pool-1-thread-1] task5 완료
16:10:02.813 [pool-1-thread-2] task6 완료
16:10:02.813 [     main] == shutdown 완료 ==
```
2개의 스레드가 안정적으로 작업을 처리하는 것을 확인할 수 있다.

이 전략은 다음과 같은 특징이 있다.

**특징**
스레드 수가 고정되어 있기 때문에 CPU, 메모리 리소스가 어느정도 예측 가능한 안정적인 방식이다.
큐 사이즈도 제한이 없어서 작업을 많이 담아두어도 문제가 없다.

**주의**
이 방식의 가장 큰 장점은 스레드 수가 고정되어서 CPU, 메모리 리소스가 어느정도 예측 가능하다는 점이다.
따라서 일반적인 상황에 가장 안정저긍로 서비스를 운영할 수 있다. 
하지만 상황에 따라 장점이 가장 큰 단점이 되기도 한다. 

**상황1 - 점진적인 사용자 확대**
- 개발한 서비스가 잘 되어서 사용자가 점점 늘어난다.
- 고정 스레드 전략을 사용해서 서비스를 안정적으로 잘 운영했는데, 언젠가부터 사용자들이 서비스 응답이 점점 느려진다고 항의한다. 

**상황2 - 갑작스런 요청 증가**
- 마케팅 팀의 이벤트가 대성공 하면서 갑자기 사용자가 폭증했다.
- 고객은 응답을 받지 못하다고 항의한다.

**확인**
- 개발자는 급하게 CPU, 메모리 사용량을 확인해보는데, 아무런 문제 없이 여유있고, 안정적으로 서비스가 운영되고 있다.
- 고정 스레드 전략은 실행되는 스레드 수가 고정되어 있다. 따라서 사용자가 늘어나도 CPU, 메모리 사용이 확 늘어나지 않는다.
- 큐의 사이즈를 확인해보니 요청이 수 만 건이 쌓여있다. 요청이 처리되는 시간보다 쌓이는 시간이 더 빠른 것이다.
참고로 고정 풀 전략의 큐 사이즈는 무한이다.
- 예를 들어서 큐에 10000건이 쌓여있는데, 고정 스레드 수가 10이고, 각 스레드가 작업을 하나 처리하는데 1초가 걸린다면 모든 작업을 다 처리하는데는 
1000초가 걸린다. 만약 처리 속도보다 작업이 쌓이는 속도가 더 빠른 경우에는 더 문제가 된다. 
- 서비스 초기에는 사용자가 적기 때문에 이런 문제가 없지만, 사용자가 늘어나면 문제가 될 수 있다. 
- 갑작스런 요청 증가도 물론 마찬가지이다. 

## 6. Executor 전략 - 캐시 풀 전략
newCachedThreadPool()
- 기본 스레드를 사용하지 않고, 60초 생존 주기를 가진 초과 스레드만 사용한다.
- 초과 스레드의 수는 제한이 없다.
- 큐에 작업을 저장하지 않는다. (SynchronousQueue)
-> 대신에 생산자의 요청을 스레드 풀의 소비자 스레드가 직접 받아서 바로 처리한다.
- 모든 요청이 대기하지 않고 스레드가 바로바로 처리한다. 따라서 빠른 처리가 가능하다.

```text
new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS
, new SynchronousQueue<Runnable>())
```

SynchronousQueue는 아주 특별한 블로킹 큐이다.
- BlockingQueue 인터페이스의 구현체 중 하나이다.
- 이 큐는 내부에 저장 공간이 없다. 대신에 생산자의 작업을 소비자 스레드에게 직접 전달한다. 
- 쉽게 이야기해서 저장 공간의 크기가 0이고, 생산자 스레드가 큐가 작업을 전달하면 소비자 스레드가 큐에서
작업을 꺼낼 때 까지 대기한다.
- 소비자 작업을 요청하면 기다리던 생산자가 소비자에게 직접 작업을 전달하고 반환된다. 그 반대의 경우도 같다.
- 이름 그대로 생산자와 소비자를 동기화하는 큐이다.
- 쉽게 이야기해서 중간에 버퍼를 두지 않는 스레드간 직거래라고 생각하면 된다. 

```java
package thread.executor.poolsize;

import thread.executor.RunnableTask;
import java.util.concurrent.*;

import static thread.executor.ExecutorUtils.printState;
import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class PoolSizeMainV3 {

  public static void main(String[] args) throws InterruptedException {
    //ExecutorService es = Executors.newCachedThreadPool();
    //keepAliveTime 60초 -> 3초로 조절
    ThreadPoolExecutor es = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
        TimeUnit.SECONDS, new SynchronousQueue<>());
    log("pool 생성");
    printState(es);
    
    for(int i = 1; i <= 4; i++) {
      String taskName = "task" + i;
      es.execute(new RunnableTask(taskName));
      printState(es, taskName);
    }
    
    sleep(3000);
    log("== 작업 수행 완료 ==");
    printState(es);
    
    sleep(3000);
    log("== maximumPoolSize 대기 시간 초과 ==");
    printState(es);
    
    es.close();
    log("== shutdown 완료 ==");
    printState(es);
    
    
  }
  
}
```

```text
17:17:31.488 [     main] pool 생성
17:17:31.511 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 0]
17:17:31.518 [pool-1-thread-1] task1 시작
17:17:31.531 [     main] task1 -> [pool=1, active = 1, queuedTasks = 0, completedTask = 0]
17:17:31.531 [     main] task2 -> [pool=2, active = 2, queuedTasks = 0, completedTask = 0]
17:17:31.532 [pool-1-thread-2] task2 시작
17:17:31.532 [     main] task3 -> [pool=3, active = 3, queuedTasks = 0, completedTask = 0]
17:17:31.533 [pool-1-thread-3] task3 시작
17:17:31.533 [     main] task4 -> [pool=4, active = 4, queuedTasks = 0, completedTask = 0]
17:17:31.533 [pool-1-thread-4] task4 시작
17:17:32.523 [pool-1-thread-1] task1 완료
17:17:32.538 [pool-1-thread-2] task2 완료
17:17:32.538 [pool-1-thread-3] task3 완료
17:17:32.538 [pool-1-thread-4] task4 완료
17:17:34.537 [     main] == 작업 수행 완료 ==
17:17:34.537 [     main] [pool=4, active = 0, queuedTasks = 0, completedTask = 4]
17:17:37.547 [     main] == maximumPoolSize 대기 시간 초과 ==
17:17:37.547 [     main] [pool=0, active = 0, queuedTasks = 0, completedTask = 4]
17:17:37.548 [     main] == shutdown 완료 ==
```
- 모든 작업이 대기하지 않고 작업의 수 만큼 스레드가 생기면서 바로 실행되는 것을 확인할 수 있다.
- "maximumPoolSize 대기 시간 초과" 로그를 통해 초과 스레드가 대기 시간이 지나서 모두 사라진 것을 확인할 수 있다. 

**특징**
캐시 스레드 풀 전략은 매우 빠르고, 유연한 전략이다.
이 전략은 기본 스레드도 없고, 대기 큐에 작업도 쌓이지 않는다. 대신에 작업 요청이 오면 초과 스레드로
작업을 바로바로 처리한다. 따라서 빠른 처리가 가능하다. 초과 스레드의 수도 제한이 없기 때문에 CPU, 메모리 자원만 허용한다면
시스템의 자원을 최대로 사용할 수 있다. 
추가로 초과 스레드는 60초간 생존하기 때문에 작업 수에 맞추어 적절한 수의 스레드가 재사용된다. 
이런 특징 때문에 요청이 갑자기 증가하면 스레드도 갑자기 증가하고, 요청이 줄어들면 스레드도 점점 줄어든다. 
이 전략은 작업의 요청 수에 따라서 스레드도 증가하고 감소하므로, 매우 유연한 전략이다. 

**Executor 스레드 풀 관리**
1. 작업을 요청하면 core 사이즈 만큼 스레드를 만든다. 
-> core 사이즈가 없다. 바로 core 사이즈를 초과한다.
2. core 사이즈를 초과하면 큐에 작업을 넣는다.
-> 큐에 작업을 넣을 수 없다. (SynchronousQueue는 큐의 저장 공간이 0인 특별한 큐이다.)
3. 큐를 초과하면 max 사이즈 만큼 스레드를 만든다. 임시로 사용되는 초과 스레드가 생성된다.
-> 초과 스레드가 생성된다. 물론 풀에 대기하는 초과 스레드가 있으면 재사용된다.
4. max 사이즈를 초과하면 요청을 거절한다. 예외가 발생한다.
-> 참고로 max 사이즈가 무제한이다. 따라서 초과 스레드를 무제한으로 만들 수 있다.

**주의**
이 방식은 작업 수에 맞추어 스레드 수가 변하기 때문에, 작업의 처리 속도가 빠르고, CPU, 메모리를 매우 유연하게
사용할 수 있다는 장점이 있다. 하지만 상황에 따라서 장점이 가장 큰 단점이 되기도 한다.

**상황1 - 점진적인 사용자 확대**
- 개발한 서비스가 잘 되어서 사용자가 점점 늘어난다.
- 캐시 스레드 전략을 사용하면 이런 경우 크게 문제가 되지 않는다.
- 캐시 스레드 전략은 이런 경우에는 문제를 빠르게 찾을 수 있다. 사용자가 점점 증가하면서 스레드 사용량도 함께
늘어난다. 따라서 CPU 메모리의 사용량도 자연스럽게 증가한다.
- 물론 CPU, 메모리 자원은 한계가 있기 때문에 적절한 시점에 시스템을 증설해야 한다.
그렇지 않으면 CPU, 메모리 같은 시스템 자원을 너무 많이 사용하면서 시스템이 다운될 수 있다.

**상황2 - 갑작스런 요청 증가**
- 마케팅 팀의 이벤트가 대성공 하면서 갑자기 사용자가 폭증했다.
- 고객은 응답을 받지 못한다고 항의한다. 

**상황2 - 확인**
- 개발자는 급하게 CPU, 메모리 사용량을 확인해보는데, CPU 사용량이 100%이고, 메모리 사용량도 지나치게
높아져있다.
- 스레드 수를 확인해보니 스레드가 수 천개 실행되고 있다. 너무 많은 스레드가 작업을 처리하면서 시스템 전체가
느려지는 현상이 발생하고 있다.
- 캐시 스레드 풀 전략은 스레드가 무한으로 생성될 수 있다.
- 수 천개의 스레드가 처리하는 속도 보다 더 많은 작업이 들어온다.
- 시스템은 너무 많은 스레드에 잠식 당해서 거의 다운된다. 메모리도 거의 다 사용되어 버린다.
- 시스템이 멈추는 장애가 발생한다.

고정 스레드 풀 전략은 서버 자원은 여유가 있는데, 사용자만 점점 느려지는 문제가 발생할 수 있다.
반면에 캐시 스레드 풀 전략은 서버의 자원을 최대한 사용하지만, 서버가 감당할 수 있는 임계점을 넘는 순간 시스템이 다운될 수 있다.

## 7. Executor 전략 - 사용자 정의 풀 전략
상황1 - 점진적인 사용자 확대
-> 개발한 서비스가 잘 되어서 사용자가 점점 늘어난다.
상황2 - 갑작스런 요청 증가
-> 마케팅 팀의 이벤트가 대성공 하면서 갑자기 사용자가 폭증했다.

다음과 같이 세분화된 전략을 사용하면 상황1, 상황2를 모두 어느정도 대응할 수 있다.
- 일반 : 일반적인 상황에서는 CPU, 메모리 자원을 예측할 수 있도록 고정 크기의 스레드로 서비스를 안정적으로 운영한다.
- 긴급 : 사용자의 요청이 갑자기 증가하면 긴급하게 스레드를 추가로 투입해서 작업을 빠르게 처리한다.
- 거절 : 사용자의 요청이 폭증해서 긴급 대응도 어렵다면 사용자의 요청을 거절한다.

이 방법은 평소에는 안정적으로 운영하다가, 사용자의 요청이 갑자기 증가하면 긴급하게 스레드를 더 투입해서 불을 끄는 방법이다.
물론 긴급 상황에는 CPU, 메모리 자원을 더 사용하기 때문에 적정 수준을 찾아야 한다. 일반적으로는 여기까지 대응이 되겠지만,
시스템이 감당할 수 없을 정도로 사용자의 요청이 폭증하면, 처리 가능한 수준의 사용자 요청만 처리하고 나머지 요청은 거절해야 한다.
어떤 경우에도 시스템이 다운되는 최악의 상황은 피해야 한다.

```text
ExecutorService es = new ThreadPoolExecutor(100, 200, 60,
TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000)); 
```
- 100개의 기본 스레드를 사용한다.
- 추가로 긴급 대응 가능한 긴급 스레드를 100개 사용한다. 긴급 스레드는 60초의 생존 주기를 가진다.
- 1000개의 작업이 큐에 대기할 수 있다. 

코드를 실행해보자.

```java
package thread.executor.poolsize;

import thread.executor.RunnableTask;

import java.util.concurrent.*;

import static thread.executor.ExecutorUtils.printState;
import static util.MyLogger.log;

public class PoolSizeMainV4 {
  
  static final int TASK_SIZE = 1100; // 1. 일반
  //static final int TASK_SIZE = 1200; // 2. 긴급
  //static final int TASK_SIZE = 1201; // 3. 거절

  public static void main(String[] args) throws InterruptedException {
    ExecutorService es = new ThreadPoolExecutor(100, 200, 60,
        TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
    printState(es);
    
    long startMs = System.currentTimeMillis();
    
    for (int i = 1; i <= TASK_SIZE; i++) {
      String taskName = "task" + i;
      try {
        es.execute(new RunnableTask(taskName));
        printState(es, taskName);
      } catch (RejectedExecutionException e) {
        log(taskName + " -> " + e);
      }
      
      es.close();
      long endMs = System.currentTimeMillis();
      log("time : " + (endMs - startMs)); 
    }
  }
  
}
```

TASK가 1100일때 실행을 해보자. 

결과가 1100줄이상이라 몇개의 결과만 보도록 하겠다.
```text
15:02:58.181 [     main] task101 -> [pool=100, active = 100, queuedTasks = 1, completedTask = 0]
```
기본 스레드 수가 100이고 101번째 같은 경우에는 아직 큐에 대기하는 작업이 없으므로 1개를 넣어서 위와 같은 결과가 나온다.

```text
15:02:58.317 [     main] task1100 -> [pool=100, active = 100, queuedTasks = 1000, completedTask = 0]
15:12:23.819 [     main] time : 11180
```
활동중인 스레드 100개와 대기중인 task가 1000개다. 1000개의 task는 Queue에 담을 수 있는 최대 양이기 때문에 1000개를 담은 것이다.
이제 여기서 task개수가 1개라도 초과되면 최대 스레드는 200이기에 Queue는 1000개만 들어갈 수 있으므로 1101번째 task는 pool과 active의 수가 1 증가하게된다.

이제 코드를 TASK가 1200일 때 실행 해보자.

```text
15:10:29.507 [     main] task1101 -> [pool=101, active = 101, queuedTasks = 1000, completedTask = 0]
15:10:35.618 [     main] time : 6299
```
1101번째에는 pool과 active의 값이 오른 것을 볼 수 있다. 작업 수행 시간을 보면 최대 스레드 수가 200개 일 때, 더 빠른 것을 볼 수 있다.
이건 당연하게도 스레드 수가 그만큼 많으니 처리할 수 있는 작업량이 많기 때문이다. 

이제 코드를 TASK가 1201일 때 실행 해보자.

```text
15:14:29.496 [     main] task1201 -> java.util.concurrent.RejectedExecutionException: Task thread.executor.RunnableTask@368239c8
rejected from java.util.concurrent.ThreadPoolExecutor@12edcd21
[Running, pool size = 200, active threads = 200, queued tasks = 1000, completed tasks = 0]
```
예외 메시지가 나온다. 당연하게도 최대 스레드는 200이라 1200번째까지는 문제가 없는데 1201번째는 최대 스레드도 다 활용을 했고
큐에 작업을 담을 수가 없기 때문에 이제 거부를 하게 되는 것이다. 

## 8. Executor 예외 정책
생산자 소비자 문제를 실무에서 사용할 때는, 결국 소비자가 처리할 수 없을 정도로 생산 요청이 가득 차면 어떻게 할지를
정해야 한다. 개발자가 인지할 수 있게 로그도 남겨야 하고, 사용자에게 현재 시스템에 문제가 있다고 알리는 것도 필요하다.
이런 것을 위해 예외 정책이 필요하다.

ThreadPoolExecutor에 작업을 요청할 때, 큐도 가득차고, 초과 스레드도 더는 할당할 수 없다면 작업을 거절한다.

ThreadPoolExecutor는 작업을 거절하는 다양한 정책을 제공한다.
- AbortPolicy : 새로운 작업을 제출할 때 RejectedExecutionException을 발생시킨다. 기본 정책이다.
- DiscardPolicy : 새로운 작업을 조용히 버린다.
- CallerRunsPolicy : 새로운 작업을 제출한 스레드가 대신해서 직접 작업을 실행한다.
- 사용자 정의(RejectedExecutionHandler) : 개발자가 직접 정의한 거절 정책을 사용할 수 있다.

**AbortPolicy**
작업이 거절되면 RejectedExecutionException을 던진다. 기본적으로 설정되어 있는 정책이다.
```java
package thread.executor.reject;

import thread.executor.RunnableTask;

import java.util.concurrent.*;

import static util.MyLogger.log;

public class RejectMainV1 {

  public static void main(String[] args) {
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new ThreadPoolExecutor.AbortPolicy());
    executor.submit(new RunnableTask("task1"));
    
    try {
      executor.submit(new RunnableTask("task2"));
    } catch (RejectedExecutionException e) {
      log("요청 초과");
      // 포기, 다시 시도 등 다양한 고민을 하면 된다.
      log(e);
    }
    
    executor.close();
  }
  
}
```

결과를 보자. 

```text
17:21:07.073 [pool-1-thread-1] task1 시작
17:21:07.073 [     main] 요청 초과
17:21:07.077 [     main] java.util.concurrent.RejectedExecutionException: 
Task java.util.concurrent.FutureTask@b1bc7ed[Not completed, task = java.util.concurrent.Executors$RunnableAdapter@1ddc4ec2[Wrapped task = thread.executor.RunnableTask@133314b]] 
rejected from java.util.concurrent.ThreadPoolExecutor@5b6f7412[Running, pool size = 1, active threads = 1, queued tasks = 0, completed tasks = 0]
17:21:08.092 [pool-1-thread-1] task1 완료
```
- task1은 풀의 스레드가 수행한다.
- task2를 요청하면 허용 작업을 초과한다. 따라서 RejectedExecutionException이 발생한다.

RejectedExecutionException 예외를 잡아서 작업을 포기하거나, 사용자에게 알리거나, 다시 시도하면 된다.
이렇게 예외를 잡아서 필요한 코드를 직접 구현해도 되고, 아니면 다른 정책들을 사용해도 된다.

**RejectedExecutionHandler**
마지막에 전달한 AbortPolicy는 RejectedExecutionHandler의 구현체이다.
ThreadPoolExecutor 생성자는 RejectedExecutionHandler의 구현제를 전달 받는다.

```java
public interface RejectedExecutionHandler {
  void rejectedExecution(Runnable r, ThreadPoolExecutor executor); 
}
```

```java
public static class AbortPolicy implements RejectedExecutionHandler {
  
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    throw new RejectedExecutionException("Task " + r.toString() + 
                                            " rejected from " + e.toString());     
  }
  
}
```
ThreadPoolExecutor는 거절해야 하는 상황이 발생하면 여기에 있는 rejectedExecution()을 호출한다.
AbortPolicy는 RejectedExecutionException을 던지는 것을 확인할 수 있다.

**DiscardPolicy**
거절된 작업을 무시하고 아무런 예외도 발생시키지 않는다.

```java
package thread.executor.reject;

import thread.executor.RunnableTask;

import java.util.concurrent.*;

public class RejectMainV2 {

  public static void main(String[] args) {
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
    
    executor.submit(new RunnableTask("task1"));
    executor.submit(new RunnableTask("task2"));
    executor.submit(new RunnableTask("task3"));
    executor.close();
    
    
  }
  
}
```

ThreadPoolExecutor 생성자 마지막에 new ThreadPoolExecutor.DiscardPolicy()를 제공하면 된다.

```text
17:37:00.674 [pool-1-thread-1] task1 시작
17:37:01.689 [pool-1-thread-1] task1 완료
```
- task2, task3은 거절된다. DisCardPolicy는 조용히 버리는 정책이다.
- 다음 구현 코드를 보면 왜 조용히 버리는 정책인지 이해가 될 것이다.

```java
public static class DiscardPolicy implements RejectedExecutionHandler {
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    // empty
  }
}
```

**CallerRunsPolicy**
호출한 스레드가 직접 작업을 수행하게 된다. 이로 인해 새로운 작업을 제출하는 스레드의 속도가 느려질 수 있다.

```java
package thread.executor.reject;

import thread.executor.RunnableTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RejectMainV3 {

  public static void main(String[] args) {
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0,
        TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
    executor.submit(new RunnableTask("task1"));
    executor.submit(new RunnableTask("task2"));
    executor.submit(new RunnableTask("task3"));
    executor.submit(new RunnableTask("task4"));
    
    executor.close(); 
  }
  
  
}
```

코드를 실행한 결과는 다음과 같다.

```text
17:44:34.276 [     main] task2 시작
17:44:34.276 [pool-1-thread-1] task1 시작
17:44:35.290 [     main] task2 완료
17:44:35.290 [pool-1-thread-1] task1 완료
17:44:35.291 [     main] task3 시작
17:44:36.305 [     main] task3 완료
17:44:36.306 [pool-1-thread-1] task4 시작
17:44:37.322 [pool-1-thread-1] task4 완료
```
- task1은 스레드 풀에 스레드가 있어서 수행한다.
- task2는 스레드 풀에 보관할 큐도 없고, 작업할 스레드가 없다. 거절해야 한다.
- 이때 작업을 거절하는 대신에, 작업을 요청한 스레드에 대신 일을 시킨다.
- task2의 작업을 main 스레드가 수행하는 것을 확인할 수 있다.

이 정책의 특징은 생산자 스레드가 소비자 대신 일을 수행하는 것도 있지만, 생산자 스레드가 대신 일을 수행하는 덕분에
작업의 생산 자체가 느려진다는 점이다. 덕분에 작업의 생산 속도가 너무 빠르다면, 생산 속도를 조절할 수 있다.
원래대로 하면 main 스레드가 task1, task2, task3, task4를 연속해서 바로 생산해야 한다.
CallerRunsPolicy 정책 덕분에 main 스레드는 task2를 본인이 직접 완료하고 나서야 task3을 수행할 수 있다.
결과적으로 생산 속도가 조절되었다.

```java
import java.util.concurrent.RejectedExecutionHandler;

public static class CallerRunsPolicy implements RejectedExecutionHandler {
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    if(!e.isShutdown()) {
      r.run();
    }
  }
}
```

- r.run() 코드를 보면 별도의 스레드에서 수행하는 것이 아니라 main 스레드가 직접 수행하는 것을 알 수 있다.
- 참고로 ThreadPoolExecutor를 shutdown()을 하면 이후에 요청하는 작업을 거절하는데, 이때도 같은 정책이 적용된다.
- 그런데 CallerRunsPolicy 정책은 shutdown() 이후에도 작업을 수행해버린다. 따라서 shutdown() 조건을 체크해서 이 경우에는 작업을 수행하지 않도록 한다.

**사용자 정의**
- 사용자 정의(RejectedExecutionHandler) : 사용자는 RejectedExecutionHandler 인터페이스를 구현하여 자신만의 거절 처리 전략을 정의할 수 있다.
이를 통해 특정 요구사항에 맞는 작업 거절 방식을 설정할 수 있다.

```java
package thread.executor.reject;

import thread.executor.RunnableTask;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static util.MyLogger.log;

public class RejectMainV4 {

  public static void main(String[] args) {
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS
      , new SynchronousQueue<>(), new MyRejectedExecutionHandler());
    
    executor.submit(new RunnableTask("task1"));
    executor.submit(new RunnableTask("task2"));
    executor.submit(new RunnableTask("task3"));
    executor.close();
  }
  
  static class MyRejectedExecutionHandler implements RejectedExecutionHandler {
    
    static AtomicInteger count = new AtomicInteger(0);
    
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      int i = count.incrementAndGet();
      log("[경고] 거절된 누적 작업 수 : " + i); 
    }
    
  }
  
}

```

```text
18:20:34.127 [     main] [경고] 거절된 누적 작업 수 : 1
18:20:34.127 [pool-1-thread-1] task1 시작
18:20:34.131 [     main] [경고] 거절된 누적 작업 수 : 2
18:20:35.142 [pool-1-thread-1] task1 완료
```



