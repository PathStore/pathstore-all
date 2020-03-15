package pathstoreweb.pathstoreadminpanel.formatter;

/** All formatters must implement this interface */
public interface IFormatter {
  /** @return a string response of what a service has done */
  String format();
}
