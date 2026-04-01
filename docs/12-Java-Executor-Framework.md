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



