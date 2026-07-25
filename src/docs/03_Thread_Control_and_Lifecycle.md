# 스레드 상태와 실행 제어 복습

> `Thread`가 제공하는 정보, 생명 주기, `sleep()`과 `join()`의 역할을 정리했다.

## 1. 스레드 정보

현재 코드를 실행 중인 스레드는 다음처럼 얻는다.

```java
Thread current = Thread.currentThread();
```

```text
current.threadId();
current.getName();
current.getPriority();
current.getThreadGroup();
current.getState();
```

이름은 로그를 읽기 쉽게 해 주지만 중복될 수 있다. 우선순위는 힌트일 뿐 실행 순서를 확정하지 않는다.

## 2. 주요 상태

| 상태 | 의미 |
|---|---|
| `NEW` | 객체를 만들었지만 시작 전 |
| `RUNNABLE` | 실행 중이거나 CPU 할당 대기 |
| `BLOCKED` | `synchronized` 락 획득 대기 |
| `WAITING` | 시간 제한 없이 다른 신호 대기 |
| `TIMED_WAITING` | 정해진 시간 또는 제한 시간 동안 대기 |
| `TERMINATED` | `run()` 종료 |

`RUNNABLE`은 CPU에서 실제로 실행 중인 상태와 실행 준비 상태를 함께 포함한다.

```text
NEW
 ↓ start()
RUNNABLE
 ↓ sleep()
TIMED_WAITING
 ↓ 시간 경과
RUNNABLE
 ↓ run() 종료
TERMINATED
```

상태는 순간적으로 바뀔 수 있으므로 `getState()`는 관찰한 시점의 정보다.

## 3. `sleep()`

```text
Thread.sleep(1000);
```

호출한 현재 스레드를 일정 시간 동안 `TIMED_WAITING` 상태로 보낸다. 다른 `Thread` 객체를 재우는 명령이 아니다.

```text
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

## 4. `Runnable.run()`과 체크 예외

`Runnable`의 메서드는 체크 예외를 선언하지 않는다.

```java
void run();
```

따라서 재정의한 `run()`도 더 넓은 체크 예외를 밖으로 던질 수 없다. 작업 안에서 처리하거나 적절히 변환해야 한다.

## 5. `start()`는 완료를 기다리지 않는다

```text
t1.start();
t2.start();

System.out.println("결과 사용");
```

`main`은 두 작업의 종료를 기다리지 않고 다음 문장을 실행할 수 있다.

## 6. `join()`

```text
t1.start();
t2.start();

t1.join();
t2.join();

System.out.println("두 작업 완료");
```

`t1.join()`을 호출한 스레드가 `t1` 종료까지 기다린다. 여기서는 `main`이 기다리는 것이다.

```text
worker-1 → 1~50 계산
worker-2 → 51~100 계산
main     → 두 결과 합산
```

최종 합계를 사용하기 전에 두 작업이 끝나야 하므로 `join()`으로 완료 시점을 맞춘다.

## 7. `sleep()`으로 완료를 추측하지 않기

```text
Thread.sleep(2000);
```

작업 시간이 평소보다 길어질 수 있고 빨리 끝나도 불필요하게 기다린다.

```text
시간 기준 대기 → sleep()
작업 완료 기준 대기 → join()
```

시간 제한이 필요하면 `join(timeout)`을 사용할 수 있지만 반환 뒤 대상 스레드가 여전히 살아 있을 수 있다.

## 핵심 정리

- `getState()`는 관찰 순간의 상태를 보여 준다.
- `sleep()`은 현재 스레드를 일정 시간 기다리게 한다.
- `Runnable.run()`은 체크 예외를 밖으로 선언할 수 없다.
- `start()`는 다른 스레드의 작업 완료를 기다리지 않는다.
- 결과가 필요하면 `join()`으로 완료 시점을 맞춘다.
