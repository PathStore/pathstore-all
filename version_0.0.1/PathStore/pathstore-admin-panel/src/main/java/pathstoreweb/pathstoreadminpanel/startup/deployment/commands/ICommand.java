package pathstoreweb.pathstoreadminpanel.startup.deployment.commands;

/** Interface used to denote a step in the installation process of pathstore */
public interface ICommand {

  /**
   * Function to call to execute command
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  void execute() throws CommandError;
}
