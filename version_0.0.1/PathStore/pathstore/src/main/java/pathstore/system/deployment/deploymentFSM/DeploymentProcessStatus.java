package pathstore.system.deployment.deploymentFSM;

public enum DeploymentProcessStatus {
  WAITING_DEPLOYMENT,
  DEPLOYING,
  DEPLOYED,
  FAILED
}
