package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentEntry;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.GetDeploymentRecordsFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

public class GetDeploymentRecords implements IService {

  @Override
  public String response() {
    return new GetDeploymentRecordsFormatter(this.getRecords()).format();
  }

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
              row.getInt(WAIT_FOR),
              UUID.fromString(row.getString(SERVER_UUID))));

    return entries;
  }
}
