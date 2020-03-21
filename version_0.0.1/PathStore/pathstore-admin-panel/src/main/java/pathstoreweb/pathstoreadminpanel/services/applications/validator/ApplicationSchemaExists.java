package pathstoreweb.pathstoreadminpanel.services.applications.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import org.springframework.web.multipart.MultipartFile;
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationSchemaExists.Validator;

/**
 * Validator for application schema to check if it exists. Reason for not using non-null is so that
 * we can display an error message
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationSchemaExists {

  /** @return error message to show */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Checks if schema is non-null */
  class Validator implements ConstraintValidator<ApplicationSchemaExists, MultipartFile> {

    /**
     * @param applicationSchema user passed schema
     * @param constraintValidatorContext todo
     * @return true if non-null
     */
    @Override
    public boolean isValid(
        final MultipartFile applicationSchema,
        final ConstraintValidatorContext constraintValidatorContext) {
      return applicationSchema != null;
    }
  }
}
