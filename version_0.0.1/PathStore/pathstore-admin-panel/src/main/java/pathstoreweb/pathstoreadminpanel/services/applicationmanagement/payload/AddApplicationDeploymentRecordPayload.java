package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationRecord;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.ADD_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD.*;

/** This payload is used to receive new deployment records from the user */
public class AddApplicationDeploymentRecordPayload extends ValidatedPayload {

  /** Records passed by user */
  public List<ApplicationRecord> records;

  /**
   * Validation:
   *
   * <p>(1): Can't pass an empty records list
   *
   * <p>(2): Every record must have the same keyspace
   *
   * <p>(3): If at least 1 node id isn't in the node id list or any node is in the node schema list
   * this is a conflict
   *
   * <p>(4): TODO: Validate waits
   *
   * @return null iff valid
   */
  @Override
  @SuppressWarnings("ALL")
  protected String[] calculateErrors() {

    // (1)
    if (records == null || records.size() == 0) return new String[] {EMPTY};

    // (2)
    if (records.stream().map(i -> i.keyspaceName).distinct().count() != 1)
      return new String[] {TO_MANY_KEYSPACES};

    Session session = PathStoreCluster.getInstance().connect();

    Select queryAllDeployment =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    Set<Integer> validNodeIdSet = new HashSet<>();

    for (Row row : session.execute(queryAllDeployment))
      validNodeIdSet.add(row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID));

    Select nodeSchemaSelect =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    Set<Integer> applicationNodeIdSet = new HashSet<>();

    for (Row row : session.execute(nodeSchemaSelect))
      if (row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME)
          .equals(this.records.get(0).keyspaceName))
        applicationNodeIdSet.add(row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID));

    // (3)
    if (!this.records.stream().map(i -> i.nodeId).allMatch(validNodeIdSet::contains)
        || this.records.stream().map(i -> i.nodeId).anyMatch(applicationNodeIdSet::contains))
      return new String[] {INVALID_RECORD};

    return new String[0];
  }
}
