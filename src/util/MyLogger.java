package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class MyLogger {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  public static void log(Object obj) {

    String time = LocalTime.now().format(formatter);

    System.out.printf("%s [%9s] %s\n", time, Thread.currentThread().getName(), obj);

  }
}

/*

스레드의 이름을 알아내기 위해 Thread.currentThread().getName()을 매번 적는 것은 불편하다.

불편함을 해소하려고 MyLogger를 만들었다.

 */
