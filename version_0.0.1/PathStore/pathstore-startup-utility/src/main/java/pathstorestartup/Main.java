package pathstorestartup;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);

    if (args.length == 1 && args[0].equals("-d")) new DevelopmentDeployment(scanner).init();
    else new ProductionDeployment(scanner).init();

    scanner.close();
  }
}
