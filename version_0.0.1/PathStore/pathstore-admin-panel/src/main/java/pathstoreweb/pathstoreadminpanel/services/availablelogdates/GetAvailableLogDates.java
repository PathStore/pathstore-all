package pathstoreweb.pathstoreadminpanel.services.availablelogdates;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.availablelogdates.formatter.GetAvailableLogDatesFormatter;

import java.util.*;

public class GetAvailableLogDates implements IService {
  @Override
  public ResponseEntity<String> response() {
    return new GetAvailableLogDatesFormatter(this.getAvailableLogDates()).format();
  }

  private Map<Integer, List<String>> getAvailableLogDates() {

    Session session = PathStoreCluster.getInstance().connect();

    Select selectAll =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);

    Map<Integer, List<String>> nodeToDates = new HashMap<>();

    for (Row row : session.execute(selectAll)) {
      int nodeId = row.getInt(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID);
      nodeToDates.computeIfAbsent(nodeId, k -> new ArrayList<>());
      nodeToDates.get(nodeId).add(row.getString(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE));
    }

    return nodeToDates;
  }
}
