package thread.executor.test;

import java.util.concurrent.ExecutionException;

import static util.MyLogger.log;

public class NewOrderServiceTestMain {

  public static void main(String[] args) {
    String orderNo = "Order#1234";
    NewOrderService orderService = new NewOrderService();
    try {
      orderService.order(orderNo);
    } catch (InterruptedException e){
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      log("e = " + e);
      Throwable cause = e.getCause();
      log("cause = " + cause);
    }

  }

}
