package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentEntry;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;

import java.util.LinkedList;
import java.util.List;

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

    Session session = PathStoreCluster.getSuperUserInstance().connect();

    Select queryAllRecords =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    LinkedList<DeploymentEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllRecords)) entries.addFirst(DeploymentEntry.fromRow(row));

    return entries;
  }
}
