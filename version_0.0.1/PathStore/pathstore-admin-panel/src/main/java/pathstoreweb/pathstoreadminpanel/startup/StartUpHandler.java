package pathstoreweb.pathstoreadminpanel.startup;

import com.jcraft.jsch.JSchException;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.startup.commands.Exec;
import pathstoreweb.pathstoreadminpanel.startup.commands.FileTransfer;
import pathstoreweb.pathstoreadminpanel.startup.commands.ICommand;
import pathstoreweb.pathstoreadminpanel.startup.commands.WaitForCassandra;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.ExecutionException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.FileTransferException;
import pathstoreweb.pathstoreadminpanel.startup.commands.errors.InternalException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * TODO: Wait for pathstore to come online
 *
 * <p>TODO: One time use passwords
 */
public class StartUpHandler {

  /** List of commands to execute inorder to install pathstore and its associated pre-requisites */
  private static final List<ICommand> commands = new ArrayList<>();

  /** Initialize the list */
  static {
    commands.add(new Exec("docker ps", 0));
    commands.add(new Exec("docker kill cassandra", -1));
    commands.add(new Exec("docker kill pathstore", -1));
    commands.add(new Exec("rm -rf pathstore-install", -1));
    commands.add(new Exec("mkdir -p pathstore-install", 0));
    commands.add(new Exec("mkdir -p pathstore-install/cassandra", 0));
    commands.add(new Exec("mkdir -p pathstore-install/pathstore", 0));
    commands.add(
        new FileTransfer(
            "../docker-files/pathstore/pathstore.properties",
            "pathstore-install/pathstore/pathstore.properties"));
    commands.add(new FileTransfer("../docker-files/deploy_key", "pathstore-install/deploy_key"));
    commands.add(
        new FileTransfer(
            "../docker-files/cassandra/Dockerfile", "pathstore-install/cassandra/Dockerfile"));
    commands.add(
        new FileTransfer(
            "../docker-files/pathstore/Dockerfile", "pathstore-install/pathstore/Dockerfile"));
    commands.add(
        new Exec(
            "docker build -t cassandra --build-arg key=\"$(cat pathstore-install/deploy_key)\" --build-arg branch=\"pathstore_init_script\" pathstore-install/cassandra",
            0));
    commands.add(new Exec("docker run --network=host -dit --rm --name cassandra cassandra", 0));
    commands.add(new WaitForCassandra());
    commands.add(
        new Exec(
            "docker build -t pathstore --build-arg key=\"$(cat pathstore-install/deploy_key)\" --build-arg branch=\"pathstore_init_script\" pathstore-install/pathstore",
            0));
    commands.add(new Exec("docker run --network=host -dit --rm --name pathstore pathstore", 0));
  }

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
   * list of commands {@link #commands}
   */
  public void createNewNetwork() {

    System.out.println(
        "\nConnection information\nNote: If the host you're connecting to already has a pathstore instance running it will be deleted.\n");

    String ip = this.askQuestionWithSpecificResponses("Host: ", null);
    String username = this.askQuestionWithInvalidResponse("Username: ", new String[] {"root"});
    String password = this.askQuestionWithInvalidResponse("Password: ", null);

    try {
      SSHUtil sshUtil = new SSHUtil(ip, username, password, 22);
      System.out.println("Connected");

      for (ICommand command : commands) {
        try {
          System.out.println(command);
          command.execute(sshUtil);
        } catch (ExecutionException e) {
          System.out.println("There was an error executing: " + ((Exec) command).command);
        } catch (FileTransferException e) {
          FileTransfer fileTransfer = (FileTransfer) command;
          System.out.println(
              "There was an error transferring "
                  + fileTransfer.relativeLocalPath
                  + " to "
                  + fileTransfer.relativeRemotePath);
        } catch (InternalException e) {
          System.out.println("There was an internal error. The stack trace is below");
          e.printStackTrace();
          System.exit(1);
        }
      }

      this.generatePathStorePropertiesFile(ip, 9052, 1099);

      sshUtil.disconnect();
    } catch (JSchException e) {
      System.out.println("\nYour connection information seems to be incorrect");
      this.createNewNetwork();
    }
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
    properties.setProperty("NodeId", String.valueOf(1));
    properties.setProperty("Role", "ROOTSERVER");
    properties.setProperty("CassandraIP", ip);
    properties.setProperty("CassandraPort", String.valueOf(cassandraPort));
    properties.setProperty("RMIRegistry", ip);
    properties.setProperty("RMIRegistryPort", String.valueOf(RMIPort));

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

  /** close scanner */
  private void finished() {
    this.scanner.close();
  }
}
