package pathstore.util;

/**
 * The function is defined as f: (T1, T2) -> R
 *
 * @param <T1> input type 1
 * @param <T2> input type 2
 * @param <R> return type
 */
public interface DoubleConsumerFunction<T1, T2, R> {
  /**
   * @param input1 input 1
   * @param input2 input 2
   * @return return value
   */
  R apply(T1 input1, T2 input2);
}
