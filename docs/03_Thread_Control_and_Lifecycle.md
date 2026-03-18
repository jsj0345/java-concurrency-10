# 스레드 제어와 생명 주기

## 1. 스레드 기본 정보

Thread 클래스는 스레드를 생성하고 관리하는 기능을 제공한다..

Thread 클래스가 제공하는 정보들을 확인해보자.

```java
package thread.control;

import thread.start.HelloRunnable;

import static util.MyLogger.log;

public class ThreadInfoMain {

  public static void main(String[] args) {
    // main 스레드
    Thread mainThread = Thread.currentThread();
    log("mainThread = " + mainThread);
    log("mainThread.threadId() = " + mainThread.threadId());
    log("mainThread.getName() = " + mainThread.getName());
    log("mainThread.getPriority() = " + mainThread.getPriority());
    log("mainThread.getThreadGroup() = " + mainThread.getThreadGroup());
    log("mainThread.getState() = " + mainThread.getState());
    
    Thread myThread = new Thread(new HelloRunnable(), "myThread");
    log("myThread = " + myThread);
    log("myThread.threadId() = " + myThread.threadId());
    log("myThread.getName() = " + myThread.getName());
    log("myThread.getPriority() = " + myThread.getPriority());
    log("myThread.getThreadGroup() = " + myThread.getThreadGroup());
    log("myThread.getState() = " + myThread.getState());
    
  }
}
```

위 코드를 실행하면 결과는 다음과 같다.

```text
13:50:13.150 [     main] mainThread = Thread[#1,main,5,main]
13:50:13.161 [     main] mainThread.threadId()=1
13:50:13.162 [     main] mainThread.getName()=main
13:50:13.167 [     main] mainThread.getPriority()=5
13:50:13.167 [     main] mainThread.getThreadGroup()=java.lang.ThreadGroup[name=main,maxpri=10]
13:50:13.168 [     main] mainThread.getState()=RUNNABLE
13:50:13.171 [     main] myThread = Thread[#22,myThread,5,main]
13:50:13.171 [     main] myThread.threadId()=22
13:50:13.172 [     main] myThread.getName()=myThread
13:50:13.173 [     main] myThread.getPriority()=5
13:50:13.173 [     main] myThread.getThreadGroup()=java.lang.ThreadGroup[name=main,maxpri=10]
13:50:13.174 [     main] myThread.getState()=NEW
```

결과를 보면 log("myThread = " + myThread); 에서

Thread[#22,myThread,5,main]을 볼 수 있는데

이것은 왼쪽에서 오른쪽 순으로 스레드 ID, 스레드 이름, 우선순위, 스레드 그룹을 포함하는 문자열이다.

---------------------------------------------------------

다음으로, log("myThread.threadId() = " + myThread.threadId());

이것은 스레드의 고유 식별자를 반환하는 메서드다. 이 ID는 JVM 내에서 각 스레드에 대해 유일하다.

---------------------------------------------------------

log("myThread.getName() = " + myThread.getName());

위에서 getName()은 스레드의 이름을 반환하는 메서드다. 스레드 ID는 중복되지 않지만,

스레드 이름은 중복될 수 있다.

---------------------------------------------------------

log("myThread.getPriority() = " + myThread.getPriority()); 는 

스레드의 우선순위를 반환하는 메서드다. 우선순위는 스레드 스케줄러가 어떤 스레드를 우선 실행할지 결정하는 데 사용된다.

하지만 실제 실행 순서는 운영체제에 따라 달라질 수 있다. 

---------------------------------------------------------

log("myThread.getThreadGroup() = " + myThread.getThreadGroup()); 는

getThreadGroup()에서 스레드가 속한 스레드 그룹을 반환하는 메서드다. 

스레드 그룹은 스레드를 그룹화하여 관리할 수 있는 기능을 제공한다.

기본적으로 모든 스레드는 부모 스레드와 동일한 스레드 그룹에 속하게 된다.

부모 스레드 -> 새로운 스레드를 생성하는 스레드를 의미한다. 스레드는 기본적으로 다른 스레드에 의해 생성된다.

이러한 생성 관계에서 새로 새성된 스레드는 생성한 스레드를 부모로 간주한다.  

---------------------------------------------------------

log("myThread.getState() = " + myThread.getState());

getState() : 스레드의 현재 상태를 반환하는 메서드이다. 반환되는 값은 Thread.State 열거형에 정의된 상수 중 하나이다.

주요 상태는 다음과 같다.

- NEW : 스레드가 아직 시작되지 않은 상태이다.

- RUNNABLE : 스레드가 실행 중이거나 실행될 준비가 된 상태이다.

- BLOCKED : 스레드가 동기화 락을 기다리는 상태이다.

- WAITING : 스레드가 다른 스레드의 특정 작업이 완료되기를 기다리는 상태이다. 

- TIMED_WAITING : 일정 시간 동안 기다리는 상태이다.

- TERMINATED : 스레드가 실행을 마친 상태이다. 

## 2. 스레드의 생명 주기

### 스레드의 상태

**1. New**

- 스레드가 생성되고 아직 시작되지 않은 상태이다.

- 이 상태에서는 Thread 객체가 생성되지만, start() 메서드가 호출되지 않은 상태이다.

- ex) Thread thread = new Thread(runnable);

**2. Runnable**

- 스레드가 실행될 준비가 된 상태이다. 이 상태에서 스레드는 실제로 CPU에서 실행될 수 있다.

- start() 메서드가 호출되면 스레드는 이 상태로 들어간다.

- 운영체제 스케줄러의 실행 대기열에 있든, CPU에서 실제 실행되고 있든 모두 RUNNABLE 상태다.

- 보통 실행 상태라고 부른다. 

**3. Blocked**

- 스레드가 다른 스레드에 의해 동기화 락을 얻기 위해 기다리는 상태다.

- 예를 들어, synchronized 블록에 진입하기 위해 락을 얻어야 하는 경우 이 상태에 들어간다.

- 동기화 락이란? 여러 스레드가 동시에 같은 자원(데이터, 파일, 객체 등)에 접근 할 때 데이터가 꼬이지 않도록 제어하는 권한이다.

- 동기화 락이 왜 필요할까? 예를 들어서, 잔액 10만원이 있는 계좌에서 스레드 A가 5만원을 뽑으려고 잔액을 확인하는데

동시에 스레드 B가 7만원을 뽑으려고 잔액을 확인한다면 둘 다 10만원이 있으니까 인출해야 겠다! 라는 생각을 할것이고 동시에 빼면 잔액은 마이너스가 된다.

이러한 이유로 동기화 락이 필요하다. 

**4. Waiting**

- 스레드가 다른 스레드의 특정 작업이 완료되기를 무기한 기다리는 상태다.

- wait(), join() 메서드가 호출될 때 이 상태가 된다.

**5. Timed Waiting**

- 스레드가 특정 시간 동안 다른 스레드의 작업이 완료되기를 기다리는 상태다.

- sleep(long millis), wait(long timeout), join(long millis) 메서드가 호출될 때 이상태가 된다.

- 주어진 시간이 경과하거나 다른 스레드가 해당 스레드를 깨우면 이 상태에서 벗어난다.

**6. Terminated**

- 스레드의 실행이 완료된 상태이다.

- 스레드가 정상적으로 종료되거나, 예외가 발생하여 종료된 경우 이 상태로 들어간다.

## 3. 스레드의 생명 주기 (2)

```java
package thread.control;

import static util.MyLogger.log;

public class ThreadStateMain {

  public static void main(String[] args) throws InterruptedException {
    
    Thread thread = new Thread(new MyRunnable(), "myThread");
    log("myThread.state1 = " + thread.getState()); // NEW
    log("myThread.start()");
    thread.start();
    Thread.sleep(1000);
    log("myThread.state3 = " + thread.getState());
    Thread.sleep(4000);
    log("myThread.state5 = " + thread.getState());
    log("end"); 
    
  }
  
  static class MyRunnable implements Runnable {
    
    @Override
    public void run() {
      try {
        log("start");
        log("myThread.state2 = " + Thread.currentThread().getState()); // RUNNABLE
        log("sleep() start");
        Thread.sleep(3000);
        log("sleep() end");
        log("myThread.state4 = " + Thread.currentThread().getState()); // RUNNABLE
        log("end");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    
  }
  
}
```

코드를 실행한 결과는 아래와 같다. 

```text
16:16:29.378 [     main] myThread.state1 = NEW
16:16:29.381 [     main] myThread.start()
16:16:29.382 [ myThread] start
16:16:29.382 [ myThread] myThread.state2 = RUNNABLE
16:16:29.382 [ myThread] sleep() start
16:16:30.388 [     main] myThread.state3 = TIMED_WAITING
16:16:32.391 [ myThread] sleep() end
16:16:32.392 [ myThread] myThread.state4 = RUNNABLE
16:16:32.392 [ myThread] end
16:16:34.397 [     main] myThread.state5 = TERMINATED
16:16:34.397 [     main] end
```

먼저 코드를 보면 처음에 myThread를 생성하고나서 start 메소드를 호출하기 전까지는 

NEW 상태다. start()를 해야 스케줄링에서 스레드를 어떤 순서로 실행해야 최적화가 되는지를 계산하고 대기 상태로 냅둘수도 있고

실행 할 수도 있다. 아무튼 이런 상태일때가 RUNNABLE인데 아직은 start()를 하기 전이므로 NEW 상태다. 

이제 메인 메서드에서 myThread한테 실행하라고 명령을 준 이후, 1초동안 잠시 멈춘다.

이런 상황에서 myThread는 자신이 할 일을 하면서 myThread.state2 = RUNNABLE 상태를 출력한다.

그리고나서 3초동안 잠시 멈춘다. 

이러는동안 main 스레드가 myThread의 상태를 보고 3초동안 멈춰 있는 상태니까 TIMED_WAITING 상태를 출력으로 내보낸다.

그리고 다시 4초동안 멈추고 3초동안 멈춘 myThread가 다시 RUNNABLE 상태 메시지를 띄우고 자신의 작업을 끝낸다.

이후에 작업을 끝낸 상태에서 메인 스레드가 myThread의 상태를 출력할때는 TERMINATED라는 메시지를 띄운다.

## 4. 체크 예외 재정의 

자바에서 메서드를 오버라이드 할 때, 재정의 메서드가 지켜야할 예외와 관련된 규칙이 있다.

**체크 예외**

- 부모 메서드가 체크 예외를 던지지 않는 경우, 재정의된 자식 메서드도 체크 예외를 던질 수 없다.

- 자식 메서드는 부모 메서드가 선언한 체크 예외와 같거나 그 하위 타입만 던질 수 있다. 

Runnable 인터페이스의 run() 메서드를 살펴보자.

```java
public interface Runnable {
  void run(); 
}
```

run() 메서드를 보면 아무런 체크 예외를 던지지 않는다. 따라서 Runnable 인터페이스의

run() 메서드를 재정의 하는 곳에서는 체크 예외를 밖으로 던질 수 없다. 

```java
package thread.control;

public class CheckedExceptionMain {

  public static void main(String[] args) throws Exception {
    throw new Exception();
  }
  
  static class CheckedRunnable implements Runnable {
    
    @Override
    public void run() /*throws Exception*/ {
        //throw new Exception();  
    }
    
  }
  
}

```

main()은 체크 예외를 밖으로 던질 수 있지만 run()은 체크 예외를 밖으로 던질 수 없다. 

이런 제약을 두는 이유는 뭘까?

예를 들어서, 다음 코드를 보자.

```java
class Parent {
  
  void method() throws InterruptedException {
     // ...
  }
  
}

class Child extends Parent {
  
  @Override
  void method() throws Exception {
    
  }
  
}

public class Test {

  public static void main(String[] args) {
    Parent p = new Child();
    
    try {
      p.method();
    } catch (InterruptedException e) {
      // 예외처리 
    }
  }
  
}
```

초창기에 배웠던 개념들을 생각해보자. 

Parent p = new Child(); 에서 

p는 결국 Parent형이라 Parent에 있는 메소드에만 접근이 가능하다.

그런데 만약에 Parent, Child에 위와 같이 똑같은 메소드 이름이 있다면

오버라이딩한 메소드가 우선권이 있다.

위 코드를 보면 void method() throws Exception 이 메소드가 우선권이 있는데

InterruptedException은 체크예외고 Exception은 예외중에서 최상위 부모다.

부모 메서드가 InterruptedException을 선언했다면, 자식 메서드는 그보다 더 구체적인 하위 체크 예외는 선언할 수 있지만 더 상위 예외인 Exception은 선언할 수 없다.

따라서 자식 클래스가 오버라이딩한 메서드는 부모 메서드가 선언한 체크 예외보다 더 넓은 예외를 던질 수 없다.

왜냐하면 부모 타입으로 해당 메서드를 사용하는 코드는 부모 메서드의 선언만 보고 예외 처리를 작성하기 때문이다. 

## 5. join

일단 간단한 코드를 한번 보자.

```java
package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV0 {

  public static void main(String[] args) {
    log("Start");
    Thread thread1 = new Thread(new Job(), "thread-1");
    Thread thread2 = new Thread(new Job(), "thread-2");

    thread1.start();
    thread2.start();
    log("End");
  }  
    
  static class Job implements Runnable {
    
    @Override
    public void run() {
      log("작업 시작");
      sleep(2000);
      log("작업 완료");
    }
    
  }
  
}
```

결과는 아래와 같다.

```text
18:56:09.932 [     main] start
18:56:09.937 [     main] End
18:56:09.937 [ thread-1] 작업 시작
18:56:09.937 [ thread-2] 작업 시작
18:56:11.944 [ thread-2] 작업 완료
18:56:11.944 [ thread-1] 작업 완료
```

앞서 언급했듯이 스레드는 실행 순서를 보장 하지 않는다. (스케줄링)

main 스레드는 thread-1, thread-2를 실행하고 바로 자신의 다음 코드를 실행한다.

여기서 핵심은 main 스레드가 thread-1, thread-2가 끝날때까지 기다리지 않는다는 점이다.

main 스레드는 단지 start()를 호출해서 다른 스레드를 실행만 하고 바로 자신의 다음 코드를 실행한다.

그런데 만약에 thread-1, thread-2가 종료된 다음에 main 스레드를 가장 마지막에 종료하려면 어떻게 해야할까?

### join - 필요한 상황

예를 들어서, 1~100까지 더하는 간단한 코드를 작성해보자.

```java
public static void main(String[] args) {

  int sum = 0;
  for(int i = 1; i<=100; i++) {
    sum+=i;
  }
  
}
```
이 코드에선 main 스레드 하나만 사용하기 때문에 CPU 코어도 하나만 사용 할 수 있다.

CPU 코어를 더 효율적으로 사용하려면 여러 스레드로 나누어 계산하면 된다.

1 ~ 100 까지 더한 결과는 5050인데 다음과 같이 두 연산으로 나눌 수 있다.

1~50 까지 더한 결과 = 1275

51~100까지 더한 결과 = 3775

두 결과를 합하면 5050이 나온다.

main 스레드가 1 ~ 100으로 더하는 작업을 thread-1, thread-2에 각각 작업을 나누어 지시하면 CPU코어를 더 

효율적으로 활용할 수 있다. CPU 코어가 2개라면 이론적으로 연산 속도가 2배 빨라진다.

코드를 작성해보자.

```java
package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV1 {

  public static void main(String[] args) {
    log("Start");
    SumTask task1 = new SumTask(1,50);
    SumTask task2 = new SumTask(51,100);
    Thread thread1 = new Thread(task1, "thread-1");
    Thread thread2 = new Thread(task2, "thread-2");
    
    thread1.start();
    thread2.start();
    
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
      sleep(2000);
      int sum = 0;
      for (int i = startValue; i <= endValue; i++) {
        sum+=i;
      }
      result = sum;
      log("작업 완료 result = " + result); 
    }
  }
  
}
```

결과는 아래와 같다.

```text
19:22:06.486 [     main] start
19:22:06.490 [ thread-1] 작업 시작
19:22:06.490 [ thread-2] 작업 시작
19:22:06.497 [     main] task1.result = 0
19:22:06.497 [     main] task2.result = 0
19:22:06.498 [     main] task1 + task2 = 0
19:22:06.498 [     main] End
19:22:08.500 [ thread-1] 작업 완료 result = 1275
19:22:08.500 [ thread-2] 작업 완료 result = 3775
```

여기서 main 스레드에선 왜 task1의 결과와 task2의 결과가 0일까?

앞에서 배웠던 내용을 생각해보자.

메인 스레드는 thread-1, thread-2에게 일을 주고나서 본인의 일을 계속 한다.

근데 thread-1, thread-2 같은 경우에는 2초동안 잠깐 멈추고나서 일을 실행한다. 

그런데 그 2초동안 메인 스레드는 본인의 일을 다 끝내서 결과값을 받아올 수 없는 것이다. 

### 참고

어떤 메소드를 호출한다는 것은, 특정 스레드가 어떤 메서드를 호출하는 것이다.

스레드는 메서드의 호출을 관리하기 위해 메서드 단위로 스택 프레임을 만들고 

해당 스택 프레임을 스택 위에 쌓아 올린다.

이때 인스턴스의 메서드를 호출하면, 어떤 인스턴스의 메서드를 호출했는지 기억하기 위해, 해당 인스턴스의

참조값을 스택 프레임 내부에 저장해둔다. 이것이 바로 우리가 자주 사용하던 this이다.

### join - sleep 사용

특정 스레드를 기다리게 하는 가장 간단한 방법은 sleep()을 사용하는 것이다.

```java
package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV2 {

  public static void main(String[] args) {
    log("Start");
    SumTask task1 = new SumTask(1,50);
    SumTask task2 = new SumTask2(51,100);
    Thread thread1 = new Thread(task1, "thread-1");
    Thread thread2 = new Thread(task2, "thread-2");
    
    thread1.start();
    thread2.start();
    
    // 정확한 타이밍을 마추어 기다리기 어려움
    log("main 스레드 sleep()");
    sleep(3000);
    log("main 스레드 깨어남");
    
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
      sleep(2000);
      int sum = 0;
      for(int i = startValue; i<=endValue; i++) {
        sum+=i;
      }
      result = sum;
      log("작업 완료 result = " + result);
    }
    
  }
  
}
```

결과는 아래와 같다. 

```text
19:52:58.963 [     main] start
19:52:58.967 [     main] main 스레드 sleep()
19:52:58.967 [ thread-2] 작업 시작
19:52:58.967 [ thread-1] 작업 시작
19:53:00.983 [ thread-1] 작업 완료 result = 1275
19:53:00.983 [ thread-2] 작업 완료 result = 3775
19:53:01.977 [     main] main 스레드 깨어남
19:53:01.977 [     main] task1.result = 1275
19:53:01.978 [     main] task2.result = 3775
19:53:01.978 [     main] task1 + task2 = 5050
19:53:01.979 [     main] End
```

이번에 결과를 보면 task1의 결과값과 task2의 결과값을 합친 결과를 잘 볼수 있었다.

이러한 이유는 main 스레드가 3초동안 멈추고 있는 동안 thread-1, thread-2는 2초동안 멈추고 남은 1초동안 작업을 다 했기 때문이다.

그러면 깨어난 main 스레드는 task1의 result값, task2의 result값을 다 받게 되는 것이다. 

하지만 이렇게 sleep()을 사용하면 task1, task2가 정확하게 언제 일이 끝나는지도 모르는데 계속 기다려야한다.

딱 스레드들의 일이 끝나면 바로 메인 스레드가 일을 수행하려면 어떻게 해야 할까?

이럴때 join() 메소드를 활용해보자. 

### join - join 사용

```java
package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV3 {

  public static void main(String[] args) {
    log("Start");
    SumTask task1 = new SumTask(1,50);
    SumTask task2 = new SumTask(51,100);
    Thread thread1 = new Thread(task1, "thread-1");
    Thread thread2 = new Thread(task2, "thread-2");
    
    thread1.start();
    thread2.start();
    
    // 스레드가 종료될 때 까지 대기
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

      sleep(2000);

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

결과는 다음과 같다.

```text
20:03:50.688 [     main] start
20:03:50.692 [     main] join() - main 스레드가 thread1, thread2 종료까지 대기
20:03:50.693 [ thread-1] 작업 시작
20:03:50.693 [ thread-2] 작업 시작
20:03:52.707 [ thread-1] 작업 완료 result = 1275
20:03:52.707 [ thread-2] 작업 완료 result = 3775
20:03:52.707 [     main] main 스레드 대기 완료
20:03:52.708 [     main] task1.result = 1275
20:03:52.709 [     main] task2.result = 3775
20:03:52.709 [     main] task1 + task2 = 5050
20:03:52.710 [     main] End
```

결과가 이렇게 나오는 이유는 메인 스레드는 thread-1, thread-2에게 이제 일을 수행하라고 얘기를 한 후,

자신의 일을 계속 수행하는데 thread-1, thread-2의 결과물을 받기 위해서 join() 메소드를 활용한다.

이러면 main 스레드는 thread-1, thread-2의 작업이 끝날때까지 대기한다.

thread-1의 일은 끝나고 thread2.join(); 에서 thread2의 일이 끝나지 않았으면 끝날때까지 대기한다.

이후에 thread-1,2가 일을 해서 얻어낸 결과를 받아낸다. 

**Waiting**

- 스레드가 다른 스레드의 특정 작업이 완료되기를 무기한 기다리는 상태다.

- join()을 호출하는 스레드는 대상 스레드가 TERMINATED 상태가 될 때 까지 대기한다.

대상 스레드가 TERMINATED 상태가 되면 호출 스레드는 다시 RUNNABLE 상태가 되면서 다음 코드를 수행한다.

하지만 위 코드에는 단점이 있다. 스레드의 일이 완료될때까지 계속 무기한 기다려야한다.

일정 시간만 기다리는 방법은 없을까? 

코드를 보자.

```java
package thread.control.join;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class JoinMainV4 {

  public static void main(String[] args) throws InterruptedException {
    log("start");
    SumTask task1 = new SumTask(1, 50);

    Thread thread1 = new Thread(task1, "thread-1");

    thread1.start();

    // 스레드가 종료될 때 까지 대기
    log("join(1000) - main 스레드가 thread1 종료까지 1초 대기");
    thread1.join(1000);

    log("main 스레드 대기 완료");

    log("task1.result = " + task1.result);

    log("End");
  }

  static class SumTask implements Runnable {

    int startValue;
    int endValue;
    int result;

    public SumTask(int startValue, int endValue) {
      this.startValue = startValue;
      this.endValue = endValue;
    }

    @Override
    public void run() {

      log("작업 시작");
      sleep(2000);

      int sum = 0;
      for (int i = startValue; i <=endValue; i++) {
        sum += i;
      }

      result = sum;
      log("작업 완료 result = " + result);

    }

  }

}
```

결과를 보자.

```text
20:19:34.059 [     main] start
20:19:34.083 [     main] join(1000) - main 스레드가 thread1 종료까지 1초 대기
20:19:34.084 [ thread-1] 작업 시작
20:19:35.085 [     main] main 스레드 대기 완료
20:19:35.093 [     main] task1.result = 0
20:19:35.094 [     main] End
20:19:36.093 [ thread-1] 작업 완료 result = 1275
```

앞에서는 join() 메소드를 이용해서 무기한 기다렸지만 매개변수가 있는 join 메소드를 이용하면 

일정 시간만 기다리고 작업을 이어서 하는 것을 볼 수 있다. 

**정리**

다른 스레드가 끝날 때 까지 무작정 기다려야 한다면 join()을 사용하고,

다른 스레드의 작업을 무한정 기다릴 수 없다면 join(ms)를 사용하면 된다.

물론, 기다리다가 중간에 나오는 상황이라 원하는 결과가 안나올 수 있다. 















