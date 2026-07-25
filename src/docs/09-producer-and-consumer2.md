# Condition과 BlockingQueue 복습

> 생산자와 소비자의 대기 공간을 분리하는 방법과 Java가 제공하는 `BlockingQueue`를 정리했다.

## 1. 대기 공간 분리

객체의 `wait()` 대기 집합 하나에 생산자와 소비자가 섞이면 원하는 역할만 깨우기 어렵다.

`ReentrantLock`과 `Condition`을 사용하면 역할별 대기 공간을 만들 수 있다.

```java
Lock lock = new ReentrantLock();

Condition notFull = lock.newCondition();
Condition notEmpty = lock.newCondition();
```

- `notFull`: 공간이 생기기를 기다리는 생산자
- `notEmpty`: 데이터가 들어오기를 기다리는 소비자

## 2. 생산자 로직

```java
public void put(String data) throws InterruptedException {
    lock.lock();

    try {
        while (queue.size() == max) {
            notFull.await();
        }

        queue.offer(data);
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
}
```

큐가 가득 차면 생산자는 `notFull`에서 기다린다. 데이터를 넣은 뒤에는 소비자가 진행할 수 있으므로 `notEmpty`에 신호를 보낸다.

## 3. 소비자 로직

```java
public String take() throws InterruptedException {
    lock.lock();

    try {
        while (queue.isEmpty()) {
            notEmpty.await();
        }

        String data = queue.poll();
        notFull.signal();
        return data;
    } finally {
        lock.unlock();
    }
}
```

데이터를 꺼내 빈 공간이 생기면 생산자 조건에 신호를 보낸다.

## 4. `await()`와 `signal()`

`await()`는 현재 스레드를 해당 Condition 대기 공간으로 보내고 락을 반환한다.

`signal()`은 그 공간의 스레드 하나가 다시 락 획득을 시도하도록 알린다.

깨어난 스레드는 바로 실행되는 것이 아니다.

```text
Condition 대기
→ signal 수신
→ 락 획득 경쟁
→ 락 획득
→ 조건 재확인
```

`await()`와 `signal()`은 연결된 `Lock`을 보유한 상태에서 사용해야 한다.

## 5. 두 가지 대기

- 락 획득 대기: 다른 스레드가 임계 영역을 실행 중이라 진입하지 못함
- 조건 대기: 락은 얻었지만 큐 상태가 맞지 않아 `await()`로 물러남

두 상태를 구분해야 실행 흐름을 이해하기 쉽다.

## 6. `BlockingQueue`

Java는 생산자·소비자 문제에 사용할 수 있는 인터페이스를 제공한다.

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(2);
```

```text
queue.put("data");
String data = queue.take();
```

- `put()`: 공간이 없으면 기다림
- `take()`: 데이터가 없으면 기다림

직접 락과 Condition을 조합하지 않아도 된다.

## 7. 상황별 메서드

| 동작 | 예외 | 특별 값 | 무한 대기 | 시간 제한 |
|---|---|---|---|---|
| 추가 | `add()` | `offer()` | `put()` | `offer(timeout)` |
| 제거 | `remove()` | `poll()` | `take()` | `poll(timeout)` |
| 조회 | `element()` | `peek()` | - | - |

```java
boolean accepted =
        queue.offer(data, 1, TimeUnit.SECONDS);
```

제한 시간 안에 공간이 생기지 않으면 `false`를 반환한다.

무한 대기보다 응답성이 중요하다면 시간 제한과 실패 처리 정책이 필요하다.

## 8. 직접 구현과 라이브러리

Condition 구현은 대기 공간 분리 원리를 이해하는 데 유용하다.

실제 코드에서는 `BlockingQueue`가 다음 문제를 이미 고려하므로 더 적합한 경우가 많다.

- 인터럽트
- 제한 시간
- 락 반환
- 조건 재확인
- 공정성
- 예외 정책

## 핵심 정리

- `Condition`을 여러 개 만들면 생산자와 소비자의 대기 공간을 나눌 수 있다.
- 생산자는 추가 후 소비자를, 소비자는 제거 후 생산자를 깨운다.
- `await()`는 락을 반환하고 조건 대기 상태로 이동한다.
- `BlockingQueue`는 생산자·소비자 동기화를 위한 검증된 기능을 제공한다.
- 무한 대기, 즉시 실패, 시간 제한 중 요구사항에 맞는 메서드를 선택해야 한다.
