package pathstore.test;

import pathstore.common.logger.LoggerLevel;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoggerTest {

  private static class TestClass {
    public final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(TestClass.class);

    void init() {
      for (int i = 0; i < 50; i++) logger.info(String.valueOf(i));
    }
  }

  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(LoggerTest.class);

  void init() {
    for (int i = 0; i < 50; i++) logger.info(String.valueOf(i));
    try {
      throw new Exception();
    } catch (Exception e) {
      logger.error(e);
    }
  }

  void writeDebug() {
    logger.debug("This is a test debug message for new test");
  }

  public static void main(String[] args) throws InterruptedException {

    TestClass testClass = new TestClass();

    LoggerTest loggerTest = new LoggerTest();

    ExecutorService service = Executors.newFixedThreadPool(4);

    service.submit(testClass::init);

    service.submit(loggerTest::init);

    service.shutdown();

    service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    loggerTest.writeDebug();

    for (LoggerLevel level : LoggerLevel.values()) print(level);
  }

  private static void print(final LoggerLevel loggerLevel) {
    System.out.println("\n\nPrinting " + loggerLevel.name() + " merged messages\n\n");

    if (PathStoreLoggerFactory.hasNew(loggerLevel)) {

      List<String> mergedMessages = PathStoreLoggerFactory.getMergedLog(loggerLevel);

      for (String s : mergedMessages) {
        System.out.println(s);
      }

      System.out.println("\n\nSize: " + mergedMessages.size());
    } else System.out.println("No new messages to display");
  }
}
