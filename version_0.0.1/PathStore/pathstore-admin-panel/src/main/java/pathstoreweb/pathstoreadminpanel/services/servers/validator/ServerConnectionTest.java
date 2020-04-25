package pathstoreweb.pathstoreadminpanel.services.servers.validator;

import com.jcraft.jsch.JSchException;
import pathstoreweb.pathstoreadminpanel.services.servers.Server;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerConnectionTest.Validator;
import pathstoreweb.pathstoreadminpanel.startup.deployment.utilities.SSHUtil;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This validator is used to check if the credentials given are valid to connect to a server */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface ServerConnectionTest {

  /** @return message to display to user if an error occurs */
  String message() default "error";

  /** @return todo */
  Class<?>[] groups() default {};

  /** @return todo */
  Class<? extends Payload>[] payload() default {};

  /**
   * This validator checks to make sure the information provided is the destination of a valid
   * server
   */
  class Validator implements ConstraintValidator<ServerConnectionTest, Server> {

    /**
     * @param server server object created from passed data
     * @param constraintValidatorContext context
     * @return false if can't connect true otherwise
     */
    @Override
    public boolean isValid(
        final Server server, final ConstraintValidatorContext constraintValidatorContext) {

      try {
        new SSHUtil(server.ip, server.username, server.password, 22);
      } catch (JSchException e) {
        return false;
      }
      return true;
    }
  }
}
