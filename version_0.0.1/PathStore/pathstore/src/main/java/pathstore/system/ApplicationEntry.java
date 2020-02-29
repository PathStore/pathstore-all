package pathstore.system;

public class ApplicationEntry {

  public final int node_id;
  public final ProccessStatus proccess_status;
  public final int waiting_for;

  public ApplicationEntry(final int node_id, final ProccessStatus proccess_status,
      final int waiting_for) {
    this.node_id = node_id;
    this.proccess_status = proccess_status;
    this.waiting_for = waiting_for;
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
