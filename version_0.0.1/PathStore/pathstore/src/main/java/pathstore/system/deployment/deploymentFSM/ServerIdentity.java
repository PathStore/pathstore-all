package pathstore.system.deployment.deploymentFSM;

import pathstore.util.BlobObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This class is used to denote a server identity for a server object. This specifically represents
 * those server objects who need RSA authentication
 *
 * <p>TODO: Finish comments
 */
public class ServerIdentity implements BlobObject {
  public final byte[] privKey;

  public final String passphrase;

  public ServerIdentity(final byte[] privKey) {
    this(privKey, null);
  }

  public ServerIdentity(final byte[] privKey, final String passphrase) {
    this.privKey = privKey;
    this.passphrase = passphrase;
  }

  public ServerIdentity(final String privKeyAbsolutePath) throws IOException {
    this(privKeyAbsolutePath, null);
  }

  public ServerIdentity(final String privKeyAbsolutePath, final String passphrase)
      throws IOException {
    File file = new File(privKeyAbsolutePath);
    this.privKey = Files.readAllBytes(file.toPath());
    this.passphrase = passphrase;
  }
}
