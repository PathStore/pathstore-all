package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

/**
 * Delete deployment record payload. This payload is used to request the deletion of a given node
 * and all of its children
 */
public class DeleteDeploymentRecordPayload extends ValidatedPayload {
  /** Deployment record */
  public AddDeploymentRecordPayload.DeploymentRecord record;

    @Override
    protected String[] calculateErrors() {
        return new String[0];
    }
}
