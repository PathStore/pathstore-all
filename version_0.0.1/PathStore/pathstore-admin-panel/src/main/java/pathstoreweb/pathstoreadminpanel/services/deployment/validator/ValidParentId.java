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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to check that all parent id's exist in either the current topology or your newly planned
 * topology
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidParentId.Validator.class)
public @interface ValidParentId {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /** Validator */
  class Validator
      implements ConstraintValidator<
          ValidParentId, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    /**
     * @param deploymentRecordList supplied record set
     * @param constraintValidatorContext todo
     * @return true iff all parent id's exist in existing topology or in your newly planned
     *     topology, else false
     */
    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      Set<Integer> parentNodeIdSet =
          deploymentRecordList.stream().map(i -> i.parentId).collect(Collectors.toSet());

      Set<Integer> newNodeIdSet =
          deploymentRecordList.stream().map(i -> i.newNodeId).collect(Collectors.toSet());

      Set<Integer> alreadyInstalledNodeSet = new HashSet<>();

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      for (Row row : session.execute(select))
        alreadyInstalledNodeSet.add(row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID));

      for (Integer parentNodeId : parentNodeIdSet)
        if (!(alreadyInstalledNodeSet.contains(parentNodeId)
            || newNodeIdSet.contains(parentNodeId))) return false;

      return true;
    }
  }
}
