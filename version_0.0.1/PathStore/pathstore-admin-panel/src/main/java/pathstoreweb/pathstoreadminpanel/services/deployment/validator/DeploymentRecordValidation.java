package pathstoreweb.pathstoreadminpanel.services.deployment.validator;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This class is used to check that the record passed is a valid failed record */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeploymentRecordValidation.Validator.class)
public @interface DeploymentRecordValidation {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator to ensure the supplied list of records is greater than zero */
  class Validator
      implements ConstraintValidator<
          DeploymentRecordValidation, AddDeploymentRecordPayload.DeploymentRecord> {

    /**
     * @param deploymentRecord record to update
     * @param constraintValidatorContext todo
     * @return true if the record exists and is failed
     */
    @Override
    public boolean isValid(
        final AddDeploymentRecordPayload.DeploymentRecord deploymentRecord,
        final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      for (Row row : session.execute(select)) {
        int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);
        int parentNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID);
        String serverUUID = row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID);
        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(
                row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));

        if (newNodeId == deploymentRecord.newNodeId
            && parentNodeId == deploymentRecord.parentId
            && serverUUID.equals(deploymentRecord.serverUUID)
            && status == DeploymentProcessStatus.FAILED) return true;
      }

      return false;
    }
  }
}
