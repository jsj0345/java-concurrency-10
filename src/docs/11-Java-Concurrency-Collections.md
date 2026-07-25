# Java 동시성 컬렉션 복습

> 일반 컬렉션이 멀티스레드 환경에서 깨질 수 있는 이유와 동기화 래퍼, 전용 동시성 컬렉션의 차이를 정리했다.

## 1. `add()` 내부의 여러 작업

```text
list.add("A");
```

겉으로는 한 줄이지만 배열 기반 리스트라면 내부에서 여러 상태를 바꾼다.

```text
현재 size 확인
→ 배열 위치에 값 저장
→ size 증가
→ 필요하면 배열 확장
```

두 스레드가 같은 `size`를 기준으로 동시에 추가하면 한 값이 덮어써지거나 크기 정보가 틀어질 수 있다.

일반적인 `ArrayList`, `HashMap`은 모든 동시 수정이 안전하도록 설계된 컬렉션이 아니다.

## 2. 스레드 안전성

여러 스레드가 동시에 사용해도 내부 상태와 약속된 결과를 유지하는 성질을 스레드 안전하다고 표현한다.

다음 상황을 구분해야 한다.

- 한 스레드만 접근
- 여러 스레드가 읽기만 함
- 여러 스레드가 읽고 수정
- 여러 메서드를 묶은 복합 연산

동시 수정이 있다면 일반 컬렉션을 그대로 공유하지 않는 편이 좋다.

## 3. 동기화 적용

```java
public synchronized void add(Object value) {
    elementData[size] = value;
    size++;
}
```

한 번에 한 스레드만 추가하도록 만들 수 있다. 하지만 모든 컬렉션 구현을 복사해 동기화 버전을 따로 만드는 것은 유지보수 비용이 크다.

## 4. 동기화 래퍼

기존 컬렉션을 감싼 객체가 락을 잡고 실제 컬렉션으로 작업을 위임할 수 있다.

```java
List<String> list =
        Collections.synchronizedList(new ArrayList<>());
```

```text
Collections.synchronizedSet(new HashSet<>());
Collections.synchronizedMap(new HashMap<>());
```

기존 구현을 재사용하며 메서드 접근을 보호한다.

## 5. 복합 연산 주의

개별 메서드가 안전해도 여러 메서드의 조합 전체가 원자적이지는 않다.

```text
if (!list.contains(value)) {
    list.add(value);
}
```

두 호출 사이에 다른 스레드가 개입할 수 있다. 전체를 하나의 락 범위로 묶어야 한다.

반복 순회도 구현의 사용 규칙에 따라 외부 동기화가 필요할 수 있다.

```text
synchronized (list) {
    for (String item : list) {
        // 순회
    }
}
```

## 6. 항상 동기화하지 않는 이유

단일 스레드에서만 쓰는 컬렉션까지 모든 메서드를 동기화하면 불필요한 비용이 생긴다.

그래서 일반 컬렉션은 가벼운 기본 구현을 제공하고 동시성이 필요한 곳에서 별도 도구를 선택하게 한다.

## 7. 전용 동시성 컬렉션

| 용도 | 구현 |
|---|---|
| Map | `ConcurrentHashMap` |
| List | `CopyOnWriteArrayList` |
| Set | `CopyOnWriteArraySet` |
| 정렬 Set | `ConcurrentSkipListSet` |
| 정렬 Map | `ConcurrentSkipListMap` |
| Queue | `ConcurrentLinkedQueue` |
| 대기 가능한 Queue | `BlockingQueue` |

전용 구현은 모든 작업에 하나의 큰 락을 거는 방식보다 동시 처리에 적합한 전략을 사용한다.

## 8. `ConcurrentHashMap`

```java
ConcurrentHashMap<String, Integer> map =
        new ConcurrentHashMap<>();
```

복합 동작에는 전용 메서드를 활용한다.

```text
map.putIfAbsent("A", 1);

map.compute("A", (key, oldValue) ->
        oldValue == null ? 1 : oldValue + 1
);
```

`get()` 후 별도 `put()`을 하는 것보다 의도가 분명하고 원자 기능을 활용할 수 있다.

## 9. `CopyOnWriteArrayList`

수정할 때 내부 배열을 복사하고 읽기는 안정된 배열을 기준으로 수행한다.

- 읽기가 많고 수정이 드물 때 유리
- 순회 중 변경에 스냅샷 성격 제공
- 추가·삭제가 잦거나 데이터가 크면 복사 비용 증가

## 10. 큐 선택

- `ConcurrentLinkedQueue`: 여러 스레드가 빠르게 추가·제거하는 비차단 큐
- `BlockingQueue`: 비었거나 가득 찬 경우 대기 기능 제공

작업 큐나 생산자·소비자 구조에는 `BlockingQueue`가 더 직접적으로 맞는다.

## 핵심 정리

- 일반 컬렉션의 메서드는 내부적으로 여러 상태를 바꿀 수 있다.
- 동시 수정이 있으면 일반 컬렉션 상태가 깨질 수 있다.
- 동기화 래퍼는 기존 컬렉션을 감싸 메서드 접근을 보호한다.
- 복합 연산은 별도 원자 메서드나 동기화가 필요하다.
- 접근 패턴과 읽기·쓰기 비율에 맞는 동시성 컬렉션을 선택해야 한다.
