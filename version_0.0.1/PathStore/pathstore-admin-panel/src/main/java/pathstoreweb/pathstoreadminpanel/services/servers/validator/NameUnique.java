package pathstoreweb.pathstoreadminpanel.services.servers.validator;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This validator is used to check to see if the name of the server they've passed is unique or not
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameUnique.Validator.class)
public @interface NameUnique {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /**
   * This validator checks all servers in the database, if the name already exists in a recorded we
   * return false else true
   */
  class Validator implements ConstraintValidator<NameUnique, String> {

    /**
     * @param ip ip passed by user
     * @param constraintValidatorContext context
     * @return false if already exists true if not
     */
    @Override
    public boolean isValid(
        final String ip, final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();
      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

      for (Row row : session.execute(select))
        if (row.getString(Constants.SERVERS_COLUMNS.NAME).equals(ip)) return false;

      return true;
    }
  }
}