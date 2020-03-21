package pathstoreweb.pathstoreadminpanel.services.applications.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationNamePathStore.Validator;

/**
 * This class is used to check to see if the application name starts with pathstore_ as that is a
 * requirement for pathstore
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationNamePathStore {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator class. Calls isvalid with our value we want */
  class Validator implements ConstraintValidator<ApplicationNamePathStore, String> {

    /**
     * @param applicationName application Name passed by user
     * @param constraintValidatorContext todo
     * @return true if applicatioName starts with pathstore_
     */
    @Override
    public boolean isValid(
        final String applicationName, final ConstraintValidatorContext constraintValidatorContext) {
      return applicationName.startsWith("pathstore_");
    }
  }
}
