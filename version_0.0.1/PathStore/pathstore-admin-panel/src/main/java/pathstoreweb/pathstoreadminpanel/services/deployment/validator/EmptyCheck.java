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

/** This class is used to check if the number of records supplied is zero */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmptyCheck.Validator.class)
public @interface EmptyCheck {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator to ensure the supplied list of records is greater than zero */
  class Validator
      implements ConstraintValidator<
          EmptyCheck, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList list of supplied records
     * @param constraintValidatorContext todo
     * @return true if there are records else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {
      return !(deploymentRecordList == null || deploymentRecordList.size() == 0);
    }
  }
}
