package pathstoreweb.pathstoreadminpanel.startup.commands;

import com.jcraft.jsch.JSchException;
import pathstoreweb.pathstoreadminpanel.startup.SSHUtil;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.ExecutionException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

import java.io.IOException;

/**
 * This class is used to denote a step in the installation process where you want to execute a
 * single command on the remote host and you except a certain exit code
 */
public class Exec implements ICommand {

  /** Command to execute */
  public final String command;

  /** Exit code you want */
  public final int wantedResponse;

  /**
   * @param command {@link #command}
   * @param wantedResponse {@link #wantedResponse}
   */
  public Exec(final String command, final int wantedResponse) {
    this.command = command;
    this.wantedResponse = wantedResponse;
  }

  /**
   * Execute command on remote host
   *
   * @param sshUtil ssh utility to access the remote host
   * @throws ExecutionException if the exit status isn't what was expected
   * @throws InternalException if theres an issue actually executing the command
   */
  @Override
  public void execute(final SSHUtil sshUtil) throws ExecutionException, InternalException {
    try {
      int response = sshUtil.execCommand(this.command).t2;

      if (this.wantedResponse != -1 && this.wantedResponse != response)
        throw new ExecutionException();

    } catch (JSchException | IOException ignored) {
      throw new InternalException();
    }
  }

  /** @return To show the user what command is currently being executed */
  @Override
  public String toString() {
    return "Executing: " + this.command;
  }
}
