package pathstoreweb.pathstoreadminpanel;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.jcraft.jsch.JSchException;
import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.*;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstore.system.deployment.utilities.SSHUtil;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.WAIT_FOR;
import static pathstore.common.Constants.PATHSTORE_COLUMNS.*;
import static pathstore.common.Constants.PATHSTORE_COLUMNS.PATHSTORE_DIRTY;
import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

/**
 * This class is used to handle the startup sequence and allows a user to deploy the root node for
 * their new pathstore network
 */
public class StartUpHandler {

  private static final int sshPort = 22;
  private static final int cassandraPort = 9052;
  private static final int rmiPort = 1099;

  /** Where the properties file will be stored locally. */
  private static final String DESTINATION_TO_STORE =
      "../docker-files/pathstore/pathstore.properties";

  /** Scanner to get user input */
  private final Scanner scanner;

  /** Setup scanner */
  public StartUpHandler() {
    this.scanner = new Scanner(System.in);
  }

  /**
   * Ask the user if they want to start a new network. If they do run them through the disclaimer
   * else start the spring application
   */
  public void init() {
    String response =
        this.askQuestionWithSpecificResponses(
            "Do you want to create a new PathStore network?: ",
            new String[] {"y", "yes", "n", "no"});

    switch (response) {
      case "y":
      case "yes":
        this.disclaimerPrompt();
        break;
      case "n":
      case "no":
        break;
    }
    this.finished();
  }

  /**
   * This function is used to display a disclaimer prompt to the user. If they don't accept the
   * disclaimer then the program is exited, else they're transitioned to the next stage of
   * deployment
   */
  public void disclaimerPrompt() {
    System.out.println(
        "\n=========================================== [DISCLAIMER] ===========================================\n"
            + "1) All servers that you want to add to the network must have a linux distribution installed\n"
            + "2) Your linux machine must be publicly accessible (You can ssh into the machine remotely on port 22)\n"
            + "3) You must have port 9052 and 1099 open as-well for pathstore communication\n"
            + "4) The user account given should be a separate user account from any other on the system (not root)\n"
            + "5) You must have docker installed and the docker daemon running\n"
            + "6) The user account given must be part of the docker group and shouldn't be a member of sudo\n"
            + "7) The system must have at least 4gb of ram (2gb for cassandra and 2gb for pathstore)\n"
            + "\nTo view our setup guide for how to configure a linux system to run pathstore see our github page\n");
    String response =
        this.askQuestionWithSpecificResponses(
            "Do you want to continue after reading the disclaimer?: ",
            new String[] {"y", "yes", "n", "no"});
    switch (response) {
      case "y":
      case "yes":
        this.createNewNetwork();
        break;
      case "n":
      case "no":
        break;
    }
  }

  /**
   * First get information about the network and then connect to the server. Then run through the
   * list of commands
   *
   * @see #initList(SSHUtil, String, String, int, int, Role, String, int, String, int, String, int,
   *     String, int)
   */
  public void createNewNetwork() {

    System.out.println(
        "\nConnection information\nNote: If the host you're connecting to already has a pathstore instance running it will be deleted.\n");

    String ip = this.askQuestionWithSpecificResponses("Host: ", null);
    String username = this.askQuestionWithInvalidResponse("Username: ", new String[] {"root"});
    String password = this.askQuestionWithInvalidResponse("Password: ", null);
    String branch = this.askQuestionWithInvalidResponse("Branch: ", null);

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, sshPort);
      System.out.println("Connected");

      try {
        for (ICommand command :
            this.initList(
                sshUtil,
                ip,
                branch,
                1,
                -1,
                Role.ROOTSERVER,
                "127.0.0.1",
                rmiPort,
                "",
                rmiPort,
                "127.0.0.1",
                cassandraPort,
                "",
                cassandraPort)) {

          System.out.println(command);
          command.execute();
        }
        this.finalizeRootNodeInstallation(ip, cassandraPort, username, password);

        this.generatePathStorePropertiesFile(ip, cassandraPort, rmiPort);
      } catch (CommandError error) {
        System.err.println(String.format("[ERROR] %s", error.errorMessage));
        System.exit(-1);
      } finally {
        sshUtil.disconnect();
      }

    } catch (JSchException e) {
      System.out.println("\nYour connection information seems to be incorrect");
      this.createNewNetwork();
    }
  }

  /**
   * @param sshUtil used for commands that need to use ssh
   * @param ip ip of new node
   * @param branch branch from github to build from
   * @param nodeID new node's id
   * @param parentNodeId new node's parent id
   * @param role role of new node
   * @param rmiRegistryIP new node's local rmi registry ip
   * @param rmiRegistryPort new node's local rmi registry port
   * @param rmiRegistryParentIP new node's parent rmi registry ip
   * @param rmiRegistryParentPort new node's parent rmi registry port
   * @param cassandraIP new node's local cassandra instance ip
   * @param cassandraPort new node's local cassandra instance port
   * @param cassandraParentIP new node's parent cassandra instance ip
   * @param cassandraParentPort new nodes' parent cassandra instance port
   * @return list of deployment commands to execute
   */
  public List<ICommand> initList(
      final SSHUtil sshUtil,
      final String ip,
      final String branch,
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

    List<ICommand> commands = new ArrayList<>();

    // Check for docker access and that docker is online
    commands.add(new Exec(sshUtil, "docker ps", 0));
    // Potentially kill old cassandra container
    commands.add(new Exec(sshUtil, "docker kill cassandra", -1));
    // Potentially remove old cassandra image
    commands.add(new Exec(sshUtil, "docker image rm cassandra", -1));
    // Potentially kill old pathstore container
    commands.add(new Exec(sshUtil, "docker kill pathstore", -1));
    // Potentially remove old pathstore image
    commands.add(new Exec(sshUtil, "docker image rm pathstore", -1));
    // Potentially remove old file associated with install
    commands.add(new Exec(sshUtil, "rm -rf pathstore-install", -1));
    // Potentially remove old base image
    commands.add(new Exec(sshUtil, "docker image rm base", -1));
    // Create pathstore install dir
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install", 0));
    // Create base dir
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/base", 0));
    // Create cassandra dir
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/cassandra", 0));
    // Create pathstore dir
    commands.add(new Exec(sshUtil, "mkdir -p pathstore-install/pathstore", 0));
    // Generate pathstore properties file
    commands.add(
        new GeneratePropertiesFile(
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
            DESTINATION_TO_STORE));
    // Transfer properties file
    commands.add(
        new FileTransfer(
            sshUtil, DESTINATION_TO_STORE, "pathstore-install/pathstore/pathstore.properties"));
    // Remove properties file
    commands.add(new RemoveGeneratedPropertiesFile(DESTINATION_TO_STORE));
    // Transfer deploy key
    commands.add(
        new FileTransfer(sshUtil, "../docker-files/deploy_key", "pathstore-install/deploy_key"));
    // Transfer base docker file
    commands.add(
        new FileTransfer(
            sshUtil, "../docker-files/base/Dockerfile", "pathstore-install/base/Dockerfile"));
    // Transfer cassandra docker file
    commands.add(
        new FileTransfer(
            sshUtil,
            "../docker-files/cassandra/Dockerfile",
            "pathstore-install/cassandra/Dockerfile"));
    // Transfer pathstore docker file
    commands.add(
        new FileTransfer(
            sshUtil,
            "../docker-files/pathstore/Dockerfile",
            "pathstore-install/pathstore/Dockerfile"));
    // Build base
    commands.add(
        new Exec(
            sshUtil,
            String.format(
                "docker build -t base --build-arg key=\"$(cat pathstore-install/deploy_key)\" --build-arg branch=\"%s\" pathstore-install/base",
                branch),
            0));
    // Build cassandra
    commands.add(new Exec(sshUtil, "docker build -t cassandra pathstore-install/cassandra", 0));
    // Save cassandra to tar file and store in pathstore directory
    commands.add(
        new Exec(sshUtil, "docker save -o pathstore-install/pathstore/cassandra.tar cassandra", 0));
    // Start cassandra
    commands.add(
        new Exec(sshUtil, "docker run --network=host -dit --rm --name cassandra cassandra", 0));
    // Wait for cassandra to start
    commands.add(new WaitForCassandra(ip, cassandraPort));
    // Build pathstore
    commands.add(new Exec(sshUtil, "docker build -t pathstore pathstore-install/pathstore", 0));
    // Save pathstore to tar file and store in pathstore directory
    commands.add(
        new Exec(sshUtil, "docker save -o pathstore-install/pathstore/pathstore.tar pathstore", 0));
    // Start pathstore
    commands.add(
        new Exec(
            sshUtil,
            "docker run --network=host -dit --rm -v ~/pathstore-install/pathstore:/etc/pathstore --name pathstore pathstore",
            0));
    // Wait for pathstore to come online
    commands.add(new WaitForPathStore(ip, cassandraPort));

    return commands;
  }

  /**
   * This function is used to write the pathstore.properties file for the webserver based on the new
   * root node that was created.
   *
   * <p>Note: Unless the user who is running this application has manually created the
   * /etc/pathstore directory and re-assigned the ownership of the directory to them they will have
   * to manually copy and paste the generated properties file to the correct location
   *
   * @param ip ip of root node
   * @param cassandraPort port of root node cassandra instance
   * @param RMIPort port of root node RMI connection
   */
  private void generatePathStorePropertiesFile(
      final String ip, final int cassandraPort, final int RMIPort) {
    Properties properties = new Properties();

    properties.setProperty(ROLE, Role.CLIENT.toString());
    properties.setProperty(CASSANDRA_IP, ip);
    properties.setProperty(CASSANDRA_PORT, String.valueOf(cassandraPort));
    properties.setProperty(RMI_REGISTRY_IP, ip);
    properties.setProperty(RMI_REGISTRY_PORT, String.valueOf(RMIPort));

    StringBuilder response = new StringBuilder();
    properties.forEach((k, v) -> response.append(k).append(": ").append(v).append("\n"));

    try {
      OutputStream outputStream = new FileOutputStream(Constants.PROPERTIESFILE);
      properties.store(outputStream, null);
    } catch (IOException e) {
      System.out.println(
          "Could not write to "
              + Constants.PROPERTIESFILE
              + " you need to manually add the following data");
      System.out.println(response.toString());

      this.askQuestionWithSpecificResponses(
          "Have you added the data manually?: ", new String[] {"y", "yes"});
    }
  }

  /**
   * This function takes in a question you want to prompt the user with and a list of
   * validResponses.
   *
   * <p>The user gets prompted with the question, then we take their response and lower case it. If
   * their response is inside the validResponses set then we return their response. If it's not we
   * notify the user that their response must be within the validResponses set and we then make a
   * recursive call to re-prompt the user
   *
   * @param question question to ask user
   * @param validResponses list of accepted answers null then we will always return their response
   * @return response from user
   */
  private String askQuestionWithSpecificResponses(
      final String question, final String[] validResponses) {
    System.out.print(question);

    String response = this.scanner.nextLine().toLowerCase();

    HashSet<String> validResponseSet =
        validResponses != null ? new HashSet<>(Arrays.asList(validResponses)) : new HashSet<>();

    if (validResponseSet.contains(response) || validResponseSet.size() == 0) return response;
    else {
      System.out.println(
          "You're response must be one of the following values: "
              + Arrays.toString(validResponses));
      return this.askQuestionWithSpecificResponses(question, validResponses);
    }
  }

  /**
   * This function is used to ask a question and accept any response accept those in the
   * invalidResponses array
   *
   * @param question question to ask
   * @param invalidResponses responses that aren't accepted
   * @return answer to question
   */
  private String askQuestionWithInvalidResponse(
      final String question, final String[] invalidResponses) {
    System.out.print(question);

    String response = this.scanner.nextLine();
    HashSet<String> inValidResponseSet =
        invalidResponses != null ? new HashSet<>(Arrays.asList(invalidResponses)) : new HashSet<>();
    if (inValidResponseSet.contains(response)) {
      System.out.print(
          "You cannot respond with the following values: " + Arrays.toString(invalidResponses));
      return this.askQuestionWithInvalidResponse(question, invalidResponses);
    } else return response;
  }

  /**
   * This function rights the recorded to the server table to disallow multiple deployments to the
   * same node and drops startup keyspace once finished
   *
   * @param ip ip address of root node
   * @param cassandraPort cassandra port
   * @param username username to connect to root node
   * @param password password for root node
   */
  private void finalizeRootNodeInstallation(
      final String ip, final int cassandraPort, final String username, final String password) {

    System.out.println("Writing server record to root's table");

    Cluster cluster = StartupUTIL.createCluster(ip, cassandraPort);
    Session session = cluster.connect();

    UUID serverUUID = UUID.randomUUID();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(PATHSTORE_VERSION, QueryBuilder.now())
            .value(PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now())
            .value(PATHSTORE_DIRTY, true)
            .value(SERVER_UUID, serverUUID.toString())
            .value(IP, ip)
            .value(USERNAME, username)
            .value(PASSWORD, password)
            .value(NAME, "Root Node");

    session.execute(insert);

    insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
            .value(PATHSTORE_VERSION, QueryBuilder.now())
            .value(PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now())
            .value(PATHSTORE_DIRTY, true)
            .value(NEW_NODE_ID, 1)
            .value(PARENT_NODE_ID, -1)
            .value(PROCESS_STATUS, DeploymentProcessStatus.DEPLOYED.toString())
            .value(WAIT_FOR, -1)
            .value(SERVER_UUID, serverUUID.toString());

    session.execute(insert);

    session.close();
    cluster.close();
  }

  /** close scanner */
  private void finished() {
    this.scanner.close();
  }
}
