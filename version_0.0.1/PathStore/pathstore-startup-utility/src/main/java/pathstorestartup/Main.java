package pathstorestartup;

import pathstore.common.Constants;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {

    System.out.println(Constants.ASCII_ART);

    Scanner scanner = new Scanner(System.in);

    new DevelopmentDeployment(scanner).init();

    scanner.close();
  }
}
