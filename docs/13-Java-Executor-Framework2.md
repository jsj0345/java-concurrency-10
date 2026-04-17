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





