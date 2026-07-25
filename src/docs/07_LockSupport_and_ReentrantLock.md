# LockSupport와 ReentrantLock 복습

> `synchronized`보다 세밀한 대기와 락 제어가 필요한 경우를 중심으로 정리했다.

## 1. `synchronized`의 제약

`synchronized`는 단순하고 락 반환도 자동이다. 하지만 락을 기다리는 흐름에 다음 요구가 생기면 제어가 부족할 수 있다.

- 기다리지 않고 즉시 포기
- 정해진 시간까지만 대기
- 락 대기 중 인터럽트 처리
- 공정성 정책
- 여러 조건별 대기 공간

이럴 때 `java.util.concurrent.locks`의 기능을 검토할 수 있다.

## 2. `LockSupport`

```text
LockSupport.park();
```

현재 스레드를 대기 상태로 보낸다.

```text
LockSupport.unpark(targetThread);
```

대상 스레드가 다시 실행 가능한 상태가 되도록 허가를 제공한다.

```text
worker → park() → WAITING
main   → unpark(worker)
worker → RUNNABLE
```

`unpark()`가 `park()`보다 먼저 호출되면 준비된 허가를 이후 `park()`가 사용할 수 있다. 허가는 여러 개가 계속 누적되는 카운터처럼 동작하지 않는다.

## 3. 시간 제한 대기

```text
LockSupport.parkNanos(2_000_000_000L);
```

일정 시간 동안 `TIMED_WAITING` 상태로 보낸다. 시간 경과, `unpark()`, 인터럽트 등의 이유로 돌아올 수 있으므로 대기 후 조건을 다시 확인해야 한다.

## 4. `ReentrantLock`

```text
Lock lock = new ReentrantLock();

lock.lock();
try {
    // 임계 영역
} finally {
    lock.unlock();
}
```

`lock()`부터 `unlock()`까지가 보호 범위다. 중간에 예외나 `return`이 발생해도 락이 반환되도록 `unlock()`은 `finally`에 둔다.

## 5. 기능 비교

| 기능 | `synchronized` | `ReentrantLock` |
|---|---|---|
| 락 획득·반환 | 자동 | 직접 호출 |
| 즉시 시도 | 없음 | `tryLock()` |
| 시간 제한 | 없음 | 시간 제한 `tryLock()` |
| 인터럽트 가능한 대기 | 제한적 | `lockInterruptibly()` |
| 공정성 옵션 | 없음 | 설정 가능 |

기능이 많은 대신 사용자가 락 반환을 정확하게 관리해야 한다.

## 6. 즉시 시도

```text
if (!lock.tryLock()) {
    return false;
}

try {
    return process();
} finally {
    lock.unlock();
}
```

다른 스레드가 락을 보유 중이면 기다리지 않고 실패 경로로 갈 수 있다.

## 7. 시간 제한

```text
if (!lock.tryLock(2, TimeUnit.SECONDS)) {
    return false;
}

try {
    return process();
} finally {
    lock.unlock();
}
```

최대 2초까지만 기다린다. 응답 시간이 중요한 서버 로직에서 무한 대기를 피할 수 있다.

## 8. 인터럽트 가능한 락 획득

```text
lock.lockInterruptibly();

try {
    process();
} finally {
    lock.unlock();
}
```

락 대기 중 인터럽트가 발생하면 `InterruptedException`으로 중단할 수 있다. 실제로 락을 얻은 경우에만 `unlock()`해야 한다.

## 9. 공정성

```java
Lock fairLock = new ReentrantLock(true);
```

오래 기다린 스레드에 기회를 더 순서 있게 주려는 정책을 적용할 수 있다. 대신 관리 비용 때문에 처리량이 낮아질 수 있다.

## 핵심 정리

- `LockSupport`는 스레드 대기와 재개를 직접 제어하는 낮은 수준의 기능이다.
- `parkNanos()`는 시간 제한 대기를 제공한다.
- `ReentrantLock`은 즉시 시도, 시간 제한, 인터럽트 가능한 대기를 제공한다.
- 락을 얻었다면 `finally`에서 반드시 반환해야 한다.
- 공정성과 처리량 사이에는 비용 차이가 있다.
