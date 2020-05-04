package pathstoreweb.pathstoreadminpanel.services.deployment.validator;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
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
import java.util.Set;
import java.util.stream.Collectors;

/** This validator checks to make sure all passed serverUUID's have already been installed. */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ServerUUIDValidator.Validator.class)
public @interface ServerUUIDValidator {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator */
  class Validator
      implements ConstraintValidator<
          ServerUUIDValidator, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList set of given records
     * @param constraintValidatorContext todo
     * @return true iff all server UUID's exist within the servers table. Else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

      Set<String> serverUUIDSet =
          deploymentRecordList.stream().map(i -> i.serverUUID).collect(Collectors.toSet());

      //  Remove all serverUUID's used in the new list
      for (Row row : session.execute(select))
        serverUUIDSet.remove(row.getString(Constants.SERVERS_COLUMNS.SERVER_UUID));

      return serverUUIDSet.size() <= 0;
    }
  }
}
