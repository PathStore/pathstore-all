package pathstoreweb.pathstoreadminpanel.services.deployment.validator;

import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Used to ensure the case where a new hypothetical node doesn't have the same parent id and
 * newNodeId. This case can occur when a user submits multiple records and a middle record has
 * matching values would not be caught by any previous validator
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NewNodeIdParentIdCheck.Validator.class)
public @interface NewNodeIdParentIdCheck {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator */
  class Validator
      implements ConstraintValidator<
          NewNodeIdParentIdCheck, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList supplied record list
     * @param constraintValidatorContext todo
     * @return true iff all records don't contain matching parentId and newNodeId's
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      for (AddDeploymentRecordPayload.DeploymentRecord deploymentRecord : deploymentRecordList)
        if (deploymentRecord.newNodeId == deploymentRecord.parentId) return false;

      return true;
    }
  }
}
