package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import pathstoreweb.pathstoreadminpanel.services.deployment.validator.DeploymentRecordValidation;

/** This payload is used to pass a record that has valid and update it to deploying */
public class UpdateDeploymentRecordPayload {

  /**
   * deployment record based by user. This record must be a failed record in order to pass
   * validation
   */
  @DeploymentRecordValidation(message = "You must enter a valid failed record")
  public AddDeploymentRecordPayload.DeploymentRecord record;
}
