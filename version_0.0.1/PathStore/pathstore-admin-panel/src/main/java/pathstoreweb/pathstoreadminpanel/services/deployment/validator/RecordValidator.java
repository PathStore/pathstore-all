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

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecordValidator.Validator.class)
public @interface RecordValidator {

  String message() default "error";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator
      implements ConstraintValidator<
          RecordValidator, List<AddDeploymentRecordPayload.DeploymentRecord>> {

    @Override
    public boolean isValid(
        final List<AddDeploymentRecordPayload.DeploymentRecord> deploymentRecordList,
        final ConstraintValidatorContext constraintValidatorContext) {

      // Check for empty
      if (deploymentRecordList == null || deploymentRecordList.size() == 0) return false;

      Set<String> serverUUIDSet =
          deploymentRecordList.stream().map(i -> i.serverUUID).collect(Collectors.toSet());

      // Check for server UUID duplicates
      if (serverUUIDSet.size() != deploymentRecordList.size()) return false;

      Set<Integer> newNodeIdSet =
          deploymentRecordList.stream().map(i -> i.newNodeId).collect(Collectors.toSet());

      // Check for new Node Id duplicates
      if (newNodeIdSet.size() != deploymentRecordList.size()) return false;

      Set<Integer> parentNodeIdSet =
          deploymentRecordList.stream().map(i -> i.parentId).collect(Collectors.toSet());

      Session session = PathStoreCluster.getInstance().connect();

      Select select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      Set<Integer> alreadyInstalledNewNodeIdSet = new HashSet<>();

      // Check for Uniqueness of new Node id and for uniqueness of serverUUID
      for (Row row : session.execute(select)) {
        int current = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);
        alreadyInstalledNewNodeIdSet.add(current);
        String serverUUID = row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID);
        if (newNodeIdSet.contains(current) || serverUUIDSet.contains(serverUUID)) return false;
      }

      // Check for valid parent Node id's either A) they're already installed nodes or B) they're
      // part of new tree
      for (Integer parentNodeId : parentNodeIdSet)
        if (!(alreadyInstalledNewNodeIdSet.contains(parentNodeId)
            || newNodeIdSet.contains(parentNodeId))) return false;

      // Check to see that no node's parent Id matches newNodeId
      for (AddDeploymentRecordPayload.DeploymentRecord deploymentRecord : deploymentRecordList)
        if (deploymentRecord.newNodeId == deploymentRecord.parentId) return false;

      select =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

      //  Remove all serverUUID's used in the new list
      for (Row row : session.execute(select))
        serverUUIDSet.remove(row.getString(Constants.SERVERS_COLUMNS.SERVER_UUID));

      // If the server UUID set is larger then 0 we have an invalid server UUID
      return serverUUIDSet.size() <= 0;
    }
  }
}
