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
import java.util.stream.Collectors;

/** Used to detect duplicate new Node id's */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NodeIdDuplicates.Validator.class)
public @interface NodeIdDuplicates {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator to detect new node id's on given record set */
  class Validator
      implements ConstraintValidator<
          NodeIdDuplicates, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList given record set
     * @param constraintValidatorContext todo
     * @return true if all newNodeId's are duplicate else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      return deploymentRecordList.stream().map(i -> i.newNodeId).collect(Collectors.toSet()).size()
          == deploymentRecordList.size();
    }
  }
}
