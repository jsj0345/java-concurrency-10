# 동기화

## 1. 출금 예제

멀티스레드를 사용할 때 가장 주의해야 할 점은, 같은 자원(리소스)에 여러 스레드가 동시에 접근할 때 발생하는 동시성 문제이다.

참고로 여러 스레드가 접근하는 자원을 공유 자원이라 한다. 

멀티스레드를 사용할 때는 이런 공유 자원에 대한 접근을 적절하게 동기화해서 동시성 문제가 발생하지 않게 

방지하는 것이 중요하다.

예제 코드를 보자.

```java
package thread.sync;

public interface BankAccount {
  
  boolean withdraw(int amount); // 출금 메소드
  
  int getBalance(); // 잔액 반환 메소드
  
}
```

```java
package thread.sync;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BankAccountV1 implements BankAccount {

  private int balance;
  //volatile private int balance;

  public BankAccountV1(int initialBalance) {
    this.balance = initialBalance;
  }

  @Override
  public boolean withdraw(int amount) {
    log("거래 시작 : " + getClass().getSimpleName());

    log("[검증 시작] 출금액 : " + amount + ", 잔액: " + balance);
    if (balance < amount) {
      log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
      return false;
    }
    log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
    sleep(1000); // 출금에 걸리는 시간으로 가정
    balance = balance - amount;
    log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);

    log("거래 종료");
    return true;
  }

  @Override
  public int getBalance() {
    return balance;
  }

}
```

```java
package thread.sync;

public class WithdrawTask implements Runnable {
  
  private BankAccount account;
  private int amount;
  
  public WithdrawTask(BankAccount account, int amount) {
    this.account = account;
    this.amount = amount;
  }
  
  @Override
  public void run() {
    account.withdraw(amount); 
  }
  
}
```

```java
package thread.sync;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BankMain {

  public static void main(String[] args) {
    BankAccount account = new BankAccountV1(1000);

    Thread t1 = new Thread(new WithdrawTask(account, 800), "t1");
    Thread t2 = new Thread(new WithdrawTask(account, 800), "t2");
    t1.start();
    t2.start();
    
    sleep(500); // 검증 완료까지 잠시 대기
    
    log("t1 state : " + t1.getState());
    log("t2 state : " + t2.getState());
    
    t1.join();
    t2.join();
    log("최종 잔액 : " + account.getBalance());

  }

}
```

코드를 실행하면 결과는 다음과 같이 나온다.

```text
21:38:16.518 [       t2] 거래 시작 : BankAccountV1
21:38:16.518 [       t1] 거래 시작 : BankAccountV1
21:38:16.531 [       t1] [검증 시작] 출금액 : 800, 잔액 : 1000
21:38:16.531 [       t2] [검증 시작] 출금액 : 800, 잔액 : 1000
21:38:16.531 [       t1] [검증 완료] 출금액 : 800, 잔액 : 1000
21:38:16.531 [       t2] [검증 완료] 출금액 : 800, 잔액 : 1000
21:38:16.993 [     main] t1 state : TIMED_WAITING
21:38:16.993 [     main] t2 state : TIMED_WAITING
21:38:17.533 [       t2] [출금 완료] 출금액 : 800, 잔액 : 200
21:38:17.533 [       t1] [출금 완료] 출금액 : 800, 잔액 : -600
21:38:17.534 [       t2] 거래 종료
21:38:17.535 [       t1] 거래 종료
21:38:17.540 [     main] 최종 잔액 : -600
```

여기서는 t1 스레드가 먼저 실행되었다. 실행 환경에 따라서 t1, t2가 완전히 동시에 실행될 수도 있다.

일단 위 결과는 좀 이상하다.

t1 스레드가 먼저 돈을 꺼냈다고 하더라도 잔액이 200원인데 t2 스레드에서는 어떻게 200원이 남은 상황에서 검증 완료를하고

800원을 빼가서 잔액이 -600원이 됐다. 왜 이런 문제가 발생했을까? 

만약에 balance에 volatile을 도입하면 어떻게 될까? 여전히 같은 문제가 발생한다.

다른 스레드에서 변경된 값을 즉시 볼 수 있게 하는 메모리 가시성이 해결 된 것이지 출금액이 잔액보다 클 때의 상황에 대해서는 처리하지 못한다.

## 2. 동시성 문제 

```text
if (balance < amount) {
      log("[검증 실패]");
      return false;
    }

// 잔고가 출금액 보다 많으면, 진행
log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
sleep(1000);
balance = balance - amount;
```

위 코드는 withdraw 메소드의 일부분인데 여기서 t1 스레드가 먼저 조건식에 도달했다고 가정하자.

처음 잔액은 1000원이고 꺼내려는 돈은 800원(amount)이니까 당연히 검증 실패를 하지않고 출금액과 잔액이 각각 1000원, 800원이 출력 될 것이다.

그리고나서 1초동안 스레드는 잠깐 멈춘다. 

이때 t2는 조건식을 지나가고 역시 출금액과 잔액을 각각 1000, 800원을 출력한다. 

이렇게 되면 조건식을 정상적으로 잘 통과했으니 1000 - 800 = 200, 200 - 800 = -600원의 상황이 발생한다. 

바로 이 부분이 문제다! t1이 아직 잔액을 줄이지 못했기 때문에 t2는 검증 로직에서 현재 잔액을 1000원으로 확인한다.

t1이 검증 로직을 통과하고 바로 잔액을 줄였다면 이런 문제가 발생하지 않겠지만, t1이 검증 로직을 통과하고

잔액을 줄이기도 전에 먼저 t2가 검증 로직을 확인한 것이다.

그렇다면 sleep(1000) 코드를 빼면 되지 않을까?

t1이 검증 로직을 통과하고 balance = balance - amount 를 계산하기 직전에 t2가 실행 되면서

검증 로직을 통과할 수도 있다. 

t1, t2가 완전히 동시에 실행되는 상황을 보도록 하자.

```text
if (balance < amount) {
      log("[검증 실패]");
      return false;
    }

// 잔고가 출금액 보다 많으면, 진행
log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
sleep(1000);
balance = balance - amount;
```

t1 스레드와 t2 스레드가 balance < amount 조건식을 동시에 통과할 것이다.

왜냐면 이때는 아직 잔액(1000원)이 출금액(800)원 보다 더 금액이 높기 때문에 정상적으로 조건식을 통과한다.

두 스레드는 1초동안 멈추고 balance = balance - amount; 를 마주친다.

그러면 1000 - 800 = 200이 된다. 왜냐면 balance = balance - amount; 를 마주치기 전엔 balance가 1000이였고

동시에 돈을 가져가려니까 1000 - 800 = 200이 된다. 근데 이렇게 되면 문제가 있다.

실제로는 1600원이 빠져나갔는데 200원이 남는다고 보인다. 이건 엄청난 문제다. 

```text
23:07:37.102 [       t1] 거래 시작 : BankAccountV1
23:07:37.102 [       t2] 거래 시작 : BankAccountV1
23:07:37.120 [       t2] [검증 시작] 출금액 : 800, 잔액 : 1000
23:07:37.120 [       t1] [검증 시작] 출금액 : 800, 잔액 : 1000
23:07:37.123 [       t2] [검증 완료] 출금액 : 800, 잔액 : 1000
23:07:37.123 [       t1] [검증 완료] 출금액 : 800, 잔액 : 1000
23:07:37.565 [     main] t1 state : TIMED_WAITING
23:07:37.566 [     main] t2 state : TIMED_WAITING
23:07:38.138 [       t2] [출금 완료] 출금액 : 800, 잔액 : 200
23:07:38.138 [       t1] [출금 완료] 출금액 : 800, 잔액 : 200
23:07:38.139 [       t2] 거래 종료
23:07:38.140 [       t1] 거래 종료
23:07:38.146 [     main] 최종 잔액 : 200
```

실행 결과에서 시간이 완전히 같다는 사실을 통해 두 스레드가 같이 실행된 것을 확인할 수 있다.

## 3. 임계 영역

이런 문제가 발생한 근본 원인은 여러 스레드가 함께 사용하는 공유 자원을 여러 단계로 나누어 사용하기 때문이다.

**공유 자원**
잔액은 여러 스레드가 함께 사용하는 공유 자원이다. 따라서 출금 로직을 수행하는 중간에

이 값을 얼마든지 변경할 수 있다. 참고로 여기서는 출금() 메서드를 호출할 때만 잔액의 값이 변경된다.

따라서 다른 스레드가 출금 메서드를 호출하면서, 사용중인 출금 값을 중간에 변경해 버릴 수 있다.

**한 번에 하나의 스레드만 실행**
만약 출금()이라는 메서드를 한 번에 하나의 스레드만 실행할 수 있게 제한한다면 어떻게 될까?

예를 들어 t1, t2 스레드가 함께 출금()을 호출하면 t1 스레드가 먼저 처음부터 끝까지 출금() 메서드를 완료하고,

그 다음에 t2 스레드가 처음부터 끝까지 출금() 메서드를 완료할 것이다.

이렇게 하면 공유 자원인 balance를 한 번에 하나의 스레드만 변경할 수 있다.

**임계 영역**

- 여러 스레드가 동시에 접근하면 데이터 불일치나 예상치 못한 동작이 발생할 수 있는 위험하고 또 중요한 코드 부분을
뜻한다.

- 여러 스레드가 동시에 접근해서는 안 되는 공유 자원을 접근하거나 수정하는 부분을 의미한다.

## 4. synchronized 메서드

자바의 synchronized 키워드를 사용하면 한 번에 하나의 스레드만 실행할 수 있는 코드 구간을 만들 수 있다.

BankAccountV1을 복사해서 BankAccountV2 코드를 만들고 synchronized를 도입하자.

```java
package thread.sync;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class BankAccountV2 implements BankAccount {
  private int balance;
  
  public BankAccountV2(int initialBalance) {
    this.balance = initialBalance;
  }
  
  @Override
  public synchronized boolean withdraw(int amount) {
    log("거래 시작 : " + getClass().getSimpleName());
    
    log("[검증 시작] 출금액 : " + amount + ", 잔액 : " + balance);
    if (balance < amount) {
      log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
      return false;
    }
    
    log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
    sleep(1000);
    balance = balance - amount;
    log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);
    
    log("거래 종료");
    return true; 
    
  }
  
  @Override
  public synchronized int getBalance() {
    return balance;
  }
  
}
```

결과는 다음과 같다. 

```text
20:22:46.028 [       t1] 거래 시작 : BankAccountV2
20:22:46.042 [       t1] [검증 시작] 출금액 : 800, 잔액 : 1000
20:22:46.043 [       t1] [검증 완료] 출금액 : 800, 잔액 : 1000
20:22:46.502 [     main] t1 state : TIMED_WAITING
20:22:46.502 [     main] t2 state : BLOCKED
20:22:47.045 [       t1] [출금 완료] 출금액 : 800, 잔액 : 200
20:22:47.045 [       t1] 거래 종료
20:22:47.046 [       t2] 거래 시작 : BankAccountV2
20:22:47.047 [       t2] [검증 시작] 출금액 : 800, 잔액 : 200
20:22:47.047 [       t2] [검증 실패]
20:22:47.051 [     main] 최종 잔액 : 200
```

실행 결과를 보면 t1이 withdraw() 메서드를 시작부터 완료까지 모두 끝내고 나서, t2가

withdraw() 메서드를 수행하는 것을 확인할 수 있다. 

물론 환경에 따라 t2가 먼저 실행될 수 있다.

이 경우에도 t2가 withdraw() 메서드를 모두 수행한 다음에 t1이 withdraw() 메서드를 수행한다. 

**synchronized 분석**

지금부터 자바의 synchronized가 어떻게 작동하는지 살펴보자.

- 모든 객체는 내부에 자신만의 락을 가지고 있다.

- 스레드가 synchronized 키워드가 있는 메서드에 진입하려면 반드시 해당 인스턴스의 락이 있어야 한다!

-> 여기서는 BankAccount 인스턴스의 synchronized withdraw() 메서드를 호출하므로 이 인스턴스의 락이 필요하다.

- t1이 먼저 실행된다고 생각해보자.

- 스레드 t1이 먼저 synchronized 키워드가 있는 withdraw() 메서드를 호출한다.

- synchronized 메서드를 호출하려면 먼저 해당 인스턴스의 락이 필요하다.

- 락이 있으므로 스레드 t1은 BankAccount 인스턴스에 있는 락을 획득한다.

- 스레드 t1은 해당 인스턴스의 락을 획득했기 때문에 withdraw() 메서드에 진입할 수 있다.

- 스레드 t2도 withdraw() 메서드 호출을 시도한다. synchronized 메서드를 호출하려면 먼저 해당 인스턴스의 락이 필요하다.

- 스레드 t2는 BankAccount 인스턴스에 있는 락 획득을 시도한다. 하지만 락이 없다.

이렇게 락이 없으면 t2 스레드는 락을 획득할 때 까지 BLOCKED 상태로 대기한다.

-> t2 스레드의 상태는 RUNNABLE -> BLOCKED 사태로 변하고, 락을 획득할 때 까지 무한정 대기한다.

- t1은 출금을 위한 검증 로직을 수행하고나서 조건을 만족하므로 검증 로직을 통과한다.

- t1 : 메서드 호출이 끝나면 락을 반납한다.

- t2 : 인스턴스에 락이 반납되면 락 획득을 대기하는 스레드는 자동으로 락을 획득한다.

-> 이때 락을 획득한 스레드는 BLOCKED -> RUNNABLE 상태가 되고, 다시 코드를 실행한다.

- 스레드 t2는 해당 인스턴스의 락을 획득했기 때문에 withdraw() 메서드에 진입할 수 있다.

- t2 : 출금을 위한 검증 로직을 수행한다. 조건을 만족하지 않으므로 false를 반환한다.

**참고**

락을 획득하는 순서는 보장되지 않는다. 

## 5. synchronized 코드 블럭

synchronized의 가장 큰 장점이자 단점은 한 번에 하나의 스레드만 실행할 수 있다는 점이다.

여러 스레드가 동시에 실행하지 못하기 때문에, 전체로 보면 성능이 떨어질 수 있다.

따라서 synchronized를 통해 여러 스레드를 동시에 실행할 수 없는 코드 구간은 꼭! 필요한 곳으로 한정해서 설정해야 한다.

이전에 작성한 다음 코드를 보자.

```java
public synchronized boolean withdraw(int amount) {
  log("거래 시작 : " + getClass().getSimpleName());
 
  log("[검증 시작] 출금액 : " + amount + ", 잔액 : " + balance);
  if(balance < amount) {
    log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
    return false;
  }
  log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
  
  sleep(1000);
  balance = balance - amount;
  log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);
  
  log("거래 종료");
  return true; 
}
```

처음에 로그를 출력하는 "거래 시작", 그리고 마지막에 로그를 출력하는 "거래 종료" 부분은 공유 자원을 전혀 사용하지 않는다.

이런 부분은 동시에 실행해도 아무 문제가 발생하지 않는다.

따라서, 진짜 임계 영역은 다음과 같다. 

```java
public synchronized boolean withdraw(int amount) {
  log("거래 시작 : " + getClass().getSimpleName());
 
  // ==임계 영역 시작== 
  log("[검증 시작] 출금액 : " + amount + ", 잔액 : " + balance);
  if(balance < amount) {
    log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
    return false;
  }
  log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
  
  sleep(1000);
  balance = balance - amount;
  log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);
  // ==임계 영역 종료==
  
  log("거래 종료");
  return true; 
}
```

메서드 앞에 적용한 synchronized의 적용 범위는 메서드 전체이다.

따라서 여러 스레드가 함께 실행해도 문제가 없는 "거래 시작", "거래 종료"를 출력하는 코드도 한 번에 하나의 스레드만 실행할 수 있다.

자바는 이런 문제를 해결하기 위해 synchronized를 메서드 단위가 아니라, 특정 코드 블럭에 최적화해서 적용할 수 있는 기능을 제공한다.

```java
public boolean withdraw(int amount) {
  log("거래 시작 : " + getClass().getSimpleName());
 
  synchronized (this) {
     log("[검증 시작] 출금액 : " + amount + ", 잔액 : " + balance);
     if(balance < amount) {
        log("[검증 실패] 출금액 : " + amount + ", 잔액 : " + balance);
        return false;
      }
      log("[검증 완료] 출금액 : " + amount + ", 잔액 : " + balance);
      
      sleep(1000);
      balance = balance - amount;
      log("[출금 완료] 출금액 : " + amount + ", 변경 잔액 : " + balance);
  }
  
  log("거래 종료");
  return true; 
}
```




