# 스레드 제어와 생명 주기2

## 1. 인터럽트 -1 

특정 스레드의 작업을 중간에 중단하려면 어떻게 해야하는가?

예시 코드를 보자.

```java
package thread.control.interrupt;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ThreadStopMainV1 {

  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread thread = new Thread(task, "work");
    thread.start();
    
    sleep(4000);
    log("작업 중단 지시 runFlag=false");
    task.runFlag = false;
  }
  
  static class MyTask implements Runnable {
    volatile boolean runFlag = true;
    
    @Override
    public void run() {
      while(runFlag) {
        log("작업 중");
        sleep(3000);
      }
      log("자원 정리");
      log("작업 종료"); 
    }
  }

}
```

결과는 아래와 같다.

```text
10:25:00.719 [     work] 작업 중
10:25:03.727 [     work] 작업 중
10:25:04.692 [     main] 작업 중단 지시 runFlag=false
10:25:06.729 [     work] 자원 정리
10:25:06.730 [     work] 자원 종료
```

코드랑 결과를 보면 당연한 결과이다.

먼저 main 스레드가 thread.start(); 로 work 스레드에게 일을 지시한다.

이후, main 스레드는 4초동안 잠시 멈추고 work 스레드는 runFlag 값이 true여서

작업 중이라는 문구를 띄우고 3초동안 멈춘다음에 다시 while문을 반복한다.

이때 main 스레드에서 runFlag 값을 false로 바꾼다. 

하지만 work 스레드는 아직 멈춘 상태이기 때문에 sleep(3000); 에서 머무른 후에

while문을 반복 하려다가 조건에 맞지 않아서 자원 정리 및 종료를 실행한다. 

이 코드에는 문제점이 있다.

작업 중단 지시를 했음에도 불구하고 work 스레드가 즉각 반응하지 않는다.

어떻게 해야 바로 반응할까? 

## 2. 인터럽트 - 2

예를 들어서, 특정 스레드가 Thread.sleep()을 통해 쉬고 있는데, 처리해야 하는 작업이 들어와서

해당 스레드를 급하게 깨워야 할 수 있다. 이런 상황에서 인터럽트를 사용해보자.

인터럽트는 WAITING, TIMED_WAITING 같은 대기 상태의 스레드를 직접 깨워서, 작동하는 

RUNNABLE 상태로 만들 수 있다.

```java
package thread.control.interrupt;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ThreadStopMainV2 {

  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread thread = new Thread(task, "work");
    thread.start(); 
    
    sleep(4000);
    log("작업 중단 지시 thread.interrupt()");
    thread.interrupt();
    log("work 스레드 인터럽트 상태1 = " + thread.isInterrupted());
  }

  static class MyTask implements Runnable {
    
    @Override
    public void run() {
      try {
        while(true) {
          log("작업 중");
          Thread.sleep(3000);
        }
      } catch (InterruptedException e) {
        log("work 스레드 인터럽트 상태2 = " + Thread.currentThread().isInterrupted());
        log("interrupt message = " + e.getMessage());
        log("state = " + Thread.currentThread().getState());
      }
      
      log("자원 정리");
      log("작업 종료"); 
    }
    
  }
}
```

결과는 아래와 같다.

```text
10:43:13.263 [     work] 작업 중
10:43:16.281 [     work] 작업 중
10:43:17.244 [     main] 작업 중단 지시 thread.interrupt()
10:43:17.251 [     main] work 스레드 인터럽트 상태 1 = true
10:43:17.251 [     work] work 스레드 인터럽트 상태 2 = false
10:43:17.252 [     work] interrupt message = sleep interrupted
10:43:17.253 [     work] state = RUNNABLE
10:43:17.253 [     work] 자원 정리
10:43:17.253 [     work] 자원 종료
```

결과를 살펴보자.

interrupt() 메소드를 호출해서 자고 있는 시간(sleep 시간)이 다 지나지 않았음에도 불구하고

강제로 깨워서 일을 종료 시키는 것을 볼 수 있다. 

인터럽트가 발생하면 해당 스레드에 InterruptedException이 발생한다.

이때 인터럽트를 받은 스레드는 대기 상태에서 깨어나 RUNNABLE 상태가 되고, 코드를 정상 수행한다.

InterruptedException을 catch로 잡아서 정상 흐름으로 변경하면 된다.

참고로, interrupt()를 호출했다고 해서 즉각 InterruptedException이 발생하는 것은 아니다.

sleep()처럼 InterruptedException을 던지는 메서드를 호출 하거나 또는 호출 중일 때 예외가 발생한다.

결과를 좀 더 자세히 보면, 인터럽트 예외가 발생하고나서 예외 처리가 되면 work 스레드는 다시 작동하는 상태가 된다.

즉, 인터럽트가 적용되고, 인터럽트 예외가 발생하면, 해당 스레드는 실행 가능 상태가 되고, 인터럽트 발생 상태도 정상으로 돌아온다. 

이 코드의 좋은 점은 대기중인 스레드를 바로 깨워서 실행 가능한 상태로 바꿀 수 있다는 점이다.

하지만 아쉬운 점도 존재한다. 

sleep 메소드에서만 인터럽트가 발생하고 while문에서 조건에 부합하는지를 볼 때에는 발생하지 않는다. 

## 3. 인터럽트 - 3

```java
package thread.control.interrupt;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ThreadStopMainV3 {

  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread thread = new Thread(task, "work");
    thread.start();
    
    sleep(100); 
    log("작업 중단 지시 - thread.interrupt()");
    thread.interrupt();
    log("work 스레드 인터럽트 상태1 = " + thread.isInterrupted());
  }
  
  static class MyTask implements Runnable {
    
    @Override
    public void run() {
      while(!Thread.currentThread().isInterrupted()) { // 인터럽트 상태 변경 X
        log("작업 중");
      }
      
      log("work 스레드 인터럽트 상태2 = " + Thread.currentThread().isInterrupted());
      
      try {
        log("자원 정리 시도");
        Thread.sleep(1000);
        log("자원 정리 완료");
      } catch (InterruptedException e) {
        log("자원 정리 실패 - 자원 정리 중 인터럽트 발생");
        log("work 스레드 인터럽트 상태3 = " + Thread.currentThread().isInterrupted());
      }
      
      log("작업 종료"); 
      
    }
    
  }
  
}
```

결과는 아래와 같다.

```text
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.503 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.504 [     work] 작업 중
11:03:28.505 [     work] 작업 중
11:03:28.505 [     work] 작업 중
11:03:28.505 [     work] 작업 중
11:03:28.505 [     main] 작업 중단 지시 thread.interrupt()
11:03:28.505 [     work] 작업 중
11:03:28.511 [     main] work 스레드 인터럽트 상태 1 = true
11:03:28.511 [     work] work 스레드 인터럽트 상태 2 = true
11:03:28.512 [     work] 자원 정리
11:03:28.512 [     work] 자원 정리 실패 - 자원 정리 중 인터럽트 발생
11:03:28.512 [     work] work 스레드 인터럽트 상태3 = false
11:03:28.512 [     work] 작업 종료
```

결과를 보면 뭔가 이상한 점이 있다.

앞에서 <2. 인터셉터 -2> 마지막 부분에 sleep에서만 인터럽트를 확인하는 것이 아닌 while문에 있는 조건에서 미리 확인하지 못한 점이 

아쉬웠는데 이 코드에서는 이 아쉬운 점을 해결했다. 

하지만 이후에, 인터럽트 상태는 계속 true여서 자원 정리 시도 및 정리 완료를 해야하는데 

예외가 발생해서 자원 정리를 실패했다. 

isInterrupted() 메서드는 인터럽트의 상태를 변경하지 않고 상태만 확인 하기 때문에 실패해버린 것이다.

while문에서 인터럽트 상태를 다시 정상으로 바꿔야하는데 바꾸지 못했다.

어떻게 해야할까? 

### 4. 인터럽트 - 4

인터럽트 상태를 단순히 확인만 하는 isInterrupted()를 사용하지말고 

직접 체크해서 사용 해야 하는 경우에는 Thread.interrupted()를 사용해야 한다.

이 메서드는 다음과 같이 작동한다.

- 스레드가 인터럽트 상태라면 true를 반환하고, 해당 스레드의 인터럽트 상태를 false로 변경한다.

- 스레드가 인터럽트 상태가 아니라면 false를 반환하고, 해당 스레드의 인터럽트 상태를 변경하지 않는다.

- 내부에 있는 상태값을 먼저 바꾼 다음, 반환한다고 보면 된다. 

코드를 보자. 

```java
package thread.control.interrupt;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class ThreadStopMainV4 {

  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread thread = new Thread(task, "work");
    thread.start();
    sleep(100);
    log("작업 중단 지시 - thread.interrupt()");
    thread.interrupt();
    log("work 스레드 인터럽트 상태1 = " + thread.isInterrupted());
  }
  
  static class MyTask implements Runnable {
    
    @Override
    public void run() {
      while(!Thread.interrupted()) {
        log("작업 중");
      }
      
      log("work 스레드 인터럽트 상태2 = " + Thread.currentThread().isInterrupted());
      
      try {
        log("자원 정리 시도");
        Thread.sleep(1000);
        log("자원 정리 완료");
        
      } catch (InterruptedException e) {
        log("자원 정리 실패 - 자원 정리 중 인터럽트 발생");
        log("work 스레드 인터럽트 상태3 = " + Thread.currentThread().isInterrupted());
      }
      
      log("작업 종료"); 
    }
    
  }
  
}
```

결과는 다음과 같다.

```text
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.415 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.417 [     work] 작업 중
22:06:33.418 [     work] 작업 중
22:06:33.418 [     work] 작업 중
22:06:33.418 [     work] 작업 중
22:06:33.418 [     main] 작업 중단 지시 thread.interrupt()
22:06:33.418 [     work] 작업 중
22:06:33.424 [     main] work 스레드 인터럽트 상태 1 = true
22:06:33.424 [     work] work 스레드 인터럽트 상태 2 = false
22:06:33.424 [     work] 자원 정리
22:06:34.427 [     work] 자원 종료
22:06:34.427 [     work] 작업 종료
```

ThreadStopMainV3 코드를 보면 isInterrupted() 라는 메소드를 써서 상태 확인만 했다.

즉, 인터럽트 상태값이 바뀌지 않고 true로 쭉 이어가니까 예외가 발생해서 코드가 정상적으로 진행되지 않음을 볼 수 있었다.

하지만 interrupted() 메소드는 true면 상태값을 false로 바꾸고 반환은 true로 하니까 무한 반복문도 탈출하면서

예외가 생기지 않음을 볼 수 있다. 

**정리** 

자바는 인터럽트 예외가 한 번 발생하면, 스레드의 인터럽트 상태를 다시 정상으로 돌린다. 

스레드의 인터럽트 상태를 정상으로 돌리지 않으면 이후에도 계속 인터럽트가 발생하게 된다.

인터럽트의 목적을 달성하면 인터럽트 상태를 다시 정상으로 돌려두어야 한다. 

## 5. 인터럽트를 활용한 실용적인 예제

인터럽트를 실제로 어떤 식으로 활용할 수 있는지 조금 더 실용적인 예제를 만들어보자.

```java
package thread.control.printer;

import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class MyPrinterV1 {

  public static void main(String[] args) {
    
    Printer printer = new Printer();
    Thread printerThread = new Thread(printer, "printer");
    printerThread.start();
    
    Scanner userInput = new Scanner(System.in);
    while(true) {
      log("프린터할 문서를 입력하세요. 종료 (q) : ");
      String input = userInput.nextLine(); 
      if(input.equals("q")) {
        printer.work = false; 
        break; 
      }
      
      printer.addJob(input); 
    }
    
  }
  
  static class Printer implements Runnable {
    
    volatile boolean work = true; 
    Queue<String> jobQueue = new ConcurrentLinkedQueue<>();
    
    @Override
    public void run() {
      
      while(work) {
        if(jobQueue.isEmpty()) {
          continue; 
        }
        
        String job = jobQueue.poll();
        log("출력 시작 : " + job + ", 대기 문서 : " + jobQueue);
        sleep(3000); // 출력에 걸리는 시간
        log("출력 완료 : " + job);
        
      }
      
      log("프린터 종료");
      
    }
    
    public void addJob(String input) {
      jobQueue.offer(input);
    } 
    
  }
  
}
```
- volatile : 여러 스레드가 동시에 접근하는 변수에는 volatile 키워드를 붙여주어야 안전하다. 여기서는 main 스레드, printer 스레드

둘다 work 변수에 동시에 접근 할 수 있다.

- ConcurrentLinkedQueue : 여러 스레드가 동시에 접근하는 경우, 컬렉션 프레임워크가 제공하는 일반적인

자료구조를 사용하면 안전하지 않다. 여러 스레드가 동시에 접근하는 경우, 동시성 컬렉션을 사용해야 한다.

결과는 아래와 같다. 

```text
22:36:55.310 [     main] 프린터할 문서를 입력하세요. 종료 (q) :
a
22:36:58.893 [     main] 프린터할 문서를 입력하세요. 종료 (q) :
22:36:58.918 [  printer] 출력 시작: a, 대기 문서 : []
b
22:36:59.150 [     main] 프린터할 문서를 입력하세요. 종료 (q) :
c
22:36:59.412 [     main] 프린터할 문서를 입력하세요. 종료 (q) :
d
22:36:59.693 [     main] 프린터할 문서를 입력하세요. 종료 (q) :
q22:37:01.925 [  printer] 출력 완료
22:37:01.925 [  printer] 출력 시작: b, 대기 문서 : [c, d]

22:37:04.939 [  printer] 출력 완료
22:37:04.939 [  printer] 프린터 종료
```

코드의 흐름은 다음과 같다.

먼저 printer 스레드가 일을 할당 받고 시작한다. 

그런데 jobQueue에는 아무런 내용물이 없으므로 계속 반복문만 무한 실행된다.

이때, 사용자가 입력을 하면 ConcurrentLinkedQueue에 내용물이 들어가고 printer 스레드에서 출력, 멈추는 상태 및 종료를 한다.

하지만 q를 입력 했을때 sleep(3000);으로 인해 바로 종료되지 않는다 이럴땐 어떻게 해야 할까? 인터럽트를 활용해보자. 










