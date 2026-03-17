package thread.sync;

public interface BankAccount {

  boolean withdraw(int amount); // 돈 출금

  int getBalance(); // 잔액 반환

}
