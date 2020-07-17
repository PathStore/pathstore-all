package pathstore.util;

public interface DoubleConsumerFunction<T1, T2, R> {
  R apply(T1 input1, T2 input2);
}
