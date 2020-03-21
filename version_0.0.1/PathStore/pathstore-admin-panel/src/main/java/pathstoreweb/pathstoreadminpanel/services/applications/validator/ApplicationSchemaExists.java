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

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationSchemaExists {
  String message() default "error";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<ApplicationSchemaExists, MultipartFile> {

    @Override
    public boolean isValid(
        final MultipartFile applicationSchema,
        final ConstraintValidatorContext constraintValidatorContext) {
      return applicationSchema != null;
    }
  }
}
