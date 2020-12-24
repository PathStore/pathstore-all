package pathstoreweb.pathstoreadminpanel.services.applications;

/** Simple application class to describe the apps table */
public class Application {
  /** Name of application */
  public final String keyspaceName;

  /** Augmented schema generate by {@link AddApplication} */
  public final String augmentedSchema;

  /**
   * @param keyspaceName {@link #keyspaceName}
   * @param augmentedSchema {@link #augmentedSchema}
   */
  public Application(final String keyspaceName, final String augmentedSchema) {
    this.keyspaceName = keyspaceName;
    this.augmentedSchema = augmentedSchema;
  }
}
