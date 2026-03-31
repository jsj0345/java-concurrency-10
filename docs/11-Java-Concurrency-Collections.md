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

## 4. 동시성 컬렉션이 필요한 이유4 - 프록시 도입

ArrayList, LinkedList, HashSet, HashMap 등의 코드도 모두 복사해서 synchronized 기능을 추가한 코드를
만들어야 할까? 예를 들어서 다음과 같이 말이다.
- ArrayList -> SyncArrayList
- LinkedList -> SyncLinkedList

하지만 이렇게 코드를 복사해서 만들면 이후에 구현이 변경될 때 같은 모양의 코드를 2곳에서 변경해야 한다.
기존 코드를 그대로 사용하면서 synchronized 기능만 살짝 추가하고 싶다면 어떻게 하면 좋을까?
예를 들어서, BasicList는 그대로 사용하면서, 멀티스레드 상황에 동기화가 필요할 때만 synchronized 기능을
살짝 추가하고 싶다면 어떻게 하면 될까?

이럴 때 사용하는 것이 바로 프록시이다.

**프록시**
우리말로 대리자, 대신 처리해주는 자라는 뜻이다.
프록시를 쉽게 풀어서 설명하자면 친구에게 대신 음식을 주문해달라고 부탁하는 상황을 생각해 볼 수 있다.
예를 들어, 내가 피자를 먹고 싶은데, 직접 전화하는 게 부담스러워서 친구에게 대신 전화해서 피자를 주문해달라고 부탁한다고
해보자. 친구가 피자 가게에 전화를 걸어 주문하고, 피자가 도착하면 당신에게 가져다주는 것이다. 여기서 친구가 프록시 역할을 하는 것이다.
- 나(클라이언트) -> 피자 가게(서버)
- 나(클라이언트) -> 친구(프록시) -> 피자 가게(서버)

객체 세상에도 이런 프록시를 만들 수 있다. 여기서는 프록시가 대신 동기화 기능을 처리해주는 것이다.

```java
package thread.collection.simple.list;

public class SyncProxyList implements SimpleList {
  
  private SimpleList target;
  
  public SyncProxyList(SimpleList target) {
    this.target = target;
  }
  
  @Override
  public synchronized void add(Object e) {
    target.add(e);
  }
  
  @Override
  public synchronized Object get(int index) {
    return target.get(index);
  }
  
  @Override
  public synchronized int size() {
    return target.size();
  }
  
  @Override
  public synchronized String toString() {
    return target.toString() + " by " + this.getClass().getSimpleName();
  }
  
}
```

- 프록시 역할을 하는 클래스이다.
- SyncProxyList는 BasicList와 같은 SimpleList 인터페이스를 구현한다.
- 이 클래스는 생성자를 통해 SimpleList target을 주입 받는다. 여기에 실제 호출되는 대상이 들어간다.
- 이 클래스는 마치 빈껍데기 처럼 보인다. 이 클래스의 역할은 모든 메서드에 synchronized를 걸어주는 일 뿐이다.
그리고나서 target에 있는 같은 기능을 호출한다. 

```java
public class SimpleListMainV2 {

  public static void main(String[] args) throws InterruptedException {
    test(new SyncProxyList(new BasicList()));
  }
  
}
```
기존에 BasicList를 직접 사용하고 있었다면, 이제 중간에 프록시를 사용하므로 다음과 같은 구조로 변경된다.
- 기존 구조 : 클라이언트 -> BasicList(서버)
- 변경 구조 : 클라이언트 -> SyncProxyList(프록시) -> BasicList 


```text
19:12:22.798 [     main] SyncProxyList
19:12:22.916 [ Thread-1] Thread-1 : list.add(A)
19:12:23.018 [ Thread-2] Thread-2 : list.add(B)
19:12:23.019 [     main] [A, B] size = 2, capacity = 5 by SyncProxyList
```

**프록시 구조 분석**
- test() 메서드를 클라이언트라고 가정하자. test() 메서드는 SimpleList라는 인터페이스에만 의존한다.
-> 이것을 추상화에 의존한다고 표현한다.
- 덕분에 SimpleList 인터페이스의 구현체인 BasicList, SyncList, SyncProxyList 중에 어떤 것을
사용하든, 클라이언트인 test() 의 코드는 전혀 변경되지 않아도 된다.
- 클라이언트인 test() 입장에서 생각해보면 BasicList가 넘어올지, SyncProxyList가 넘어올지 알 수 없다.
단순히 SimpleList의 구현체 중의 하나가 넘어와서 실행된다는 정도만 알 수 있다.

**프록시 정리**
- 프록시인 SyncProxyList는 원본인 BasicList와 똑같은 SimpleList를 구현한다. 따라서 클라이언트인
test() 입장에서는 원본 구현체가 전달되든, 아니면 프록시 구현체가 전달되든 아무런 상관이 없다. 단지
수많은 SimpleList의 구현체 중의 하나가 전달되었다고 생각할 뿐이다.
- 클라이언트 입장에서 보면 프록시는 원본과 똑같이 생겼고, 호출할 메서드도 똑같다.
- 프록시는 내부에 원본을 가지고 있다. 그래서 프록시가 필요한 일부의 일을 처리하고, 그다음에 원본을 호출하는
구조를 만들 수 있다. 여기서 프록시는 synchronized를 통한 동기화를 적용한다.
- 프록시가 동기화를 적용하고 원본을 호출하기 때문에 원본 코드도 이미 동기화가 적용된 상태로 호출된다. 

## 5. 자바 동시성 컬렉션1 - synchronized
자바가 제공하는 java.util 패키지에 있는 컬렉션 프레임워크들은 대부분 스레드 안전하지 않다.

일반적으로 사용하는 ArrayList, LinkedList, HashSet, HashMap 등 수 많은 기본 자료 구조들은 내부에서
수 많은 연산들이 함께 사용된다. 배열에 데이터를 추가하고 사이즈를 변경하고, 배열을 새로 만들어서 배열의 크기도 늘리고,
노드를 만들어서 링크에 연결하는 등 수 많은 복잡한 연산이 함께 사용된다.

그렇다면 처음부터 모든 자료 구조에 synchronized를 사용해서 동기화를 해두면 어떨까?
synchronized, Lock, CAS 등 모든 방식은 정도의 차이는 있지만 성능과 트레이드 오프가 있다.
결국 동기화를 사용하지 않는 것이 가장 빠르다. 
그리고 컬렉션이 항상 멀티스레드에서 사용되는 것도 아니다. 미리 동기화를 해둔다면 단일 스레드에서 사용할 때
동기화로 인해 성능이 저하된다. 따라서 동기화의 필요성을 정확히 판단하고 꼭 필요한 경우에만 동기화를 적용하는 것이 필요하다. 

참고 : 과거에 자바는 이런 실수를 한번 했다. 그것이 바로 java.util.Vector 클래스다. 이 클래스는 지금의
ArrayList와 같은 기능을 제공하는데, 메서드에 synchronized를 통한 동기화가 되어 있다. 쉽게 이야기해서 동기화된
ArrayList이다. 그러나 이에 따라 단일 스레드 환경에서도 불필요한 동기화로 성능이 저하되었고, 결과적으로 Vector는 널리 사용되지 않게 되었다.
지금은 하위 호환을 위해서 남겨져 있고 다른 대안이 많기 때문에 사용을 권장하지 않는다.

좋은 대안으로는 우리가 앞서 배운 것처럼 synchronized를 대신 적용해 주는 프록시를 만드는 방법이 있다.
List, Set, Map등 주요 인터페이스를 구현해서 synchronized를 적용할 수 있는 프록시를 만들면 된다.
이 방법을 사용하면 기존 코드를 그대로 유지하면서 필요한 경우에만 동기화를 적용할 수 있다.

**자바 synchronized 프록시**
```java
package thread.collection.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynchronizedListMain {

  public static void main(String[] args) {
    List<String> list = Collections.synchronizedList(new ArrayList<>());
    list.add("data1");
    list.add("data2");
    list.add("data3");
    System.out.println(list.getClass());
    System.out.println("list = " + list);
  }
  
}
```

실행 결과는 다음과 같다. 

```text
class java.util.Collections$SynchronizedRandomAccessList
list = [data1, data2, data3]
```

Collections.synchronizedList()는 다음과 같다.

```java
public static <T> List<T> synchronizedList(List<T> list) {
  return new SynchronizedRandomAccessList<>(list);
}
```

이 코드는 결과적으로 이런 코드다.

```text
new SynchronizedRandomAccessList<>(new ArrayList())
```
클라이언트 -> SynchronizedRandomAccessList(프록시) -> ArrayList

예를 들어서, 이 클래스의 add() 메서드를 보면 synchronized 코드 블럭을 적용하고, 그 다음에 원본 대상의
add()를 호출하는 것을 확인할 수 있다.

```java
public boolean add(E e) {
  synchronized (mutex) {
    return c.add(e);
  }
}
```

Collections가 제공하는 동기화 프록시 기능 덕분에 스레드 안전하지 않은 수 많은 컬렉션들을 매우 편리하게 스레드
안전한 컬렉션으로 변경해서 사용 할 수 있다.

**synchronized 프록시 방식의 단점**
하지만 synchronized 프록시를 사용하는 방식은 다음과 같은 단점이 있다.
- 첫째, 동기화 오버헤드가 발생한다. 비록 synchronized 키워드가 멀티스레드 환경에서 안전한 접근을 보장하지만, 각 메서드 호출 시마다 동기화 비용이 추가된다.
이로 인해 성능 저하가 발생할 수 있다.
-> 동기화 오버헤드란? 여러 쓰레드가 공유 자원에 안전하게 접근하기 위해 사용하는 동기화 기법(synchronized, Lock 등) 때문에 발생하는 추가적인 시간과 자원 소모를 말한다.
- 둘째, 전체 컬렉션에 대해 동기화가 이루어지기 때문에, 잠금 범위가 넓어질 수 있다.
-> 이는 잠금 경합을 증가시키고, 병렬 처리의 효율성을 저하시키는 요인이 된다. 모든 메서드에 대해 동기화를 적용하다 보면,
특정 스레드가 컬렉션을 사용하고 있을 때 다른 스레드들이 대기해야 하는 상황이 빈번해질 수 있다.

## 6. 자바 동시성 컬렉션2 - 동시성 컬렉션
**동시성 컬렉션**
자바 1.5부터 동시성에 대한 많은 혁신이 이루어졌다. 그 중에 동시성을 위한 컬렉션도 있다.
여기서 말하는 동시성 컬렉션은 스레드 안전한 컬렉션을 뜻한다.
java.util.concurrent 패키지에는 고성능 멀티스레드 환경을 지원하는 다양한 동시성 컬렉션 클래스들을 제공한다.
예를 들어, ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue 등이 있다. 이 컬렉션들은 더 정교한 잠금 메커니즘을 사용하여
동시 접근을 효율적으로 처리하며, 필요한 경우 일부 메서드에 대해서만 동기화를 적용하는 등 유연한 동기화 전략을 제공한다.

여기에 다양한 성능 최적화 기법들이 적용되어 있는데, synchronized, Lock(ReentrantLock), CAS, 분할 잠금 기술등 다양한 방법을
섞어서 매우 정교한 동기화를 구현하면서 동시에 성능도 최적화했다. 

**동시성 컬렉션의 종류**
- List
=> CopyOnWriteArrayList -> ArrayList의 대안

- Set
=> CopyOnWriteArraySet -> HashSet의 대안
=> ConcurrentSkipListSet -> TreeSet의 대안(정렬된 순서 유지, Comparator 사용 가능)

- Map
=> ConcurrentHashMap : HashMap의 대안
=> ConcurrentSkipListMap : TreeMap의 대안(정렬된 순서 유지, Comparator 사용 가능)

참고로 LinkedHashSet, LinkedHashMap 처럼 입력 순서를 유지하는 동시에 멀티스레드 환경에서 이용할 수 있는
Set, Map 구현체는 제공하지 않는다. 

List - 예시
```java
package thread.collection.java;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListMain {

  public static void main(String[] args) {
    List<Integer> list = new CopyOnWriteArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    System.out.println("list = " + list);
  }
  
}
```

실행 결과
```text
list = [1, 2, 3]
```
CopyOnWriteArrayList는 ArrayList의 대안이다.

Set - 예시

```java
package thread.collection.java;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SetMain {

  public static void main(String[] args) {
    Set<Integer> copySet = new CopyOnWriteArraySet<>();
    copySet.add(1);
    copySet.add(2);
    copySet.add(3);
    System.out.println("copySet = " + copySet);
    
    Set<Integer> skipSet = new ConcurrentSkipListSet<>();
    skipSet.add(3);
    skipSet.add(2);
    skipSet.add(1);
    System.out.println("skipSet = " + skipSet);
  }
  
}
```
- CopyOnWriteArraySet은 HashSet의 대안이다.
- ConcurrentSkipListSet은 TreeSet의 대안이다. 데이터의 정렬 순서를 유지한다.

```text
copySet = [1, 2, 3]
skipSet = [1, 2, 3]
```

