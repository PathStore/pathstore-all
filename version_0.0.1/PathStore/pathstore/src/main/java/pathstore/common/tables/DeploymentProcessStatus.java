package pathstore.common.tables;

/** Denotes the states a node can go through during deployment */
public enum DeploymentProcessStatus {
  WAITING_DEPLOYMENT,
  DEPLOYING,
  PROCESSING_DEPLOYING,
  DEPLOYED,
  WAITING_REMOVAL,
  REMOVING,
  PROCESSING_REMOVING,
  FAILED
}
