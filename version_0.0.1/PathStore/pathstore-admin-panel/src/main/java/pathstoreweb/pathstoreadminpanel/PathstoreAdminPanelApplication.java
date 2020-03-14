package pathstoreweb.pathstoreadminpanel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;

@SpringBootApplication
public class PathstoreAdminPanelApplication {

  public static void main(String[] args) {
    SpringApplication.run(PathstoreAdminPanelApplication.class, args);
  }
}
