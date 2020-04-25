package pathstoreweb.pathstoreadminpanel.startup.deployment.utilities;

/**
 * Simple pair class used for the response of {@link SSHUtil#execCommand(String)}
 *
 * @param <T1> type 1
 * @param <T2> type 2
 */
public class Pair<T1, T2> {

  /** Value in first slot */
  public final T1 t1;

  /** Value in second slot */
  public final T2 t2;

  /**
   * Store values
   *
   * @param t1 {@link #t1}
   * @param t2 {@link #t2}
   */
  public Pair(final T1 t1, final T2 t2) {
    this.t1 = t1;
    this.t2 = t2;
  }
}
