package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentEntry;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

/**
 * This class is used when the user wants to get all deployment records to understand the topology
 * of the network
 */
public class GetDeploymentRecords implements IService {

  /** @return {@link DeploymentRecordsFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new DeploymentRecordsFormatter(this.getRecords()).format();
  }

  /**
   * Query all records from database and parse them into a entry class
   *
   * @return list of all parsed entries
   */
  private List<DeploymentEntry> getRecords() {

    Session session = PathStoreCluster.getInstance().connect();

    Select queryAllRecords =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    LinkedList<DeploymentEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllRecords))
      entries.addFirst(
          new DeploymentEntry(
              row.getInt(NEW_NODE_ID),
              row.getInt(PARENT_NODE_ID),
              DeploymentProcessStatus.valueOf(row.getString(PROCESS_STATUS)),
              row.getList(WAIT_FOR, Integer.class),
              UUID.fromString(row.getString(SERVER_UUID))));

    return entries;
  }
}
