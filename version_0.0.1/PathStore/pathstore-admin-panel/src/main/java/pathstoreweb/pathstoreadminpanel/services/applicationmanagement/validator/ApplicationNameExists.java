package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.Constants.APPS_COLUMNS;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator.ApplicationNameExists.Validator;

/** Validator to check to see if an application name exists within the apps table */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationNameExists {

  /** @return error message to display to user */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /**
   * Validator class to check the application name. Returns true iff the name is not inside the apps
   * table of the rootserver already
   */
  class Validator implements ConstraintValidator<ApplicationNameExists, String> {

    /**
     * @param applicationName application name
     * @param constraintValidatorContext todo
     * @return iff application name is valid
     */
    @Override
    public boolean isValid(
        final String applicationName, final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select queryApps =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);

      for (Row row : session.execute(queryApps))
        if (row.getString(APPS_COLUMNS.KEYSPACE_NAME).equals(applicationName)) return true;

      return false;
    }
  }
}
