package thread.executor.test;

import java.util.concurrent.*;

import static util.MyLogger.log;
import static util.ThreadUtils.sleep;

public class NewOrderService {

  private final ExecutorService es = Executors.newFixedThreadPool(3);

  public void order(String orderNo) throws ExecutionException, InterruptedException {

    // 작업 요청
    Future<Boolean> future1 = es.submit(new InventoryWork(orderNo));
    Future<Boolean> future2 = es.submit(new ShippingWork(orderNo));
    Future<Boolean> future3 = es.submit(new AccountingWork(orderNo));

    // 결과물 기다리기
    Boolean inventoryResult = future1.get();
    Boolean shippingResult = future2.get();
    Boolean accountingResult = future3.get();

    // 결과 확인
    if (inventoryResult && shippingResult && accountingResult) {
      log("모든 주문 처리가 성공적으로 완료되었습니다.");
    } else {
      log("일부 작업이 실패했습니다.");
    }

    es.close();

  }

  static class InventoryWork implements Callable<Boolean> {

    private final String orderNo;

    public InventoryWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("재고 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class ShippingWork implements Callable<Boolean> {

    private final String orderNo;

    public ShippingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("배송 시스템 알림 : " + orderNo);
      sleep(1000);
      return true;
    }

  }

  static class AccountingWork implements Callable<Boolean> {

    private final String orderNo;

    public AccountingWork(String orderNo) {
      this.orderNo = orderNo;
    }

    @Override
    public Boolean call() {
      log("회계 시스템 업데이트 : " + orderNo);
      sleep(1000);
      return true;
    }

  }
}
