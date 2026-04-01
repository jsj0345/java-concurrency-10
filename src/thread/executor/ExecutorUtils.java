package thread.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static util.MyLogger.log;

public abstract class ExecutorUtils {

  public static void printState(ExecutorService executorService) {
    if (executorService instanceof ThreadPoolExecutor poolExecutor) {
      int pool = poolExecutor.getPoolSize(); // 스레드 풀에서 현재 생성된 스레드 개수가 몇개인가.
      int active = poolExecutor.getActiveCount(); // 실제 작업하고 있는 스레드의 개수
      int queuedTasks = poolExecutor.getQueue().size(); // 생산자-소비자 구조에서의 큐라고 생각하면 되고 큐에 몇개가 들어가있는지를 물어보는 것.
      long completedTask = poolExecutor.getCompletedTaskCount(); // 완료한 작업 개수
      log("[pool=" + pool + ", active = " + active + ", queuedTasks = " + queuedTasks +
          ", completedTask = " + completedTask + "]");
    } else {
      log(executorService);

    }
  }

}
