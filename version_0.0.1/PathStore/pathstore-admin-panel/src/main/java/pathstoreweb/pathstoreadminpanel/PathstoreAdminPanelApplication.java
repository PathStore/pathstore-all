package pathstoreweb.pathstoreadminpanel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pathstoreweb.pathstoreadminpanel.startup.StartUpHandler;

@SpringBootApplication
public class PathstoreAdminPanelApplication {

  public static void main(String[] args) {
    new StartUpHandler().init();

    SpringApplication.run(PathstoreAdminPanelApplication.class, args);
  }
}
