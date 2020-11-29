package pathstorestartup;

import pathstorestartup.constants.LocalCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to simplify the command sequence that is required to package, build and save a
 * local project.
 *
 * @see DevelopmentDeployment#init() to see how this builder is used
 */
public class DevelopmentBuilder {

  /** Synchronous list of processes to execute */
  private final List<LocalCommand> processes = new ArrayList<>();

  /**
   * Add a command to the end of the sequence
   *
   * @param commandName what the given command is doing
   * @param commandContext contextual information of this command
   * @param commands what is the actual command to execute
   * @return reference to this builder to add to the sequence of commands or to execute theml
   */
  public DevelopmentBuilder execute(
      final String commandName,
      final String commandContext,
      final List<String> commands,
      final int desiredExitStatus) {
    this.processes.add(
        new LocalCommand(
            commands,
            this.format(commandName + ": %s", commandContext),
            this.format("Finished " + commandName + " %s", commandContext),
            this.format("Error " + commandName + " %s", commandContext),
            desiredExitStatus));
    return this;
  }

  /**
   * Format entry, exit and error messages
   *
   * @param message message to format
   * @param commandContext command context to format with
   * @return formatted entry, exit or error message
   */
  private String format(final String message, final String commandContext) {
    return String.format(message, commandContext);
  }

  /**
   * Forall processes in the process sequence do the following:
   *
   * <p>Print the entry message
   *
   * <p>Start the process with redirected output and error to STDOUT
   *
   * <p>Wait for the process to complete
   *
   * <p>If it errors print the error message and stop the startup util or print the exit message and
   * continue to the next process
   */
  public void build() {
    processes.forEach(
        event -> {
          System.out.println(event.entry);
          try {
            Process p =
                event
                    .processBuilder
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            p.waitFor();

            if (event.desiredExitStatus != -1 && event.desiredExitStatus != p.exitValue()) {
              System.err.println(event.error);
              System.exit(-1);
            }

            System.out.println(event.exit);
          } catch (InterruptedException | IOException e) {
            System.err.println(event.error);
            e.printStackTrace();
            System.exit(-1);
          }
        });
  }
}
