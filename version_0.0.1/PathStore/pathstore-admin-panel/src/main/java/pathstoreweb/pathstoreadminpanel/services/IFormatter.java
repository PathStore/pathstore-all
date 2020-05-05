package pathstoreweb.pathstoreadminpanel.services;

import org.springframework.http.ResponseEntity;

/** All formatters must implement this interface */
public interface IFormatter {
  /** @return a string response of what a service has done */
  ResponseEntity<String> format();
}
