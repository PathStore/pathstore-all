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
import java.util.UUID;

/** This class is used to check to see if a server object is attached to a pathstore node or not */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ServerDetached.Validator.class)
public @interface ServerDetached {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /**
   * If the server object is attached to a pathstore node return false as they must first un-deploy
   * that node before server object removal.
   */
  class Validator implements ConstraintValidator<ServerDetached, UUID> {

    /**
     * @param serverUUID server UUID passed by users
     * @param constraintValidatorContext context
     * @return false if already exists true if not
     */
    @Override
    public boolean isValid(
        final UUID serverUUID, final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();
      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      for (Row row : session.execute(select))
        if (row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID).equals(serverUUID.toString()))
          return false;

      return true;
    }
  }
}
