package pathstore.system.deployment.utilities;

import com.opencsv.CSVReader;
import pathstore.common.Role;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.deployment.commands.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to convert a deployment csv file into a list of {@link
 * pathstore.system.deployment.commands.ICommand} to be used for node deployment. Either by the
 * website or any pathstore node
 */
public class DeploymentFileParser {

  /**
   * This is an internal class to denote the different types of parsable commands that are offered
   */
  private static final class COMMAND_TYPES {

    /**
     * Exec command. Param 1 should be the command to execute. Param 2 should be the desired integer
     * response code
     *
     * @see Exec
     */
    public static final String EXEC = "exec";

    /**
     * File transfer command. Param 1 should be the relative local path or {@link
     * #DESTINATION_TO_STORE} to denote the properties file. Param 2 should be the location of the
     * external directory
     *
     * @see FileTransfer
     */
    public static final String FILE_TRANSFER = "file_transfer";

    /**
     * Denotes the step in deployment where you want to generate the properties file for the new
     * node. It does not take any other parameters except for the name
     *
     * @see GeneratePropertiesFile
     */
    public static final String GENERATE_PROPERTIES_FILE = "generate_properties_file";

    /**
     * Denotes the step in deployment where you want to delete the local copy of the properties
     * file. It will delete the file stored in {@link #destinationToStore}
     *
     * @see RemoveGeneratedPropertiesFile
     */
    public static final String REMOVE_GENERATED_PROPERTIES_FILE =
        "remove_generated_properties_file";

    /**
     * Denotes the step in deployment where you want to wait for cassandra to start up. This step
     * should always come after cassandra has been ran.
     *
     * @see WaitForCassandra
     */
    public static final String WAIT_FOR_CASSANDRA = "wait_for_cassandra";

    /**
     * Denotes the step in deployment where you want to wait for pathstore to start up. This step
     * should always come after pathstore has been ran
     *
     * @see WaitForPathStore
     */
    public static final String WAIT_FOR_PATHSTORE = "wait_for_pathstore";
  }

  /**
   * Denotes the single exception where the user can use a keyword instead of literal command. This
   * denotes that they want to transfer the properties file to a dir in the remote host
   */
  private static final String DESTINATION_TO_STORE = "destination_to_store";

  /** Where the properties file will be stored locally. */
  private static final String destinationToStore = "../docker-files/pathstore/pathstore.properties";

  /** Logger for this class to report any errors that occur during deployment */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(DeploymentFileParser.class);

  /** Where is the deployment csv file you want to load */
  private final String csvFileLocation;

  /** Parsed data from csv */
  private List<String[]> csvData = null;

  /** @param csvFileLocation location of csvfile to load */
  public DeploymentFileParser(final String csvFileLocation) {
    this.csvFileLocation = csvFileLocation;
  }

  /**
   * This function must be called before calling {@link #parseToICommands(SSHUtil, String, String,
   * int, int, Role, String, int, String, int, String, int, String, int)}
   *
   * @return true iff the data was a valid csv file and was able to be parsed (No guarantees that
   *     the csv file has valid contents)
   */
  public boolean readData() {
    try {
      FileReader fileReader = new FileReader(this.csvFileLocation);

      CSVReader csvReader = new CSVReader(fileReader);

      this.csvData = csvReader.readAll();
    } catch (IOException ignored) {
      logger.error(
          String.format(
              "Could not parse the Deployment CSV file located at: %s\n", this.csvFileLocation));
      return false;
    }

    return true;
  }

  /**
   * This function will parse the given csv file data into a list of commands to run
   *
   * @param sshUtil connection to remote host
   * @param branch what branch to load from
   * @param ip external ip of node
   * @param nodeID node id
   * @param parentNodeId parent node id
   * @param role what role the new node is
   * @param rmiRegistryIP rmi ip (normally localhost)
   * @param rmiRegistryPort rmi port (normally 1099)
   * @param rmiRegistryParentIP rmi parent ip
   * @param rmiRegistryParentPort rmi parent port (normally 1099)
   * @param cassandraIP cassandra ip localhost for now.
   * @param cassandraPort cassandra port (normally 9052)
   * @param cassandraParentIP cassandra parent ip
   * @param cassandraParentPort cassandra parent port (normally 9052)
   * @return list of commands parsed from csv file iff they follow the proper format
   * @throws Exception if any command was not able to be parsed
   */
  public List<ICommand> parseToICommands(
      final SSHUtil sshUtil,
      final String branch,
      final String ip,
      final int nodeID,
      final int parentNodeId,
      final Role role,
      final String rmiRegistryIP,
      final int rmiRegistryPort,
      final String rmiRegistryParentIP,
      final int rmiRegistryParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort)
      throws Exception {

    if (this.csvData == null) throw new Exception();

    List<ICommand> commands = new LinkedList<>();

    for (String[] record : this.csvData)
      commands.add(
          this.parseICommand(
              record,
              branch,
              sshUtil,
              ip,
              nodeID,
              parentNodeId,
              role,
              rmiRegistryIP,
              rmiRegistryPort,
              rmiRegistryParentIP,
              rmiRegistryParentPort,
              cassandraIP,
              cassandraPort,
              cassandraParentIP,
              cassandraParentPort));

    return commands;
  }

  /**
   * This function will take a generic csv record and parse it into its corresponding ICommand if
   * valid
   *
   * @param data csv row record
   * @param branch branch to load (null if Role doesn't equal {@link Role#ROOTSERVER})
   * @param sshUtil connection to node
   * @param ip external ip of node
   * @param nodeID node id
   * @param parentNodeId parent node id
   * @param role role of new node
   * @param rmiRegistryIP rmi ip of new node (normally localhost)
   * @param rmiRegistryPort rmi port of new node (normally 1099)
   * @param rmiRegistryParentIP rmi parent ip
   * @param rmiRegistryParentPort rmi parent port (normally 1099)
   * @param cassandraIP cassandra ip (normally localhost)
   * @param cassandraPort cassandra port (normally 9052)
   * @param cassandraParentIP cassandra parent ip
   * @param cassandraParentPort cassandra parent port (normally 9052)
   * @return parsed command iff the command was valid
   * @throws Exception if the data given was not valid
   */
  private ICommand parseICommand(
      final String[] data,
      final String branch,
      final SSHUtil sshUtil,
      final String ip,
      final int nodeID,
      final int parentNodeId,
      final Role role,
      final String rmiRegistryIP,
      final int rmiRegistryPort,
      final String rmiRegistryParentIP,
      final int rmiRegistryParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort)
      throws Exception {

    switch (data[0]) {
      case COMMAND_TYPES.EXEC:
        return parseExec(sshUtil, data[1], Integer.parseInt(data[2]), branch);
      case COMMAND_TYPES.FILE_TRANSFER:
        return parseFileTransfer(sshUtil, data[1], data[2]);
      case COMMAND_TYPES.GENERATE_PROPERTIES_FILE:
        return parseGeneratePropertiesFile(
            ip,
            nodeID,
            parentNodeId,
            role,
            rmiRegistryIP,
            rmiRegistryPort,
            rmiRegistryParentIP,
            rmiRegistryParentPort,
            cassandraIP,
            cassandraPort,
            cassandraParentIP,
            cassandraParentPort);
      case COMMAND_TYPES.REMOVE_GENERATED_PROPERTIES_FILE:
        return parseRemoveGeneratedPropertiesFile();
      case COMMAND_TYPES.WAIT_FOR_CASSANDRA:
        return parseWaitForCassandra(ip, cassandraPort);
      case COMMAND_TYPES.WAIT_FOR_PATHSTORE:
        return parseWaitForPathStore(ip, cassandraPort);
      default:
        throw new Exception();
    }
  }

  /**
   * @param sshUtil connection to new node
   * @param command command to send
   * @param response desired response
   * @param branch branch to load (only used in one command instance)
   * @return parsed exec command
   * @apiNote The branch is there so you can have a generic build line for the base container for
   *     the root node. This is so that you can put %s and the branch will be inserted using {@link
   *     String#format(String, Object...)}
   */
  private Exec parseExec(
      final SSHUtil sshUtil, final String command, final int response, final String branch) {
    return new Exec(
        sshUtil, command.contains("branch") ? String.format(command, branch) : command, response);
  }

  /**
   * @param sshUtil connection to new node
   * @param relativeLocalPath relative local path
   * @param relativeRemotePath relative remove path
   * @return file transfer object
   * @apiNote relativeLocalPath can be {@link #DESTINATION_TO_STORE} to denote that you want to
   *     transfer the properties file to a remote destination
   */
  private FileTransfer parseFileTransfer(
      final SSHUtil sshUtil, final String relativeLocalPath, final String relativeRemotePath) {
    return new FileTransfer(
        sshUtil,
        relativeLocalPath.equals(DESTINATION_TO_STORE) ? destinationToStore : relativeLocalPath,
        relativeRemotePath);
  }

  /**
   * @param ip external ip of node
   * @param nodeID node id
   * @param parentNodeId parent node id
   * @param role role of new node
   * @param rmiRegistryIP rmi ip of new node (normally localhost)
   * @param rmiRegistryPort rmi port of new node (normally 1099)
   * @param rmiRegistryParentIP rmi parent ip
   * @param rmiRegistryParentPort rmi parent port (normally 1099)
   * @param cassandraIP cassandra ip (normally localhost)
   * @param cassandraPort cassandra port (normally 9052)
   * @param cassandraParentIP cassandra parent ip
   * @param cassandraParentPort cassandra parent port (normally 9052)
   * @return generate properties file command based on given info
   */
  private GeneratePropertiesFile parseGeneratePropertiesFile(
      final String ip,
      final int nodeID,
      final int parentNodeId,
      final Role role,
      final String rmiRegistryIP,
      final int rmiRegistryPort,
      final String rmiRegistryParentIP,
      final int rmiRegistryParentPort,
      final String cassandraIP,
      final int cassandraPort,
      final String cassandraParentIP,
      final int cassandraParentPort) {
    return new GeneratePropertiesFile(
        nodeID,
        ip,
        parentNodeId,
        role,
        rmiRegistryIP,
        rmiRegistryPort,
        rmiRegistryParentIP,
        rmiRegistryParentPort,
        cassandraIP,
        cassandraPort,
        cassandraParentIP,
        cassandraParentPort,
        DeploymentFileParser.destinationToStore);
  }

  /** @return remove generate properties file command based on {@link #destinationToStore} */
  private RemoveGeneratedPropertiesFile parseRemoveGeneratedPropertiesFile() {
    return new RemoveGeneratedPropertiesFile(DeploymentFileParser.destinationToStore);
  }

  /**
   * @param ip ip of the server to connect to
   * @param cassandraPort port for cassandra server
   * @return wait for cassandra command instance
   */
  private WaitForCassandra parseWaitForCassandra(final String ip, final int cassandraPort) {
    return new WaitForCassandra(ip, cassandraPort);
  }

  /**
   * @param ip ip of the server to connect to
   * @param cassandraPort port for cassandra server to read startup records from
   * @return wait for pathstore command instance
   */
  private WaitForPathStore parseWaitForPathStore(final String ip, final int cassandraPort) {
    return new WaitForPathStore(ip, cassandraPort);
  }
}
