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

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationNamePathStore {
  String message() default "error";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<ApplicationNamePathStore, String> {

    @Override
    public boolean isValid(
        final String applicationName, final ConstraintValidatorContext constraintValidatorContext) {
      return applicationName.startsWith("pathstore_");
    }
  }
}
