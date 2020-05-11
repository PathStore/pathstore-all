package pathstoreweb.pathstoreadminpanel.services.logs;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.logger.LoggerLevel;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.logs.formatter.LogRecordsFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
   * @return list of logs from all nodes
   */
  private List<Log> getLogs() {
    Session session = PathStoreCluster.getInstance().connect();

    Select selectAllLogs =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

    LinkedList<Log> logs = new LinkedList<>();

    for (Row row : session.execute(selectAllLogs))
      logs.addLast(
          new Log(
              row.getInt(Constants.LOGS_COLUMNS.NODE_ID),
              row.getList(Constants.LOGS_COLUMNS.LOG, UDTValue.class).stream()
                  .map(
                      i ->
                          new LogMessage(
                              LoggerLevel.valueOf(
                                  i.getString(Constants.LOG_MESSAGE_PROPERTIES.MESSAGE_TYPE)),
                              i.getString(Constants.LOG_MESSAGE_PROPERTIES.MESSAGE)))
                  .collect(Collectors.toList())));

    return logs;
  }
}
