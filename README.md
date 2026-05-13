# Java Concurrency 10

Java 동시성 프로그래밍을 학습하고 예제 코드와 문서로 정리한 저장소입니다.

Thread 기본 개념부터 Thread 제어, 생명주기, `volatile`, 동기화, `LockSupport`, `ReentrantLock`, Producer-Consumer 문제, CAS, Concurrent Collection, Executor Framework까지 Java 동시성 학습에 필요한 내용을 정리했습니다.

## 학습 목적

멀티스레드 환경에서 발생할 수 있는 동시성 문제를 이해하고, Java에서 제공하는 동시성 제어 방식과 실행 관리 도구를 학습하기 위해 정리했습니다.

단순히 Thread를 생성하는 방법만 익히는 것이 아니라, 공유 자원 접근 문제와 동기화 필요성을 이해하고 상황에 따라 적절한 동시성 도구를 선택하는 흐름을 익히는 데 중점을 두었습니다.

## 학습 내용

- Process와 Thread의 차이
- Thread 생성과 실행
- Thread 상태와 생명주기
- Thread 제어 방식
- `volatile`과 메모리 가시성
- `synchronized`를 활용한 동기화
- `LockSupport`와 `ReentrantLock`
- Producer-Consumer 문제
- CAS 기반 원자적 연산
- Concurrent Collection
- Executor Framework
- Thread Pool 기반 작업 처리

## 디렉터리 구조

```text
java-concurrency-10
├── docs
│   ├── 01_Process_and_Thread.md
│   ├── 02_Thread.md
│   ├── 03_Thread_Control_and_Lifecycle.md
│   ├── 04_Thread_Control_and_LifeCycle2.md
│   ├── 05_java-concurrency-volatile.md
│   ├── 06_Thread_Synchronization.md
│   ├── 07_LockSupport_and_ReentrantLock.md
│   ├── 08-producer-consumer.md
│   ├── 09-producer-and-consumer2.md
│   ├── 10-java-concurrency-cas.md
│   ├── 11-Java-Concurrency-Collections.md
│   ├── 12-Java-Executor-Framework.md
│   └── 13-Java-Executor-Framework2.md
├── src
│   ├── thread
│   ├── util
│   └── Main.java
└── java-adv1.iml
```

## 학습 포인트

- Thread가 생성되고 실행되는 기본 흐름을 학습했습니다.
- 멀티스레드 환경에서 공유 자원 접근 시 발생할 수 있는 문제를 확인했습니다.
- `volatile`, `synchronized`, Lock 계열 도구의 차이를 이해했습니다.
- Producer-Consumer 예제를 통해 스레드 간 작업 흐름을 제어하는 방식을 학습했습니다.
- CAS와 Concurrent Collection을 통해 락을 최소화하는 동시성 처리 방식을 확인했습니다.
- Executor Framework를 사용해 Thread를 직접 관리하는 방식에서 Thread Pool 기반 작업 관리 방식으로 전환되는 흐름을 학습했습니다.

## 실행 환경

- Java
- IntelliJ IDEA
- 각 예제 클래스의 `main` 메서드 실행
