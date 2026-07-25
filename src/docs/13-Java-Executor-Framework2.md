# Executor 종료와 스레드 풀 전략 복습

> 서비스 종료, `ThreadPoolExecutor`의 작업 수용 순서, 풀 크기와 거절 정책을 운영 관점에서 정리했다.

## 1. Executor 종료

풀의 스레드는 다음 작업을 기다리며 살아 있을 수 있다. 사용이 끝난 Executor를 종료하지 않으면 애플리케이션 종료와 자원 정리에 문제가 생길 수 있다.

종료 시 목표:

- 새 작업은 받지 않기
- 이미 받은 작업은 가능한 범위에서 마무리하기

## 2. `shutdown()`

```text
executor.shutdown();
```

새 작업을 거절하고 실행 중이거나 큐에 있는 작업은 계속 처리한다. 호출 메서드 자체는 모든 작업이 끝날 때까지 기다리지 않는다.

```text
새 작업 차단
→ 기존 작업 처리
→ 큐 비우기
→ 풀 종료
```

## 3. `shutdownNow()`

```java
List<Runnable> notStarted =
        executor.shutdownNow();
```

대기 큐의 시작 전 작업을 반환하고 실행 중인 스레드에는 인터럽트를 보낸다.

작업 코드가 인터럽트에 협력해야 하므로 즉시 종료가 보장되는 것은 아니다.

## 4. 종료 상태와 대기

```text
executor.isShutdown();
executor.isTerminated();
executor.awaitTermination(10, TimeUnit.SECONDS);
```

- `isShutdown()`: 종료 절차 시작 여부
- `isTerminated()`: 모든 작업과 스레드가 실제 종료됐는지
- `awaitTermination()`: 제한 시간 동안 완료 대기

## 5. 우아한 종료

```text
executor.shutdown();

try {
    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();

        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("풀 종료 실패");
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

먼저 정상 종료를 시도하고 제한 시간을 넘으면 중단 요청 방식으로 전환한다.

## 6. `ThreadPoolExecutor` 설정

```text
new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        timeUnit,
        workQueue,
        rejectionHandler
);
```

- `corePoolSize`: 기본 스레드 수
- `maximumPoolSize`: 허용할 최대 스레드 수
- `keepAliveTime`: 초과 스레드의 유휴 생존 시간
- `workQueue`: 대기 작업 저장소
- 거절 정책: 풀과 큐가 모두 찼을 때 처리 방식

## 7. 작업 수용 순서

```text
1. 스레드 수가 core보다 적음
   → 새 스레드 생성, 바로 실행

2. core만큼 존재
   → 큐에 저장 시도

3. 큐가 가득 참
   → maximum까지 추가 스레드 생성

4. 최대 스레드도 가득 참
   → 거절 정책
```

처음부터 최대 스레드까지 만든 뒤 큐를 쓰는 순서가 아니다.

## 8. 고정 풀

```text
Executors.newFixedThreadPool(10);
```

동시 실행 수를 예측하기 쉽고 갑작스러운 스레드 증가를 막는다.

다만 기본 큐가 사실상 무제한일 수 있어 처리량보다 요청이 계속 많으면 메모리 사용과 대기 시간이 증가할 수 있다.

## 9. 단일 풀

```text
Executors.newSingleThreadExecutor();
```

한 스레드가 작업을 순서대로 처리한다. 실행 순서가 중요할 때 쓸 수 있지만 긴 작업 하나가 뒤의 모든 작업을 지연시킨다.

## 10. 캐시 풀

```text
Executors.newCachedThreadPool();
```

사용 가능한 스레드가 없으면 새 스레드를 만들고 일정 시간 사용하지 않으면 제거한다. 일반 저장 버퍼 대신 `SynchronousQueue`로 작업을 직접 전달한다.

짧고 간헐적인 작업에는 빠르게 반응하지만 요청 폭증 시 스레드 수가 크게 늘어 자원이 고갈될 수 있다.

## 11. 사용자 정의 전략

```text
new ThreadPoolExecutor(
        10,
        20,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100)
);
```

```text
평상시 기본 스레드 10개
→ 초과 작업 100개까지 큐 대기
→ 큐가 차면 최대 20개까지 확장
→ 모두 차면 거절
```

수치는 작업 시간, 요청량, 서버 자원, 목표 응답 시간을 측정해 결정해야 한다.

## 12. 거절 정책

- `AbortPolicy`: `RejectedExecutionException` 발생
- `DiscardPolicy`: 새 작업을 조용히 버림
- `DiscardOldestPolicy`: 가장 오래 기다린 작업을 버리고 새 작업 시도
- `CallerRunsPolicy`: 제출한 스레드가 직접 실행

`CallerRunsPolicy`는 제출 스레드를 작업 처리에 묶어 요청 생성 속도를 늦추는 효과가 있다.

## 13. 설정 전 확인할 항목

- CPU 계산 중심인지 I/O 대기가 긴지
- 평균 및 최대 작업 시간
- 순간 요청량과 지속 요청량
- 허용할 큐 대기 시간
- 실패와 재시도 정책
- 사용할 수 있는 메모리와 스레드 수

## 핵심 정리

- `shutdown()`은 기존 작업을 마친 뒤 종료한다.
- `shutdownNow()`는 대기 작업을 반환하고 실행 작업에 인터럽트를 보낸다.
- 우아한 종료에는 정상 대기 시간과 강제 전환 정책이 필요하다.
- 작업은 core 생성, 큐 저장, maximum 확장, 거절 순으로 처리된다.
- 고정 풀도 무제한 큐 문제를 따로 봐야 한다.
- 큐와 풀 한계를 넘을 때의 거절 정책을 명확히 정해야 한다.
