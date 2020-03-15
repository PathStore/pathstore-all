package pathstore.system.schemaloader;

import java.util.List;
import java.util.UUID;

/**
 * This class describes a row in the node_schemas table.
 *
 * <p>The reason we parse this data into an object is because the typical PathStore result set
 * compression of a log of data is not possible on this dataset as we have multiple points of
 * concern where as that compression only compresses on duplicate primary keys. So we do custom
 * filtering
 */
public class ApplicationEntry {

  /** Node identification number */
  public final int node_id;

  /** Application name that {@link #proccess_status} has an action for */
  public final String keyspace_name;

  /** Action to take. */
  public final ProccessStatus proccess_status;

  /** Process group that this is apart of see */
  public final UUID process_uuid;

  /**
   * Node identification for node we are waiting for. If this is set {@link #proccess_status} must
   * be either {@link ProccessStatus#WAITING_INSTALL} or {@link ProccessStatus#WAITING_REMOVE}
   */
  public final List<Integer> waiting_for;

  /**
   * Set all values
   *
   * @param node_id {@link #node_id}
   * @param keyspace_name {@link #keyspace_name}
   * @param proccess_status {@link #proccess_status}
   * @param process_uuid {@link #process_uuid}
   * @param waiting_for {@link #waiting_for}
   */
  public ApplicationEntry(
      final int node_id,
      final String keyspace_name,
      final ProccessStatus proccess_status,
      final UUID process_uuid,
      final List<Integer> waiting_for) {
    this.node_id = node_id;
    this.keyspace_name = keyspace_name;
    this.proccess_status = proccess_status;
    this.waiting_for = waiting_for;
    this.process_uuid = process_uuid;
  }

  /**
   * For debug purposes of schema loaders
   *
   * @return string
   */
  @Override
  public String toString() {
    return "ApplicationEntry{"
        + "node_id="
        + node_id
        + ", keyspace_name='"
        + keyspace_name
        + '\''
        + ", proccess_status="
        + proccess_status
        + ", process_uuid='"
        + process_uuid.toString()
        + '\''
        + ", waiting_for="
        + waiting_for
        + '}';
  }
}
