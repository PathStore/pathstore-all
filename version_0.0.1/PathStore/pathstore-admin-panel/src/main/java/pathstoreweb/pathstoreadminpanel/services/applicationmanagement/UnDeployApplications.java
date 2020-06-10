package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.schemaFSM.ProccessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.DeleteApplicationDeploymentRecordPayload;

import java.util.LinkedList;

/** This service is used to queue applications for removal from the network */
public class UnDeployApplications implements IService {

  /** Validated payload */
  private final DeleteApplicationDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public UnDeployApplications(final DeleteApplicationDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /**
   * Write deletion records
   *
   * @return TODO: Update return
   */
  @Override
  public ResponseEntity<String> response() {
    writeRecords();
    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Writes all records to the table with the WAITING_REMOVE status */
  private void writeRecords() {
    Session session = PathStoreCluster.getInstance().connect();

    for (ApplicationRecord record : this.payload.records) {
      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS)
              .value(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, record.nodeId)
              .value(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, record.keyspaceName)
              .value(
                  Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                  ProccessStatus.WAITING_REMOVE.toString())
              .value(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, new LinkedList<>(record.waitFor));

      session.execute(insert);
    }
  }
}
