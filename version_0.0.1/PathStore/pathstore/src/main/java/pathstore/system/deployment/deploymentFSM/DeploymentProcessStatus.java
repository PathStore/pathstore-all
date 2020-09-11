package pathstore.system.deployment.deploymentFSM;

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
