# 스레드

## 1. 스레드 

**메모리 구조 복습**

### 메서드 영역 : 메서드 영역은 프로그램을 실행하는데 필요한 공통 데이터를 관리한다.

- 클래스 정보 : 클래스의 실행 코드(바이트 코드), 필드, 메서드와 생성자 코드등 모든 실행 코드가 존재한다.

- static 영역 : static 변수들을 보관한다.

- 런타임 상수 풀 : 프로그램을 실행하는데 필요한 공통 리터럴 상수를 보관한다.

### 스택 영역 : 자바 실행 시, 하나의 실행 스택이 생성된다. 각 스택 프레임은 지역 변수, 중간 연산 결과, 메서드 호출 정보등을 포함한다.

### 힙 영역 : 객체(인스턴스)와 배열이 생성되는 영역이다. 가비지 컬렉션이 이루어지는 주요 영역이며, 더 이상 참조되지 않는 객체는 GC에 의해 제거된다.

**스레드 생성 - Thread 상속**

```java
package thread.start;

public class HelloThread extends Thread {
  
  @Override
  public void run() {

    System.out.println(Thread.currentThread().getName() + " : run()");
    
  }
  
}
```
- Thread 클래스를 상속하고, 스레드가 실행할 코드를 run() 메서드에 재정의한다.

- Thread.currentThread()를 호출하면 해당 코드를 실행하는 스레드 객체를 조회할 수 있다.

- Thread.currentThread().getName() : 실행 중인 스레드의 이름을 조회한다.

```java

package thread.start;

public class HelloThreadMain {

  public static void main(String[] args) {

    System.out.println(Thread.currentThread().getName() + ": main() start");

    HelloThread helloThread = new HelloThread();

    System.out.println(Thread.currentThread().getName() + ": start() 호출 전");

    helloThread.start();

    System.out.println(Thread.currentThread().getName() + ": start() 호출 후");

    System.out.println(Thread.currentThread().getName() + ": main() end"); 
  }

}

```
- start() 메서드는 스레드를 실행하는 아주 특별한 메서드이다. 

- start()를 호출하면 HelloThread 스레드가 run() 메서드를 실행한다. 

- 주의해야 할 점 : run() 메서드가 아니라 반드시 start() 메서드를 호출해야 한다.

위 예시 코드를 실행하면 아래와 같이 결과가 나온다. 

```text
main: main() start
main: start() 호출 전
main: start() 호출 후
main: main() end
Thread-0: run()
```

여기서 알아야 할 것은 start() 메서드가 호출되면 새로 생성된 스레드한테 작업을 하라고 명령하는거고 main 스레드는

하던 일을 마저 한다. 스케줄링에 의해서 스레드를 호출했을때의 결과 순서는 호출 후에 Thread-0이 바로 코드를 실행해서 main 스레드가

작업을 마치기 전에 Thread-0: run()이 먼저 나올 수도 있다. (실행 순서는 고정되어 있지 않다.)

start() 메서드를 호출 하기 전에 상황이 어떤지 살펴보자.

실행 결과를 보면 main() 메서드는 main이라는 이름의 스레드가 실행하는 것을 확인할 수 있다.

프로세스가 작동하려면 스레드가 최소한 하나는 있어야 한다.

- 자바는 실행 시점에 main이라는 이름의 스레드를 만들고 프로그램의 시작점인 main() 메서드를 실행한다.

- HelloThread 스레드 객체를 생성한 다음에 start() 메서드를 호출하면 자바는 스레드를 위한 별도의 스택 공간을 할당한다.

- 스레드 객체를 생성하고, 반드시 start()를 호출해야 스택 공간을 할당 받고 스레드가 작동한다.

- 스레드에 이름을 주지 않으면 자바는 스레드에 Thread-0, Thread-1과 같은 임의의 이름을 부여한다. 


------------------------------------------------------------------------------------------

그런데 만약에 start() 대신에 오버라이딩한 run() 메서드를 직접 호출하면 어떻게 될까?

```java
package thread.start;

public class BadThreadMain {

  public static void main(String[] args) {
    System.out.println(Thread.currentThread().getName() + ": main() start");
    
    HelloThread helloThread = new HelloThread();
    System.out.println(Thread.currentThread().getName() + ": run() 호출 전");
    helloThread.run();

    System.out.println(Thread.currentThread().getName() + ": run() 호출 후");

    System.out.println(Thread.currentThread().getName() + ": main() end");
  }
  
}
```

```text
결과 : 
main: main() start
main: run() 호출 전
main: run()
main: run() 호출 후
main: main() end
```

- 실행 결과를 보면 다른 스레드가 run()을 실행하지 않는다. main 스레드가 run() 메서드를 호출 한 것을 확인할 수 있다.

- main 스레드는 HelloThread 인스턴스에 있는 run() 이라는 메서드를 호출한다.

- main 스레드가 run() 메서드를 실행했기 때문에 main 스레드가 사용하는 스택위에 run() 스택 프레임이 올라간다.

## 2. 데몬 스레드

- 스레드는 사용자 스레드와 데몬 스레드 2가지 종류로 구분할 수 있다.

### 사용자 스레드

- 프로그램의 주요 작업을 수행한다.

- 작업이 완료될 때까지 실행된다.

- 모든 user 스레드가 종료되면 JVM도 종료된다.

### 데몬 스레드

- 백그라운드에서 보조적인 작업을 수행한다.

- 모든 user 스레드가 종료되면 데몬 스레드는 자동으로 종료된다.

```java
package thread.start;

public class DaemonThreadMain {

  public static void main(String[] args) {
    System.out.println(Thread.currentThread().getName() + ": main() start");
    
    DaemonThread daemonThread = new DaemonThread();
    daemonThread.setDaemon(true); // 데몬 스레드 여부
    daemonThread.start();
    System.out.println(Thread.currentThread().getName() + ": main() end");
  }
  
  static class DaemonThread extends Thread {
    @Override
    public void run() {

      System.out.println(Thread.currentThread().getName() + ": run() start");
      
      try {
        Thread.sleep(10000); // 10초간 멈추고 나서 실행
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      System.out.println(Thread.currentThread().getName() + ": run() end");
      
    }
    
  }
  
}
```

```text
결과 : 

main: main() start
main: main() end
Thread-0: run() start
```

- 결과를 보고 알수 있는 것은 데몬 스레드를 보면 Thread.sleep(10000); 10초간 멈추고 나서 실행 해야하는데

실제로는 사용자 스레드인 메인 스레드가 종료되면서 자바 프로그램도 종료되기에 데몬 스레드는 10초동안 멈추고나서 실행되지 않는다. (이미 끝났기 때문)

- 만약에 setDaemon(false)로 바꾼다면? 이때는 사용자 스레드로 바껴서 10초동안 멈추고나서 실행된다. 따라서 결과는 다음과 같다.

```text
main: main() start
main: main() end
Thread-0: run() start
Thread-0: end()
```

-> main 스레드가 종료됐다고 해서 user 스레드인 Thread-0은 종료되지 않아서 자바 프로그램은 Thread-0이 종료되어야 종료된다. 

## 3. 스레드 생성 - Runnable

스레드를 생성하는 방법은 Thread 클래스를 상속 받는 방법과 Runnable 인터페이스를 구현하는 방법이 있다.

```java
package java.lang;

public interface Runnable {
  void run();
}
```

-> 자바가 제공하는 스레드 실행용 인터페이스 

```java
package thread.start;

public class HelloRunnable implements Runnable {
  
  @Override
  public void run() {
    System.out.println(Thread.currentThread().getName() + ": run()");
  }
  
}
```

```java
package thread.start;

public class HelloRunnableMain {

  public static void main(String[] args) {
    System.out.println(Thread.currentThread().getName() + ": main() start");

    HelloRunnable runnable = new HelloRunnable();
    Thread thread = new Thread(runnable);
    /*
    (참고) 
    public Thread(Runnable task) {
        this(null, null, 0, task, 0, null);
    }
    */
    thread.start();

    System.out.println(Thread.currentThread().getName() + ": main() end");
    
  }

}
```

```text
결과 :
main: main() start
main: main() end
Thread-0: run()
```

-> 실행 결과는 기존과 같다. 차이점은 앞에서 Thread를 상속 받은 객체에다가 Thread의 메소드인 start()를 호출 했지만

위 코드는 Thread 객체를 직접 생성하여 start 메소드를 실행 했다는 점이다. 

근데 주로 쓰는 방식은 Runnable을 implements 하는 방식이다.

상속은 다중 상속이 안되기 때문에 인터페이스를 구현하는 방식을 쓴다. 

### Thread 상속 vs Runnable 구현

**Thread 클래스 상속 방식**

**장점**

- 간단한 구현 : Thread 클래스를 상속받아 run() 메서드만 재정의하면 된다.

**단점**

- 상속의 제한 : 자바는 단일 상속만을 허용하므로 이미 다른 클래스를 상속 받고 있는 경우 Thread 클래스를 상속 받을 수 없다.

- 유연성 부족 : 인터페이스를 사용하는 방법에 비해 유연성이 떨어진다.

**Runnable 인터페이스를 구현 하는 방식**

**장점**

- 상속의 자유로움 : Runnable 인터페이스 방식은 다른 클래스를 상속받아도 문제없이 구현할 수 있다.

- 코드의 분리 : 스레드와 실행할 작업을 분리하여 코드의 가독성을 높일 수 있다.

- 여러 스레드가 동일한 Runnable 객체를 공유할 수 있어 자원 관리를 효율적으로 할 수 있다.

**단점**

- 코드가 약간 복잡해질 수 있다. Runnable 객체를 생성하고 이를 Thread에 전달하는 과정이 추가된다.

-------------------------------------------------------------

위 코드들을 보면, 매번 스레드 이름을 알아내기 위해 Thread.currentThread().getName()을 적는것은 다소 불편하다.

이러한 불편함을 해소하기 위해 메소드를 하나 만들자. 

```java
package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class MyLogger {
  
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  
  public static void log(Object obj) {
    String time = LocalTime.now().format(formatter);
    System.out.printf("%s [%9s] %s\n", time, Thread.currentThread().getName(), obj);
  }
  
}
```

```java
package util;

import static util.MyLogger.log; 

public class MyLoggerMain {
  public static void main(String[] args) {
    log("hello thread");
    log(123);
  }
}
```

```text
결과 : 
16:18:17.767 [     main] hello thread
16:18:17.771 [     main] 123
```

## 4. 여러 스레드 만들기 

여러 스레드를 한번에 만들어보자.

```java
package thread.start;

import static util.MyLogger.*;

public class ManyThreadMainV1 {
  public static void main(String[] args) {
    log("main() start");
    
    HelloRunnable runnable = new HelloRunnable();
    Thread thread1 = new Thread(runnable);
    thread1.start();
    
    Thread thread2 = new Thread(runnable);
    thread2.start();
    
    Thread thread3 = new Thread(runnable);
    thread3.start();
    
    log("main() end"); 
  }
  
}
```

```text
결과 : 

16:24:25.123 [     main] main() start
16:24:25.129 [     main] main() end
Thread-2: run()
Thread-0: run()
Thread-1: run()
```

-> 스레드의 실행 순서는 보장되지 않는다. 스케줄링의 최적화에 따라 다르다.
    
-> 각각 다른 스레드가 생성된것이므로 스택 영역에는 각 스레드의 개수에 맞게 자신만의 스택 메모리 영역이 생성된다.

## 5. Runnable을 만드는 다양한 방법

### 정적 중첩 클래스를 사용

```java
package thread.start;

import static util.MyLogger.log;

public class InnerRunnableMainV1 {

  public static void main(String[] args) {
    log("main() start");
    
    Runnable runnable = new MyRunnable();
    Thread thread = new Thread(runnable);
    thread.start();
    
    log("main() end");
  }
  
  static class MyRunnable implements Runnable {
    
    @Override
    public void run() {
      log("run()"); 
    }
    
  }
  
}
```

```text
결과 : 

16:35:10.383 [     main] main() start
16:35:10.397 [     main] main() end
16:35:10.397 [ Thread-0] : run()
```

### 익명 클래스를 사용

```java
package thread.start;

import static util.MyLogger.log;

public class InnerRunnableMainV2 {

  public static void main(String[] args) {
    log("main() start");
    
    Runnable runnable = new Runnable() {
      
      @Override
      public void run() {
        log("run()");
      }
      
    };
    
    Thread thread = new Thread(runnable);
    thread.start();
    
    log("main() end"); 
  }
  
}
```

```text
결과 : 

16:41:02.720 [     main] main() start
16:41:02.734 [     main] main() end
16:41:02.734 [ Thread-0] run()
```

### 익명 클래스 변수 없이 직접 전달

```java
package thread.start;

import static util.MyLogger.log;

public class InnerRunnableMainV3 {

  public static void main(String[] args) {
    log("main() start");
    
    Thread thread = new Thread(new Runnable() {
      
      @Override
      public void run() {
        log("run()");
      }
      
    });
    
    thread.start();
    
    log("main() end");
  }
  
}
```

```text
결과 : 
16:44:38.612 [     main] main() start
16:44:38.632 [     main] main() end
16:44:38.632 [ Thread-0] run()
```














