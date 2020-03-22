package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator.NodesExist.Validator;

/** Validator to check to see if all nodes passed are valid node id's */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface NodesExist {

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
  class Validator implements ConstraintValidator<NodesExist, Set<Integer>> {

    /**
     * @param nodes list of nodes passed by user
     * @param constraintValidatorContext todo
     * @return true if all nodes exist else false (nodes could also be empty)
     */
    @Override
    public boolean isValid(
        final Set<Integer> nodes, final ConstraintValidatorContext constraintValidatorContext) {

      if (nodes == null || nodes.size() == 0) return false;

      Session session = PathStoreCluster.getInstance().connect();

      Select queryTopology =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

      Set<Integer> nodeExists = new HashSet<>();

      for (Row row : session.execute(queryTopology)) {
        int currentNode = row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID);
        if (nodes.contains(currentNode)) nodeExists.add(currentNode);
      }

      return nodeExists.size() == nodes.size();
    }
  }
}
