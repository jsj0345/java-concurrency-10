# 메모리 가시성

## 1. volatile, 메모리 가시성1

volatile과 메모리 가시성을 이해하기 위해, 간단한 예제를 만들어보자. 

```java
package thread.volatile1;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class VolatileFlagMain {
  
  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread t = new Thread(task, "work");
    log("runFlag = " + task.runFlag);
    t.start();
    
    sleep(1000);
    log("runFlag를 false로 변경 시도");
    task.runFlag = false;
    log("runFlag = " + task.runFlag);
    log("main 종료");
  }
  
  static class MyTask implements Runnable {
    boolean runFlag = true;
    //volatile boolean runFlag = true;
    
    @Override
    public void run() {
      log("task 시작");
      while(runFlag) {
        // runFlag가 false로 변하면 탈출
      }
      log("task 종료");
    }
  }
  
}
```

먼저 결과를 보기전에, 생각을 해보자.

처음에 runFlag = true가 나올것이고

메인 스레드가 1초동안 잠깐 쉬고 있는 동안, work 스레드는 일을 계속 하고있다.

1초가 지난후, main 스레드가 다시 작업을 하면서 runFlag를 false로 초기화한다.

이러면 main 종료, task 종료가 나와야 할 것이다.

결과를 보자.

```text
21:04:42.859 [     main] runFlag = true
21:04:42.869 [     work] task 시작
21:04:43.876 [     main] runFlag를 false로 변경 시도
21:04:43.877 [     main] runFlag = false
21:04:43.878 [     main] main 종료
```

거의 똑같지만 task 종료가 나오지 않는다. 

자바 프로그램도 멈추지 않고 계속 실행된다.

어떻게 된걸까? 

## volatile, 메모리 가시성2

**메모리 가시성 문제**

먼저 일반적인 메모리 접근 방식으로 생각해보자.

main 스레드와 work 스레드는 각각의 CPU 코어에 할당되어서 실행된다.

똑같은 참조값(new MyTask())으로 runFlag = true에 접근 할 것이다. 

자바 프로그램을 실행하면 main 스레드와 work 스레드는 모두 메인 메모리의 runFlag 값을 읽는다.

프로그램의 시작 시점에는 runFlag를 변경하지 않기 때문에 모든 스레드에서 true의 값을 읽는다. 

work 스레드의 경우 while(runFlag[true])가 만족하기 때문에 while문을 계속 반복해서 수행한다.

main 스레드는 runFlag 값을 false로 설정한다. 이러면 메인 메모리의 runFlag 값이 false로 설정된다.

work 스레드는 while(runFlag)를 실행할 때 runFlag의 데이터를 메인 메모리에서 확인한다.

runFlag의 값이 false이므로 while문을 탈출하고, "task 종료"를 출력한다. 라고 생각할 것이다.

**실제 메모리의 접근 방식**

CPU는 처리 성능을 개선하기 위해 중간에 캐시 메모리라는 것을 사용한다. 

각각의 CPU 코어가 각각의 스레드를 실행할때 멀리 있는 메인 메모리에 있는 값을 갖고 오지 않고

가까운 위치에 있는 캐시 메모리에서 값을 갖고온다.

- 메인 메모리는 CPU 입장에서 보면 거리도 멀고, 속도도 상대적으로 느리다. 대신에 상대적으로 가격이 저렴해서

큰 용량을 쉽게 구성할 수 있다.

- CPU 연산은 매우 빠르기 때문에 CPU 연산의 빠른 성능을 따라가려면, CPU 가까이에 매우 빠른 메모리가 필요한데,

이것이 바로 캐시 메모리이다. 캐시 메모리는 CPU와 가까이 붙어있고, 속도도 매우 빠른 메모리이다. 하지만 상대적으로 가격이

비싸기 때문에 큰 용량을 구성하기는 어렵다. 

- 이러한 이유로 캐시 메모리는 메인 메모리에 있는 데이터를 갖고온다. (처음에는 true니까 true를 갖고옴)

- 이후에, main 스레드에서 runFlag의 값을 false로 설정한다.

- 이때 캐시 메모리의 runFlag가 false로 설정된다. 

여기서 정말 중요한 것은 main 스레드가 사용하는 CPU 코어 캐시 메모리의 runFlag의 값이 변하는 것이다.

이렇게 되면 work 스레드가 사용하는 CPU 코어 캐시 메모리의 runFlag값은 여전히 true다. 즉, while문을 탈출하지 못한다.

main 스레드가 사용하는 CPU 코어 캐시 메모리의 runFlag값이 메인 메모리에 있는 runFlag값에다가 언제 전달될까? 알 수 없다.

설령 runFlag 값을 가져왔어도 work 스레드가 사용하는 CPU 코어 캐시 메모리에는 runFlag값이 false로 언제 바뀔지는 모른다.

이처럼 멀티스레드 환경에서 한 스레드가 변경한 값이 다른 스레드에서 언제 보이는지에 대한 문제를 메모리 가시성이라 한다.

이름 그대로 메모리에 변경한 값이 보이는가, 보이지 않는가의 문제다.

그렇다면 어떤 스레드에서 변경한 값이 다른 스레드에서 바로 보이게 하려면 어떻게 해야할까?

## 3. volatile, 메모리 가시성3

캐시 메모리를 사용하면 CPU 처리 성능을 개선할 수 있다.

하지만 때로는 이런 성능 상향보다는, 여러 스레드에서 같은 시점에 정확히 같은 데이터를 보는 것이 더 중요할 수 있다.

해결방안은 아주 단순하다. 성능을 약간 포기하는 대신에, 값을 읽을 때, 값을 쓸 때 모두 메인 메모리에 직접 접근하면 된다.

자바에서는 volatile이라는 키워드로 이런 기능을 제공한다.

1. volatile, 메모리 가시성1 파트에 있는 예시 코드에서 boolean runFlag를 주석 처리하고

volatile boolean runFlag = true; 를 주석 해제하자.

코드를 실행하면 결과는 다음과 같다. 

```text
21:33:39.328 [     main] runFlag = true
21:33:39.332 [     work] task 시작
21:33:40.346 [     main] runFlag를 false로 변경 시도
21:33:40.346 [     work] task 종료
21:33:40.347 [     main] runFlag = false
21:33:40.347 [     main] main 종료
```

이제 예상했던대로 결과가 나오는 것을 볼 수 있다. 

여러 스레드에서 같은 값을 읽고 써야 한다면 volatile 키워드를 사용하면 된다.

단, 캐시 메모리를 사용할 때 성능이 느려지는 단점이 있기 때문에 꼭 필요한 곳에만 사용하는 것이 좋다.

## 4. volatile, 메모리 가시성4

volatile을 미적용 한 코드를 보자.

```java
package thread.volatile1;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class VolatileCountMain {

  public static void main(String[] args) {
    MyTask task = new MyTask();
    Thread t = new Thread(task, "work");
    
    t.start();
    
    sleep(1000);
    
    task.flag = false;
    log("flag = " + task.flag + ", count = " + task.count + " in main");
  }
  
  static class MyTask implements Runnable {
    boolean flag = true;
    log count;
    //volatile boolean flag = true;
    //volatile long count;
    
    @Override
    public void run() {
      while(flag) {
        count++;
        
        if(count % 100_000_000 == 0) {
          log("flag = " + flag + ", count = " + count  + " in while()");
        }
      }
      
      log("flag = " + flag + ", count = " + count + "종료"); 
      
    }
  }
  
}
```

결과는 다음과 같다.

```text
19:34:54.560 [     work] flag = true, count = 100000000 in while()
19:34:54.840 [     work] flag = true, count = 200000000 in while()
19:34:55.119 [     work] flag = true, count = 300000000 in while()
19:34:55.291 [     main] flag = false, count = 369389701 in while()
19:34:55.361 [     work] flag = true, count = 400000000 in while()
19:34:55.362 [     work] flag = false, count = 400000000 종료
```

결과를 보면 main 스레드에서 flag가 false일때, 카운트 값이 369389701인데 

work 스레드가 false일때는 카운트 값이 400000000이다.

서로 false일때 값이 다르다. 

캐시 메모리를 메인 메모리에 반영하거나, 메인 메모리의 변경 내역을 캐시 메모리에 다시 불러오는 것은 언제 발생할까?

이 부분은 CPU 설계 방식과 실행 환경에 따라 다를 수 있다. 즉시 반영될 수도 있고, 몇 밀리초 후에 될수도 있고,

평생 반영되지 않을 수도 있다.

결국 메모리 가시성 문제를 해결하려면 volatile 키워드를 사용해야 한다.

위 코드에서 flag와 count에 volatile 키워드를 추가해보자.

그리고나서 코드를 실행하면 다음과 같은 결과가 나온다.

```text
19:41:17.380 [     work] flag = false, count = 79322136 종료
19:41:17.380 [     main] flag = false, count = 79322136 in while()
```

main 스레드가 flag를 변경하는 시점에 work 스레드도 flag의 변경 값을 정확하게 확인 할 수 있다.









