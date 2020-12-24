package pathstoreweb.pathstoreadminpanel.validator;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class is used to extend every payload.
 *
 * <p>Each payload with implement {@link #calculateErrors()} and the controller will call hasErrors
 * which will generate the list of errors that
 */
public abstract class ValidatedPayload {

  /** Internal list of errors */
  private String[] errors = null;

  /**
   * Sets {@link #errors} to what {@link #calculateErrors()} returns
   *
   * @return whether or not there are errors
   */
  public boolean hasErrors() {
    this.errors = this.calculateErrors();
    return Arrays.stream(errors).anyMatch(Objects::nonNull);
  }

  /** @return generated errors */
  public String[] getErrors() {
    return this.errors;
  }

  /**
   * Simple checker to ensure all values are non-null
   *
   * @param objects objects from payload
   * @return true iff all objects are non-null
   */
  protected boolean bulkNullCheck(final Object... objects) {
    return Arrays.stream(objects).anyMatch(Objects::isNull);
  }

  /** @return list of errors (internal use only) */
  protected abstract String[] calculateErrors();
}
