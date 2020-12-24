package pathstore.system.deployment.commands;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;

/**
 * This class is used to denote a step in the installation process that transfers a local file to
 * the remote host
 */
@RequiredArgsConstructor
public class FileTransfer implements ICommand {

  /** Used to transfer {@link #relativeLocalPath} to {@link #relativeRemotePath} */
  private final SSHUtil sshUtil;

  /** Relative local path that will be converted to absolute */
  private final String relativeLocalPath;

  /** Relative remote path with respect to the logged in user's home directory */
  private final String relativeRemotePath;

  /**
   * Sends the localfile to the remote destination
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {

    try {
      this.sshUtil.sendFile(
          StartupUTIL.getAbsolutePathFromRelativePath(this.relativeLocalPath),
          this.relativeRemotePath);
    } catch (JSchException ignored) {
      throw new CommandError(
          "We where unable to create the sftp channel to transfer the file. Please ensure the machine is online and is accessible over WAN");
    } catch (SftpException e) {
      throw new CommandError(
          String.format(
              "We where unable to transfer the file to the destination %s. Please ensure the account provide has sufficient permissions to write to that directory",
              this.relativeRemotePath));
    }
  }

  /** @return States which file is being transferred to where on the remote host */
  @Override
  public String toString() {
    return String.format("Transferring %s to %s", this.relativeLocalPath, this.relativeRemotePath);
  }
}
