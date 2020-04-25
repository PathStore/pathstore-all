package pathstore.system.deployment.deploymentFSM;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;

/**
 * This utility class is used to share functionality between {@link PathStoreMasterDeploymentServer}
 * and {@link PathStoreSlaveDeploymentServer}. It also gives functionality to nodes to write task
 * completions on startup
 */
public class PathStoreDeploymentUtils {

  /**
   * Write that a task has been completed in the startup sequence
   *
   * @param session session of local db
   * @param task task to write to
   */
  public static void writeTaskDone(final Session session, final int task) {
    session.execute(
        QueryBuilder.insertInto(Constants.LOCAL_KEYSPACE, Constants.STARTUP)
            .value(Constants.STARTUP_COLUMNS.TASK_DONE, task));
  }
}
