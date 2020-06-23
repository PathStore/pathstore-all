package pathstorestartup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

/** This utility class is used to store functions to handle input from the user */
public class Utils {
  /**
   * This function takes in a question you want to prompt the user with and a list of
   * validResponses.
   *
   * <p>The user gets prompted with the question, then we take their response and lower case it. If
   * their response is inside the validResponses set then we return their response. If it's not we
   * notify the user that their response must be within the validResponses set and we then make a
   * recursive call to re-prompt the user
   *
   * @param scanner scanner to read from
   * @param question question to ask user
   * @param validResponses list of accepted answers null then we will always return their response
   * @return response from user
   */
  public static String askQuestionWithSpecificResponses(
      final Scanner scanner, final String question, final String[] validResponses) {
    System.out.print(question);

    String response = scanner.nextLine().toLowerCase();

    HashSet<String> validResponseSet =
        validResponses != null ? new HashSet<>(Arrays.asList(validResponses)) : new HashSet<>();

    if (validResponseSet.contains(response) || validResponseSet.size() == 0) return response;
    else {
      System.out.println(
          "You're response must be one of the following values: "
              + Arrays.toString(validResponses));
      return askQuestionWithSpecificResponses(scanner, question, validResponses);
    }
  }

  /**
   * This function is used to ask a question and accept any response accept those in the
   * invalidResponses array
   *
   * @param scanner scanner to read from
   * @param question question to ask
   * @param invalidResponses responses that aren't accepted
   * @return answer to question
   */
  public static String askQuestionWithInvalidResponse(
      final Scanner scanner, final String question, final String[] invalidResponses) {
    System.out.print(question);

    String response = scanner.nextLine();
    HashSet<String> inValidResponseSet =
        invalidResponses != null ? new HashSet<>(Arrays.asList(invalidResponses)) : new HashSet<>();
    if (inValidResponseSet.contains(response)) {
      System.out.print(
          "You cannot respond with the following values: " + Arrays.toString(invalidResponses));
      return askQuestionWithInvalidResponse(scanner, question, invalidResponses);
    } else return response;
  }

  /**
   * Ask a question to the user but get an integer response
   *
   * @param scanner scanner to read from
   * @param question question to ask
   * @param invalidResponses list of invalid responses
   * @return integer from user
   */
  public static int askQuestionWithInvalidResponseInteger(
      final Scanner scanner, final String question, final String[] invalidResponses) {
    int response;

    try {
      response =
          Integer.parseInt(askQuestionWithInvalidResponse(scanner, question, invalidResponses));
    } catch (NumberFormatException e) {
      System.out.println(
          "The data you entered is not a number. Please make sure you enter a number");
      return askQuestionWithInvalidResponseInteger(scanner, question, invalidResponses);
    }

    return response;
  }
}
