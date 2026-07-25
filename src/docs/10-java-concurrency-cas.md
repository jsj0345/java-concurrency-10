# 원자 연산과 CAS 복습

> `value++`에서 증가 결과가 사라지는 이유와 `AtomicInteger`, CAS 반복, 스핀 락의 한계를 정리했다.

## 1. 원자적 연산

원자적 연산은 다른 스레드가 중간 단계에 끼어들 수 없는 하나의 작업 단위다.

```text
value++;
```

이 한 줄은 실제 개념상 여러 단계다.

```text
value 읽기
→ 1 더하기
→ 결과 저장
```

두 스레드가 같은 값을 읽으면 증가 한 번이 사라질 수 있다.

```text
초기값 0
A가 0 읽음
B가 0 읽음
A가 1 저장
B가 1 저장
결과 1
```

## 2. `volatile`의 한계

```java
private volatile int value;
```

최신 값을 보게 해도 읽기·계산·쓰기 전체를 하나의 연산으로 만들지는 않는다.

정확한 증가가 필요하면 락이나 원자 연산 기능이 필요하다.

## 3. `synchronized` 증가

```java
public synchronized void increment() {
    value++;
}
```

한 번에 한 스레드만 실행하므로 증가가 겹치지 않는다. 안전하지만 락 경쟁이 생기면 다른 스레드는 기다린다.

## 4. `AtomicInteger`

```java
AtomicInteger value = new AtomicInteger();

int result = value.incrementAndGet();
```

자주 사용하는 원자 연산:

```text
value.get();
value.set(10);
value.incrementAndGet();
value.getAndIncrement();
value.addAndGet(5);
value.compareAndSet(10, 20);
```

여러 메서드를 임의로 조합한 전체 로직까지 자동으로 원자화되는 것은 아니다.

## 5. CAS

CAS는 현재 값이 예상 값과 같을 때만 새 값으로 교체한다.

```java
boolean changed =
        value.compareAndSet(expected, update);
```

```text
현재 값 == expected
→ update로 변경, true

현재 값 != expected
→ 변경 없음, false
```

비교와 변경이 하나의 원자적 과정으로 처리되는 것이 핵심이다.

## 6. CAS로 증가

```java
int incrementAndGet(AtomicInteger value) {
    while (true) {
        int current = value.get();
        int next = current + 1;

        if (value.compareAndSet(current, next)) {
            return next;
        }
    }
}
```

다른 스레드가 먼저 값을 바꾸면 CAS가 실패하고 최신 값을 다시 읽어 계산한다.

```text
A와 B가 0 읽음
A: 0 → 1 성공
B: 예상값 0과 현재값 1이 달라 실패
B: 1을 다시 읽고 2로 변경
```

## 7. 락과 CAS

### 락

- 임계 영역 진입 전에 잠금을 얻는다.
- 다른 스레드는 기다린다.
- 여러 상태를 묶어 보호하기 쉽다.

### CAS

- 먼저 변경을 시도한다.
- 충돌하면 다시 계산한다.
- 짧고 단순한 상태 변경에 잘 맞는다.

CAS가 모든 락을 대체하는 것은 아니다.

## 8. 스핀 락 예시

```java
class SpinLock {

    private final AtomicBoolean locked = new AtomicBoolean();

    void lock() {
        while (!locked.compareAndSet(false, true)) {
            // 반복 시도
        }
    }

    void unlock() {
        locked.set(false);
    }
}
```

락 확인과 상태 변경을 CAS 하나로 수행하므로 두 스레드가 동시에 획득하는 문제를 막는다.

하지만 락을 못 얻은 동안 반복문이 계속 CPU를 사용한다. 임계 영역이 길거나 경쟁이 심하면 비효율적이다.

## 9. 더 복잡한 한계

값이 `A → B → A`로 바뀌면 단순 비교만으로 중간 변경을 알아차리지 못하는 ABA 문제가 생길 수 있다.

단순 카운터에서는 크게 드러나지 않지만 복잡한 락 프리 구조에서는 버전 같은 추가 정보가 필요할 수 있다.

## 핵심 정리

- `value++`는 원자적이지 않다.
- `volatile`은 증가 연산 전체를 안전하게 만들지 않는다.
- `AtomicInteger`는 숫자 변경을 위한 원자 메서드를 제공한다.
- CAS는 예상 값이 유지될 때만 새 값으로 교체한다.
- CAS 실패 시 최신 값을 읽어 다시 시도할 수 있다.
- 스핀 락은 대기가 길어지면 CPU를 낭비한다.
