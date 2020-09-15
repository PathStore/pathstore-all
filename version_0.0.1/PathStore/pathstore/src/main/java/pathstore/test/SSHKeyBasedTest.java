package pathstore.test;

import com.jcraft.jsch.JSchException;
import pathstore.system.deployment.utilities.SSHUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// username, host, port, priv key location, passphrase
public class SSHKeyBasedTest {
  public static void main(String args[]) throws IOException, JSchException {
    if (args.length <= 4) {
      System.out.println(
          "arg 1 is username, arg 2 is host, arg 3 is port, arg 4 is priv key location, arg 5 is passphrase (optional)");
      return;
    }

    File privKey = new File(args[3]);

    byte[] file = Files.readAllBytes(privKey.toPath());

    SSHUtil sshUtil =
        new SSHUtil(
            args[0], args[1], Integer.parseInt(args[2]), file, args.length == 5 ? args[4] : null);

    sshUtil.execCommand("docker ps");

    sshUtil.disconnect();
  }
}
