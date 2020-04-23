package pathstoreweb.pathstoreadminpanel.startup;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a simple SSH Utility class that uses {@link JSch} in order to send commands and transfer
 * files.
 *
 * <p>This wrapper makes it easier to do both those tasks.
 *
 * <p>TODO: Add known hosts
 */
public class SSHUtil {

  /**
   * Host connected to
   */
  public final String host;

  /** Session object used to denote our connection */
  private final Session session;

  /**
   * Creates session object
   *
   * @param host host to connect to
   * @param username username
   * @param password password
   * @param port ssh port (may not be 22)
   * @throws JSchException if some of the information isn't right
   */
  public SSHUtil(final String host, final String username, final String password, final int port)
      throws JSchException {
    this.host = host;
    this.session = new JSch().getSession(username, host, port);
    this.session.setPassword(password);
    this.session.setConfig("StrictHostKeyChecking", "no");
    this.session.connect();
  }

  /**
   * Executes a command on the remote server and returns the response
   *
   * @param command command to execute on remote host
   * @return pair of response and exit status. (Response may not actually be needed)
   * @throws JSchException if channel cannot be opened
   * @throws IOException thrown if can't read from input stream
   */
  public Pair<String, Integer> execCommand(final String command) throws JSchException, IOException {
    Channel channel = this.session.openChannel("exec");
    ((ChannelExec) channel).setCommand(command);
    InputStream in = channel.getInputStream();
    channel.connect();

    StringBuilder builder = new StringBuilder();

    byte[] tmp = new byte[1024];
    while (true) {
      while (in.available() > 0) {
        int i = in.read(tmp, 0, 1024);
        if (i < 0) break;
        builder.append(new String(tmp, 0, i));
      }
      if (channel.isClosed()) {
        if (in.available() > 0) continue;
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (Exception ignored) {
      }
    }
    channel.disconnect();
    return new Pair<>(builder.toString(), channel.getExitStatus());
  }

  /**
   * Sends local file to remote file location. You can use absolute path and relative path but
   * cannot use '~' in your path to denote your home directory
   *
   * @param localFile local file to transfer
   * @param remoteFile where to store the local file
   * @throws JSchException if channel cannot be created
   * @throws SftpException if error with transfer
   */
  public void sendFile(final String localFile, final String remoteFile)
      throws JSchException, SftpException {
    Channel channel = this.session.openChannel("sftp");
    channel.connect();
    ((ChannelSftp) channel).put(localFile, remoteFile);
    channel.disconnect();
  }

  /** Closes session */
  public void disconnect() {
    this.session.disconnect();
  }
}
