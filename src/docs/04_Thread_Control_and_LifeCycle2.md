# 인터럽트와 실행 양보 복습

> 실행 중인 스레드에 중단 의사를 전달하는 방법과 인터럽트 상태 처리 시 주의점을 정리했다.

## 1. 공유 플래그

```java
class Task implements Runnable {

    private volatile boolean running = true;

    void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            // 작업
        }
    }
}
```

스레드가 반복 조건을 자주 확인한다면 플래그로 종료 의사를 전달할 수 있다. 하지만 긴 `sleep()`이나 대기 상태에 있으면 변경을 즉시 확인하지 못한다.

## 2. 인터럽트는 협력적인 요청

```text
thread.interrupt();
```

대상 스레드에 중단 요청 상태를 표시한다. `sleep()`, `wait()`, `join()` 같은 대기 메서드 안에 있다면 `InterruptedException`이 발생하면서 깨어날 수 있다.

```text
try {
    Thread.sleep(10_000);
} catch (InterruptedException e) {
    // 중단 요청 처리
}
```

JVM이 스레드를 강제로 제거하는 것은 아니다. 작업 코드가 요청을 확인하고 종료 흐름을 결정해야 한다.

## 3. 예외 이후 상태 보존

대기 메서드에서 `InterruptedException`이 발생하면 인터럽트 상태가 해제된 채 `catch`로 들어올 수 있다.

상위 흐름에도 중단 의도를 전달해야 한다면 상태를 다시 설정한다.

```text
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

## 4. 상태 확인 메서드 차이

```text
thread.isInterrupted();
```

해당 스레드의 상태를 확인하고 유지한다.

```text
Thread.interrupted();
```

현재 스레드의 상태를 확인한 뒤 초기화한다.

| 메서드 | 확인 대상 | 확인 후 |
|---|---|---|
| `isInterrupted()` | 해당 Thread | 상태 유지 |
| `Thread.interrupted()` | 현재 스레드 | 상태 초기화 |

## 5. 반복 작업의 종료 구조

```java
@Override
public void run() {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            process();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    cleanup();
}
```

반복 조건에서 상태를 확인하고, 대기 중 예외가 발생하면 상태를 복원한 뒤 빠져나온다. 종료 요청을 받더라도 자원 정리 코드는 수행해야 한다.

## 6. 큐 확인과 대기

작업이 없을 때 큐를 무한 반복해서 확인하면 CPU를 낭비한다. `ConcurrentLinkedQueue`는 동시 접근에는 안전하지만 작업이 없을 때 자동으로 기다리게 하지는 않는다.

이 문제는 이후 `BlockingQueue`처럼 대기 기능이 포함된 구조로 더 직접적으로 해결할 수 있다.

## 7. `yield()`

```text
Thread.yield();
```

현재 스레드가 다른 실행 가능한 스레드에 CPU 기회를 양보할 의사가 있음을 알린다. 실제로 즉시 양보된다는 보장은 없다.

`yield()`는 실행 순서 조정, 완료 대기, 공유 데이터 보호 수단이 아니다.

| 기능 | 상태 | 용도 |
|---|---|---|
| `sleep()` | `TIMED_WAITING` | 일정 시간 쉬기 |
| `yield()` | 보통 `RUNNABLE` | 다른 스레드에 기회 제안 |

## 핵심 정리

- 공유 플래그는 작업 스레드가 값을 확인할 때만 반응한다.
- 인터럽트는 강제 종료가 아니라 중단 요청이다.
- 대기 중 인터럽트가 발생하면 예외로 깨어날 수 있다.
- 예외 처리 후 중단 의도를 유지하려면 상태를 다시 설정한다.
- `isInterrupted()`와 `Thread.interrupted()`는 초기화 여부가 다르다.
- `yield()`는 실행 순서를 보장하지 않는다.
