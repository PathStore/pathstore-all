package pathstore.util;

import lombok.RequiredArgsConstructor;
import pathstore.system.deployment.utilities.SSHUtil;

/**
 * Simple pair class used for the response of {@link SSHUtil#execCommand(String)}
 *
 * @param <T1> type 1
 * @param <T2> type 2
 */
@RequiredArgsConstructor
public class Pair<T1, T2> {

  /** Value in first slot */
  public final T1 t1;

  /** Value in second slot */
  public final T2 t2;
}
