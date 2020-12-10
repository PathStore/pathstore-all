package pathstore.system.deployment.commands;

import lombok.RequiredArgsConstructor;

/**
 * This class is used to denote an error that has occured during command execution and contains a
 * message to denote why
 */
@RequiredArgsConstructor
public final class CommandError extends Exception {

  /** Error message to display to the user */
  public final String errorMessage;
}
