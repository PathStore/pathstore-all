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

/**
 * Used to detect already used newNodeId's. As in if the id as already been used to define a
 * pre-existing node in the topology
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniquenessNodeId.Validator.class)
public @interface UniquenessNodeId {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validtor */
  class Validator
      implements ConstraintValidator<
          UniquenessNodeId, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param records passed records
     * @param constraintValidatorContext todo
     * @return true if all new node id's are new and not already used else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> records,
        final ConstraintValidatorContext constraintValidatorContext) {

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      Set<Integer> newNodeIdSet =
          records.stream().map(i -> i.newNodeId).collect(Collectors.toSet());

      for (Row row : session.execute(select))
        if (newNodeIdSet.contains(row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID)))
          return false;

      return true;
    }
  }
}
