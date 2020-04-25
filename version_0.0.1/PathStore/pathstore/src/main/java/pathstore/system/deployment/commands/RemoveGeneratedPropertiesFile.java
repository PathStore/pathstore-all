package pathstore.system.deployment.commands;

import pathstore.system.deployment.utilities.StartupUTIL;

import java.io.File;

/**
 * This command is used to delete the generated properties file for the new node after transferring
 * the file.
 */
public class RemoveGeneratedPropertiesFile implements ICommand {

  /** Where the generate properties file is stored */
  private final String pathToFile;

  /** @param pathToFile {@link #pathToFile} */
  public RemoveGeneratedPropertiesFile(final String pathToFile) {
    this.pathToFile = pathToFile;
  }

  /**
   * Load file from path into memory and delete it.
   *
   * @throws CommandError if file could not be deleted or if the relative path is invalid
   */
  @Override
  public void execute() throws CommandError {

    File file = new File(StartupUTIL.getAbsolutePathFromRelativePath(this.pathToFile));

    if (file.delete()) System.out.println("File delete");
    else throw new CommandError(String.format("Could not delete file %s", this.pathToFile));
  }

  /** @return inform user that the file is being deleted */
  @Override
  public String toString() {
    return String.format(
        "Attempting to delete generated properties file in location %s", this.pathToFile);
  }
}
