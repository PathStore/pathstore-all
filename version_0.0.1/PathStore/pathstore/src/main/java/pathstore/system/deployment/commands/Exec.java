package pathstore.system.deployment.commands;

import com.jcraft.jsch.JSchException;
import pathstore.util.Pair;
import pathstore.system.deployment.utilities.SSHUtil;

import java.io.IOException;

/**
 * This class is used to denote a step in the installation process where you want to execute a
 * single command on the remote host and you except a certain exit code
 */
public class Exec implements ICommand {

  /** Used to remotely execute {@link #command} */
  private final SSHUtil sshUtil;

  /** Command to execute */
  private final String command;

  /** Exit code you want */
  private final int wantedResponse;

  /**
   * @param command {@link #command}
   * @param wantedResponse {@link #wantedResponse}
   */
  public Exec(final SSHUtil sshUtil, final String command, final int wantedResponse) {
    this.sshUtil = sshUtil;
    this.command = command;
    this.wantedResponse = wantedResponse;
  }

  /**
   * Execute command on remote host
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {
    try {
      Pair<String, Integer> response = this.sshUtil.execCommand(this.command);

      if (this.wantedResponse != -1 && this.wantedResponse != response.t2)
        throw new CommandError(
            String.format(
                "We expected the exit status of the command %s to be %d but we received %d instead. The response of the command is %s",
                this.command, this.wantedResponse, response.t2, response.t1));

    } catch (JSchException ignored) {
      throw new CommandError(
          "We were unable to create an exec channel to execute the command please ensure that the system is online and is connectable over ssh");
    } catch (IOException ignore) {
      throw new CommandError(
          " We were unable to read the output stream of the command. This is most likely caused by a local issue. Please ensure the machine executing the deployment has no issues and try again");
    }
  }

  /** @return To show the user what command is currently being executed */
  @Override
  public String toString() {
    return String.format(
        "Executing command: %s looking for response %d", this.command, this.wantedResponse);
  }
}
