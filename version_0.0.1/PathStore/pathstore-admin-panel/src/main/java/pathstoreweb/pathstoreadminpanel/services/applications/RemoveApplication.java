package pathstoreweb.pathstoreadminpanel.services.applications;

import org.springframework.http.ResponseEntity;
import pathstoreweb.pathstoreadminpanel.services.IService;

/**
 * TODO: Remove all instances of the application from the network. Wait for completion. Once
 * complete remove the application from the apps table
 *
 * <p>TODO: Or maybe don't allow removal?
 */
public class RemoveApplication implements IService {
  @Override
  public ResponseEntity<String> response() {
    return null;
  }
}
