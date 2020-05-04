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

/** This class is used to check if there are duplicate serverUUID's in the list */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ServerUUIDDuplicates.Validator.class)
public @interface ServerUUIDDuplicates {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator class to check for duplicate serverUUID's */
  class Validator
      implements ConstraintValidator<
          ServerUUIDDuplicates, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList list of passed records
     * @param constraintValidatorContext todo
     * @return if there are duplicates return false else true
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      return deploymentRecordList.stream().map(i -> i.serverUUID).collect(Collectors.toSet()).size()
          == deploymentRecordList.size();
    }
  }
}
