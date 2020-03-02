package pathstore.system.schemaloader;

public class ApplicationEntry {

  public final int node_id;
  public final String keyspace_name;
  public final ProccessStatus proccess_status;
  public final String process_uuid;
  public final int waiting_for;

  public ApplicationEntry(
      final int node_id,
      final String keyspace_name,
      final ProccessStatus proccess_status,
      final String process_uuid,
      final int waiting_for) {
    this.node_id = node_id;
    this.keyspace_name = keyspace_name;
    this.proccess_status = proccess_status;
    this.waiting_for = waiting_for;
    this.process_uuid = process_uuid;
  }

  @Override
  public String toString() {
    return "ApplicationEntry{"
        + "node_id="
        + node_id
        + ", proccess_status="
        + proccess_status
        + ", waiting_for="
        + waiting_for
        + '}';
  }
}
