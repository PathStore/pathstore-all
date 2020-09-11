package pathstore.system.deployment.commands;

/**
 * This class is used to denote an error that has occured during command execution and contains a
 * message to denote why
 */
public final class CommandError extends Exception {

  /** Error message to display to the user */
  public final String errorMessage;

  /** @param errorMessage {@link #errorMessage} */
  public CommandError(final String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
