package pathstorestartup.commands;

import pathstore.common.Role;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;

/** This command is used to generate a local copy of the website properties file */
public class CreateWebsitePropertiesFile implements ICommand {

  /** Ip of pathstore root node */
  private final String ip;

  /** cassandra port for pathstore root node */
  private final int cassandraPort;

  /** rmi to connect to pathstore root node */
  private final int rmiPort;

  /** Destination to store file locally */
  private final String destinationToWrite;

  /**
   * @param ip {@link #ip}
   * @param cassandraPort {@link #cassandraPort}
   * @param rmiPort {@link #rmiPort}
   * @param destinationToWrite {@link #destinationToWrite}
   */
  public CreateWebsitePropertiesFile(
      final String ip,
      final int cassandraPort,
      final int rmiPort,
      final String destinationToWrite) {
    this.ip = ip;
    this.cassandraPort = cassandraPort;
    this.rmiPort = rmiPort;
    this.destinationToWrite = destinationToWrite;
  }

  /**
   * This function will generate the properties file based on the information provided by the user
   * for the website
   *
   * @throws CommandError iff the properties file can not be written
   */
  @Override
  public void execute() throws CommandError {
    Properties properties = new Properties();

    properties.setProperty(ROLE, Role.CLIENT.toString());
    properties.setProperty(CASSANDRA_IP, ip);
    properties.setProperty(CASSANDRA_PORT, String.valueOf(cassandraPort));
    properties.setProperty(RMI_REGISTRY_IP, ip);
    properties.setProperty(RMI_REGISTRY_PORT, String.valueOf(rmiPort));

    try {
      OutputStream outputStream = new FileOutputStream(this.destinationToWrite);
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CommandError(
          String.format(
              "Could not write the website configuration file to %s", this.destinationToWrite));
    }
  }

  /** @return used to display to user what is going on */
  @Override
  public String toString() {
    return "Creating website properties file";
  }
}
