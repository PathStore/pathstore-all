package pathstoreweb.pathstoreadminpanel.startup.commands;

import pathstoreweb.pathstoreadminpanel.startup.SSHUtil;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.ExecutionException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.FileTransferException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

/** Interface used to denote a step in the installation process of pathstore */
public interface ICommand {

  /**
   * Function to call to execute the current step
   *
   * @param sshUtil ssh utility to access the remote host
   * @throws ExecutionException if an execution error occurs (Response instead equal to what was
   *     expected)
   * @throws FileTransferException if there was an error transferring a file to the remote host
   * @throws InternalException if there was an arbitrary error like a interrupt exception (Shouldn't
   *     occur unless issue with you or the host's connection)
   */
  void execute(final SSHUtil sshUtil)
      throws ExecutionException, FileTransferException, InternalException;
}
