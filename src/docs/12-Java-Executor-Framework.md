# ExecutorService와 Future 복습

> 스레드를 직접 생성할 때의 부담을 줄이는 Executor 구조와 작업 결과를 다루는 `Future`를 정리했다.

## 1. 스레드를 직접 만들 때의 문제

```text
new Thread(task).start();
```

요청마다 만들면 다음 부담이 생긴다.

- 스택과 운영체제 자원을 준비하는 생성 비용
- 스레드 수 제한과 추적
- 종료 및 취소 관리
- `Runnable` 결과 반환의 불편함

요청 폭증 시 스레드를 무제한 생성하면 CPU와 메모리가 먼저 고갈될 수 있다.

## 2. 스레드 풀

```text
작업 제출
→ 작업 큐
→ 풀의 스레드가 작업 획득
→ 실행
→ 다음 작업 대기
```

작업 스레드를 재사용하고 동시에 실행할 수를 제한할 수 있다.

## 3. `ExecutorService`

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);
```

```text
executor.execute(() -> doWork());
```

`ExecutorService`는 작업 제출뿐 아니라 결과, 취소, 종료 기능도 제공한다.

풀 상태를 이해할 때는 스레드 수, 실행 중 수, 큐 대기 작업 수, 완료 작업 수를 함께 본다.

## 4. `Runnable`의 불편함

`Runnable.run()`은 반환값이 없고 체크 예외를 밖으로 선언할 수 없다.

결과를 필드에 저장하고 별도로 완료를 기다리는 구조보다 `Callable`을 사용할 수 있다.

## 5. `Callable`

```java
Callable<Integer> task = () -> calculate();
```

- 결과 타입을 반환
- 체크 예외 선언 가능
- `submit()`으로 제출

```java
Future<Integer> future = executor.submit(task);
```

## 6. `Future`

작업 제출 시점에는 결과가 준비되지 않았을 수 있다. `submit()`은 실제 결과 대신 나중의 결과를 확인할 `Future`를 반환한다.

```text
요청 스레드 → 작업 제출
Executor    → Future 반환
작업 스레드 → 작업 수행
Future      → 결과나 예외 보관
```

## 7. 결과 받기

```java
Integer result = future.get();
```

완료됐다면 즉시 반환하고, 아직 실행 중이면 호출한 스레드가 기다린다.

여러 작업을 병렬로 처리하려면 먼저 모두 제출한 뒤 결과를 모으는 편이 좋다.

```java
Future<Integer> f1 = executor.submit(task1);
Future<Integer> f2 = executor.submit(task2);

int r1 = f1.get();
int r2 = f2.get();
```

첫 작업 제출 직후 `get()`으로 기다리면 다음 작업 제출이 늦어져 병렬 기회를 잃을 수 있다.

## 8. 상태와 시간 제한

```text
future.isDone();
future.isCancelled();
future.get(2, TimeUnit.SECONDS);
```

제한 시간 내 완료되지 않으면 `TimeoutException`이 발생한다.

## 9. 취소

```text
future.cancel(true);
```

실행 중이면 인터럽트를 보내 중단을 시도한다.

```java
future.cancel(false);
```

Future는 취소 상태가 되지만 실행 중 작업에는 인터럽트를 보내지 않는다.

작업이 인터럽트에 반응하지 않으면 즉시 종료된다고 보장할 수 없다. 취소된 Future의 `get()`은 `CancellationException`을 발생시킨다.

## 10. 작업 예외

작업 스레드의 예외는 Future에 저장되고 `get()`에서 `ExecutionException`으로 전달된다.

```text
try {
    future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause();
}
```

실제 원인은 `getCause()`로 확인한다.

## 11. 여러 작업 처리

```java
List<Future<Integer>> futures =
        executor.invokeAll(tasks);
```

`invokeAll()`은 모든 작업의 완료를 기다린다.

```java
Integer result =
        executor.invokeAny(tasks);
```

`invokeAny()`는 성공적으로 가장 먼저 끝난 결과를 반환하고 나머지 작업은 취소를 시도한다.

## 12. 비동기와 병렬

```text
비동기 → 호출 흐름이 결과를 즉시 기다리지 않음
병렬   → 여러 작업이 같은 시점에 실제 실행됨
```

Future는 비동기 작업의 결과를 나중에 연결하는 수단이다. 실제 병렬 실행 여부는 풀 크기와 CPU 자원에 달려 있다.

## 핵심 정리

- 스레드 풀은 스레드를 재사용하고 개수를 관리한다.
- `Callable`은 결과 반환과 체크 예외를 지원한다.
- `Future`는 결과, 완료, 취소, 예외 상태를 나타낸다.
- 여러 작업은 먼저 제출한 뒤 결과를 모아야 병렬 기회를 살릴 수 있다.
- `get()`은 결과가 없으면 호출 스레드를 기다리게 한다.
