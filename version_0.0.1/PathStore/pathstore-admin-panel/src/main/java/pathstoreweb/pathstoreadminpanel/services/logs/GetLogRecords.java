package pathstoreweb.pathstoreadminpanel.services.logs;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.logger.LoggerLevel;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.logs.formatter.LogRecordsFormatter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This service is used to get all logs from the root node and parse them into a list of {@link Log}
 */
public class GetLogRecords implements IService {

  /** @return {@link LogRecordsFormatter} */
  @Override
  public ResponseEntity<String> response() {
    return new LogRecordsFormatter(this.getLogs()).format();
  }

  /**
   * Query all records and parse them into log objects
   *
   * @return map from node_id -> List of Logs
   */
  private Map<Integer, LinkedList<Log>> getLogs() {
    Session session = PathStoreCluster.getInstance().connect();

    Select selectAllLogs =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

    Map<Integer, LinkedList<Log>> map = new HashMap<>();

    for (Row row : session.execute(selectAllLogs)) {
      int nodeId = row.getInt(Constants.LOGS_COLUMNS.NODE_ID);
      map.computeIfAbsent(nodeId, k -> new LinkedList<>());
      map.get(nodeId)
          .addLast(
              new Log(
                  row.getInt(Constants.LOGS_COLUMNS.NODE_ID),
                  row.getString(Constants.LOGS_COLUMNS.DATE),
                  row.getInt(Constants.LOGS_COLUMNS.COUNT),
                  LoggerLevel.valueOf(row.getString(Constants.LOGS_COLUMNS.LOG_LEVEL)),
                  row.getString(Constants.LOGS_COLUMNS.LOG)));
    }

    return map;
  }
}
