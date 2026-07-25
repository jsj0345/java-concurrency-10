# Java 스레드 생성 복습

> Java에서 작업 흐름을 만드는 방법과 `Thread`, `Runnable`, 데몬 스레드의 차이를 정리했다.

## 1. 스레드와 JVM 메모리

JVM 메모리를 스레드 관점에서 보면 공용 영역과 개별 영역으로 나눌 수 있다.

- 메서드 영역: 클래스 정보, 실행 코드, 정적 데이터
- 힙: 객체와 배열
- 스택: 메서드 호출과 지역 변수를 스레드별로 관리

새 스레드가 시작되면 그 스레드만의 호출 스택이 필요하다. 힙 객체는 같은 프로세스의 여러 스레드가 함께 참조할 수 있다.

## 2. `Thread` 상속

```java
class MessageThread extends Thread {

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}
```

실행할 때는 `run()`을 직접 부르지 않고 `start()`를 호출한다.

```text
Thread thread = new MessageThread();
thread.start();
```

`start()`는 별도의 실행 흐름을 준비한 뒤 새 스레드가 `run()`을 수행하게 한다. 반대로 `thread.run()`은 호출한 현재 스레드가 일반 메서드처럼 실행한다.

한 번 종료된 `Thread` 인스턴스를 다시 시작할 수 없으므로 새 실행이 필요하면 새 객체를 만들어야 한다.

## 3. 실행 순서는 고정되지 않는다

```text
thread.start();
System.out.println("main 작업");
```

새 스레드가 먼저 실행될 수도 있고 `main`이 다음 코드를 먼저 처리할 수도 있다.

> `start()`는 실행 기회를 요청하지만 호출 직후의 순서를 약속하지 않는다.

## 4. `Runnable`로 역할 분리

```java
class MessageTask implements Runnable {

    @Override
    public void run() {
        System.out.println("작업 실행");
    }
}
```

```text
Runnable task = new MessageTask();
Thread thread = new Thread(task, "worker");
thread.start();
```

```text
Runnable → 무엇을 실행할지
Thread   → 어느 스레드에서 실행할지
```

`Runnable`은 작업과 실행 수단을 분리하고 클래스 상속 자리를 차지하지 않는다. 이후 Executor 프레임워크에도 같은 작업 표현을 전달할 수 있다.

## 5. 람다와 여러 스레드

```text
Thread thread = new Thread(
        () -> System.out.println("람다 작업"),
        "worker"
);
thread.start();
```

작업이 짧다면 람다가 간결하다. 로직이 길거나 재사용된다면 별도 클래스로 분리하는 편이 읽기 쉽다.

```text
for (int i = 1; i <= 3; i++) {
    Thread worker = new Thread(
            () -> System.out.println(Thread.currentThread().getName()),
            "worker-" + i
    );
    worker.start();
}
```

반복문은 생성 순서만 정할 뿐 실제 CPU 실행 순서는 정하지 않는다.

## 6. 사용자 스레드와 데몬 스레드

사용자 스레드는 프로그램의 주요 작업을 수행하며 하나라도 살아 있으면 JVM이 보통 계속 유지된다.

데몬 스레드는 백그라운드 보조 작업에 적합하다.

```text
Thread monitor = new Thread(task, "monitor");
monitor.setDaemon(true);
monitor.start();
```

사용자 스레드가 모두 끝나면 데몬 스레드가 남아 있어도 JVM이 종료될 수 있다. 반드시 완료되어야 하는 저장이나 결제 작업을 데몬 스레드에만 맡기면 위험하다.

## 핵심 정리

- 새 스레드는 `run()`이 아니라 `start()`로 시작한다.
- `start()` 이후의 실행 순서는 스케줄러가 결정한다.
- 스레드마다 스택은 따로 있고 힙 객체는 공유할 수 있다.
- `Runnable`은 작업 내용과 실행 수단을 분리한다.
- 데몬 스레드는 사용자 스레드가 모두 종료되면 함께 끝날 수 있다.
