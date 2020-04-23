package pathstoreweb.pathstoreadminpanel.startup.commands;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import pathstoreweb.pathstoreadminpanel.startup.SSHUtil;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.FileTransferException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

import java.io.File;
import java.io.IOException;

/**
 * This class is used to denote a step in the installation process that transfers a local file to
 * the remote host
 */
public class FileTransfer implements ICommand {
  /** Relative local path that will be converted to absolute */
  public final String relativeLocalPath;

  /** Relative remote path with respect to the logged in user's home directory */
  private final String relativeRemotePath;

  /**
   * @param relativeLocalPath {@link #relativeLocalPath}
   * @param relativeRemotePath {@link #relativeRemotePath}
   */
  public FileTransfer(final String relativeLocalPath, final String relativeRemotePath) {
    this.relativeLocalPath = relativeLocalPath;
    this.relativeRemotePath = relativeRemotePath;
  }

  /**
   * Sends the localfile to the remote destination
   *
   * @param sshUtil ssh utility to access the remote host
   * @throws FileTransferException if file cannot be transferred
   * @throws InternalException if the local relative path does not exist (Can't convert to absolute
   *     path)
   */
  @Override
  public void execute(SSHUtil sshUtil) throws FileTransferException, InternalException {

    try {
      sshUtil.sendFile(
          this.getAbsolutePathFromRelativePath(this.relativeLocalPath), this.relativeRemotePath);
    } catch (JSchException | SftpException e) {
      throw new FileTransferException();
    } catch (IOException e) {
      throw new InternalException();
    }
  }

  /**
   * Convert a local relative path to an absolute
   *
   * @param relativePath local relative path
   * @return local absolute path
   * @throws IOException if relative path is bad
   */
  private String getAbsolutePathFromRelativePath(final String relativePath) throws IOException {
    return new File(relativePath).getCanonicalPath();
  }

  /** @return States which file is being transferred to where on the remote host */
  @Override
  public String toString() {
    return "Transferring " + this.relativeLocalPath + " to " + this.relativeRemotePath;
  }
}
