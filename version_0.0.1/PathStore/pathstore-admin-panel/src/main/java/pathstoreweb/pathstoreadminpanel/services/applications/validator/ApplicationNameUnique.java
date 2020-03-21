package pathstoreweb.pathstoreadminpanel.services.applications.validator;

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
import pathstoreweb.pathstoreadminpanel.services.applications.validator.ApplicationNameUnique.Validator;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ApplicationNameUnique {
  String message() default "error";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<ApplicationNameUnique, String> {

    @Override
    public boolean isValid(
        final String applicationName, final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select queryApps =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);

      for (Row row : session.execute(queryApps))
        if (row.getString(APPS_COLUMNS.KEYSPACE_NAME).equals(applicationName)) return false;

      return true;
    }
  }
}
