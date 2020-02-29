package pathstore.util;

/**
 * The point of this class is to calculate the path from one node to another and write to
 * pathstore_applications.node_schemas to either install an application along the path or delete an
 * application between a node and all its children.
 *
 * <p>Disclaimer: This should only ever be ran on the root node otherwise you most likely will get
 * unexpected behaviour
 */
public class ApplicationInstaller {

  private void install_application(final int nodeid, final String keyspace_name) {}

  private void remove_application(final int nodeid, final String keyspace_name) {}

  public static void main(String[] args) {}
}
