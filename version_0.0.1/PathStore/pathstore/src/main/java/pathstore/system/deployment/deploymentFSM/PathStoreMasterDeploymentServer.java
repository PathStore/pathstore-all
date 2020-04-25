package pathstore.system.deployment.deploymentFSM;

/**
 * This class is used to read the deployment table and determine when to transition nodes.
 *
 * <p>Once a record has been transitions to deploying the slave deployment server will then execute
 * the deployment step. Once this step occurs it can either transition to deployed or failed. If
 * failed the administrator of the network will need to login to the web page in order to see the
 * error and request a retry, this retry rewrites the record of that node to deploying instead of
 * failed. This cycle could possibly continue until all errors are resolved. In order to avoid such
 * errors the administrator should follow the server setup guide on our github page.
 */
public class PathStoreMasterDeploymentServer extends Thread {

  @Override
  public void run() {}
}
