package pathstoreweb.pathstoreadminpanel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pathstore.authentication.Credential;
import pathstore.authentication.grpc.PathStoreClientInterceptor;
import pathstore.common.PathStoreProperties;

@SpringBootApplication
public class PathstoreAdminPanelApplication {

  public static void main(String[] args) {

    // Myles: This is a temporary fix that needs to be addressed at a wider scale
    Credential<Integer> credential = PathStoreProperties.getInstance().credential;

    // Since the admin panel has the daemon account
    PathStoreClientInterceptor.getInstance()
        .setCredential(new Credential<>("1", credential.username, credential.password));

    SpringApplication.run(PathstoreAdminPanelApplication.class, args);
  }
}
