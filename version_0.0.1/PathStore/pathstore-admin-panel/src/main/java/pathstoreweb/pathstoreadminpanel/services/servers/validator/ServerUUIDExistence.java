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

/** This validator is used to determine if the server uuid passed exists */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ServerUUIDExistence.Validator.class)
public @interface ServerUUIDExistence {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Returns true iff the server UUID exists */
  class Validator implements ConstraintValidator<ServerUUIDExistence, UUID> {

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
          QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
      select.where(QueryBuilder.eq(Constants.SERVERS_COLUMNS.SERVER_UUID, serverUUID.toString()));

      for (Row row : session.execute(select)) return true;

      return false;
    }
  }
}
