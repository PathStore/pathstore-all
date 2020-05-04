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

/** Used to detect already used serverUUID's in the new record set */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniquenessServerUUID.Validator.class)
public @interface UniquenessServerUUID {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator */
  class Validator
      implements ConstraintValidator<
          UniquenessServerUUID, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList list of records passed by the user
     * @param constraintValidatorContext todo
     * @return true iff all serverUUID's are not already in the deployment table. else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      Set<String> serverUUIDSet =
          deploymentRecordList.stream().map(i -> i.serverUUID).collect(Collectors.toSet());

      for (Row row : session.execute(select))
        if (serverUUIDSet.contains(row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID)))
          return false;

      return true;
    }
  }
}
