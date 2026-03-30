# 동시성 컬렉션

## 1. 동시성 컬렉션이 필요한 이유
java.util 패키지에 소속되어 있던 컬렉션 프레임워크는 원자적인 연산을 제공할까?
예를 들어서, 하나의 ArrayList 인스턴스에 여러 스레드가 동시에 접근해도 괜찮을까?
여러 스레드가 동시에 접근해도 괜찮은 경우를 스레드 세이프하다고 한다.

```java
package thread.collection.simple;

import java.util.ArrayList;
import java.util.List;

public class SimpleListMainV0 {

  public static void main(String[] args) {
    List<String> list = new ArrayList<>();
    
    // 스레드1, 스레드2가 동시에 실행 가정
    list.add("A"); // 스레드1 실행 가정
    list.add("B"); // 스레드2 실행 가정
  }
  
}
```

```text
[A, B]
```

여기서는 멀티스레드를 사용하지 않았지만, 스레드1과 스레드2가 동시에 다음 코드를 실행한다고 가정해보자.
- 스레드1 : list에 A를 추가한다.
- 스레드2 : list에 B를 추가한다.

컬렉션에 데이터를 추가하는 add() 메서드를 생각해보면, 단순히 컬렉션에 데이터를 하나 추가하는 것뿐이다.
따라서 이것은 마치 연산이 하나만 있는 원자적인 연산처럼 느껴진다.
원자적인 연산은 쪼갤 수 없기 때문에 멀티스레드 상황에 문제가 되지 않는다.
하지만 컬렉션 프레임워크가 제공하는 대부분의 연산은 원자적인 연산이 아니다.

**컬렉션 직접 만들기**

```java
package thread.collection.simple.list;

public interface SimpleList {
  int size();
  
  void add(Object e);
  
  Object get(int index);
}
```

```java
package thread.collection.simple.list;

public class BasicList implements SimpleList {

  private static final int DEFAULT_CAPACITY = 5;

  private Object[] elementData;
  private int size = 0;

  public BasicList() {
    elementData = new Object[DEFAULT_CAPACITY];
  }

  @Override
  public int size() {
    return size;
  }
  
  @Override
  public void add(Object e) {
    elementData[size] = e;
    sleep(100); // 멀티스레드 문제를 쉽게 확인하는 코드
    size++;
  }
  
  @Override
  public Object get(int index) {
    return elementData[index];
  }
  
  @Override
  public String toString() {
    return Arrays.toString(Arrays.copyOf(elementData, size)) + " size=" +
        size + ", capacity = " + elementData.length; 
  }
  
}
```

- 가장 간단한 컬렉션의 구현이다. 내부에서는 배열을 사용해서 데이터를 보관한다.
- ArrayList의 최소 구현 버전이라 생각하면 된다.
- DEFAULT_CAPACITY : 최대 5의 데이터를 저장할 수 있다.
- size : 저장한 데이터의 크기를 나타낸다.
- add() : 컬렉션에 데이터를 추가한다. 

이렇게 만든 컬렉션을 실행해보자.

```java
package thread.collection.simple;

import thread.collection.simple.list.BasicList;
import thread.collection.simple.list.SimpleList;

public class SimpleListMainV1 {

  public static void main(String[] args) {
    SimpleList list = new BasicList();
    list.add("A");
    list.add("B");
    System.out.println("list = " + list);
  }

}
```

```text
list = [A, B] size = 2, capacity = 5
```

단일 스레드로 실행했기 때문에 아직까지는 아무런 문제 없이 잘 작동한다.

## 2. 동시성 컬렉션이 필요한 이유2 - 동시성 문제
```java
public void add(Object e) {
  elementData[size] = e;
  sleep(100);
  size++;
}
```

이 메서드를 보지 않고 add 메서드의 기능만 이용하면 원자적인 것처럼 보인다.
하지만 이 메서드는 단순히 데이터를 추가하는 것으로 끝나지 않고, 내부에 있는 배열에 데이터를 추가해야 하고,
size도 함께 하나 증가시켜야 한다.
심지어 size++; 연산 자체도 원자적이지 않다. size++ 연산은 size = size + 1; 연산과 같다.

이렇게 원자적이지 않은 연산을 멀티스레드 상황에서 안전하게 사용하려면 synchronized, Lock 등을 사용해서
동기화를 해야한다.

```java
package thread.collection.simple;

import thread.collection.simple.list.BasicList;

public class SimpleListMainV2 {

  public static void main(String[] args) throws InterruptedException {
    test(new BasicList());
  }
  
  private static void test(SimpleList list) throws InterruptedException {
    log(list.getClass().getSimpleName());
    
    // A를 리스트에 저장하는 코드
    Runnable addA = new Runnable() {
      @Override
      public void run() {
        list.add("A");
        log("Thread-1 : list.add(A)");
      }
    };
    
    // B를 리스트에 저장하는 코드
    Runnable addB = new Runnable() {
      @Override
      public void run() {
        list.add("B");
        log("Thread-2 : list.add(B)");
      }
    };
    
    Thread thread1 = new Thread(addA, "Thread-1");
    Thread thread2 = new Thread(addB, "Thread-2");
    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();
    log(list);
  }

}
```

일단 코드를 실행하고 결과를 보자.

```text
21:45:48.087 [     main] BasicList
21:45:48.205 [ Thread-2] Thread-2 : list.add(B)
21:45:48.205 [ Thread-1] Thread-1 : list.add(A)
21:45:48.206 [     main] [B, null] size = 2, capacity = 5
```

결과는 위처럼 나왔다. 왜 이렇게 나왔을까?
먼저 예상되는 과정은 이렇다.
스레드-1, 스레드-2가 과정을 수행할 때, 스레드-1이 먼저 메소드(add())를 호출 해서 인덱스 0번에 데이터를 넣고
size값을 1로 증가시킨다. 이후에 스레드-2가 메소드(add())를 호출 해서 1번에 데이터를 넣고 size값을 2로 증가시킨다.
이렇게 예상이 된다.

물론 이런 결과도 있을 것이다. 두 스레드가 동시에 add()를 호출 해서 size가 0 -> 1 만 되는 문제가 생긴다. 원래는
2가 되어야 정상인데 임계 영역에 동기화를 안해줬기 때문에 size=1이고 하나의 데이터만 있을 수 있다. 

이렇게 과정을 예상한걸 토대로 컬렉션 프레임워크는 스레드 세이프 하지 않다.
일반적으로 자주 사용하는 ArrayList, LinkedList, HashSet, HashMap 등 수 많은 자료 구조들은 단순한 연산을 제공하는 것 처럼 보인다.
예를 들어서, 데이터를 추가하는 add() 와 같은 연산은 마치 원자적인 연산처럼 느껴진다.
하지만 그 내부에서는 수 많은 연산들이 함께 사용된다. 배열에 데이터를 추가하고, 사이즈를 변경하고, 배열을 새로 만들어서
배열의 크기도 늘리고, 노드를 만들어서 링크에 연결하는 등 수 많은 복잡한 연산이 함께 사용된다. 

그럼 일반적인 컬렉션들은 스레드 세이프 하지 않는데 그럼 컬렉션을 어떻게 사용해야 할까? 

## 3. 동시성 컬렉션이 필요한 이유3 - 동기화

여러 스레드가 접근해야 한다면 synchronized, Lock 등을 통해 안전한 임계 영역을 적절히 만들면 문제를 해결 할 수 있다. 

```java
package thread.collection.simple.list;

public class SyncList implements SimpleList {
  
  private static final int DEFAULT_CAPACITY = 5;
  
  private Object[] elementData;
  private int size = 0;
  
  public SyncList() {
    elementData = new Object[DEFAULT_CAPACITY];
  }
  
  @Override
  public synchronized int size() {
    return size;
  }
  
  @Override
  public synchronized void add(Object e) {
    elementData[size] = e;
    sleep(100); // 멀티스레드 문제를 만드는 코드
    size++; 
  }
  
  @Override
  public synchronized Object get(int index) {
    return elementData[index];
  }
  
  @Override
  public synchronized String toString() {
    return Arrays.toString(Arrays.copyOf(elementData, size)) + " size= " + size + ", capacity = "
        + elementData.length; 
  }
}
```

- 앞서 만든 BasicList에 synchronized 키워드만 추가했다.
- 모든 메서드가 동기화 되어 있으므로 멀티스레드 상황에 안전하게 사용할 수 있다.

코드를 변경하자.

```java
public class SimpleListMainV2 {

  public static void main(String[] args) throws InterruptedException {
    //test(new BasicList());
    test(new SyncList()); 
  }
  
}
```

결과는 다음과 같다.

```text
22:11:19.439 [     main] SyncList
22:11:19.565 [ Thread-1] Thread-1 : list.add(A)
22:11:19.675 [ Thread-2] Thread-2 : list.add(B)
22:11:19.675 [     main] [A, B] size = 2, capacity = 5
```

실행 결과를 보면 데이터가 [A, B], size=2로 정상 수행된 것을 확인할 수 있다.
add() 메서드에 synchronized를 통해 안전한 임계 영역을 만들었기 때문에, 하나의 스레드만 add() 메서드를 수행한다.

스레드1, 스레드2가 add() 코드를 동시에 수행한다. 여기서는 스레드1이 약간 빠르게 수행했다.
- 스레드1 수행 : add("A")를 수행한다.
-> 락을 획득한다.
-> size 값은 0이다.
-> elementData[0] = A : elementData[0]의 값은 A가 된다.
-> size++를 호출해서 size는 1이 된다.
-> 락을 반납한다.

- 스레드2 수행 : add("B")를 수행한다.
-> 스레드1이 락이 가져간 락을 획득하기 위해 BLOCKED 상태로 대기한다.
-> 스레드 1이 락을 반납하면 락을 획득한다.
-> size 값은 1이다.
-> elementData[1] = B, elementData[1]의 값은 B가 된다. size++를 호출해서 size는 2가 된다.
-> 락을 반납한다. 

동기화를 해서 안전한 임계 영역을 만들었지만 문제가 있다.
매번 코드에 synchronized를 추가해야한다. 즉, 모든 컬렉션을 다 복사해서 동기화 용으로 새로 구현해야 한다.
이것은 비효율적이다. 




